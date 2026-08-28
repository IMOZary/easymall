package com.easymall.order;

import com.easymall.cart.CartItem;
import com.easymall.cart.CartItemRepository;
import com.easymall.catalog.Product;
import com.easymall.catalog.ProductRepository;
import com.easymall.catalog.ProductStatus;
import com.easymall.common.BusinessException;
import com.easymall.coupon.CouponService;
import com.easymall.user.User;
import com.easymall.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.easymall.order.OrderDtos.*;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final CouponService couponService;
    private final UserRepository userRepository;
    private final long paymentTimeoutMinutes;
    private final EntityManager entityManager;

    public OrderService(OrderRepository orderRepository, CartItemRepository cartItemRepository,
                        ProductRepository productRepository, CouponService couponService, UserRepository userRepository,
                        @Value("${easymall.order.payment-timeout-minutes:30}") long paymentTimeoutMinutes,
                        EntityManager entityManager) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.couponService = couponService;
        this.userRepository = userRepository;
        this.paymentTimeoutMinutes = paymentTimeoutMinutes;
        this.entityManager = entityManager;
    }

    @Transactional
    public OrderView checkout(User user, CheckoutRequest request) {
        // 锁定用户后再执行“查幂等键 -> 消费购物车 -> 创建订单”，封闭并发的先查后插竞态。
        User lockedUser = userRepository.findByIdForUpdate(user.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        var existing = orderRepository.findByUserIdAndIdempotencyKey(lockedUser.getId(), request.idempotencyKey());
        if (existing.isPresent()) return OrderView.from(existing.get());

        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByUpdatedAtDesc(lockedUser.getId());
        if (cartItems.isEmpty()) throw new BusinessException("购物车是空的");
        // 固定加锁顺序，减少多商品并发下单时出现数据库死锁的概率。
        cartItems = cartItems.stream().sorted(Comparator.comparing(item -> item.getProduct().getId())).toList();

        ShopOrder order = new ShopOrder();
        order.setOrderNo(generateOrderNo());
        order.setUser(lockedUser);
        order.setReceiver(request.receiver());
        order.setPhone(request.phone());
        order.setAddress(request.address());
        order.setRemark(request.remark());
        order.setIdempotencyKey(request.idempotencyKey());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setExpiresAt(LocalDateTime.now().plusMinutes(paymentTimeoutMinutes));

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            // 购物车会提前加载商品；加锁时同步刷新，避免并发事务使用旧 @Version 更新。
            entityManager.refresh(product, LockModeType.PESSIMISTIC_WRITE);
            if (product.getStatus() != ProductStatus.ON_SALE) throw new BusinessException(product.getName() + " 已下架");
            if (product.getStock() < cartItem.getQuantity())
                throw new BusinessException(product.getName() + " 库存不足，仅剩 " + product.getStock() + " 件");

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductIcon(product.getIcon());
            item.setProductPrice(product.getPrice());
            item.setQuantity(cartItem.getQuantity());
            item.setSubtotal(subtotal);
            order.addItem(item);
            total = total.add(subtotal);

            product.setStock(product.getStock() - cartItem.getQuantity());
            product.setSales(product.getSales() + cartItem.getQuantity());
        }

        BigDecimal discount = couponService.calculateAndUse(request.couponCode(), total);
        order.setTotalAmount(total);
        order.setDiscountAmount(discount);
        order.setPayAmount(total.subtract(discount));
        order.setCouponCode(request.couponCode() == null || request.couponCode().isBlank()
                ? null : request.couponCode().trim().toUpperCase());
        ShopOrder saved = orderRepository.save(order);
        cartItemRepository.deleteByUserId(lockedUser.getId());
        return OrderView.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<OrderView> myOrders(User user, int page, int size) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId(),
                PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 30))).map(OrderView::from);
    }

    @Transactional(readOnly = true)
    public OrderView detail(User user, Long id) {
        return OrderView.from(requireMine(user, id));
    }

    @Transactional
    public OrderView pay(User user, Long id) {
        ShopOrder order = requireMineForUpdate(user, id);
        requireStatus(order, OrderStatus.PENDING_PAYMENT, "只有待支付订单可以支付");
        if (order.getExpiresAt() != null && !order.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException("订单已超时，请刷新后重试");
        }
        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        return OrderView.from(order);
    }

    @Transactional
    public OrderView cancel(User user, Long id) {
        ShopOrder order = requireMineForUpdate(user, id);
        if (!order.getStatus().canCancel()) throw new BusinessException("当前订单状态不能取消");
        restoreStock(order);
        couponService.release(order.getCouponCode());
        order.setStatus(OrderStatus.CANCELED);
        return OrderView.from(order);
    }

    @Transactional
    public OrderView complete(User user, Long id) {
        ShopOrder order = requireMineForUpdate(user, id);
        requireStatus(order, OrderStatus.SHIPPED, "只有已发货订单可以确认收货");
        order.setStatus(OrderStatus.COMPLETED);
        return OrderView.from(order);
    }

    @Transactional
    public OrderView ship(Long id) {
        ShopOrder order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "订单不存在"));
        requireStatus(order, OrderStatus.PAID, "只有已支付订单可以发货");
        order.setStatus(OrderStatus.SHIPPED);
        order.setShippedAt(LocalDateTime.now());
        return OrderView.from(order);
    }

    @Transactional
    public int closeExpiredOrders(int batchSize) {
        List<ShopOrder> expiredOrders = orderRepository.findExpiredForUpdate(LocalDateTime.now(),
                PageRequest.of(0, Math.min(Math.max(batchSize, 1), 500)));
        for (ShopOrder order : expiredOrders) {
            restoreStock(order);
            couponService.release(order.getCouponCode());
            order.setStatus(OrderStatus.CANCELED);
        }
        return expiredOrders.size();
    }

    private ShopOrder requireMine(User user, Long id) {
        return orderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "订单不存在"));
    }

    private ShopOrder requireMineForUpdate(User user, Long id) {
        return orderRepository.findByIdAndUserIdForUpdate(id, user.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "订单不存在"));
    }

    private void requireStatus(ShopOrder order, OrderStatus expected, String message) {
        if (order.getStatus() != expected) throw new BusinessException(message);
    }

    private void restoreStock(ShopOrder order) {
        order.getItems().stream().sorted(Comparator.comparing(OrderItem::getProductId)).forEach(item -> {
            Product product = productRepository.findByIdForUpdate(item.getProductId())
                    .orElseThrow(() -> new BusinessException("商品不存在，无法回补库存"));
            product.setStock(product.getStock() + item.getQuantity());
            product.setSales(Math.max(0, product.getSales() - item.getQuantity()));
        });
    }

    private String generateOrderNo() {
        return "EM" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + ThreadLocalRandom.current().nextInt(100, 1000);
    }
}

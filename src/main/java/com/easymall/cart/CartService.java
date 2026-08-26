package com.easymall.cart;

import com.easymall.catalog.CatalogDtos.ProductView;
import com.easymall.catalog.Product;
import com.easymall.catalog.ProductRepository;
import com.easymall.catalog.ProductStatus;
import com.easymall.common.BusinessException;
import com.easymall.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.easymall.cart.CartDtos.*;

@Service
public class CartService {
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartService(CartItemRepository cartItemRepository, ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public CartView getCart(User user) {
        List<CartItemView> items = cartItemRepository.findByUserIdOrderByUpdatedAtDesc(user.getId()).stream()
                .map(this::toView).toList();
        int count = items.stream().mapToInt(CartItemView::quantity).sum();
        BigDecimal total = items.stream().map(CartItemView::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartView(items, count, total);
    }

    @Transactional
    public CartView add(User user, AddCartRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "商品不存在"));
        if (product.getStatus() != ProductStatus.ON_SALE) throw new BusinessException("商品已下架");
        int addQuantity = request.quantity() == null ? 1 : request.quantity();
        CartItem item = cartItemRepository.findByUserIdAndProductId(user.getId(), product.getId())
                .orElseGet(() -> {
                    CartItem created = new CartItem();
                    created.setUser(user);
                    created.setProduct(product);
                    created.setQuantity(0);
                    return created;
                });
        int target = item.getQuantity() + addQuantity;
        validateQuantity(product, target);
        item.setQuantity(target);
        cartItemRepository.save(item);
        return getCart(user);
    }

    @Transactional
    public CartView update(User user, Long itemId, int quantity) {
        CartItem item = cartItemRepository.findByIdAndUserId(itemId, user.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "购物车商品不存在"));
        validateQuantity(item.getProduct(), quantity);
        item.setQuantity(quantity);
        return getCart(user);
    }

    @Transactional
    public CartView remove(User user, Long itemId) {
        CartItem item = cartItemRepository.findByIdAndUserId(itemId, user.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "购物车商品不存在"));
        cartItemRepository.delete(item);
        return getCart(user);
    }

    private void validateQuantity(Product product, int quantity) {
        if (quantity < 1 || quantity > 99) throw new BusinessException("购买数量需为1-99");
        if (quantity > product.getStock()) throw new BusinessException("库存不足，当前仅剩 " + product.getStock() + " 件");
    }

    private CartItemView toView(CartItem item) {
        BigDecimal subtotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemView(item.getId(), ProductView.from(item.getProduct()), item.getQuantity(), subtotal);
    }
}

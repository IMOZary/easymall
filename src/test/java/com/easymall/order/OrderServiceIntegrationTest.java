package com.easymall.order;

import com.easymall.cart.CartDtos.AddCartRequest;
import com.easymall.cart.CartService;
import com.easymall.catalog.Product;
import com.easymall.catalog.ProductRepository;
import com.easymall.common.BusinessException;
import com.easymall.user.User;
import com.easymall.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.easymall.order.OrderDtos.CheckoutRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class OrderServiceIntegrationTest {
    @Autowired OrderService orderService;
    @Autowired CartService cartService;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;

    @Test
    void duplicateCheckoutReturnsSameOrderAndDeductsStockOnlyOnce() {
        User user = userRepository.findByUsername("demo").orElseThrow();
        Product product = productRepository.findAll().get(0);
        int stockBefore = product.getStock();
        cartService.add(user, new AddCartRequest(product.getId(), 2));
        CheckoutRequest request = checkout("idem-" + UUID.randomUUID());

        var first = orderService.checkout(user, request);
        var duplicate = orderService.checkout(user, request);

        assertThat(duplicate.id()).isEqualTo(first.id());
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(stockBefore - 2);
        assertThat(cartService.getCart(user).items()).isEmpty();
    }

    @Test
    void orderStatusFollowsStateMachineAndCancelRestoresStock() {
        User user = userRepository.findByUsername("demo").orElseThrow();
        Product product = productRepository.findAll().get(1);
        int stockBefore = product.getStock();
        cartService.add(user, new AddCartRequest(product.getId(), 1));
        var order = orderService.checkout(user, checkout("state-" + UUID.randomUUID()));

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThatThrownBy(() -> orderService.complete(user, order.id()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已发货");
        assertThat(orderService.pay(user, order.id()).status()).isEqualTo(OrderStatus.PAID);
        assertThat(orderService.cancel(user, order.id()).status()).isEqualTo(OrderStatus.CANCELED);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(stockBefore);
    }

    private CheckoutRequest checkout(String key) {
        return new CheckoutRequest("张同学", "13800138000", "上海市测试路1号", "测试订单", null, key);
    }
}

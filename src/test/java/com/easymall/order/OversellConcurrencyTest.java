package com.easymall.order;

import com.easymall.cart.CartDtos.AddCartRequest;
import com.easymall.cart.CartService;
import com.easymall.catalog.Product;
import com.easymall.catalog.ProductRepository;
import com.easymall.user.Role;
import com.easymall.user.User;
import com.easymall.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static com.easymall.order.OrderDtos.CheckoutRequest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OversellConcurrencyTest {
    @Autowired OrderService orderService;
    @Autowired CartService cartService;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;

    @Test
    void twoUsersCompetingForLastItemOnlyOneSucceeds() throws Exception {
        Product product = productRepository.findAll().get(2);
        product.setStock(1);
        productRepository.saveAndFlush(product);
        User userA = createUser("race_a");
        User userB = createUser("race_b");
        cartService.add(userA, new AddCartRequest(product.getId(), 1));
        cartService.add(userB, new AddCartRequest(product.getId(), 1));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var results = List.of(userA, userB).stream().map(user -> pool.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    orderService.checkout(user, new CheckoutRequest("并发用户", "13800138000", "测试地址", null,
                            null, UUID.randomUUID().toString()));
                    return true;
                } catch (RuntimeException ex) {
                    return false;
                }
            })).toList();
            ready.await();
            start.countDown();
            long successCount = 0;
            for (var result : results) if (result.get()) successCount++;
            assertThat(successCount).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isZero();
    }

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("not-used-in-service-test");
        user.setNickname(username);
        user.setRole(Role.USER);
        return userRepository.saveAndFlush(user);
    }
}

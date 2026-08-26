package com.easymall.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<ShopOrder, Long> {
    Page<ShopOrder> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Optional<ShopOrder> findByIdAndUserId(Long id, Long userId);
    Optional<ShopOrder> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);
    Page<ShopOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByStatus(OrderStatus status);

    @Query("select coalesce(sum(o.payAmount), 0) from ShopOrder o where o.status in (com.easymall.order.OrderStatus.PAID, com.easymall.order.OrderStatus.SHIPPED, com.easymall.order.OrderStatus.COMPLETED)")
    BigDecimal sumPaidAmount();

    @Query("select coalesce(sum(i.quantity), 0) from OrderItem i where i.order.status in (com.easymall.order.OrderStatus.PAID, com.easymall.order.OrderStatus.SHIPPED, com.easymall.order.OrderStatus.COMPLETED)")
    Long sumSoldQuantity();
}

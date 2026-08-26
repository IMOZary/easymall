package com.easymall.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<ShopOrder, Long> {
    Page<ShopOrder> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Optional<ShopOrder> findByIdAndUserId(Long id, Long userId);
    Optional<ShopOrder> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);
    Page<ShopOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByStatus(OrderStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from ShopOrder o where o.id = :id and o.user.id = :userId")
    Optional<ShopOrder> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from ShopOrder o where o.id = :id")
    Optional<ShopOrder> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from ShopOrder o where o.status = com.easymall.order.OrderStatus.PENDING_PAYMENT and o.expiresAt <= :now order by o.expiresAt")
    List<ShopOrder> findExpiredForUpdate(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("select coalesce(sum(o.payAmount), 0) from ShopOrder o where o.status in (com.easymall.order.OrderStatus.PAID, com.easymall.order.OrderStatus.SHIPPED, com.easymall.order.OrderStatus.COMPLETED)")
    BigDecimal sumPaidAmount();

    @Query("select coalesce(sum(i.quantity), 0) from OrderItem i where i.order.status in (com.easymall.order.OrderStatus.PAID, com.easymall.order.OrderStatus.SHIPPED, com.easymall.order.OrderStatus.COMPLETED)")
    Long sumSoldQuantity();
}

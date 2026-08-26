package com.easymall.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class OrderDtos {
    private OrderDtos() {}

    public record CheckoutRequest(
            @NotBlank(message = "收货人不能为空") @Size(max = 30) String receiver,
            @NotBlank(message = "手机号不能为空") @Pattern(regexp = "1\\d{10}", message = "请输入正确的11位手机号") String phone,
            @NotBlank(message = "收货地址不能为空") @Size(max = 200) String address,
            @Size(max = 200, message = "备注最多200字") String remark,
            @Size(max = 30) String couponCode,
            @NotBlank(message = "缺少幂等键") @Size(max = 64) String idempotencyKey) {}

    public record OrderItemView(Long productId, String productName, String productIcon,
                                BigDecimal productPrice, Integer quantity, BigDecimal subtotal) {
        public static OrderItemView from(OrderItem item) {
            return new OrderItemView(item.getProductId(), item.getProductName(), item.getProductIcon(),
                    item.getProductPrice(), item.getQuantity(), item.getSubtotal());
        }
    }

    public record OrderView(Long id, String orderNo, String customer, List<OrderItemView> items,
                            BigDecimal totalAmount, BigDecimal discountAmount, BigDecimal payAmount,
                            String couponCode, OrderStatus status, String receiver, String phone,
                            String address, String remark, LocalDateTime createdAt, LocalDateTime paidAt,
                            LocalDateTime shippedAt) {
        public static OrderView from(ShopOrder order) {
            return new OrderView(order.getId(), order.getOrderNo(), order.getUser().getNickname(),
                    order.getItems().stream().map(OrderItemView::from).toList(), order.getTotalAmount(),
                    order.getDiscountAmount(), order.getPayAmount(), order.getCouponCode(), order.getStatus(),
                    order.getReceiver(), order.getPhone(), order.getAddress(), order.getRemark(),
                    order.getCreatedAt(), order.getPaidAt(), order.getShippedAt());
        }
    }
}

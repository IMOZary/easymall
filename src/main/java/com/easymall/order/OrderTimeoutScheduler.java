package com.easymall.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderTimeoutScheduler {
    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutScheduler.class);
    private final OrderService orderService;

    public OrderTimeoutScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(fixedDelayString = "${easymall.order.timeout-scan-ms:60000}",
            initialDelayString = "${easymall.order.timeout-scan-ms:60000}")
    public void closeExpiredOrders() {
        int closed = orderService.closeExpiredOrders(100);
        if (closed > 0) log.info("已关闭 {} 个超时未支付订单", closed);
    }
}

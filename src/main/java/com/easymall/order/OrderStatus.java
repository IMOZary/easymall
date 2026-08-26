package com.easymall.order;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    SHIPPED,
    COMPLETED,
    CANCELED;

    public boolean canCancel() {
        return this == PENDING_PAYMENT || this == PAID;
    }
}

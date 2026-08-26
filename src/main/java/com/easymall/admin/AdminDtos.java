package com.easymall.admin;

import java.math.BigDecimal;

public final class AdminDtos {
    private AdminDtos() {}

    public record DashboardView(long users, long products, long orders, long pendingShipment,
                                long soldItems, BigDecimal revenue) {}
}

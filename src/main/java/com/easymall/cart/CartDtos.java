package com.easymall.cart;

import com.easymall.catalog.CatalogDtos.ProductView;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class CartDtos {
    private CartDtos() {}

    public record AddCartRequest(@NotNull(message = "商品不能为空") Long productId,
                                 @Min(value = 1, message = "数量至少为1") @Max(value = 99, message = "单件商品最多购买99件") Integer quantity) {}
    public record UpdateCartRequest(@NotNull @Min(value = 1, message = "数量至少为1") @Max(value = 99, message = "单件商品最多购买99件") Integer quantity) {}
    public record CartItemView(Long id, ProductView product, Integer quantity, BigDecimal subtotal) {}
    public record CartView(List<CartItemView> items, Integer totalQuantity, BigDecimal totalAmount) {}
}

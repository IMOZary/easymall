package com.easymall.catalog;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public final class CatalogDtos {
    private CatalogDtos() {}

    public record CategoryView(Long id, String name, String icon) {
        public static CategoryView from(Category category) {
            return new CategoryView(category.getId(), category.getName(), category.getIcon());
        }
    }

    public record ProductView(Long id, CategoryView category, String name, String subtitle,
                              BigDecimal price, Integer stock, Integer sales, String icon,
                              String theme, String description, ProductStatus status) {
        public static ProductView from(Product product) {
            return new ProductView(product.getId(), CategoryView.from(product.getCategory()),
                    product.getName(), product.getSubtitle(), product.getPrice(), product.getStock(),
                    product.getSales(), product.getIcon(), product.getTheme(), product.getDescription(), product.getStatus());
        }
    }

    public record ProductRequest(
            @NotNull(message = "请选择分类") Long categoryId,
            @NotBlank(message = "商品名称不能为空") @Size(max = 80) String name,
            @NotBlank(message = "商品副标题不能为空") @Size(max = 120) String subtitle,
            @NotNull(message = "价格不能为空") @DecimalMin(value = "0.01", message = "价格必须大于0") BigDecimal price,
            @NotNull(message = "库存不能为空") @Min(value = 0, message = "库存不能小于0") Integer stock,
            @NotBlank(message = "图标不能为空") @Size(max = 10) String icon,
            @NotBlank(message = "主题不能为空") @Pattern(regexp = "(mint|peach|sky|lilac|sun)") String theme,
            @NotBlank(message = "商品描述不能为空") @Size(max = 1000) String description,
            ProductStatus status) {}
}

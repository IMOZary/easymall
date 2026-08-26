package com.easymall.catalog;

import com.easymall.common.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.easymall.catalog.CatalogDtos.*;

@Service
public class CatalogService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public CatalogService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductView> search(Long categoryId, String keyword, int page, int size, String sort) {
        Sort sorting = switch (sort == null ? "newest" : sort) {
            case "priceAsc" -> Sort.by("price").ascending();
            case "priceDesc" -> Sort.by("price").descending();
            case "sales" -> Sort.by("sales").descending();
            default -> Sort.by("createdAt").descending();
        };
        return productRepository.search(categoryId, keyword == null ? "" : keyword.trim(),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), sorting)).map(ProductView::from);
    }

    @Transactional(readOnly = true)
    public ProductView detail(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "商品不存在"));
        return ProductView.from(product);
    }

    @Transactional(readOnly = true)
    public List<CategoryView> categories() {
        return categoryRepository.findAllByOrderBySortOrderAsc().stream().map(CategoryView::from).toList();
    }

    @Transactional
    public ProductView save(Long id, ProductRequest request) {
        Product product = id == null ? new Product() : productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "商品不存在"));
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new BusinessException("分类不存在"));
        product.setCategory(category);
        product.setName(request.name());
        product.setSubtitle(request.subtitle());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setIcon(request.icon());
        product.setTheme(request.theme());
        product.setDescription(request.description());
        product.setStatus(request.status() == null ? ProductStatus.ON_SALE : request.status());
        return ProductView.from(productRepository.save(product));
    }
}

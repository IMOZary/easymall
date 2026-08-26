package com.easymall.catalog;

import com.easymall.common.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.easymall.catalog.CatalogDtos.*;

@RestController
public class CatalogController {
    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) { this.catalogService = catalogService; }

    @GetMapping("/api/products")
    public ApiResponse<Page<ProductView>> products(@RequestParam(required = false) Long categoryId,
                                                    @RequestParam(defaultValue = "") String keyword,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "12") int size,
                                                    @RequestParam(defaultValue = "newest") String sort) {
        return ApiResponse.ok(catalogService.search(categoryId, keyword, page, size, sort));
    }

    @GetMapping("/api/products/{id}")
    public ApiResponse<ProductView> product(@PathVariable Long id) {
        return ApiResponse.ok(catalogService.detail(id));
    }

    @GetMapping("/api/categories")
    public ApiResponse<List<CategoryView>> categories() {
        return ApiResponse.ok(catalogService.categories());
    }
}

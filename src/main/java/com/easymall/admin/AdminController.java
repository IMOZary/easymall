package com.easymall.admin;

import com.easymall.catalog.CatalogDtos.ProductRequest;
import com.easymall.catalog.CatalogDtos.ProductView;
import com.easymall.catalog.CatalogService;
import com.easymall.catalog.ProductRepository;
import com.easymall.common.ApiResponse;
import com.easymall.order.OrderDtos.OrderView;
import com.easymall.order.OrderRepository;
import com.easymall.order.OrderService;
import com.easymall.order.OrderStatus;
import com.easymall.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import static com.easymall.admin.AdminDtos.DashboardView;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CatalogService catalogService;
    private final OrderService orderService;

    public AdminController(UserRepository userRepository, ProductRepository productRepository,
                           OrderRepository orderRepository, CatalogService catalogService, OrderService orderService) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.catalogService = catalogService;
        this.orderService = orderService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<DashboardView> dashboard() {
        return ApiResponse.ok(new DashboardView(userRepository.count(), productRepository.count(), orderRepository.count(),
                orderRepository.countByStatus(OrderStatus.PAID), orderRepository.sumSoldQuantity(), orderRepository.sumPaidAmount()));
    }

    @PostMapping("/products")
    public ApiResponse<ProductView> createProduct(@Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok("商品创建成功", catalogService.save(null, request));
    }

    @PutMapping("/products/{id}")
    public ApiResponse<ProductView> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok("商品更新成功", catalogService.save(id, request));
    }

    @GetMapping("/orders")
    @Transactional(readOnly = true)
    public ApiResponse<Page<OrderView>> orders(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(orderRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50))).map(OrderView::from));
    }

    @PostMapping("/orders/{id}/ship")
    public ApiResponse<OrderView> ship(@PathVariable Long id) {
        return ApiResponse.ok("订单已发货", orderService.ship(id));
    }
}

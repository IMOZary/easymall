package com.easymall.order;

import com.easymall.common.ApiResponse;
import com.easymall.user.CurrentUserService;
import com.easymall.user.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.easymall.order.OrderDtos.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    private final CurrentUserService currentUserService;

    public OrderController(OrderService orderService, CurrentUserService currentUserService) {
        this.orderService = orderService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ApiResponse<OrderView> checkout(@Valid @RequestBody CheckoutRequest request, Authentication authentication) {
        return ApiResponse.ok("订单创建成功", orderService.checkout(user(authentication), request));
    }

    @GetMapping
    public ApiResponse<Page<OrderView>> list(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             Authentication authentication) {
        return ApiResponse.ok(orderService.myOrders(user(authentication), page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderView> detail(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.ok(orderService.detail(user(authentication), id));
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<OrderView> pay(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.ok("模拟支付成功", orderService.pay(user(authentication), id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderView> cancel(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.ok("订单已取消，库存已回补", orderService.cancel(user(authentication), id));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<OrderView> complete(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.ok("已确认收货", orderService.complete(user(authentication), id));
    }

    private User user(Authentication authentication) { return currentUserService.require(authentication); }
}

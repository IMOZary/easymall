package com.easymall.cart;

import com.easymall.common.ApiResponse;
import com.easymall.user.CurrentUserService;
import com.easymall.user.User;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.easymall.cart.CartDtos.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService cartService;
    private final CurrentUserService currentUserService;

    public CartController(CartService cartService, CurrentUserService currentUserService) {
        this.cartService = cartService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<CartView> get(Authentication authentication) {
        return ApiResponse.ok(cartService.getCart(currentUserService.require(authentication)));
    }

    @PostMapping
    public ApiResponse<CartView> add(@Valid @RequestBody AddCartRequest request, Authentication authentication) {
        User user = currentUserService.require(authentication);
        return ApiResponse.ok("已加入购物车", cartService.add(user, request));
    }

    @PutMapping("/{itemId}")
    public ApiResponse<CartView> update(@PathVariable Long itemId, @Valid @RequestBody UpdateCartRequest request,
                                        Authentication authentication) {
        return ApiResponse.ok(cartService.update(currentUserService.require(authentication), itemId, request.quantity()));
    }

    @DeleteMapping("/{itemId}")
    public ApiResponse<CartView> remove(@PathVariable Long itemId, Authentication authentication) {
        return ApiResponse.ok("已移除商品", cartService.remove(currentUserService.require(authentication), itemId));
    }
}

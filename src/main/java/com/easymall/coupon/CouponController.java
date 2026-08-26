package com.easymall.coupon;

import com.easymall.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.easymall.coupon.CouponDtos.CouponView;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {
    private final CouponService couponService;
    public CouponController(CouponService couponService) { this.couponService = couponService; }

    @GetMapping
    public ApiResponse<List<CouponView>> available() { return ApiResponse.ok(couponService.available()); }
}

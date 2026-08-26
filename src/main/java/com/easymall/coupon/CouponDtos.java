package com.easymall.coupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class CouponDtos {
    private CouponDtos() {}

    public record CouponView(String code, String name, CouponType type, BigDecimal discountValue,
                             BigDecimal minAmount, LocalDateTime endAt, String description) {
        public static CouponView from(Coupon coupon) {
            String text = coupon.getType() == CouponType.FIXED
                    ? "满 " + coupon.getMinAmount().stripTrailingZeros().toPlainString() + " 减 " + coupon.getDiscountValue().stripTrailingZeros().toPlainString()
                    : "满 " + coupon.getMinAmount().stripTrailingZeros().toPlainString() + " 享 " + coupon.getDiscountValue().multiply(BigDecimal.TEN).stripTrailingZeros().toPlainString() + " 折";
            return new CouponView(coupon.getCode(), coupon.getName(), coupon.getType(), coupon.getDiscountValue(),
                    coupon.getMinAmount(), coupon.getEndAt(), text);
        }
    }
}

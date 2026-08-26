package com.easymall.coupon;

import com.easymall.common.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import static com.easymall.coupon.CouponDtos.CouponView;

@Service
public class CouponService {
    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) { this.couponRepository = couponRepository; }

    @Transactional(readOnly = true)
    public List<CouponView> available() {
        return couponRepository.findAvailable(LocalDateTime.now()).stream().map(CouponView::from).toList();
    }

    public BigDecimal calculateAndUse(String code, BigDecimal total) {
        if (code == null || code.isBlank()) return BigDecimal.ZERO;
        Coupon coupon = couponRepository.findByCodeForUpdate(code.trim())
                .orElseThrow(() -> new BusinessException("优惠码不存在"));
        LocalDateTime now = LocalDateTime.now();
        if (!coupon.getEnabled() || now.isBefore(coupon.getStartAt()) || now.isAfter(coupon.getEndAt()))
            throw new BusinessException("优惠码不在有效期内");
        if (coupon.getUsedQuantity() >= coupon.getTotalQuantity()) throw new BusinessException("优惠码已领完");
        if (total.compareTo(coupon.getMinAmount()) < 0)
            throw new BusinessException("订单金额未达到优惠门槛 " + coupon.getMinAmount());
        BigDecimal discount = coupon.getType() == CouponType.FIXED
                ? coupon.getDiscountValue()
                : total.multiply(BigDecimal.ONE.subtract(coupon.getDiscountValue()));
        discount = discount.min(total).setScale(2, RoundingMode.HALF_UP);
        coupon.setUsedQuantity(coupon.getUsedQuantity() + 1);
        return discount;
    }

    public void release(String code) {
        if (code == null || code.isBlank()) return;
        couponRepository.findByCodeForUpdate(code).ifPresent(coupon ->
                coupon.setUsedQuantity(Math.max(0, coupon.getUsedQuantity() - 1)));
    }
}

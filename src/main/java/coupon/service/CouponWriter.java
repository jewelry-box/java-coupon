package coupon.service;

import coupon.domain.Coupon;
import coupon.domain.DiscountAmount;
import coupon.domain.MinOrderAmount;
import coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponWriter {

    private final CouponRepository couponRepository;

    @Transactional
    public Coupon save(Coupon coupon) {
        return couponRepository.save(coupon);
    }

    @Transactional
    public Coupon updateDiscountAmount(long couponId, long amount) {
        DiscountAmount discountAmount = new DiscountAmount(amount);
        Coupon coupon = findCoupon(couponId);
        return coupon.updateDiscountAmount(discountAmount);
    }

    @Transactional
    public Coupon updateMinOrderAmount(long couponId, long amount) {
        MinOrderAmount minOrderAmount = new MinOrderAmount(amount);
        Coupon coupon = findCoupon(couponId);
        return coupon.updateMinOrderAmount(minOrderAmount);
    }

    private Coupon findCoupon(long couponId) {
        return couponRepository.findByIdForUpdate(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰이 존재하지 않습니다."));
    }
}

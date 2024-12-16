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
    public Coupon updateDiscountAmount(Coupon coupon, long amount) {
        DiscountAmount discountAmount = new DiscountAmount(amount);
        coupon.setDiscountAmount(discountAmount);
        return couponRepository.save(coupon);
    }

    @Transactional
    public Coupon updateMinOrderAmount(Coupon coupon, int amount) {
        MinOrderAmount minOrderAmount = new MinOrderAmount(amount);
        coupon.setMinOrderAmount(minOrderAmount);
        return couponRepository.save(coupon);
    }
}

package coupon.service;

import coupon.domain.Coupon;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponWriter couponWriter;
    private final CouponReader couponReader;

    public Coupon save(Coupon coupon) {
        return couponWriter.save(coupon);
    }

    public Coupon findById(long couponId) {
        return couponReader.findById(couponId);
    }

    public Coupon updateDiscountAmount(long couponId, long discountAmount) {
        return couponWriter.updateDiscountAmount(couponId, discountAmount);
    }

    public Coupon updateMinOrderAmount(long couponId, int minOrderAmount) {
        return couponWriter.updateMinOrderAmount(couponId, minOrderAmount);
    }
}

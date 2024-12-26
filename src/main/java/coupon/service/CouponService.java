package coupon.service;

import coupon.domain.Coupon;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponWriter couponWriter;
    private final CouponReader couponReader;

    public Coupon save(Coupon coupon) {
        return couponWriter.save(coupon);
    }

    @Cacheable(value = "coupon", key = "#couponId")
    public Coupon findById(long couponId) {
        return couponReader.findById(couponId);
    }

    @CachePut(value = "coupon", key = "#couponId")
    public Coupon updateDiscountAmount(long couponId, long discountAmount) {
        return couponWriter.updateDiscountAmount(couponId, discountAmount);
    }

    @CachePut(value = "coupon", key = "#couponId")
    public Coupon updateMinOrderAmount(long couponId, long minOrderAmount) {
        return couponWriter.updateMinOrderAmount(couponId, minOrderAmount);
    }
}

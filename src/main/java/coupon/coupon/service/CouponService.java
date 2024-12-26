package coupon.coupon.service;

import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import coupon.cache.CacheService;
import coupon.coupon.domain.Coupon;
import coupon.coupon.domain.DiscountAmount;
import coupon.coupon.domain.MinimumOrderAmount;
import coupon.coupon.repository.CouponRepository;
import coupon.coupon.service.dto.DiscountAmountRequest;
import coupon.coupon.service.dto.MinimumOrderAmountRequest;
import coupon.util.Retry;

@Service
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;
    private final CacheService cacheService;

    public CouponService(CouponRepository couponRepository, CacheService cacheService) {
        this.couponRepository = couponRepository;
        this.cacheService = cacheService;
    }

    @Transactional
    public void create(Coupon coupon) {
        couponRepository.save(coupon);
    }

    @Transactional
    public Coupon getCoupon(long id) {
        return cacheService.getCoupon(id);
    }

    @Retry
    @Transactional
    @CachePut(value = "coupon", key = "#id")
    public Coupon updateCouponMinimumOrderAmount(long id, MinimumOrderAmountRequest request) {
        MinimumOrderAmount minimumOrderAmount = request.toMinimumOrderAmount();
        Coupon coupon = cacheService.getCoupon(id);
        coupon.changeMinimumOrderAmount(minimumOrderAmount);
        return coupon;
    }

    @Retry
    @Transactional
    @CachePut(value = "coupon", key = "#id")
    public Coupon updateCouponDiscountAmount(long id, DiscountAmountRequest request) {
        DiscountAmount discountAmount = request.toDiscountAmount();
        Coupon coupon = cacheService.getCoupon(id);
        coupon.changeDiscountAmount(discountAmount);
        return coupon;
    }
}

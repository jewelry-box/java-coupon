package coupon.coupon.service;

import coupon.coupon.domain.Coupon;
import coupon.coupon.domain.repository.CouponRepository;
import coupon.util.NewTransactionExecutor;
import java.math.BigDecimal;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final NewTransactionExecutor newTransactionExecutor;

    @Transactional
    @CachePut(key = "#result.id", value = "coupon")
    public Coupon createCoupon(Coupon coupon) {
        return couponRepository.save(coupon);
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "#couponId", value = "coupon")
    public Coupon getCoupon(long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다. couponId: %d ".formatted(couponId)));
    }

    @Transactional(readOnly = true)
    public Coupon getCouponForce(long couponId) {
        return couponRepository.findById(couponId)
                .orElseGet(() -> {
                            log.info("Switching to write DB");
                            return newTransactionExecutor.execute(() -> getCouponForce(couponId));
                        }
                );
    }

    @Transactional
    public void updateCouponField(Long couponId, Consumer<Coupon> updateField) {
        Coupon coupon = getCouponForce(couponId);
        updateField.accept(coupon);
    }

    @Transactional
    public void updateDiscountAmount(Long couponId, BigDecimal newDiscountAmount) {
        updateCouponField(couponId, coupon -> coupon.updateDiscountAmount(newDiscountAmount));
    }

    @Transactional
    public void updateMinimumOrderPrice(Long couponId, BigDecimal newMinimumOrderPrice) {
        updateCouponField(couponId, coupon -> coupon.updateMinimumOrderPrice(newMinimumOrderPrice));
    }
}

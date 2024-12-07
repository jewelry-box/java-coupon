package coupon.coupon.service;

import coupon.coupon.domain.Coupon;
import coupon.coupon.domain.repository.CouponRepository;
import coupon.util.NewTransactionExecutor;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
    public void updateDiscountAmount(Long couponId, BigDecimal newDiscountAmount) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다. couponId: %d ".formatted(couponId)));

        coupon.updateDiscountAmount(newDiscountAmount);
        try {
            couponRepository.flush();
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new RuntimeException("할인 금액을 업데이트하는 도중에 다른 사용자가 데이터를 변경하였습니다. couponId: %d".formatted(couponId));
        }
    }

    @Transactional
    public void updateMinimumOrderPrice(Long couponId, BigDecimal newMinimumOrderPrice) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다. couponId: %d ".formatted(couponId)));
        coupon.updateMinimumOrderPrice(newMinimumOrderPrice);
        try {
            couponRepository.flush();
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new RuntimeException("최소 주문 금액을 업데이트하는 도중에 다른 사용자가 데이터를 변경하였습니다. couponId: %d".formatted(couponId));
        }
    }
}

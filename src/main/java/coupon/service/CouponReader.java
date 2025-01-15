package coupon.service;

import coupon.domain.Coupon;
import coupon.repository.CouponRepository;
import coupon.util.FallbackExecutor;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponReader {

    private final CouponRepository couponRepository;
    private final FallbackExecutor fallbackExecutor;

    @Transactional(readOnly = true)
    public Coupon findById(long couponId) {
        Supplier<Coupon> retryFindById = () -> couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰이 존재하지 않습니다."));

        return couponRepository.findById(couponId)
                .orElse(fallbackExecutor.execute(retryFindById));
    }
}

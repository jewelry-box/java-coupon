package coupon.coupon.service;

import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import coupon.CouponException;
import coupon.ServiceTest;
import coupon.coupon.domain.Coupon;
import coupon.coupon.service.dto.DiscountAmountRequest;
import coupon.coupon.service.dto.MinimumOrderAmountRequest;
import coupon.fixture.CouponFixture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class CouponServiceTest extends ServiceTest {

    @Autowired
    private CouponService couponService;
    @Autowired
    private CacheManager cacheManager;

    @AfterEach
    void tearDown() {
        cacheManager.getCache("coupon").clear();
    }

    @DisplayName("요청한 쿠폰을 조회한다.")
    @Test
    void getCoupon() {
        // given
        Coupon coupon = CouponFixture.create(LocalDate.now(), LocalDate.now().plusDays(7));
        couponRepository.save(coupon);

        // when
        Coupon savedCoupon = couponService.getCoupon(coupon.getId());

        // then
        assertThat(savedCoupon).isNotNull();
    }

    @DisplayName("존재하지 않는 쿠폰을 조회하면 예외가 발생한다.")
    @Test
    void cannotGetCoupon() {
        // given
        long notExistCouponId = 0;

        // when & then
        assertThatThrownBy(() -> couponService.getCoupon(notExistCouponId))
                .isInstanceOf(CouponException.class)
                .hasMessage("요청하신 쿠폰을 찾을 수 없어요.");
    }

    @DisplayName("쿠폰을 생성한다.")
    @Test
    void createCoupon() {
        // given
        Coupon coupon = CouponFixture.create(LocalDate.now(), LocalDate.now().plusDays(7));

        // when
        couponService.create(coupon);

        // then
        assertThat(couponRepository.findById(coupon.getId())).isNotNull();
    }

    @DisplayName("동시에 쿠폰 할인 금액과 최소 주문 금액을 수정할 때 제약 조건을 위반하지 않으면 수정에 성공한다.")
    @Test
    void updateCouponConcurrently() {
        // given
        Coupon coupon = couponRepository.save(CouponFixture.create(1000, 10000));
        DiscountAmountRequest discountAmountRequest = new DiscountAmountRequest(2000);
        MinimumOrderAmountRequest minimumOrderAmountRequest = new MinimumOrderAmountRequest(20000);

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        Future<?> future = executorService.submit(() -> couponService.updateCouponDiscountAmount(coupon.getId(), discountAmountRequest));
        Future<?> future2 = executorService.submit(() -> couponService.updateCouponMinimumOrderAmount(coupon.getId(), minimumOrderAmountRequest));

        // then
        assertThatNoException().isThrownBy(() -> {
            future.get();
            future2.get();
        });
    }

    @DisplayName("동시에 쿠폰 할인 금액과 최소 주문 금액을 수정할 때 제약 조건을 위반할 수 없다.")
    @Test
    void cannotUpdateCouponConcurrentlyIfViolateCondition() {
        // given
        couponService.create(CouponFixture.create(1000, 10000));
        DiscountAmountRequest discountAmountRequest = new DiscountAmountRequest(2000);
        MinimumOrderAmountRequest minimumOrderAmountRequest = new MinimumOrderAmountRequest(8000);
        long couponId = 1;

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        // when
        Future<?> future = executorService.submit(() -> couponService.updateCouponDiscountAmount(couponId, discountAmountRequest));
        Future<?> future2 = executorService.submit(() -> couponService.updateCouponMinimumOrderAmount(couponId, minimumOrderAmountRequest));

        // then
        assertThatThrownBy(() -> {
            future.get();
            future2.get();
        }).hasCauseInstanceOf(CouponException.class)
                .hasMessageContaining("할인율은 3% 이상, 20% 이하이어야 합니다.");
    }
}


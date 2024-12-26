package coupon.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;

import coupon.coupon.domain.Coupon;
import coupon.coupon.domain.CouponCategory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CouponServiceTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @AfterEach
    void clearRedisCache() {
        redisTemplate.keys("*coupon*").forEach(redisTemplate::delete);
    }

    @Test
    @Sql(scripts = "/reset-database.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void 복제지연테스트() {
        // given
        String name = "냥인의쿠폰";
        BigDecimal discountAmount = BigDecimal.valueOf(1_000);
        BigDecimal minimumOrderPrice = BigDecimal.valueOf(5_000);
        CouponCategory couponCategory = CouponCategory.FOOD;
        LocalDateTime issueStartedAt = LocalDateTime.of(2024, 10, 16, 0, 0, 0, 0);
        LocalDateTime issueEndedAt = LocalDateTime.of(2024, 10, 26, 23, 59, 59, 999_999_000);
        Coupon coupon = new Coupon(
                name, discountAmount, minimumOrderPrice, couponCategory, issueStartedAt, issueEndedAt
        );

        // when
        couponService.createCoupon(coupon);
        Coupon savedCoupon = couponService.getCoupon(coupon.getId());

        // then
        assertThat(savedCoupon.getId()).isEqualTo(1L);
    }

    @Test
    @Sql(scripts = "/reset-database.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void 쿠폰_수정_동시성_문제_테스트() throws InterruptedException {
        // given
        String name = "냥인의쿠폰";
        BigDecimal discountAmount = BigDecimal.valueOf(2_500);
        BigDecimal minimumOrderPrice = BigDecimal.valueOf(30_000);
        CouponCategory couponCategory = CouponCategory.FOOD;
        LocalDateTime issueStartedAt = LocalDateTime.of(2024, 10, 16, 0, 0, 0, 0);
        LocalDateTime issueEndedAt = LocalDateTime.of(2024, 10, 26, 23, 59, 59, 999_999_000);
        Coupon coupon = new Coupon(
                name, discountAmount, minimumOrderPrice, couponCategory, issueStartedAt, issueEndedAt
        );
        Coupon savedCoupon = couponService.createCoupon(coupon);

        // when
        AtomicReference<Exception> exceptionFromUser1 = new AtomicReference<>();
        AtomicReference<Exception> exceptionFromUser2 = new AtomicReference<>();
        // 사용자 1: 쿠폰 할인 금액 변경 기능을 이용하여 할인 금액을 1,000원으로 수정
        Thread user1 = new Thread(() -> {
            try {
                couponService.updateDiscountAmount(savedCoupon.getId(), BigDecimal.valueOf(1_000));
            } catch (Exception e) {
                exceptionFromUser1.set(e);
            }
        });
        // 사용자 2: 쿠폰 최소 주문 금액 변경 기능을 이용하여 최소 주문 금액을 40,000원으로 수정
        Thread user2 = new Thread(() -> {
            try {
                couponService.updateMinimumOrderPrice(savedCoupon.getId(), BigDecimal.valueOf(40_000));
            } catch (Exception e) {
                exceptionFromUser2.set(e);
            }
        });

        user1.start();
        user2.start();

        user1.join();
        user2.join();

        // then
        if (exceptionFromUser1.get() != null) {
            assertThat(exceptionFromUser1.get()).isInstanceOf(ObjectOptimisticLockingFailureException.class);
        }
        if (exceptionFromUser2.get() != null) {
            assertThat(exceptionFromUser2.get()).isInstanceOf(ObjectOptimisticLockingFailureException.class);
        }
        Coupon updatedCoupon = couponService.getCouponForce(savedCoupon.getId());
        int discountRate = updatedCoupon.getDiscountAmount()
                .multiply(BigDecimal.valueOf(100))
                .divide(updatedCoupon.getMinimumOrderPrice(), 0, RoundingMode.DOWN)
                .intValue();
        assertThat(discountRate).isBetween(3, 20);
    }
}

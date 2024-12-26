package coupon.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;

import coupon.coupon.domain.Coupon;
import coupon.coupon.domain.CouponCategory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        // given & when
        Coupon coupon = createTestCoupon(BigDecimal.valueOf(1_000), BigDecimal.valueOf(5_000));
        Coupon savedCoupon = couponService.getCoupon(coupon.getId());

        // then
        assertThat(savedCoupon.getId()).isEqualTo(1L);
    }

    @Test
    @Sql(scripts = "/reset-database.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void 쿠폰_수정_동시성_문제_테스트() throws InterruptedException {
        // given
        Coupon savedCoupon = createTestCoupon(BigDecimal.valueOf(2_500), BigDecimal.valueOf(30_000));

        // when
        List<Exception> exceptions = executeConcurrentUpdates(
                () -> couponService.updateDiscountAmount(savedCoupon.getId(), BigDecimal.valueOf(1_000)),
                () -> couponService.updateMinimumOrderPrice(savedCoupon.getId(), BigDecimal.valueOf(40_000))
        );

        // then
        Coupon updatedCoupon = couponService.getCouponForce(savedCoupon.getId());
        int discountRate = updatedCoupon.getDiscountAmount()
                .multiply(BigDecimal.valueOf(100))
                .divide(updatedCoupon.getMinimumOrderPrice(), 0, RoundingMode.DOWN)
                .intValue();
        assertThat(discountRate).isBetween(3, 20);
        assertThat(exceptions)
                .hasSize(1)
                .allMatch(e -> e instanceof ObjectOptimisticLockingFailureException);
    }

    private Coupon createTestCoupon(BigDecimal discountAmount, BigDecimal minimumOrderPrice) {
        String name = "냥인의쿠폰";
        CouponCategory couponCategory = CouponCategory.FOOD;
        LocalDateTime issueStartedAt = LocalDateTime.of(2024, 10, 16, 0, 0, 0, 0);
        LocalDateTime issueEndedAt = LocalDateTime.of(2024, 10, 26, 23, 59, 59, 999_999_000);
        Coupon coupon = new Coupon(
                name, discountAmount, minimumOrderPrice, couponCategory, issueStartedAt, issueEndedAt
        );
        return couponService.createCoupon(coupon);
    }

    private List<Exception> executeConcurrentUpdates(Runnable user1Task, Runnable user2Task)
            throws InterruptedException {
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // 사용자 1: 쿠폰 할인 금액 변경 기능을 이용하여 할인 금액을 1,000원으로 수정
        Thread user1 = new Thread(() -> {
            try {
                user1Task.run();
            } catch (Exception e) {
                exceptions.add(e);
            }
        });
        // 사용자 2: 쿠폰 최소 주문 금액 변경 기능을 이용하여 최소 주문 금액을 40,000원으로 수정
        Thread user2 = new Thread(() -> {
            try {
                user2Task.run();
            } catch (Exception e) {
                exceptions.add(e);
            }
        });

        user1.start();
        user2.start();

        user1.join();
        user2.join();

        return exceptions;
    }
}

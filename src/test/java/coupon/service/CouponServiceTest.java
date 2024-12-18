package coupon.service;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import coupon.domain.Coupon;
import coupon.repository.CouponRepository;
import coupon.support.Fixture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class CouponServiceTest {

    private static final String COUPON_CACHE_NAME = "coupon";

    @Autowired
    private CouponService couponService;

    @SpyBean
    private CacheManager cacheManager;

    @SpyBean
    private CouponRepository couponRepository;

    @Nested
    class 복제_지연 {

        @Test
        void 쿠폰_생성_후_바로_조회시_복제_지연으로_인한_예외가_발생하지_않는다() {
            Coupon saved = couponService.save(Fixture.createCoupon());

            assertThatCode(() -> couponService.findById(saved.getId()))
                    .doesNotThrowAnyException();
        }

        @Test
        void 조회_시_캐시에_없으면_DB_조회_후_캐시_쓰기한다() {
            Coupon dbCoupon = couponService.save(Fixture.createCoupon());

            Coupon firstSearch = couponService.findById(dbCoupon.getId());

            Cache couponCache = requireNonNull(cacheManager.getCache(COUPON_CACHE_NAME));

            assertAll(
                    () -> assertThat(firstSearch.getId()).isEqualTo(dbCoupon.getId()),
                    () -> verify(couponRepository, atLeastOnce()).findById(dbCoupon.getId()),
                    () -> assertThat(couponCache.get(dbCoupon.getId(), Coupon.class))
                            .extracting(Coupon::getId)
                            .isEqualTo(dbCoupon.getId())
            );
        }

        @Test
        void 조회_시_캐시에_있으면_DB를_조회하지_않는다() {
            Coupon dbCoupon = couponService.save(Fixture.createCoupon());
            couponService.findById(dbCoupon.getId());

            clearInvocations(couponRepository);

            Coupon secondSearch = couponService.findById(dbCoupon.getId());

            assertAll(
                    () -> assertThat(secondSearch.getId()).isEqualTo(dbCoupon.getId()),
                    () -> verify(couponRepository, never()).findById(dbCoupon.getId())
            );
        }
    }

    @Nested
    class 쿠폰_금액_수정 {

        @Test
        void 쿠폰_할인_금액을_변경한다() {
            Coupon coupon = couponService.save(Fixture.createCoupon());

            couponService.updateDiscountAmount(coupon.getId(), 2000);

            Long updatedAmount = couponService.findById(coupon.getId())
                    .getDiscountAmount()
                    .getAmount();

            assertThat(updatedAmount).isEqualTo(2000);
        }

        @Test
        void 제약조건에_부합하지_않으면_쿠폰_할인_금액을_변경할_수_없다() {
            Coupon validCoupon = couponService.save(Fixture.createCoupon(1000, 30000));

            assertThatThrownBy(() -> couponService.updateDiscountAmount(validCoupon.getId(), 500))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void 최소_주문_금액을_변경한다() {
            Coupon coupon = couponService.save(Fixture.createCoupon());

            couponService.updateMinOrderAmount(coupon.getId(), 25000);

            Long updatedAmount = couponService.findById(coupon.getId())
                    .getMinOderAmount()
                    .getAmount();

            assertThat(updatedAmount).isEqualTo(25000);
        }

        @Test
        void 제약조건에_부합하지_않으면_최소_주문_금액을_변경할_수_없다() {
            Coupon validCoupon = couponService.save(Fixture.createCoupon(1000, 30000));

            assertThatThrownBy(() -> couponService.updateMinOrderAmount(validCoupon.getId(), 1000))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }


    @Nested
    class 쿠폰_금액_수정_동시성_테스트 {

        @Test
        void 사용자_2명이_동시에_쿠폰을_수정하는_경우_제약조건에_부합하지_않으면_예외가_발생한다() throws InterruptedException {
            Coupon coupon = couponService.save(Fixture.createCoupon(2000, 30000));

            ExecutorService executorService = Executors.newFixedThreadPool(2);
            CountDownLatch countDownLatch = new CountDownLatch(2);
            AtomicInteger exceptionCount = new AtomicInteger(0);

            executorService.submit(() -> {
                try {
                    couponService.updateDiscountAmount(coupon.getId(), 1000);
                } catch (IllegalArgumentException e) {
                    exceptionCount.incrementAndGet();
                } finally {
                    countDownLatch.countDown();
                }
            });

            executorService.submit(() -> {
                try {
                    couponService.updateMinOrderAmount(coupon.getId(), 40000);
                } catch (IllegalArgumentException e) {
                    exceptionCount.incrementAndGet();
                } finally {
                    countDownLatch.countDown();
                }
            });

            countDownLatch.await();

            assertThat(exceptionCount.get()).isEqualTo(1);
        }

        @Test
        void 사용자_2명이_동시에_쿠폰을_수정하는_경우_제약조건에_부합하면_예외가_발생하지_않는다() throws InterruptedException {
            Coupon coupon = couponService.save(Fixture.createCoupon(2000, 30000));

            ExecutorService executorService = Executors.newFixedThreadPool(2);
            CountDownLatch countDownLatch = new CountDownLatch(2);
            AtomicInteger exceptionCount = new AtomicInteger(0);

            executorService.submit(() -> {
                try {
                    couponService.updateDiscountAmount(coupon.getId(), 2500);
                } catch (IllegalArgumentException e) {
                    exceptionCount.incrementAndGet();
                } finally {
                    countDownLatch.countDown();
                }
            });

            executorService.submit(() -> {
                try {
                    couponService.updateMinOrderAmount(coupon.getId(), 40000);
                } catch (IllegalArgumentException e) {
                    exceptionCount.incrementAndGet();
                } finally {
                    countDownLatch.countDown();
                }
            });

            countDownLatch.await();

            assertThat(exceptionCount.get()).isZero();
        }
    }
}

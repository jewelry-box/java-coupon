package coupon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import coupon.cache.CachedCoupon;
import coupon.domain.Category;
import coupon.domain.Coupon;
import coupon.domain.vo.DiscountAmount;
import coupon.domain.vo.IssuePeriod;
import coupon.domain.vo.MinimumOrderPrice;
import coupon.domain.vo.Name;
import coupon.repository.CachedCouponRepository;
import coupon.repository.CouponRepository;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CouponServiceTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CachedCouponRepository cachedCouponRepository;

    @Autowired
    private CouponRepository couponRepository;

    private Coupon coupon;

    @BeforeEach
    void setUp() {
        Name name = new Name("쿠폰이름");
        DiscountAmount discountAmount = new DiscountAmount(1_500);
        MinimumOrderPrice minimumOrderPrice = new MinimumOrderPrice(30_000);
        IssuePeriod issuePeriod = new IssuePeriod(LocalDateTime.now(), LocalDateTime.now());
        this.coupon = new Coupon(name, discountAmount, minimumOrderPrice, Category.FASHION, issuePeriod);
    }

    @Test
    @DisplayName("복제 지연으로 쿠폰이 조회되지 않는 현상을 방지한다.")
    void occurReplicationLag() {
        // when
        couponService.create(coupon);
        Coupon savedCoupon = couponService.getCouponInReplicationLag(coupon.getId());

        // then
        assertThat(savedCoupon).isNotNull();
    }

    @Test
    @DisplayName("쿠폰 정보를 업데이트할 때 캐시에 쿠폰이 있다면 캐시 정보를 업데이트한다.")
    void updateCouponInCache() {
        // given
        Coupon createdCoupon = couponRepository.save(coupon);
        cachedCouponRepository.save(new CachedCoupon(createdCoupon));
        DiscountAmount discountAmount = new DiscountAmount(2_000);

        // when
        couponService.updateDiscountAmount(discountAmount, createdCoupon.getId());

        // then
        CachedCoupon cachedCoupon = cachedCouponRepository.findById(createdCoupon.getId()).get();
        assertThat(cachedCoupon.getCoupon().getDiscountAmount().getValue()).isEqualTo(2_000);
    }

    @Test
    @DisplayName("쿠폰 할인 금액을 수정한다.")
    void updateDiscountAmount() {
        // given
        Coupon createdCoupon = couponRepository.save(coupon);
        cachedCouponRepository.save(new CachedCoupon(createdCoupon));
        DiscountAmount discountAmount = new DiscountAmount(2_000);

        // when
        couponService.updateDiscountAmount(discountAmount, createdCoupon.getId());

        // then
        DiscountAmount updatedDiscountAmount = couponService.getCouponInReplicationLag(createdCoupon.getId())
                .getDiscountAmount();
        assertThat(updatedDiscountAmount.getValue()).isEqualTo(2_000);
    }

    @Test
    @DisplayName("쿠폰 최소 주문 금액을 수정한다.")
    void updateMinimumOrderPrice() {
        // given
        Coupon createdCoupon = couponRepository.save(coupon);
        cachedCouponRepository.save(new CachedCoupon(createdCoupon));
        MinimumOrderPrice minimumOrderPrice = new MinimumOrderPrice(30_001);

        // when
        couponService.updateMinimumOrderPrice(minimumOrderPrice, createdCoupon.getId());

        // then
        MinimumOrderPrice updatedMinimumOrderPrice = couponService.getCouponInReplicationLag(createdCoupon.getId())
                .getMinimumOrderPrice();
        assertThat(updatedMinimumOrderPrice.getValue()).isEqualTo(30_001);
    }

    @Test
    @DisplayName("동시에 쿠폰 할인 금액과 최소 주문 금액을 수정하면 제약조건을 위반하는 쿠폰이 생성된다.")
    void updateDiscountAmountAndMinimumOrderPriceSimultaneously() throws Exception {
        // given
        Coupon createdCoupon = couponRepository.save(coupon);
        MinimumOrderPrice minimumOrderPrice = new MinimumOrderPrice(40_000);
        DiscountAmount discountAmount = new DiscountAmount(1_000);

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        // when
        Future<?> future1 = executorService.submit(
                () -> couponService.updateMinimumOrderPrice(minimumOrderPrice, createdCoupon.getId()));
        Future<?> future2 = executorService.submit(
                () -> couponService.updateDiscountAmount(discountAmount, createdCoupon.getId()));
        future1.get();
        future2.get();

        // then
        Coupon updatedCoupon = couponService.getCouponInReplicationLag(createdCoupon.getId());
        DiscountAmount updatedDiscountAmount = updatedCoupon.getDiscountAmount();
        MinimumOrderPrice updatedMinimumOrderPrice = updatedCoupon.getMinimumOrderPrice();
        int discountRate = updatedDiscountAmount.calculateDiscountRate(updatedMinimumOrderPrice.getValue());
        assertAll(() -> {
            assertThat(updatedDiscountAmount.getValue()).isEqualTo(1_000);
            assertThat(updatedMinimumOrderPrice.getValue()).isEqualTo(40_000);
            assertThat(discountRate).isEqualTo(2);
        });
    }
}

package coupon.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CouponTest {

    private final CouponName COUPON_NAME = new CouponName("행운쿠폰");
    private final IssuablePeriod ISSUABLE_PERIOD = new IssuablePeriod(LocalDateTime.now(),
            LocalDateTime.now().plusDays(1));

    @Nested
    class 쿠폰_생성 {

        @ParameterizedTest
        @CsvSource({"6500,30000", "2000,100000"})
        void 할인율이_3퍼센트_미만이거나_20퍼센트_초과이면_예외가_발생한다(long discountValue, long minOrderValue) {
            DiscountAmount discountAmount = new DiscountAmount(discountValue);
            MinOrderAmount minOrderAmount = new MinOrderAmount(minOrderValue);
            assertThatThrownBy(
                    () -> new Coupon(COUPON_NAME, discountAmount, minOrderAmount, Category.FOOD, ISSUABLE_PERIOD))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("할인율은 3% 이상 20% 이하여야 합니다.");
        }

        @Test
        void 할인율이_3퍼센트_이상이고_20퍼센트_이하이면_예외가_발생하지_않는다() {
            DiscountAmount discountAmount = new DiscountAmount(1000);
            MinOrderAmount minOrderAmount = new MinOrderAmount(30000);
            assertThatCode(
                    () -> new Coupon(COUPON_NAME, discountAmount, minOrderAmount, Category.FOOD, ISSUABLE_PERIOD))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class 쿠폰_금액_변경 {

        @Test
        void 할인율이_3퍼센트_미만이거나_20퍼센트_초과이면_예외가_발생한다() {
            DiscountAmount discountAmount = new DiscountAmount(1000);
            MinOrderAmount minOrderAmount = new MinOrderAmount(30000);
            Coupon coupon = new Coupon(COUPON_NAME, discountAmount, minOrderAmount, Category.FOOD,
                    ISSUABLE_PERIOD);

            DiscountAmount invalidDiscountAmount = new DiscountAmount(6500);
            assertThatThrownBy(() -> coupon.updateDiscountAmount(invalidDiscountAmount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("할인율은 3% 이상 20% 이하여야 합니다.");
        }

        @Test
        void 할인율이_3퍼센트_이상이고_20퍼센트_이하이면_예외가_발생하지_않는다() {
            DiscountAmount discountAmount = new DiscountAmount(1000);
            MinOrderAmount minOrderAmount = new MinOrderAmount(30000);
            Coupon coupon = new Coupon(COUPON_NAME, discountAmount, minOrderAmount, Category.FOOD,
                    ISSUABLE_PERIOD);

            DiscountAmount validDiscountAmount = new DiscountAmount(2000);
            coupon.updateDiscountAmount(validDiscountAmount);

            Long updatedAmount = coupon.getDiscountAmount().getAmount();
            assertThat(updatedAmount).isEqualTo(2000);
        }
    }

    @Nested
    class 최소_주문_금액_변경 {

        @Test
        void 할인율이_3퍼센트_미만이거나_20퍼센트_초과이면_예외가_발생한다() {
            DiscountAmount discountAmount = new DiscountAmount(2000);
            MinOrderAmount minOrderAmount = new MinOrderAmount(30000);
            Coupon coupon = new Coupon(COUPON_NAME, discountAmount, minOrderAmount, Category.FOOD,
                    ISSUABLE_PERIOD);

            MinOrderAmount invalidMinOrderAmount = new MinOrderAmount(100000);
            assertThatThrownBy(() -> coupon.updateMinOrderAmount(invalidMinOrderAmount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("할인율은 3% 이상 20% 이하여야 합니다.");
        }

        @Test
        void 할인율이_3퍼센트_이상이고_20퍼센트_이하이면_예외가_발생하지_않는다() {
            DiscountAmount discountAmount = new DiscountAmount(2000);
            MinOrderAmount minOrderAmount = new MinOrderAmount(30000);
            Coupon coupon = new Coupon(COUPON_NAME, discountAmount, minOrderAmount, Category.FOOD,
                    ISSUABLE_PERIOD);

            MinOrderAmount validMinOrderAmount = new MinOrderAmount(40000);
            coupon.updateMinOrderAmount(validMinOrderAmount);

            Long updatedAmount = coupon.getMinOderAmount().getAmount();
            assertThat(updatedAmount).isEqualTo(40000);
        }
    }
}

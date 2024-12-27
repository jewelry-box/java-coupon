package coupon.coupon.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import coupon.CouponException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class MinimumOrderAmountTest {

    @DisplayName("최소 주문 금액이 5000원 미만이면 예외가 발생한다.")
    @Test
    void cannotCreateIfMinimumOrderAmountUnder() {
        // given
        int minimumOrderAmount = 4990;

        // when & then
        assertThatThrownBy(() -> new MinimumOrderAmount(minimumOrderAmount))
                .isInstanceOf(CouponException.class)
                .hasMessage("최소 주문 금액은 5000원 이상, 100000원 이하이어야 합니다.");
    }

    @DisplayName("최소 주문 금액이 100000원 초과이면 예외가 발생한다.")
    @Test
    void cannotCreateIfMinimumOrderAmountOver() {
        // given
        int minimumOrderAmount = 100010;

        // when & then
        assertThatThrownBy(() -> new MinimumOrderAmount(minimumOrderAmount))
                .isInstanceOf(CouponException.class)
                .hasMessage("최소 주문 금액은 5000원 이상, 100000원 이하이어야 합니다.");
    }

    @DisplayName("최소 주문 금액을 변경할 수 있다.")
    @Test
    void canChangeMinimumOrderAmount() {
        // given
        MinimumOrderAmount minimumOrderAmount = new MinimumOrderAmount(5000);
        MinimumOrderAmount newMinimumOrderAmount = new MinimumOrderAmount(10000);

        //when
        minimumOrderAmount.change(newMinimumOrderAmount);

        //then
        assertThat(minimumOrderAmount).isEqualTo(newMinimumOrderAmount);
    }
}

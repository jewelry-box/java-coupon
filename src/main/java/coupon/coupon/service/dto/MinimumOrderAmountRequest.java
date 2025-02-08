package coupon.coupon.service.dto;

import coupon.coupon.domain.MinimumOrderAmount;

public record MinimumOrderAmountRequest(long amount) {

    public MinimumOrderAmount toMinimumOrderAmount(){
        return new MinimumOrderAmount(amount);
    }
}

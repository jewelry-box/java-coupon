package coupon.coupon.service.dto;

import coupon.coupon.domain.DiscountAmount;

public record DiscountAmountRequest(long amount) {

    public DiscountAmount toDiscountAmount() {
        return new DiscountAmount(amount);
    }
}

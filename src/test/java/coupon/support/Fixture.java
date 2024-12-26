package coupon.support;

import coupon.domain.Category;
import coupon.domain.Coupon;
import coupon.domain.CouponName;
import coupon.domain.DiscountAmount;
import coupon.domain.IssuablePeriod;
import coupon.domain.Member;
import coupon.domain.MinOrderAmount;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Fixture {

    public static Member createMember() {
        return new Member(UUID.randomUUID().toString(), "1234");
    }

    public static Coupon createCoupon() {
        return createCoupon(
                "반짝 쿠폰",
                1000,
                30000,
                Category.FOOD,
                LocalDateTime.now(),
                LocalDateTime.now().plusMonths(1)
        );
    }

    public static Coupon createCoupon(String couponName) {
        return createCoupon(
                couponName,
                1000,
                30000,
                Category.FOOD,
                LocalDateTime.now(),
                LocalDateTime.now().plusMonths(1)
        );
    }

    public static Coupon createCoupon(long discountAmount, long minOrderAmount) {
        return createCoupon(
                "반짝 쿠폰",
                discountAmount,
                minOrderAmount,
                Category.FOOD,
                LocalDateTime.now(),
                LocalDateTime.now().plusMonths(1)
        );
    }

    public static Coupon createCoupon(
            String couponName,
            long discountAmount,
            long minOrderAmount,
            Category category,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        return new Coupon(
                new CouponName(couponName),
                new DiscountAmount(discountAmount),
                new MinOrderAmount(minOrderAmount),
                category,
                new IssuablePeriod(startAt, endAt)
        );
    }
}

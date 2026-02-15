package com.example.promotionservice.dto.response;

import com.example.promotionservice.domain.entity.DiscountType;
import com.example.promotionservice.domain.entity.UserCoupon;
import com.example.promotionservice.domain.entity.UserCouponStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "쿠폰 등록 응답")
@Getter
@Builder
public class CouponClaimResponse {

    @Schema(description = "사용자 쿠폰 ID", example = "1")
    private Long userCouponId;

    @Schema(description = "쿠폰 코드", example = "WELCOME10")
    private String couponCode;

    @Schema(description = "쿠폰명", example = "신규 가입 환영 쿠폰")
    private String couponName;

    @Schema(description = "할인 유형", example = "RATE")
    private DiscountType discountType;

    @Schema(description = "할인 금액 또는 할인율", example = "10.00")
    private BigDecimal discountValue;

    @Schema(description = "최소 구매 금액", example = "10000.00")
    private BigDecimal minOrderAmount;

    @Schema(description = "최대 할인 금액", example = "5000.00")
    private BigDecimal maxDiscountAmount;

    @Schema(description = "쿠폰 상태", example = "ISSUED")
    private UserCouponStatus couponStatus;

    @Schema(description = "쿠폰 유효 시작 일시", example = "2024-01-01 00:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validFrom;

    @Schema(description = "쿠폰 유효 종료 일시", example = "2024-12-31 23:59:59")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validTo;

    @Schema(description = "발급 일시", example = "2024-01-15 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime issuedAt;

    public static CouponClaimResponse from(UserCoupon userCoupon) {
        return CouponClaimResponse.builder()
                .userCouponId(userCoupon.getId())
                .couponCode(userCoupon.getCoupon().getCouponCode())
                .couponName(userCoupon.getCoupon().getCouponName())
                .discountType(userCoupon.getCoupon().getDiscountType())
                .discountValue(userCoupon.getCoupon().getDiscountValue())
                .minOrderAmount(userCoupon.getCoupon().getMinOrderAmount())
                .maxDiscountAmount(userCoupon.getCoupon().getMaxDiscountAmount())
                .couponStatus(userCoupon.getCouponStatus())
                .validFrom(userCoupon.getCoupon().getValidFrom())
                .validTo(userCoupon.getCoupon().getValidTo())
                .issuedAt(userCoupon.getIssuedAt())
                .build();
    }
}

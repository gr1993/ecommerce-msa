package com.example.promotionservice.dto.response;

import com.example.promotionservice.domain.entity.Coupon;
import com.example.promotionservice.domain.entity.CouponStatus;
import com.example.promotionservice.domain.entity.DiscountType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "관리자 쿠폰 목록 응답")
@Getter
@Builder
public class AdminCouponResponse {

    @Schema(description = "쿠폰 ID", example = "1")
    private Long couponId;

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

    @Schema(description = "쿠폰 시작 일시", example = "2024-01-01 00:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validFrom;

    @Schema(description = "쿠폰 종료 일시", example = "2024-12-31 23:59:59")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validTo;

    @Schema(description = "쿠폰 상태", example = "ACTIVE")
    private CouponStatus status;

    @Schema(description = "발급 수량", example = "245")
    private int issuedCount;

    @Schema(description = "생성 일시", example = "2024-01-01 00:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "수정 일시", example = "2024-01-15 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public static AdminCouponResponse from(Coupon coupon) {
        return AdminCouponResponse.builder()
                .couponId(coupon.getId())
                .couponCode(coupon.getCouponCode())
                .couponName(coupon.getCouponName())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minOrderAmount(coupon.getMinOrderAmount())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .validFrom(coupon.getValidFrom())
                .validTo(coupon.getValidTo())
                .status(coupon.getStatus())
                .issuedCount(coupon.getUserCoupons() != null ? coupon.getUserCoupons().size() : 0)
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .build();
    }
}

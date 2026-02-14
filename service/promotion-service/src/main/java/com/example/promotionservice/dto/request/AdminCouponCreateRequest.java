package com.example.promotionservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "관리자 쿠폰 등록 요청")
@Getter
@NoArgsConstructor
public class AdminCouponCreateRequest {

    @Schema(description = "쿠폰 코드", example = "WELCOME10")
    @NotBlank(message = "쿠폰 코드는 필수입니다")
    @Size(max = 50, message = "쿠폰 코드는 최대 50자까지 입력 가능합니다")
    private String couponCode;

    @Schema(description = "쿠폰명", example = "신규 가입 환영 쿠폰")
    @NotBlank(message = "쿠폰명은 필수입니다")
    @Size(max = 100, message = "쿠폰명은 최대 100자까지 입력 가능합니다")
    private String couponName;

    @Schema(description = "할인 유형 (FIXED, RATE)", example = "RATE")
    @NotBlank(message = "할인 유형은 필수입니다")
    private String discountType;

    @Schema(description = "할인 금액 또는 할인율", example = "10.00")
    @NotNull(message = "할인 값은 필수입니다")
    private BigDecimal discountValue;

    @Schema(description = "최소 구매 금액", example = "10000.00")
    private BigDecimal minOrderAmount;

    @Schema(description = "최대 할인 금액 (정률 할인 시)", example = "5000.00")
    private BigDecimal maxDiscountAmount;

    @Schema(description = "쿠폰 시작 일시", example = "2024-01-01T00:00:00")
    @NotNull(message = "쿠폰 시작 일시는 필수입니다")
    private LocalDateTime validFrom;

    @Schema(description = "쿠폰 종료 일시", example = "2024-12-31T23:59:59")
    @NotNull(message = "쿠폰 종료 일시는 필수입니다")
    private LocalDateTime validTo;

    @Schema(description = "쿠폰 상태 (ACTIVE, INACTIVE)", example = "ACTIVE")
    @NotBlank(message = "쿠폰 상태는 필수입니다")
    private String status;

    @Builder
    public AdminCouponCreateRequest(String couponCode, String couponName, String discountType,
                                    BigDecimal discountValue, BigDecimal minOrderAmount,
                                    BigDecimal maxDiscountAmount, LocalDateTime validFrom,
                                    LocalDateTime validTo, String status) {
        this.couponCode = couponCode;
        this.couponName = couponName;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount;
        this.maxDiscountAmount = maxDiscountAmount;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.status = status;
    }
}

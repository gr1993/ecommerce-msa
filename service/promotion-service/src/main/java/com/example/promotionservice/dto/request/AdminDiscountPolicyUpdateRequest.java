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

@Schema(description = "관리자 할인 정책 수정 요청")
@Getter
@NoArgsConstructor
public class AdminDiscountPolicyUpdateRequest {

    @Schema(description = "할인 정책명", example = "신상품 10% 할인")
    @NotBlank(message = "할인 정책명은 필수입니다")
    @Size(max = 100, message = "할인 정책명은 최대 100자까지 입력 가능합니다")
    private String discountName;

    @Schema(description = "할인 유형 (FIXED, RATE)", example = "RATE")
    @NotBlank(message = "할인 유형은 필수입니다")
    private String discountType;

    @Schema(description = "할인 금액 또는 할인율", example = "10.00")
    @NotNull(message = "할인 값은 필수입니다")
    private BigDecimal discountValue;

    @Schema(description = "적용 대상 (PRODUCT, CATEGORY, ORDER)", example = "PRODUCT")
    @NotBlank(message = "적용 대상은 필수입니다")
    private String targetType;

    @Schema(description = "적용 대상 ID (상품 ID, 카테고리 ID 등)", example = "1")
    private Long targetId;

    @Schema(description = "적용 최소 구매 금액", example = "10000.00")
    private BigDecimal minOrderAmount;

    @Schema(description = "최대 할인 금액 (정률 할인 시)", example = "50000.00")
    private BigDecimal maxDiscountAmount;

    @Schema(description = "할인 시작 일시", example = "2024-01-01T00:00:00")
    @NotNull(message = "할인 시작 일시는 필수입니다")
    private LocalDateTime validFrom;

    @Schema(description = "할인 종료 일시", example = "2024-12-31T23:59:59")
    @NotNull(message = "할인 종료 일시는 필수입니다")
    private LocalDateTime validTo;

    @Schema(description = "할인 상태 (ACTIVE, INACTIVE, EXPIRED)", example = "ACTIVE")
    @NotBlank(message = "할인 상태는 필수입니다")
    private String status;

    @Builder
    public AdminDiscountPolicyUpdateRequest(String discountName, String discountType,
                                            BigDecimal discountValue, String targetType,
                                            Long targetId, BigDecimal minOrderAmount,
                                            BigDecimal maxDiscountAmount, LocalDateTime validFrom,
                                            LocalDateTime validTo, String status) {
        this.discountName = discountName;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.targetType = targetType;
        this.targetId = targetId;
        this.minOrderAmount = minOrderAmount;
        this.maxDiscountAmount = maxDiscountAmount;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.status = status;
    }
}

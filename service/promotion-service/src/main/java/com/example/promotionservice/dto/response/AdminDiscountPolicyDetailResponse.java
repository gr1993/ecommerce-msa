package com.example.promotionservice.dto.response;

import com.example.promotionservice.domain.entity.DiscountPolicy;
import com.example.promotionservice.domain.entity.DiscountPolicyStatus;
import com.example.promotionservice.domain.entity.DiscountTargetType;
import com.example.promotionservice.domain.entity.DiscountType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "관리자 할인 정책 상세 응답")
@Getter
@Builder
public class AdminDiscountPolicyDetailResponse {

    @Schema(description = "할인 정책 ID", example = "1")
    private Long discountId;

    @Schema(description = "할인 정책명", example = "신상품 10% 할인")
    private String discountName;

    @Schema(description = "할인 유형", example = "RATE")
    private DiscountType discountType;

    @Schema(description = "할인 금액 또는 할인율", example = "10.00")
    private BigDecimal discountValue;

    @Schema(description = "적용 대상", example = "PRODUCT")
    private DiscountTargetType targetType;

    @Schema(description = "적용 대상 ID", example = "1")
    private Long targetId;

    @Schema(description = "적용 최소 구매 금액", example = "10000.00")
    private BigDecimal minOrderAmount;

    @Schema(description = "최대 할인 금액", example = "50000.00")
    private BigDecimal maxDiscountAmount;

    @Schema(description = "할인 시작 일시", example = "2024-01-01 00:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validFrom;

    @Schema(description = "할인 종료 일시", example = "2024-12-31 23:59:59")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validTo;

    @Schema(description = "할인 상태", example = "ACTIVE")
    private DiscountPolicyStatus status;

    @Schema(description = "생성 일시", example = "2024-01-01 00:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "수정 일시", example = "2024-01-15 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public static AdminDiscountPolicyDetailResponse from(DiscountPolicy policy) {
        return AdminDiscountPolicyDetailResponse.builder()
                .discountId(policy.getId())
                .discountName(policy.getDiscountName())
                .discountType(policy.getDiscountType())
                .discountValue(policy.getDiscountValue())
                .targetType(policy.getTargetType())
                .targetId(policy.getTargetId())
                .minOrderAmount(policy.getMinOrderAmount())
                .maxDiscountAmount(policy.getMaxDiscountAmount())
                .validFrom(policy.getValidFrom())
                .validTo(policy.getValidTo())
                .status(policy.getStatus())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }
}

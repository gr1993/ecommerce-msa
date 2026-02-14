package com.example.promotionservice.dto.response;

import com.example.promotionservice.domain.entity.UserCoupon;
import com.example.promotionservice.domain.entity.UserCouponStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "쿠폰 발급 내역 응답")
@Getter
@Builder
public class AdminCouponUsageResponse {

    @Schema(description = "사용자 쿠폰 ID", example = "1")
    private Long userCouponId;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "쿠폰 상태", example = "ISSUED")
    private UserCouponStatus couponStatus;

    @Schema(description = "사용 일시", example = "2024-01-10 14:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime usedAt;

    @Schema(description = "발급 일시", example = "2024-01-01 00:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime issuedAt;

    public static AdminCouponUsageResponse from(UserCoupon userCoupon) {
        return AdminCouponUsageResponse.builder()
                .userCouponId(userCoupon.getId())
                .userId(userCoupon.getUserId())
                .couponStatus(userCoupon.getCouponStatus())
                .usedAt(userCoupon.getUsedAt())
                .issuedAt(userCoupon.getIssuedAt())
                .build();
    }
}

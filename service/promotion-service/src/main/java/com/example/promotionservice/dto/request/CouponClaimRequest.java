package com.example.promotionservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "쿠폰 등록 요청")
@Getter
@NoArgsConstructor
public class CouponClaimRequest {

    @Schema(description = "쿠폰 코드", example = "WELCOME10", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "쿠폰 코드는 필수입니다")
    @Size(max = 50, message = "쿠폰 코드는 50자를 초과할 수 없습니다")
    private String couponCode;
}

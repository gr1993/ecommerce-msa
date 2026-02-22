package com.example.shippingservice.returns.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "관리자 반품 거절 요청")
@Getter
@NoArgsConstructor
public class AdminReturnRejectRequest {

    @Schema(description = "거절 사유", example = "반품 기간이 초과되었습니다.")
    private String rejectReason;
}

package com.example.shippingservice.exchange.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "관리자 교환 거절 요청")
@Getter
@NoArgsConstructor
public class AdminExchangeRejectRequest {

    @Schema(description = "거절 사유", example = "교환 불가 상품입니다.")
    private String rejectReason;
}

package com.example.orderservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "교환 신청 요청")
@Getter
@NoArgsConstructor
public class ExchangeOrderRequest {

    @Schema(description = "교환 사유", example = "사이즈 교환")
    private String reason;
}

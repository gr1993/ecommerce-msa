package com.example.orderservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "반품 신청 요청")
@Getter
@NoArgsConstructor
public class ReturnOrderRequest {

    @Schema(description = "반품 사유", example = "상품 불량")
    private String reason;
}

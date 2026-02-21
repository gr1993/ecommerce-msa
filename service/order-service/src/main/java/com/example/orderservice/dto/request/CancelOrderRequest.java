package com.example.orderservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "주문 취소 요청")
@Getter
@NoArgsConstructor
public class CancelOrderRequest {

    @Schema(description = "취소 사유 (생략 시 기본값 사용)", example = "단순 변심")
    private String cancellationReason;
}

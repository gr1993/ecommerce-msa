package com.example.shippingservice.exchange.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "내부 교환 생성 요청 (order-service → shipping-service)")
@Getter
@NoArgsConstructor
public class InternalCreateExchangeRequest {

    @Schema(description = "주문 ID", example = "1")
    @NotNull(message = "주문 ID는 필수입니다.")
    private Long orderId;

    @Schema(description = "사용자 ID", example = "1")
    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;

    @Schema(description = "교환 사유", example = "사이즈 교환")
    private String reason;
}

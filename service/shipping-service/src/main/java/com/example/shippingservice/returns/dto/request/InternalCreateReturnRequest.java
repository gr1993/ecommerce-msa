package com.example.shippingservice.returns.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "내부 반품 생성 요청 (order-service → shipping-service)")
@Getter
@NoArgsConstructor
public class InternalCreateReturnRequest {

    @Schema(description = "주문 ID", example = "1")
    @NotNull(message = "주문 ID는 필수입니다.")
    private Long orderId;

    @Schema(description = "사용자 ID", example = "1")
    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;

    @Schema(description = "반품 사유", example = "상품 불량")
    private String reason;
}

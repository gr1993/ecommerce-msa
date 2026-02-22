package com.example.shippingservice.returns.dto.response;

import com.example.shippingservice.returns.entity.OrderReturn;
import com.example.shippingservice.returns.enums.ReturnStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "내부 반품 생성 응답 (shipping-service → order-service)")
@Getter
@Builder
public class InternalCreateReturnResponse {

    @Schema(description = "반품 ID", example = "1")
    private Long returnId;

    @Schema(description = "주문 ID", example = "1")
    private Long orderId;

    @Schema(description = "반품 상태", example = "RETURN_REQUESTED")
    private ReturnStatus returnStatus;

    @Schema(description = "반품 사유", example = "상품 불량")
    private String reason;

    @Schema(description = "신청 일시")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestedAt;

    public static InternalCreateReturnResponse from(OrderReturn orderReturn) {
        return InternalCreateReturnResponse.builder()
                .returnId(orderReturn.getReturnId())
                .orderId(orderReturn.getOrderId())
                .returnStatus(orderReturn.getReturnStatus())
                .reason(orderReturn.getReason())
                .requestedAt(orderReturn.getRequestedAt())
                .build();
    }
}

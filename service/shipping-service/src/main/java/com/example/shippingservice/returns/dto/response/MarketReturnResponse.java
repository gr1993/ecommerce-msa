package com.example.shippingservice.returns.dto.response;

import com.example.shippingservice.returns.entity.OrderReturn;
import com.example.shippingservice.returns.enums.ReturnStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "사용자 반품 조회 응답")
@Getter
@Builder
public class MarketReturnResponse {

    @Schema(description = "반품 ID", example = "1")
    private Long returnId;

    @Schema(description = "주문 ID", example = "1")
    private Long orderId;

    @Schema(description = "반품 상태", example = "RETURN_REQUESTED")
    private ReturnStatus returnStatus;

    @Schema(description = "반품 사유", example = "상품 불량")
    private String reason;

    @Schema(description = "거절 사유", example = "반품 기간 초과")
    private String rejectReason;

    @Schema(description = "택배사", example = "CJ대한통운")
    private String courier;

    @Schema(description = "운송장 번호", example = "1234567890123")
    private String trackingNumber;

    @Schema(description = "수거지 주소", example = "서울특별시 강남구 물류센터로 1")
    private String returnAddress;

    @Schema(description = "신청 일시")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestedAt;

    @Schema(description = "수정 일시")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public static MarketReturnResponse from(OrderReturn orderReturn) {
        return MarketReturnResponse.builder()
                .returnId(orderReturn.getReturnId())
                .orderId(orderReturn.getOrderId())
                .returnStatus(orderReturn.getReturnStatus())
                .reason(orderReturn.getReason())
                .rejectReason(orderReturn.getRejectReason())
                .courier(orderReturn.getCourier())
                .trackingNumber(orderReturn.getTrackingNumber())
                .returnAddress(orderReturn.getReturnAddress())
                .requestedAt(orderReturn.getRequestedAt())
                .updatedAt(orderReturn.getUpdatedAt())
                .build();
    }
}

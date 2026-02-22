package com.example.shippingservice.exchange.dto.response;

import com.example.shippingservice.exchange.entity.OrderExchange;
import com.example.shippingservice.exchange.enums.ExchangeStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "사용자 교환 조회 응답")
@Getter
@Builder
public class MarketExchangeResponse {

    @Schema(description = "교환 ID", example = "1")
    private Long exchangeId;

    @Schema(description = "주문 ID", example = "1")
    private Long orderId;

    @Schema(description = "교환 상태", example = "EXCHANGE_REQUESTED")
    private ExchangeStatus exchangeStatus;

    @Schema(description = "교환 사유", example = "사이즈 교환")
    private String reason;

    @Schema(description = "거절 사유", example = "교환 불가 상품")
    private String rejectReason;

    @Schema(description = "택배사", example = "CJ대한통운")
    private String courier;

    @Schema(description = "운송장 번호", example = "1234567890123")
    private String trackingNumber;

    @Schema(description = "교환품 배송 주소", example = "서울특별시 강남구 테헤란로 123")
    private String exchangeAddress;

    @Schema(description = "신청 일시")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestedAt;

    @Schema(description = "수정 일시")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public static MarketExchangeResponse from(OrderExchange orderExchange) {
        return MarketExchangeResponse.builder()
                .exchangeId(orderExchange.getExchangeId())
                .orderId(orderExchange.getOrderId())
                .exchangeStatus(orderExchange.getExchangeStatus())
                .reason(orderExchange.getReason())
                .rejectReason(orderExchange.getRejectReason())
                .courier(orderExchange.getCourier())
                .trackingNumber(orderExchange.getTrackingNumber())
                .exchangeAddress(orderExchange.getExchangeAddress())
                .requestedAt(orderExchange.getRequestedAt())
                .updatedAt(orderExchange.getUpdatedAt())
                .build();
    }
}

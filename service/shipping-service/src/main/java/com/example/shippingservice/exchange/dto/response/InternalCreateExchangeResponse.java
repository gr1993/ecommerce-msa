package com.example.shippingservice.exchange.dto.response;

import com.example.shippingservice.exchange.dto.ExchangeItemDto;
import com.example.shippingservice.exchange.entity.OrderExchange;
import com.example.shippingservice.exchange.enums.ExchangeStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Schema(description = "내부 교환 생성 응답 (shipping-service → order-service)")
@Getter
@Builder
public class InternalCreateExchangeResponse {

    @Schema(description = "교환 ID", example = "1")
    private Long exchangeId;

    @Schema(description = "주문 ID", example = "1")
    private Long orderId;

    @Schema(description = "교환 상품 목록")
    private List<ExchangeItemDto> exchangeItems;

    @Schema(description = "교환 상태", example = "EXCHANGE_REQUESTED")
    private ExchangeStatus exchangeStatus;

    @Schema(description = "교환 사유", example = "사이즈 교환")
    private String reason;

    @Schema(description = "신청 일시")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestedAt;

    public static InternalCreateExchangeResponse from(OrderExchange orderExchange) {
        List<ExchangeItemDto> exchangeItems = orderExchange.getExchangeItems().stream()
                .map(item -> ExchangeItemDto.builder()
                        .orderItemId(item.getOrderItemId())
                        .originalOptionId(item.getOriginalOptionId())
                        .newOptionId(item.getNewOptionId())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        return InternalCreateExchangeResponse.builder()
                .exchangeId(orderExchange.getExchangeId())
                .orderId(orderExchange.getOrderId())
                .exchangeItems(exchangeItems)
                .exchangeStatus(orderExchange.getExchangeStatus())
                .reason(orderExchange.getReason())
                .requestedAt(orderExchange.getRequestedAt())
                .build();
    }
}

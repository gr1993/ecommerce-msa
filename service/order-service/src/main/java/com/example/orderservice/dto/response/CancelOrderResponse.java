package com.example.orderservice.dto.response;

import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "주문 취소 응답")
@Getter
@Builder
public class CancelOrderResponse {

    @Schema(description = "주문 ID", example = "1")
    private Long orderId;

    @Schema(description = "주문 번호", example = "ORD-20240115-ABCD1234")
    private String orderNumber;

    @Schema(description = "주문 상태", example = "CANCELED")
    private OrderStatus orderStatus;

    @Schema(description = "취소 사유", example = "단순 변심")
    private String cancellationReason;

    @Schema(description = "취소 처리 일시")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelledAt;

    public static CancelOrderResponse of(Order order, String cancellationReason) {
        return CancelOrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getOrderStatus())
                .cancellationReason(cancellationReason)
                .cancelledAt(LocalDateTime.now())
                .build();
    }
}

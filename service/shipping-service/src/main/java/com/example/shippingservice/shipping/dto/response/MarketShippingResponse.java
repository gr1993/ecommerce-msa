package com.example.shippingservice.shipping.dto.response;

import com.example.shippingservice.shipping.entity.OrderShipping;
import com.example.shippingservice.shipping.enums.DeliveryServiceStatus;
import com.example.shippingservice.shipping.enums.ShippingStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "사용자 배송 조회 응답")
@Getter
@Builder
public class MarketShippingResponse {

    @Schema(description = "배송 ID", example = "1")
    private Long shippingId;

    @Schema(description = "주문 ID", example = "1")
    private Long orderId;

    @Schema(description = "주문 번호", example = "ORD-20240115-ABCD1234")
    private String orderNumber;

    @Schema(description = "배송 상태", example = "SHIPPING")
    private ShippingStatus shippingStatus;

    @Schema(description = "배송사", example = "CJ대한통운")
    private String shippingCompany;

    @Schema(description = "운송장 번호", example = "1234567890123")
    private String trackingNumber;

    @Schema(description = "수령인 이름", example = "홍길동")
    private String receiverName;

    @Schema(description = "수령인 연락처", example = "010-1234-5678")
    private String receiverPhone;

    @Schema(description = "배송 주소", example = "서울특별시 강남구 테헤란로 123")
    private String address;

    @Schema(description = "우편번호", example = "06234")
    private String postalCode;

    @Schema(description = "배송사 연동 상태", example = "IN_TRANSIT")
    private DeliveryServiceStatus deliveryServiceStatus;

    @Schema(description = "생성 일시")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "수정 일시")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public static MarketShippingResponse from(OrderShipping orderShipping) {
        return MarketShippingResponse.builder()
                .shippingId(orderShipping.getShippingId())
                .orderId(orderShipping.getOrderId())
                .orderNumber(orderShipping.getOrderNumber())
                .shippingStatus(orderShipping.getShippingStatus())
                .shippingCompany(orderShipping.getShippingCompany())
                .trackingNumber(orderShipping.getTrackingNumber())
                .receiverName(orderShipping.getReceiverName())
                .receiverPhone(orderShipping.getReceiverPhone())
                .address(orderShipping.getAddress())
                .postalCode(orderShipping.getPostalCode())
                .deliveryServiceStatus(orderShipping.getDeliveryServiceStatus())
                .createdAt(orderShipping.getCreatedAt())
                .updatedAt(orderShipping.getUpdatedAt())
                .build();
    }
}

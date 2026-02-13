package com.example.orderservice.dto.response;

import com.example.orderservice.domain.entity.OrderDelivery;
import com.example.orderservice.domain.entity.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "관리자 주문 배송 정보 응답")
@Getter
@Builder
public class AdminOrderShippingResponse {

    @Schema(description = "배송 ID", example = "1")
    private Long shippingId;

    @Schema(description = "주문 ID", example = "1")
    private Long orderId;

    @Schema(description = "수령인", example = "홍길동")
    private String receiverName;

    @Schema(description = "연락처", example = "010-1234-5678")
    private String receiverPhone;

    @Schema(description = "배송 주소", example = "서울특별시 강남구 테헤란로 123")
    private String address;

    @Schema(description = "우편번호", example = "06234")
    private String postalCode;

    @Schema(description = "배송 상태", example = "READY")
    private String shippingStatus;

    @Schema(description = "생성 일시", example = "2024-01-15 10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public static AdminOrderShippingResponse from(OrderDelivery delivery, OrderStatus orderStatus) {
        String shippingStatus = mapShippingStatus(orderStatus);

        return AdminOrderShippingResponse.builder()
                .shippingId(delivery.getId())
                .orderId(delivery.getOrder().getId())
                .receiverName(delivery.getReceiverName())
                .receiverPhone(delivery.getReceiverPhone())
                .address(buildFullAddress(delivery))
                .postalCode(delivery.getZipcode())
                .shippingStatus(shippingStatus)
                .createdAt(delivery.getCreatedAt())
                .build();
    }

    private static String mapShippingStatus(OrderStatus orderStatus) {
        return switch (orderStatus) {
            case SHIPPING -> "SHIPPING";
            case DELIVERED -> "DELIVERED";
            default -> "READY";
        };
    }

    private static String buildFullAddress(OrderDelivery delivery) {
        if (delivery.getAddressDetail() != null && !delivery.getAddressDetail().isBlank()) {
            return delivery.getAddress() + " " + delivery.getAddressDetail();
        }
        return delivery.getAddress();
    }
}

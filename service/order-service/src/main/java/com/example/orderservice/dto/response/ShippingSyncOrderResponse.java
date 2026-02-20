package com.example.orderservice.dto.response;

import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderDelivery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "배송 동기화용 주문 응답")
public class ShippingSyncOrderResponse {

    @Schema(description = "주문 ID", example = "1")
    private Long orderId;

    @Schema(description = "주문 번호", example = "ORD-20240101-ABC123")
    private String orderNumber;

    @Schema(description = "수령인 이름", example = "홍길동")
    private String receiverName;

    @Schema(description = "수령인 연락처", example = "010-1234-5678")
    private String receiverPhone;

    @Schema(description = "배송 주소 (주소 + 상세주소)", example = "서울시 강남구 테헤란로 123 456호")
    private String address;

    @Schema(description = "우편번호", example = "06234")
    private String postalCode;

    @Schema(description = "배송 메모", example = "부재시 문 앞에 놓아주세요")
    private String deliveryMemo;

    @Schema(description = "주문 일시", example = "2024-01-15T10:30:00")
    private LocalDateTime orderedAt;

    public static ShippingSyncOrderResponse from(Order order) {
        OrderDelivery delivery = order.getOrderDelivery();

        String fullAddress = delivery.getAddress();
        if (delivery.getAddressDetail() != null && !delivery.getAddressDetail().isBlank()) {
            fullAddress = fullAddress + " " + delivery.getAddressDetail();
        }

        return ShippingSyncOrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .receiverName(delivery.getReceiverName())
                .receiverPhone(delivery.getReceiverPhone())
                .address(fullAddress)
                .postalCode(delivery.getZipcode())
                .deliveryMemo(delivery.getDeliveryMemo())
                .orderedAt(order.getOrderedAt())
                .build();
    }
}

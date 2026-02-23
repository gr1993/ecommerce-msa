package com.example.shippingservice.shipping.dto.response;

import com.example.shippingservice.shipping.entity.OrderShipping;
import com.example.shippingservice.shipping.enums.DeliveryServiceStatus;
import com.example.shippingservice.shipping.enums.ShippingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 테스트용 배송 정보 생성 응답 DTO
 */
@Schema(description = "테스트용 배송 정보 생성 응답")
@Getter
@Builder
public class TestCreateShippingResponse {

    @Schema(description = "배송 ID", example = "1")
    private Long shippingId;

    @Schema(description = "주문 ID", example = "1")
    private Long orderId;

    @Schema(description = "주문 번호", example = "ORD-20250223-0001")
    private String orderNumber;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "배송 상태", example = "READY")
    private ShippingStatus shippingStatus;

    @Schema(description = "배송사 연동 상태", example = "NOT_SENT")
    private DeliveryServiceStatus deliveryServiceStatus;

    @Schema(description = "생성일시")
    private LocalDateTime createdAt;

    public static TestCreateShippingResponse from(OrderShipping shipping) {
        return TestCreateShippingResponse.builder()
                .shippingId(shipping.getShippingId())
                .orderId(shipping.getOrderId())
                .orderNumber(shipping.getOrderNumber())
                .userId(shipping.getUserId())
                .shippingStatus(shipping.getShippingStatus())
                .deliveryServiceStatus(shipping.getDeliveryServiceStatus())
                .createdAt(shipping.getCreatedAt())
                .build();
    }
}

package com.example.orderservice.client.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * shipping-service 테스트용 배송 정보 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
public class TestCreateShippingRequest {

    private Long orderId;
    private String orderNumber;
    private Long userId;
    private DeliveryInfo delivery;

    @Builder
    public TestCreateShippingRequest(Long orderId, String orderNumber, Long userId, DeliveryInfo delivery) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.delivery = delivery;
    }

    @Getter
    @NoArgsConstructor
    public static class DeliveryInfo {
        private String receiverName;
        private String receiverPhone;
        private String zipcode;
        private String address;
        private String addressDetail;

        @Builder
        public DeliveryInfo(String receiverName, String receiverPhone, String zipcode,
                            String address, String addressDetail) {
            this.receiverName = receiverName;
            this.receiverPhone = receiverPhone;
            this.zipcode = zipcode;
            this.address = address;
            this.addressDetail = addressDetail;
        }
    }
}

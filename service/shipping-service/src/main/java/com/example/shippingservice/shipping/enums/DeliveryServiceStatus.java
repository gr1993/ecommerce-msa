package com.example.shippingservice.shipping.enums;

public enum DeliveryServiceStatus {
    NOT_SENT,    // 배송사 미전송
    SENT,        // 배송사 전송 완료
    IN_TRANSIT,  // 배송 중 (배송사 연동)
    DELIVERED    // 배송 완료 (배송사 연동)
}

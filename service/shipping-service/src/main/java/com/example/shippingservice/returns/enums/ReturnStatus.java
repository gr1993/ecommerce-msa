package com.example.shippingservice.returns.enums;

public enum ReturnStatus {
    RETURN_REQUESTED,  // 반품 신청
    RETURN_APPROVED,   // 반품 승인
    RETURN_IN_TRANSIT, // 반품 수거 중
    RETURN_REJECTED,   // 반품 거절
    RETURNED           // 반품 완료
}

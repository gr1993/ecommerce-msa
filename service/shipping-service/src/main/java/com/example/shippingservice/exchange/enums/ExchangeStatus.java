package com.example.shippingservice.exchange.enums;

public enum ExchangeStatus {
    EXCHANGE_REQUESTED,          // 교환 신청
    EXCHANGE_APPROVED,           // 교환 승인 (회수/발송 운송장 발급)
    EXCHANGE_REJECTED,           // 교환 거절
    EXCHANGE_COLLECTING,         // 기존 물품 회수 중
    EXCHANGE_RETURN_COMPLETED,   // 회수 완료 및 검수 대기
    EXCHANGE_SHIPPING,           // 새 물품 발송 중
    EXCHANGED                    // 교환 최종 완료
}

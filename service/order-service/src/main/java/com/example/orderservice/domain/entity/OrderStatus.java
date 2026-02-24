package com.example.orderservice.domain.entity;

public enum OrderStatus {
    // 주문 및 기본 배송 상태
    CREATED,
    PAID,
    FAILED,
    SHIPPING,
    DELIVERED,
    CANCELED,

    // 반품(Return) 상태
    RETURN_REQUESTED,
    RETURN_APPROVED,
    RETURN_REJECTED,
    RETURN_IN_TRANSIT,
    RETURNED,

    // 교환(Exchange) 상태
    EXCHANGE_REQUESTED,           // 교환 신청 완료
    EXCHANGE_APPROVED,            // 교환 승인 (회수/발송 운송장 발급)
    EXCHANGE_REJECTED,            // 교환 거절 (DELIVERED 상태로 복구되거나 이력으로 남음)
    EXCHANGE_COLLECTING,          // 기존 물품 회수 중 (택배사가 수거하여 이동 중)
    EXCHANGE_RETURN_COMPLETED,    // 기존 물품 회수 완료 (창고 도착 및 검수 대기)
    EXCHANGE_SHIPPING,            // 새 물품 배송 중
    EXCHANGED                     // 교환 최종 완료 (고객이 새 물품 수령)
}
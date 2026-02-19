package com.example.mockdelivery.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DeliveryStatus {
    PREPARING("배송준비중"),
    IN_TRANSIT("배송중"),
    DELIVERED("배송완료"),
    CANCELLED("취소됨");

    private final String description;
}

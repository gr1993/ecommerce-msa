package com.example.mockdelivery.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DeliveryStatus {
    ACCEPTED("접수완료"),
    PICKED_UP("집하완료"),
    IN_TRANSIT("간선상차"),
    AT_DESTINATION("간선하차"),
    OUT_FOR_DELIVERY("배송출발"),
    DELIVERED("배송완료"),
    CANCELLED("취소됨");

    private final String description;

    public DeliveryStatus next() {
        return switch (this) {
            case ACCEPTED -> PICKED_UP;
            case PICKED_UP -> IN_TRANSIT;
            case IN_TRANSIT -> AT_DESTINATION;
            case AT_DESTINATION -> OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> DELIVERED;
            default -> null;
        };
    }

    public boolean canProgress() {
        return this != DELIVERED && this != CANCELLED;
    }
}

package com.example.orderservice.domain.entity;

public enum OrderStatus {
    CREATED,
    PAID,
    FAILED,
    SHIPPING,
    DELIVERED,
    CANCELED,
    RETURN_REQUESTED,
    RETURN_APPROVED,
    EXCHANGE_REQUESTED,
    RETURNED
}

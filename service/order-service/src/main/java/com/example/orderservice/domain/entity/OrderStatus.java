package com.example.orderservice.domain.entity;

public enum OrderStatus {
    CREATED,
    PAID,
    FAILED,
    SHIPPING,
    DELIVERED,
    CANCELED,
    RETURN_REQUESTED,
    EXCHANGE_REQUESTED
}

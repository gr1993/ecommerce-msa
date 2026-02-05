package com.example.orderservice.global.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String message) {
        super(message);
    }

    public OrderNotFoundException(Long orderId) {
        super("주문을 찾을 수 없습니다. ID: " + orderId);
    }
}

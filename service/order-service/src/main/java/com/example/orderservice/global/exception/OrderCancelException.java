package com.example.orderservice.global.exception;

public class OrderCancelException extends RuntimeException {

    public OrderCancelException(String message) {
        super(message);
    }
}

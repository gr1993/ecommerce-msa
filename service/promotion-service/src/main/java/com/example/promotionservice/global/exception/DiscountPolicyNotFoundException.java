package com.example.promotionservice.global.exception;

public class DiscountPolicyNotFoundException extends RuntimeException {

    public DiscountPolicyNotFoundException(String message) {
        super(message);
    }

    public DiscountPolicyNotFoundException(Long discountId) {
        super("할인 정책을 찾을 수 없습니다. ID: " + discountId);
    }
}

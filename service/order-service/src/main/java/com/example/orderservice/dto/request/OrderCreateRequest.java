package com.example.orderservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
public class OrderCreateRequest {

    @NotNull(message = "사용자 ID는 필수입니다")
    private Long userId;

    @NotEmpty(message = "주문 상품은 1개 이상이어야 합니다")
    @Valid
    private List<OrderItemRequest> orderItems;

    private BigDecimal discountAmount;

    private String orderMemo;

    @Builder
    public OrderCreateRequest(Long userId, List<OrderItemRequest> orderItems,
                              BigDecimal discountAmount, String orderMemo) {
        this.userId = userId;
        this.orderItems = orderItems;
        this.discountAmount = discountAmount;
        this.orderMemo = orderMemo;
    }
}

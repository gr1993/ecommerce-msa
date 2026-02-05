package com.example.orderservice.dto.response;

import com.example.orderservice.domain.entity.OrderItem;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class OrderItemResponse {

    private Long orderItemId;
    private Long productId;
    private Long skuId;
    private String productName;
    private String productCode;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;

    public static OrderItemResponse from(OrderItem orderItem) {
        return OrderItemResponse.builder()
                .orderItemId(orderItem.getId())
                .productId(orderItem.getProductId())
                .skuId(orderItem.getSkuId())
                .productName(orderItem.getProductName())
                .productCode(orderItem.getProductCode())
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .totalPrice(orderItem.getTotalPrice())
                .build();
    }
}

package com.example.orderservice.dto.response;

import com.example.orderservice.domain.entity.OrderItem;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Schema(description = "내 주문 상품 응답")
@Getter
@Builder
public class MyOrderItemResponse {

    @Schema(description = "주문 상품 ID", example = "1")
    private Long orderItemId;

    @Schema(description = "상품 ID", example = "1")
    private Long productId;

    @Schema(description = "상품명", example = "프리미엄 노트북")
    private String productName;

    @Schema(description = "상품 코드", example = "PRD-001")
    private String productCode;

    @Schema(description = "수량", example = "2")
    private Integer quantity;

    @Schema(description = "단가", example = "50000")
    private BigDecimal unitPrice;

    @Schema(description = "총 금액", example = "100000")
    private BigDecimal totalPrice;

    public static MyOrderItemResponse from(OrderItem orderItem) {
        return MyOrderItemResponse.builder()
                .orderItemId(orderItem.getId())
                .productId(orderItem.getProductId())
                .productName(orderItem.getProductName())
                .productCode(orderItem.getProductCode())
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .totalPrice(orderItem.getTotalPrice())
                .build();
    }
}

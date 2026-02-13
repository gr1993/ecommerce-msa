package com.example.orderservice.dto.response;

import com.example.orderservice.domain.entity.OrderItem;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "관리자 주문 상품 응답")
@Getter
@Builder
public class AdminOrderItemResponse {

    @Schema(description = "주문 상품 ID", example = "1")
    private Long orderItemId;

    @Schema(description = "주문 ID", example = "1")
    private Long orderId;

    @Schema(description = "상품 ID", example = "1")
    private Long productId;

    @Schema(description = "상품명", example = "노트북")
    private String productName;

    @Schema(description = "상품 코드", example = "PRD-001")
    private String productCode;

    @Schema(description = "수량", example = "1")
    private Integer quantity;

    @Schema(description = "단가", example = "150000")
    private BigDecimal unitPrice;

    @Schema(description = "총 금액", example = "150000")
    private BigDecimal totalPrice;

    @Schema(description = "생성 일시", example = "2024-01-15 10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public static AdminOrderItemResponse from(OrderItem item) {
        return AdminOrderItemResponse.builder()
                .orderItemId(item.getId())
                .orderId(item.getOrder().getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .productCode(item.getProductCode())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .createdAt(item.getCreatedAt())
                .build();
    }
}

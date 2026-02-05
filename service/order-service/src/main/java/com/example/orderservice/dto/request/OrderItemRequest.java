package com.example.orderservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class OrderItemRequest {

    @NotNull(message = "상품 ID는 필수입니다")
    private Long productId;

    @NotNull(message = "SKU ID는 필수입니다")
    private Long skuId;

    @NotBlank(message = "상품명은 필수입니다")
    private String productName;

    private String productCode;

    @NotNull(message = "수량은 필수입니다")
    @Min(value = 1, message = "수량은 1개 이상이어야 합니다")
    private Integer quantity;

    @NotNull(message = "단가는 필수입니다")
    private BigDecimal unitPrice;

    @Builder
    public OrderItemRequest(Long productId, Long skuId, String productName,
                            String productCode, Integer quantity, BigDecimal unitPrice) {
        this.productId = productId;
        this.skuId = skuId;
        this.productName = productName;
        this.productCode = productCode;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }
}

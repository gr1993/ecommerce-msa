package com.example.orderservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "주문 상품 요청")
@Getter
@NoArgsConstructor
public class OrderItemRequest {

    @Schema(description = "상품 ID", example = "456")
    @NotNull(message = "상품 ID는 필수입니다")
    private Long productId;

    @Schema(description = "SKU ID", example = "789")
    @NotNull(message = "SKU ID는 필수입니다")
    private Long skuId;

    @Schema(description = "수량", example = "2")
    @NotNull(message = "수량은 필수입니다")
    @Min(value = 1, message = "수량은 1개 이상이어야 합니다")
    private Integer quantity;

    @Builder
    public OrderItemRequest(Long productId, Long skuId, Integer quantity) {
        this.productId = productId;
        this.skuId = skuId;
        this.quantity = quantity;
    }
}

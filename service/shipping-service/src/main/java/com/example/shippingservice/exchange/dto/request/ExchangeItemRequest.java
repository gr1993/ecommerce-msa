package com.example.shippingservice.exchange.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "교환 상품 정보")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeItemRequest {

    @Schema(description = "주문 상품 ID", example = "1")
    @NotNull(message = "주문 상품 ID는 필수입니다.")
    private Long orderItemId;

    @Schema(description = "기존 옵션 ID (반품할 상품 옵션)", example = "10")
    @NotNull(message = "기존 옵션 ID는 필수입니다.")
    private Long originalOptionId;

    @Schema(description = "새 옵션 ID (교환받을 상품 옵션, 동일 옵션 교환 시 기존 옵션 ID와 같음)", example = "11")
    @NotNull(message = "새 옵션 ID는 필수입니다.")
    private Long newOptionId;

    @Schema(description = "교환 수량", example = "1")
    @Min(value = 1, message = "교환 수량은 1 이상이어야 합니다.")
    private Integer quantity;
}

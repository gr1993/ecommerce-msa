package com.example.productservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "SKU 요청")
public class SkuRequest {

    @Schema(description = "프론트 임시 ID (매핑용)", example = "sku_1234567890")
    private String id;

    @NotBlank(message = "SKU 코드는 필수입니다")
    @Schema(description = "SKU 코드", example = "SKU-001", required = true)
    private String skuCode;

    @NotNull(message = "가격은 필수입니다")
    @Min(value = 0, message = "가격은 0 이상이어야 합니다")
    @Schema(description = "가격", example = "120000", required = true)
    private BigDecimal price;

    @NotNull(message = "재고 수량은 필수입니다")
    @Min(value = 0, message = "재고 수량은 0 이상이어야 합니다")
    @Schema(description = "재고 수량", example = "100", required = true)
    private Integer stockQty;

    @NotBlank(message = "상태는 필수입니다")
    @Schema(description = "상태 (ACTIVE, SOLD_OUT, INACTIVE)", example = "ACTIVE", required = true)
    private String status;

    @Schema(description = "옵션 값 ID 목록 (프론트 임시 ID)", example = "[\"value_1\", \"value_2\"]")
    @Builder.Default
    private List<String> optionValueIds = new ArrayList<>();
}

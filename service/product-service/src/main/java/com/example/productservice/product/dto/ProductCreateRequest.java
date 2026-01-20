package com.example.productservice.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "상품 생성 요청")
public class ProductCreateRequest {

    @NotBlank(message = "상품명은 필수입니다")
    @Schema(description = "상품명", example = "나이키 에어맥스", required = true)
    private String productName;

    @Schema(description = "상품 코드", example = "NIKE-001")
    private String productCode;

    @Schema(description = "상품 상세 설명", example = "편안한 운동화")
    private String description;

    @NotNull(message = "기본 가격은 필수입니다")
    @Min(value = 0, message = "가격은 0 이상이어야 합니다")
    @Schema(description = "기본 가격", example = "150000", required = true)
    private BigDecimal basePrice;

    @Min(value = 0, message = "가격은 0 이상이어야 합니다")
    @Schema(description = "할인 가격", example = "120000")
    private BigDecimal salePrice;

    @NotBlank(message = "상태는 필수입니다")
    @Schema(description = "상품 상태 (ACTIVE, INACTIVE, SOLD_OUT)", example = "ACTIVE", required = true)
    private String status;

    @Schema(description = "진열 여부", example = "true")
    private Boolean isDisplayed = true;

    @Schema(description = "카테고리 ID 목록", example = "[1, 2, 3]")
    @Builder.Default
    private Set<Long> categoryIds = new HashSet<>();

    @Valid
    @Schema(description = "옵션 그룹 목록")
    @Builder.Default
    private List<OptionGroupRequest> optionGroups = new ArrayList<>();

    @Valid
    @Schema(
            description = "SKU 목록",
            example = """
            [
              {
                "skuCode": "SKU-001",
                "price": 120000,
                "stockQty": 100,
                "status": "ACTIVE",
                "optionValueIds": [
                  "value_1"
                ]
              },
              {
                "skuCode": "SKU-002",
                "price": 120000,
                "stockQty": 90,
                "status": "ACTIVE",
                "optionValueIds": [
                  "value_2"
                ]
              }
            ]
            """
    )
    @Builder.Default
    private List<SkuRequest> skus = new ArrayList<>();

    @Valid
    @Schema(description = "이미지 목록")
    @Builder.Default
    private List<ProductImageRequest> images = new ArrayList<>();
}

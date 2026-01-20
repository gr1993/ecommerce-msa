package com.example.productservice.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "상품 검색 요청")
public class ProductSearchRequest {

    @Schema(description = "상품명 (부분 검색)", example = "나이키")
    private String productName;

    @Schema(description = "상품 코드", example = "PROD-001")
    private String productCode;

    @Schema(description = "상품 상태 (ACTIVE, INACTIVE, SOLD_OUT)", example = "ACTIVE")
    private String status;

    @Schema(description = "진열 여부", example = "true")
    private Boolean isDisplayed;

    @Schema(description = "카테고리 ID", example = "1")
    private Long categoryId;

    @Schema(description = "최소 가격", example = "10000")
    private Double minPrice;

    @Schema(description = "최대 가격", example = "100000")
    private Double maxPrice;

    @Schema(description = "페이지 번호 (0부터 시작)", example = "0")
    private Integer page = 0;

    @Schema(description = "페이지 크기", example = "10")
    private Integer size = 10;

    @Schema(description = "정렬 기준 (예: createdAt,desc)", example = "createdAt,desc")
    private String sort;
}

package com.example.productservice.product.dto;

import com.example.productservice.category.domain.Category;
import com.example.productservice.product.domain.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "카탈로그 동기화용 상품 응답")
public class CatalogSyncProductResponse {

    @Schema(description = "상품 ID", example = "1")
    private Long productId;

    @Schema(description = "상품명", example = "나이키 에어맥스")
    private String productName;

    @Schema(description = "상품 설명", example = "편안한 운동화")
    private String description;

    @Schema(description = "기본 가격", example = "150000")
    private Long basePrice;

    @Schema(description = "할인 가격", example = "120000")
    private Long salePrice;

    @Schema(description = "상품 상태", example = "ACTIVE")
    private String status;

    @Schema(description = "대표 이미지 URL", example = "https://example.com/image.jpg")
    private String primaryImageUrl;

    @Schema(description = "카테고리 ID 목록", example = "[1, 2, 3]")
    private List<Long> categoryIds;

    @Schema(description = "검색 키워드 목록", example = "[\"운동화\", \"나이키\", \"에어맥스\"]")
    private List<String> searchKeywords;

    @Schema(description = "SKU 목록")
    private List<SkuSnapshot> skus;

    @Schema(description = "생성일시", example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2024-01-01T10:00:00")
    private LocalDateTime updatedAt;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "SKU 스냅샷")
    public static class SkuSnapshot {
        @Schema(description = "SKU ID", example = "1")
        private Long skuId;

        @Schema(description = "SKU 코드", example = "SKU-001")
        private String skuCode;

        @Schema(description = "가격", example = "120000")
        private BigDecimal price;

        @Schema(description = "재고 수량", example = "50")
        private Integer stockQty;

        @Schema(description = "상태", example = "ACTIVE")
        private String status;
    }

    public static CatalogSyncProductResponse from(Product product, List<String> searchKeywords) {
        String primaryImage = product.getImages().stream()
                .filter(image -> image.getIsPrimary() != null && image.getIsPrimary())
                .findFirst()
                .map(image -> image.getImageUrl())
                .orElse(null);

        List<Long> categoryIds = product.getCategories().stream()
                .map(Category::getCategoryId)
                .collect(Collectors.toList());

        List<SkuSnapshot> skuSnapshots = product.getSkus().stream()
                .map(sku -> SkuSnapshot.builder()
                        .skuId(sku.getSkuId())
                        .skuCode(sku.getSkuCode())
                        .price(sku.getPrice())
                        .stockQty(sku.getStockQty())
                        .status(sku.getStatus())
                        .build())
                .collect(Collectors.toList());

        return CatalogSyncProductResponse.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .description(product.getDescription())
                .basePrice(product.getBasePrice() != null ? product.getBasePrice().longValue() : null)
                .salePrice(product.getSalePrice() != null ? product.getSalePrice().longValue() : null)
                .status(product.getStatus())
                .primaryImageUrl(primaryImage)
                .categoryIds(categoryIds)
                .searchKeywords(searchKeywords)
                .skus(skuSnapshots)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}

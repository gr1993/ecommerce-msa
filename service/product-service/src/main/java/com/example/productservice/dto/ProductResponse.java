package com.example.productservice.dto;

import com.example.productservice.domain.entity.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "상품 응답")
public class ProductResponse {

    @Schema(description = "상품 ID", example = "1")
    private Long productId;

    @Schema(description = "상품명", example = "나이키 에어맥스")
    private String productName;

    @Schema(description = "상품 코드", example = "PROD-001")
    private String productCode;

    @Schema(description = "상품 설명", example = "편안한 운동화")
    private String description;

    @Schema(description = "기본 가격", example = "150000")
    private BigDecimal basePrice;

    @Schema(description = "할인 가격", example = "120000")
    private BigDecimal salePrice;

    @Schema(description = "상품 상태", example = "ACTIVE")
    private String status;

    @Schema(description = "진열 여부", example = "true")
    private Boolean isDisplayed;

    @Schema(description = "대표 이미지 URL", example = "https://example.com/image.jpg")
    private String primaryImageUrl;

    @Schema(description = "총 재고 수량", example = "100")
    private Integer totalStockQty;

    @Schema(description = "생성일시", example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2024-01-01T10:00:00")
    private LocalDateTime updatedAt;

    public static ProductResponse from(Product product) {
        Integer totalStock = product.getSkus().stream()
                .mapToInt(sku -> sku.getStockQty() != null ? sku.getStockQty() : 0)
                .sum();

        String primaryImage = product.getImages().stream()
                .filter(image -> image.getIsPrimary() != null && image.getIsPrimary())
                .findFirst()
                .map(image -> image.getImageUrl())
                .orElse(null);

        return ProductResponse.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .productCode(product.getProductCode())
                .description(product.getDescription())
                .basePrice(product.getBasePrice())
                .salePrice(product.getSalePrice())
                .status(product.getStatus())
                .isDisplayed(product.getIsDisplayed())
                .primaryImageUrl(primaryImage)
                .totalStockQty(totalStock)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}

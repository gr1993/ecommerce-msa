package com.example.catalogservice.controller.dto;

import com.example.catalogservice.domain.document.ProductDocument;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ProductDetailResponse {

    private String productId;
    private String productName;
    private String description;
    private Long basePrice;
    private Long salePrice;
    private String status;
    private String primaryImageUrl;
    private List<Long> categoryIds;
    private List<String> searchKeywords;
    private List<SkuInfoResponse> skus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    public static class SkuInfoResponse {
        private Long skuId;
        private String skuCode;
        private Long price;
        private Integer stockQty;
        private String status;

        public static SkuInfoResponse from(ProductDocument.SkuInfo skuInfo) {
            return SkuInfoResponse.builder()
                    .skuId(skuInfo.getSkuId())
                    .skuCode(skuInfo.getSkuCode())
                    .price(skuInfo.getPrice())
                    .stockQty(skuInfo.getStockQty())
                    .status(skuInfo.getStatus())
                    .build();
        }
    }

    public static ProductDetailResponse from(ProductDocument document) {
        return ProductDetailResponse.builder()
                .productId(document.getProductId())
                .productName(document.getProductName())
                .description(document.getDescription())
                .basePrice(document.getBasePrice())
                .salePrice(document.getSalePrice())
                .status(document.getStatus())
                .primaryImageUrl(document.getPrimaryImageUrl())
                .categoryIds(document.getCategoryIds())
                .searchKeywords(document.getSearchKeywords())
                .skus(document.getSkus() != null
                        ? document.getSkus().stream().map(SkuInfoResponse::from).toList()
                        : null)
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}

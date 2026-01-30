package com.example.catalogservice.client.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CatalogSyncProductResponse {

    private Long productId;
    private String productName;
    private String description;
    private Long basePrice;
    private Long salePrice;
    private String status;
    private String primaryImageUrl;
    private List<Long> categoryIds;
    private List<String> searchKeywords;
    private List<SkuSnapshot> skus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class SkuSnapshot {
        private Long skuId;
        private String skuCode;
        private BigDecimal price;
        private Integer stockQty;
        private String status;
    }
}

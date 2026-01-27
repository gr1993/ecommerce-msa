package com.example.catalogservice.controller.dto;

import com.example.catalogservice.domain.document.ProductDocument;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ProductResponse {

    private String productId;
    private String productName;
    private String description;
    private Long basePrice;
    private Long salePrice;
    private String status;
    private String primaryImageUrl;
    private List<Long> categoryIds;
    private List<String> searchKeywords;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductResponse from(ProductDocument document) {
        return ProductResponse.builder()
                .productId(document.getProductId())
                .productName(document.getProductName())
                .description(document.getDescription())
                .basePrice(document.getBasePrice())
                .salePrice(document.getSalePrice())
                .status(document.getStatus())
                .primaryImageUrl(document.getPrimaryImageUrl())
                .categoryIds(document.getCategoryIds())
                .searchKeywords(document.getSearchKeywords())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}

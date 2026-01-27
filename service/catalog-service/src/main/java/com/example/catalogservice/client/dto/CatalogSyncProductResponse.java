package com.example.catalogservice.client.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

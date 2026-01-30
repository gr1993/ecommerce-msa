package com.example.catalogservice.consumer.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreatedEvent {

    private Long productId;
    private String productCode;
    private String productName;
    private String description;
    private BigDecimal basePrice;
    private BigDecimal salePrice;
    private String status;
    private Boolean isDisplayed;
    private String primaryImageUrl;
    private List<Long> categoryIds;
    private List<SkuSnapshot> skus;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkuSnapshot {
        private Long skuId;
        private String skuCode;
        private BigDecimal price;
        private Integer stockQty;
        private String status;
    }
}

package com.example.orderservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailResponse {

    private Long productId;
    private String productName;
    private String productCode;
    private String description;
    private Long basePrice;
    private Long salePrice;
    private String status;
    private Boolean isDisplayed;
    private List<OptionGroupResponse> optionGroups;
    private List<SkuResponse> skus;
    private List<ImageResponse> images;
    private List<CategoryResponse> categories;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionGroupResponse {
        private Long id;
        private String optionGroupName;
        private Integer displayOrder;
        private List<OptionValueResponse> optionValues;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionValueResponse {
        private Long id;
        private String optionValueName;
        private Integer displayOrder;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkuResponse {
        private Long id;
        private String skuCode;
        private Long price;
        private Integer stockQty;
        private String status;
        private List<Long> optionValueIds;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageResponse {
        private Long id;
        private Long fileId;
        private String imageUrl;
        private Boolean isPrimary;
        private Integer displayOrder;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryResponse {
        private Long categoryId;
        private String categoryName;
        private Integer displayOrder;
    }
}
package com.example.productservice.dto;

import com.example.productservice.domain.entity.*;
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
@Schema(description = "상품 상세 응답")
public class ProductDetailResponse {

    @Schema(description = "상품 ID", example = "1")
    private Long productId;

    @Schema(description = "상품명", example = "노트북")
    private String productName;

    @Schema(description = "상품 코드", example = "PRD-001")
    private String productCode;

    @Schema(description = "상품 설명", example = "고성능 노트북입니다.")
    private String description;

    @Schema(description = "기본 가격", example = "1200000")
    private BigDecimal basePrice;

    @Schema(description = "할인 가격", example = "1000000")
    private BigDecimal salePrice;

    @Schema(description = "상품 상태", example = "ACTIVE")
    private String status;

    @Schema(description = "진열 여부", example = "true")
    private Boolean isDisplayed;

    @Schema(description = "옵션 그룹 목록")
    private List<OptionGroupResponse> optionGroups;

    @Schema(description = "SKU 목록")
    private List<SkuResponse> skus;

    @Schema(description = "이미지 목록")
    private List<ImageResponse> images;

    @Schema(description = "생성일시")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시")
    private LocalDateTime updatedAt;

    public static ProductDetailResponse from(Product product) {
        return ProductDetailResponse.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .productCode(product.getProductCode())
                .description(product.getDescription())
                .basePrice(product.getBasePrice())
                .salePrice(product.getSalePrice())
                .status(product.getStatus())
                .isDisplayed(product.getIsDisplayed())
                .optionGroups(product.getOptionGroups().stream()
                        .map(OptionGroupResponse::from)
                        .collect(Collectors.toList()))
                .skus(product.getSkus().stream()
                        .map(SkuResponse::from)
                        .collect(Collectors.toList()))
                .images(product.getImages().stream()
                        .map(ImageResponse::from)
                        .collect(Collectors.toList()))
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "옵션 그룹 응답")
    public static class OptionGroupResponse {

        @Schema(description = "옵션 그룹 ID", example = "1")
        private Long id;

        @Schema(description = "옵션 그룹명", example = "색상")
        private String optionGroupName;

        @Schema(description = "표시 순서", example = "0")
        private Integer displayOrder;

        @Schema(description = "옵션 값 목록")
        private List<OptionValueResponse> optionValues;

        public static OptionGroupResponse from(ProductOptionGroup optionGroup) {
            return OptionGroupResponse.builder()
                    .id(optionGroup.getOptionGroupId())
                    .optionGroupName(optionGroup.getOptionGroupName())
                    .displayOrder(optionGroup.getDisplayOrder())
                    .optionValues(optionGroup.getOptionValues().stream()
                            .map(OptionValueResponse::from)
                            .collect(Collectors.toList()))
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "옵션 값 응답")
    public static class OptionValueResponse {

        @Schema(description = "옵션 값 ID", example = "1")
        private Long id;

        @Schema(description = "옵션 값명", example = "Black")
        private String optionValueName;

        @Schema(description = "표시 순서", example = "0")
        private Integer displayOrder;

        public static OptionValueResponse from(ProductOptionValue optionValue) {
            return OptionValueResponse.builder()
                    .id(optionValue.getOptionValueId())
                    .optionValueName(optionValue.getOptionValueName())
                    .displayOrder(optionValue.getDisplayOrder())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "SKU 응답")
    public static class SkuResponse {

        @Schema(description = "SKU ID", example = "1")
        private Long id;

        @Schema(description = "SKU 코드", example = "SKU-001")
        private String skuCode;

        @Schema(description = "가격", example = "1200000")
        private BigDecimal price;

        @Schema(description = "재고 수량", example = "10")
        private Integer stockQty;

        @Schema(description = "상태", example = "ACTIVE")
        private String status;

        @Schema(description = "옵션 값 ID 목록")
        private List<Long> optionValueIds;

        public static SkuResponse from(ProductSku sku) {
            List<Long> optionValueIds = sku.getSkuOptions().stream()
                    .map(skuOption -> skuOption.getOptionValue().getOptionValueId())
                    .collect(Collectors.toList());

            return SkuResponse.builder()
                    .id(sku.getSkuId())
                    .skuCode(sku.getSkuCode())
                    .price(sku.getPrice())
                    .stockQty(sku.getStockQty())
                    .status(sku.getStatus())
                    .optionValueIds(optionValueIds)
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "이미지 응답")
    public static class ImageResponse {

        @Schema(description = "이미지 ID", example = "1")
        private Long id;

        @Schema(description = "파일 ID", example = "123")
        private Long fileId;

        @Schema(description = "이미지 URL", example = "/files/images/product.jpg")
        private String imageUrl;

        @Schema(description = "대표 이미지 여부", example = "true")
        private Boolean isPrimary;

        @Schema(description = "표시 순서", example = "0")
        private Integer displayOrder;

        public static ImageResponse from(ProductImage image) {
            return ImageResponse.builder()
                    .id(image.getImageId())
                    .fileId(image.getFileId())
                    .imageUrl(image.getImageUrl())
                    .isPrimary(image.getIsPrimary())
                    .displayOrder(image.getDisplayOrder())
                    .build();
        }
    }
}

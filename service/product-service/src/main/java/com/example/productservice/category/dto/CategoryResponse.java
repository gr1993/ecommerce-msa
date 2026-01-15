package com.example.productservice.category.dto;

import com.example.productservice.category.domain.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "카테고리 응답")
public class CategoryResponse {

    @Schema(description = "카테고리 ID", example = "1")
    private Long categoryId;

    @Schema(description = "상위 카테고리 ID", example = "null")
    private Long parentId;

    @Schema(description = "카테고리명", example = "상의")
    private String categoryName;

    @Schema(description = "전시 순서", example = "1")
    private Integer displayOrder;

    @Schema(description = "전시 여부", example = "true")
    private Boolean isDisplayed;

    @Schema(description = "생성일시")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시")
    private LocalDateTime updatedAt;

    public static CategoryResponse from(Category category) {
        return CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .parentId(category.getParent() != null ? category.getParent().getCategoryId() : null)
                .categoryName(category.getCategoryName())
                .displayOrder(category.getDisplayOrder())
                .isDisplayed(category.getIsDisplayed())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}

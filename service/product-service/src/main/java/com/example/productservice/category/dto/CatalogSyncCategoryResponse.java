package com.example.productservice.category.dto;

import com.example.productservice.category.domain.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "카탈로그 동기화용 카테고리 응답")
public class CatalogSyncCategoryResponse {

    @Schema(description = "카테고리 ID", example = "1")
    private Long categoryId;

    @Schema(description = "상위 카테고리 ID", example = "null")
    private Long parentId;

    @Schema(description = "카테고리명", example = "상의")
    private String categoryName;

    @Schema(description = "전시 순서", example = "1")
    private Integer displayOrder;

    @Schema(description = "카테고리 깊이 (1단계부터 시작)", example = "1")
    private Integer depth;

    @Schema(description = "생성일시", example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2024-01-01T10:00:00")
    private LocalDateTime updatedAt;

    public static CatalogSyncCategoryResponse from(Category category) {
        return CatalogSyncCategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .parentId(category.getParent() != null ? category.getParent().getCategoryId() : null)
                .categoryName(category.getCategoryName())
                .displayOrder(category.getDisplayOrder())
                .depth(calculateDepth(category))
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    private static int calculateDepth(Category category) {
        int depth = 1;
        Category parent = category.getParent();
        while (parent != null) {
            depth++;
            parent = parent.getParent();
        }
        return depth;
    }
}

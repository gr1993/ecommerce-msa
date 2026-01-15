package com.example.productservice.category.dto;

import com.example.productservice.category.domain.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "카테고리 트리 응답")
public class CategoryTreeResponse {

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

    @Schema(description = "카테고리 깊이 (1단계부터 시작)", example = "1")
    private Integer depth;

    @Schema(description = "하위 카테고리 목록")
    private List<CategoryTreeResponse> children;

    public static CategoryTreeResponse from(Category category, int depth) {
        List<CategoryTreeResponse> childResponses = null;

        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            childResponses = category.getChildren().stream()
                    .sorted(Comparator.comparing(Category::getDisplayOrder))
                    .map(child -> CategoryTreeResponse.from(child, depth + 1))
                    .collect(Collectors.toList());
        }

        return CategoryTreeResponse.builder()
                .categoryId(category.getCategoryId())
                .parentId(category.getParent() != null ? category.getParent().getCategoryId() : null)
                .categoryName(category.getCategoryName())
                .displayOrder(category.getDisplayOrder())
                .isDisplayed(category.getIsDisplayed())
                .depth(depth)
                .children(childResponses)
                .build();
    }
}

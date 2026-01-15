package com.example.productservice.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "카테고리 등록 요청")
public class CategoryCreateRequest {

    @Schema(description = "상위 카테고리 ID (최상위 카테고리인 경우 null)", example = "1")
    private Long parentId;

    @NotBlank(message = "카테고리명은 필수입니다")
    @Size(max = 100, message = "카테고리명은 100자를 초과할 수 없습니다")
    @Schema(description = "카테고리명", example = "상의", required = true)
    private String categoryName;

    @Schema(description = "전시 순서", example = "1", defaultValue = "0")
    @Builder.Default
    private Integer displayOrder = 0;

    @Schema(description = "전시 여부", example = "true", defaultValue = "true")
    @Builder.Default
    private Boolean isDisplayed = true;
}

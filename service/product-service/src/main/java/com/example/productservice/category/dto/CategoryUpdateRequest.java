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
@Schema(description = "카테고리 수정 요청")
public class CategoryUpdateRequest {

    @NotBlank(message = "카테고리명은 필수입니다")
    @Size(max = 100, message = "카테고리명은 100자를 초과할 수 없습니다")
    @Schema(description = "카테고리명", example = "상의", required = true)
    private String categoryName;

    @Schema(description = "전시 순서", example = "1")
    private Integer displayOrder;

    @Schema(description = "전시 여부", example = "true")
    private Boolean isDisplayed;
}

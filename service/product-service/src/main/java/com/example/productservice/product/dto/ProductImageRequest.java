package com.example.productservice.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "상품 이미지 요청")
public class ProductImageRequest {

    @Schema(description = "파일 업로드 ID", example = "123")
    private Long fileId;

    @Schema(description = "대표 이미지 여부", example = "true")
    private Boolean isPrimary = false;

    @Schema(description = "정렬 순서", example = "0")
    private Integer displayOrder = 0;
}

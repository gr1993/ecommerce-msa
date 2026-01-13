package com.example.productservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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

    @Schema(description = "프론트 임시 ID (매핑용)", example = "img_1234567890")
    private String id;

    @NotBlank(message = "이미지 URL은 필수입니다")
    @Schema(description = "이미지 URL", example = "https://example.com/image.jpg", required = true)
    private String imageUrl;

    @Schema(description = "대표 이미지 여부", example = "true")
    private Boolean isPrimary = false;

    @Schema(description = "정렬 순서", example = "0")
    private Integer displayOrder = 0;
}

package com.example.productservice.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "검색 키워드 등록 요청")
public class SearchKeywordRequest {

    @NotBlank(message = "키워드는 필수 입력값입니다.")
    @Size(max = 100, message = "키워드는 최대 100자까지 입력 가능합니다.")
    @Schema(description = "검색 키워드", example = "랩탑", maxLength = 100)
    private String keyword;
}

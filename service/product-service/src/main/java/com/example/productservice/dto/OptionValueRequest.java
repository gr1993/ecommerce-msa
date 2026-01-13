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
@Schema(description = "옵션 값 요청")
public class OptionValueRequest {

    @Schema(description = "프론트 임시 ID (매핑용)", example = "value_1234567890")
    private String id;

    @NotBlank(message = "옵션 값명은 필수입니다")
    @Schema(description = "옵션 값명", example = "Red", required = true)
    private String optionValueName;

    @Schema(description = "정렬 순서", example = "0")
    private Integer displayOrder = 0;
}

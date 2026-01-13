package com.example.productservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "옵션 그룹 요청")
public class OptionGroupRequest {

    @Schema(description = "프론트 임시 ID (매핑용)", example = "group_1234567890")
    private String id;

    @NotBlank(message = "옵션 그룹명은 필수입니다")
    @Schema(description = "옵션 그룹명", example = "색상", required = true)
    private String optionGroupName;

    @Schema(description = "정렬 순서", example = "0")
    private Integer displayOrder = 0;

    @NotEmpty(message = "옵션 값은 최소 1개 이상 필요합니다")
    @Valid
    @Schema(description = "옵션 값 목록", required = true)
    @Builder.Default
    private List<OptionValueRequest> optionValues = new ArrayList<>();
}

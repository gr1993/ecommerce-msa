package com.example.mockdelivery.dto.courier;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "송장 일괄 발급 응답")
public class BulkUploadResponse {

    @Schema(description = "전체 성공 여부", example = "true")
    private Boolean isSuccess;

    @Schema(description = "발급 요약")
    private BulkUploadSummary summary;

    @Schema(description = "발급 결과 목록")
    private List<BulkUploadResult> results;
}

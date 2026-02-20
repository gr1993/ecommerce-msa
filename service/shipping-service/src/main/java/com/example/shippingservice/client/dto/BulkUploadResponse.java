package com.example.shippingservice.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "송장 일괄 발급 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkUploadResponse {

    @Schema(description = "전체 성공 여부", example = "true")
    private Boolean isSuccess;

    @Schema(description = "발급 요약")
    private BulkUploadSummary summary;

    @Schema(description = "발급 결과 목록")
    private List<BulkUploadResult> results;
}

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
@Schema(description = "송장 일괄 취소 응답")
public class BulkCancelResponse {

    @Schema(description = "전체 성공 여부", example = "true")
    private Boolean isSuccess;

    @Schema(description = "취소 요약")
    private BulkCancelSummary summary;

    @Schema(description = "취소 결과 목록")
    private List<BulkCancelResult> results;
}

package com.example.shippingservice.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "송장 발급 요약")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkUploadSummary {

    @Schema(description = "전체 요청 수", example = "2")
    private Integer total;

    @Schema(description = "성공 수", example = "2")
    private Integer success;

    @Schema(description = "실패 수", example = "0")
    private Integer failed;
}

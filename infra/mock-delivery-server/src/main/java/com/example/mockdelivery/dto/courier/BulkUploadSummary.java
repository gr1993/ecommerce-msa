package com.example.mockdelivery.dto.courier;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "송장 발급 요약")
public class BulkUploadSummary {

    @Schema(description = "전체 요청 수", example = "2")
    private Integer total;

    @Schema(description = "성공 수", example = "2")
    private Integer success;

    @Schema(description = "실패 수", example = "0")
    private Integer failed;
}

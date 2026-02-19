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
@Schema(description = "송장 취소 결과 항목")
public class BulkCancelResult {

    @Schema(description = "요청 항목 인덱스", example = "0")
    private Integer index;

    @Schema(description = "취소 성공 여부", example = "true")
    private Boolean isSuccess;

    @Schema(description = "송장 번호", example = "100000000000")
    private String trackingNumber;

    @Schema(description = "처리 결과 메시지", example = "취소 완료")
    private String message;
}

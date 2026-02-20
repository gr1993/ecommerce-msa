package com.example.shippingservice.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "송장 발급 결과 항목")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkUploadResult {

    @Schema(description = "요청 항목 인덱스", example = "0")
    private Integer index;

    @Schema(description = "발급 성공 여부", example = "true")
    private Boolean isSuccess;

    @Schema(description = "발급된 송장 번호", example = "123456789012")
    private String trackingNumber;
}

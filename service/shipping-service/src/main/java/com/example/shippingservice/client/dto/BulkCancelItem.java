package com.example.shippingservice.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "송장 취소 항목")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkCancelItem {

    @Schema(description = "취소할 송장 번호", example = "100000000000")
    private String trackingNumber;
}

package com.example.shippingservice.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "송장 일괄 발급 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkUploadRequest {

    @Schema(description = "택배사 계정 키", example = "CJ_COURIER_ACCOUNT_KEY_2024")
    private String courierAccountKey;

    @Schema(description = "발급할 송장 목록")
    private List<BulkUploadItem> items;
}

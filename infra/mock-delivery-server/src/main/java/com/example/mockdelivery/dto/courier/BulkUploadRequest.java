package com.example.mockdelivery.dto.courier;

import io.swagger.v3.oas.annotations.media.ArraySchema;
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
@Schema(description = "송장 일괄 발급 요청")
public class BulkUploadRequest {

    @Schema(description = "택배사 계정 키", example = "CJ_COURIER_ACCOUNT_KEY_2024")
    private String courierAccountKey;

    @ArraySchema(schema = @Schema(implementation = BulkUploadItem.class))
    @Schema(description = "발급할 송장 목록")
    private List<BulkUploadItem> items;
}

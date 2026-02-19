package com.example.mockdelivery.dto.tracking;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "배송 조회 요청")
public class TrackingInfoRequest {

    @Schema(description = "택배사 코드 (04: CJ대한통운, 05: 한진택배 등)", example = "04")
    private String tCode;

    @Schema(description = "운송장 번호", example = "100000000000")
    private String tInvoice;

    @Schema(description = "스마트 택배 API 계정 키", example = "SMART_DELIVERY_API_KEY_2024")
    private String tKey;

    @JsonProperty("t_code")
    public String getTCode() {
        return tCode;
    }

    @JsonProperty("t_invoice")
    public String getTInvoice() {
        return tInvoice;
    }

    @JsonProperty("t_key")
    public String getTKey() {
        return tKey;
    }
}

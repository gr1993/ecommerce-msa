package com.example.shippingservice.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "배송 조회 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingInfoRequest {

    @Schema(description = "택배사 코드", example = "04")
    @JsonProperty("t_code")
    private String code;

    @Schema(description = "운송장 번호", example = "100000000000")
    @JsonProperty("t_invoice")
    private String invoice;

    @Schema(description = "API 키", example = "SMART_DELIVERY_API_KEY_2024")
    @JsonProperty("t_key")
    private String key;
}

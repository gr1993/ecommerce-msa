package com.example.shippingservice.shipping.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "운송장 등록 요청")
@Getter
@NoArgsConstructor
public class RegisterTrackingRequest {

    @Schema(description = "택배사 코드 (01: 우체국, 04: CJ대한통운, 05: 한진, 06: 로젠, 08: 롯데)", example = "04")
    @NotBlank(message = "택배사 코드는 필수입니다.")
    private String carrierCode;
}

package com.example.shippingservice.returns.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "반품 운송장 등록 요청")
@Getter
@NoArgsConstructor
public class ReturnTrackingRequest {

    @Schema(description = "택배사", example = "CJ대한통운")
    @NotBlank(message = "택배사는 필수입니다.")
    private String courier;

    @Schema(description = "운송장 번호", example = "1234567890123")
    @NotBlank(message = "운송장 번호는 필수입니다.")
    private String trackingNumber;
}

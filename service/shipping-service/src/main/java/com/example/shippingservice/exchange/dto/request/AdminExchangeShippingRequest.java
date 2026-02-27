package com.example.shippingservice.exchange.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "관리자 교환 배송 시작 요청")
@Getter
@NoArgsConstructor
public class AdminExchangeShippingRequest {

    @Schema(description = "택배사 코드 (01: 우체국, 04: CJ대한통운, 05: 한진택배, 06: 로젠택배, 08: 롯데택배)",
            example = "04")
    @NotBlank(message = "택배사 코드는 필수입니다.")
    private String carrierCode;
}

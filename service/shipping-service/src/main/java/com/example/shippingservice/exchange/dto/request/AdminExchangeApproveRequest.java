package com.example.shippingservice.exchange.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "관리자 교환 승인 요청 (회수 수거지 정보)")
@Getter
@NoArgsConstructor
public class AdminExchangeApproveRequest {

    @Schema(description = "회수 수령인", example = "홍길동")
    @NotBlank(message = "회수 수령인은 필수입니다.")
    private String collectReceiverName;

    @Schema(description = "회수 수령인 연락처", example = "010-1234-5678")
    @NotBlank(message = "회수 수령인 연락처는 필수입니다.")
    private String collectReceiverPhone;

    @Schema(description = "회수 주소", example = "서울특별시 강남구 테헤란로 123")
    @NotBlank(message = "회수 주소는 필수입니다.")
    private String collectAddress;

    @Schema(description = "회수 우편번호", example = "06234")
    private String collectPostalCode;
}

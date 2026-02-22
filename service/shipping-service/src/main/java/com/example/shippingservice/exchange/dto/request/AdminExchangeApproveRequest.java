package com.example.shippingservice.exchange.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "관리자 교환 승인 요청")
@Getter
@NoArgsConstructor
public class AdminExchangeApproveRequest {

    @Schema(description = "교환품 수령인", example = "홍길동")
    @NotBlank(message = "수령인은 필수입니다.")
    private String receiverName;

    @Schema(description = "교환품 수령 연락처", example = "010-1234-5678")
    @NotBlank(message = "연락처는 필수입니다.")
    private String receiverPhone;

    @Schema(description = "교환품 배송 주소", example = "서울특별시 강남구 테헤란로 123")
    @NotBlank(message = "배송 주소는 필수입니다.")
    private String exchangeAddress;

    @Schema(description = "우편번호", example = "06234")
    private String postalCode;
}

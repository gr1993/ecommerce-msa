package com.example.shippingservice.returns.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "관리자 반품 승인 요청")
@Getter
@NoArgsConstructor
public class AdminReturnApproveRequest {

    @Schema(description = "수거지 수령인", example = "물류센터")
    @NotBlank(message = "수령인은 필수입니다.")
    private String receiverName;

    @Schema(description = "수거지 연락처", example = "02-1234-5678")
    @NotBlank(message = "연락처는 필수입니다.")
    private String receiverPhone;

    @Schema(description = "수거지 주소", example = "서울특별시 강남구 물류센터로 1")
    @NotBlank(message = "수거지 주소는 필수입니다.")
    private String returnAddress;

    @Schema(description = "우편번호", example = "06234")
    private String postalCode;
}

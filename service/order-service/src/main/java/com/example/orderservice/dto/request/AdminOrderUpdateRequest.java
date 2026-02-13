package com.example.orderservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "관리자 주문 수정 요청")
@Getter
@NoArgsConstructor
public class AdminOrderUpdateRequest {

    @Schema(description = "주문 상태", example = "PAID")
    @NotBlank(message = "주문 상태는 필수입니다")
    private String orderStatus;

    @Schema(description = "주문 메모", example = "고객 요청으로 배송 보류")
    @Size(max = 1000, message = "메모는 최대 1000자까지 입력 가능합니다")
    private String orderMemo;
}

package com.example.orderservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "교환 신청 요청")
@Getter
@NoArgsConstructor
public class ExchangeOrderRequest {

    @Schema(description = "교환 상품 목록")
    @NotEmpty(message = "교환 상품 목록은 비어 있을 수 없습니다.")
    @Valid
    private List<ExchangeItemRequest> exchangeItems;

    @Schema(description = "교환 사유", example = "사이즈 교환")
    private String reason;
}

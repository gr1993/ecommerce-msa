package com.example.orderservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "배송 동기화 요청")
public class ShippingSyncRequest {

    @Schema(description = "페이지 번호 (0부터 시작)", example = "0")
    @Builder.Default
    private Integer page = 0;

    @Schema(description = "페이지 크기", example = "100")
    @Builder.Default
    private Integer size = 100;
}

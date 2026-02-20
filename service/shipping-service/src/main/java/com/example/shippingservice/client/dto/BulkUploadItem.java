package com.example.shippingservice.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "송장 발급 항목")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkUploadItem {

    @Schema(description = "수령인 이름", example = "홍길동")
    private String receiverName;

    @Schema(description = "수령인 전화번호", example = "01012345678")
    private String receiverPhone1;

    @Schema(description = "수령인 주소", example = "서울시 강남구 테헤란로 123")
    private String receiverAddress;

    @Schema(description = "상품명", example = "애플망고 3kg")
    private String goodsName;

    @Schema(description = "상품 수량", example = "1")
    private Integer goodsQty;
}

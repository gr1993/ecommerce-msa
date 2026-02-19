package com.example.mockdelivery.dto.tracking;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "배송 조회 응답")
public class TrackingInfoResponse {

    @Schema(description = "운송장 번호", example = "100000000000")
    private String invoiceNo;

    @Schema(description = "주문 번호", example = "ORD-0001")
    private String orderNumber;

    @Schema(description = "상품명", example = "애플망고 3kg")
    private String itemName;

    @Schema(description = "수령인 이름", example = "홍길동")
    private String receiverName;

    @Schema(description = "수령인 주소", example = "서울시 강남구 테헤란로 123")
    private String receiverAddr;

    @Schema(description = "발송인 이름", example = "샘플 판매자")
    private String senderName;

    @Schema(description = "최근 배송 상태")
    private TrackingDetail lastDetail;

    @Schema(description = "배송 이력 목록")
    private List<TrackingDetail> trackingDetails;
}

package com.example.orderservice.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "반품 신청 응답")
@Getter
@Builder
public class ReturnOrderResponse {

    @Schema(description = "반품 ID", example = "1")
    private Long returnId;

    @Schema(description = "주문 ID", example = "1")
    private Long orderId;

    @Schema(description = "주문 번호", example = "ORD-20240115-ABCD1234")
    private String orderNumber;

    @Schema(description = "반품 상태", example = "RETURN_REQUESTED")
    private String returnStatus;

    @Schema(description = "반품 사유", example = "상품 불량")
    private String reason;

    @Schema(description = "신청 일시")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestedAt;
}

package com.example.paymentservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 테스트용 결제 완료 주문 생성 요청 DTO
 * order.created 이벤트 수신 후 결제 확정까지 완료된 상태를 시뮬레이션합니다.
 */
@Schema(description = "테스트용 결제 완료 주문 생성 요청 (결제 확정 상태로 생성)")
@Getter
@NoArgsConstructor
public class TestCreateOrderRequest {

    @Schema(description = "주문 번호", example = "ORD-20250223-0001")
    @NotBlank(message = "주문 번호는 필수입니다.")
    private String orderNumber;

    @Schema(description = "사용자 ID", example = "1")
    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;

    @Schema(description = "총 결제 금액", example = "50000")
    @NotNull(message = "총 결제 금액은 필수입니다.")
    @Positive(message = "총 결제 금액은 양수여야 합니다.")
    private BigDecimal totalPaymentAmount;

    @Schema(description = "주문 상품 목록")
    @NotNull(message = "주문 상품 목록은 필수입니다.")
    @Valid
    private List<OrderItemInfo> orderItems;

    @Schema(description = "결제 키 (미입력 시 자동 생성)", example = "tgen_20250223000000abcd")
    private String paymentKey;

    @Builder
    public TestCreateOrderRequest(String orderNumber, Long userId, BigDecimal totalPaymentAmount,
                                   List<OrderItemInfo> orderItems, String paymentKey) {
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.totalPaymentAmount = totalPaymentAmount;
        this.orderItems = orderItems;
        this.paymentKey = paymentKey;
    }

    @Schema(description = "주문 상품 정보")
    @Getter
    @NoArgsConstructor
    public static class OrderItemInfo {

        @Schema(description = "상품명", example = "테스트 상품")
        @NotBlank(message = "상품명은 필수입니다.")
        private String productName;

        @Schema(description = "수량", example = "1")
        @NotNull(message = "수량은 필수입니다.")
        @Positive(message = "수량은 양수여야 합니다.")
        private Integer quantity;

        @Builder
        public OrderItemInfo(String productName, Integer quantity) {
            this.productName = productName;
            this.quantity = quantity;
        }
    }

    /**
     * 주문 이름 생성 (결제창 표시용)
     * 예: "상품명 외 2개"
     */
    public String generateOrderName() {
        if (orderItems == null || orderItems.isEmpty()) {
            return "주문";
        }
        String firstProductName = orderItems.get(0).getProductName();
        int additionalCount = orderItems.size() - 1;
        if (additionalCount > 0) {
            return firstProductName + " 외 " + additionalCount + "개";
        }
        return firstProductName;
    }
}

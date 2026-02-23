package com.example.orderservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 테스트용 결제 완료 주문 생성 요청 DTO
 * 결제까지 완료된 PAID 상태의 주문을 생성하고,
 * shipping-service와 payment-service에도 테스트 데이터를 생성합니다.
 */
@Schema(description = "테스트용 결제 완료 주문 생성 요청")
@Getter
@NoArgsConstructor
public class TestCreateOrderRequest {

    @Schema(description = "사용자 ID", example = "2")
    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;

    @Schema(description = "주문 상품 목록")
    @NotEmpty(message = "주문 상품은 최소 1개 이상이어야 합니다.")
    @Valid
    private List<OrderItemInfo> orderItems;

    @Schema(description = "배송지 정보")
    @NotNull(message = "배송지 정보는 필수입니다.")
    @Valid
    private DeliveryInfo delivery;

    @Schema(description = "주문 메모", example = "빠른 배송 부탁합니다.")
    private String orderMemo;

    @Builder
    public TestCreateOrderRequest(Long userId, List<OrderItemInfo> orderItems,
                                   DeliveryInfo delivery, String orderMemo) {
        this.userId = userId;
        this.orderItems = orderItems;
        this.delivery = delivery;
        this.orderMemo = orderMemo;
    }

    @Schema(description = "주문 상품 정보")
    @Getter
    @NoArgsConstructor
    public static class OrderItemInfo {

        @Schema(description = "상품 ID", example = "133")
        @NotNull(message = "상품 ID는 필수입니다.")
        private Long productId;

        @Schema(description = "SKU ID", example = "256")
        @NotNull(message = "SKU ID는 필수입니다.")
        private Long skuId;

        @Schema(description = "상품명", example = "데일리 티셔츠 화이트(화이트)")
        @NotBlank(message = "상품명은 필수입니다.")
        private String productName;

        @Schema(description = "상품 코드", example = "DAILY-TOP-001")
        private String productCode;

        @Schema(description = "수량", example = "1")
        @NotNull(message = "수량은 필수입니다.")
        @Positive(message = "수량은 양수여야 합니다.")
        private Integer quantity;

        @Schema(description = "단가", example = "25000")
        @NotNull(message = "단가는 필수입니다.")
        @Positive(message = "단가는 양수여야 합니다.")
        private BigDecimal unitPrice;

        @Builder
        public OrderItemInfo(Long productId, Long skuId, String productName, String productCode,
                             Integer quantity, BigDecimal unitPrice) {
            this.productId = productId;
            this.skuId = skuId;
            this.productName = productName;
            this.productCode = productCode;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public BigDecimal getTotalPrice() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    @Schema(description = "배송지 정보")
    @Getter
    @NoArgsConstructor
    public static class DeliveryInfo {

        @Schema(description = "수령인 이름", example = "박강림")
        @NotBlank(message = "수령인 이름은 필수입니다.")
        private String receiverName;

        @Schema(description = "수령인 연락처", example = "010-1234-5678")
        @NotBlank(message = "수령인 연락처는 필수입니다.")
        private String receiverPhone;

        @Schema(description = "우편번호", example = "04524")
        @NotBlank(message = "우편번호는 필수입니다.")
        private String zipcode;

        @Schema(description = "주소", example = "서울특별시 강남구 테스트로 123")
        @NotBlank(message = "주소는 필수입니다.")
        private String address;

        @Schema(description = "상세주소", example = "테스트빌딩 10층")
        private String addressDetail;

        @Schema(description = "배송 메모", example = "빠른 배송 부탁합니다.")
        private String deliveryMemo;

        @Builder
        public DeliveryInfo(String receiverName, String receiverPhone, String zipcode,
                            String address, String addressDetail, String deliveryMemo) {
            this.receiverName = receiverName;
            this.receiverPhone = receiverPhone;
            this.zipcode = zipcode;
            this.address = address;
            this.addressDetail = addressDetail;
            this.deliveryMemo = deliveryMemo;
        }
    }

    /**
     * 총 상품 금액 계산
     */
    public BigDecimal calculateTotalProductAmount() {
        return orderItems.stream()
                .map(OrderItemInfo::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

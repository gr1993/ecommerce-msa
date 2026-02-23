package com.example.shippingservice.shipping.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 테스트용 배송 정보 생성 요청 DTO
 * order.created 이벤트를 수동으로 시뮬레이션하기 위해 사용됩니다.
 */
@Schema(description = "테스트용 배송 정보 생성 요청 (order.created 이벤트 시뮬레이션)")
@Getter
@NoArgsConstructor
public class TestCreateShippingRequest {

    @Schema(description = "주문 ID", example = "1")
    @NotNull(message = "주문 ID는 필수입니다.")
    private Long orderId;

    @Schema(description = "주문 번호", example = "ORD-20250223-0001")
    @NotBlank(message = "주문 번호는 필수입니다.")
    private String orderNumber;

    @Schema(description = "사용자 ID", example = "1")
    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;

    @Schema(description = "배송지 정보")
    @NotNull(message = "배송지 정보는 필수입니다.")
    @Valid
    private DeliveryInfo delivery;

    @Builder
    public TestCreateShippingRequest(Long orderId, String orderNumber, Long userId, DeliveryInfo delivery) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.delivery = delivery;
    }

    @Schema(description = "배송지 정보")
    @Getter
    @NoArgsConstructor
    public static class DeliveryInfo {

        @Schema(description = "수령인 이름", example = "홍길동")
        @NotBlank(message = "수령인 이름은 필수입니다.")
        private String receiverName;

        @Schema(description = "수령인 연락처", example = "010-1234-5678")
        @NotBlank(message = "수령인 연락처는 필수입니다.")
        private String receiverPhone;

        @Schema(description = "우편번호", example = "12345")
        private String zipcode;

        @Schema(description = "주소", example = "서울시 강남구 테헤란로 123")
        @NotBlank(message = "주소는 필수입니다.")
        private String address;

        @Schema(description = "상세주소", example = "456호")
        private String addressDetail;

        @Builder
        public DeliveryInfo(String receiverName, String receiverPhone, String zipcode,
                            String address, String addressDetail) {
            this.receiverName = receiverName;
            this.receiverPhone = receiverPhone;
            this.zipcode = zipcode;
            this.address = address;
            this.addressDetail = addressDetail;
        }
    }
}

package com.example.orderservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "배송 정보 요청")
@Getter
@NoArgsConstructor
public class DeliveryInfoRequest {

    @Schema(description = "수령인 이름", example = "홍길동")
    @NotBlank(message = "수령인 이름은 필수입니다")
    private String receiverName;

    @Schema(description = "수령인 연락처", example = "010-1234-5678")
    @NotBlank(message = "수령인 연락처는 필수입니다")
    private String receiverPhone;

    @Schema(description = "우편번호", example = "12345")
    @NotBlank(message = "우편번호는 필수입니다")
    private String zipcode;

    @Schema(description = "주소", example = "서울특별시 강남구 테헤란로 123")
    @NotBlank(message = "주소는 필수입니다")
    private String address;

    @Schema(description = "상세 주소", example = "아파트 101동 202호")
    private String addressDetail;

    @Schema(description = "배송 메모", example = "문 앞에 놓아주세요.")
    private String deliveryMemo;

    @Builder
    public DeliveryInfoRequest(String receiverName, String receiverPhone, String zipcode,
                               String address, String addressDetail, String deliveryMemo) {
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.zipcode = zipcode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.deliveryMemo = deliveryMemo;
    }
}

package com.example.shippingservice.shipping.dto.response;

import com.example.shippingservice.shipping.enums.ShippingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "배송 취소 가능 여부 응답")
@Getter
@Builder
public class ShippingCancellableResponse {

    @Schema(description = "취소 가능 여부", example = "true")
    private boolean cancellable;

    @Schema(description = "취소 불가 사유 (취소 불가 시에만 존재)", example = "배송이 이미 진행 중입니다.")
    private String reason;

    @Schema(description = "현재 배송 상태", example = "READY")
    private ShippingStatus shippingStatus;

    public static ShippingCancellableResponse cancellable(ShippingStatus status) {
        return ShippingCancellableResponse.builder()
                .cancellable(true)
                .shippingStatus(status)
                .build();
    }

    public static ShippingCancellableResponse notFound() {
        return ShippingCancellableResponse.builder()
                .cancellable(true)
                .build();
    }

    public static ShippingCancellableResponse notCancellable(ShippingStatus status, String reason) {
        return ShippingCancellableResponse.builder()
                .cancellable(false)
                .shippingStatus(status)
                .reason(reason)
                .build();
    }
}

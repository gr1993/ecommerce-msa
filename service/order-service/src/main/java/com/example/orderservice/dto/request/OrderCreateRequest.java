package com.example.orderservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "주문 생성 요청")
@Getter
@NoArgsConstructor
public class OrderCreateRequest {

    @Schema(description = "주문 상품 목록")
    @NotEmpty(message = "주문 상품은 1개 이상이어야 합니다")
    @Valid
    private List<OrderItemRequest> orderItems;

    @Schema(description = "배송 정보")
    @NotNull(message = "배송 정보는 필수입니다")
    @Valid
    private DeliveryInfoRequest deliveryInfo;

    @Builder
    public OrderCreateRequest(List<OrderItemRequest> orderItems, DeliveryInfoRequest deliveryInfo) {
        this.orderItems = orderItems;
        this.deliveryInfo = deliveryInfo;
    }
}

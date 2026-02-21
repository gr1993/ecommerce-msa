package com.example.shippingservice.shipping.service;

import com.example.shippingservice.shipping.dto.response.ShippingCancellableResponse;

public interface InternalShippingService {

    /**
     * 주문 ID 기준으로 배송 취소 가능 여부를 확인합니다.
     * 배송사에 이미 전송된 경우(SENT)는 취소 가능하지만,
     * 실제 배송이 시작된(IN_TRANSIT) 경우부터는 취소할 수 없습니다.
     *
     * @param orderId 주문 ID
     * @return 취소 가능 여부 응답
     */
    ShippingCancellableResponse checkCancellable(Long orderId);
}

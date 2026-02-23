package com.example.shippingservice.shipping.service;

import com.example.shippingservice.shipping.dto.request.TestCreateShippingRequest;
import com.example.shippingservice.shipping.dto.response.ShippingCancellableResponse;
import com.example.shippingservice.shipping.dto.response.TestCreateShippingResponse;

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

    /**
     * 테스트용 배송 정보 생성 API
     * order.created 이벤트를 수동으로 시뮬레이션하여 배송 정보를 생성합니다.
     * 이미 존재하는 orderId인 경우 기존 데이터를 반환합니다 (멱등성 보장).
     *
     * @param request 배송 정보 생성 요청
     * @return 생성된 배송 정보
     */
    TestCreateShippingResponse createShippingForTest(TestCreateShippingRequest request);

    /**
     * 테스트 데이터 삭제 API
     * 주문 ID에 해당하는 배송 정보를 삭제합니다.
     *
     * @param orderId 주문 ID
     */
    void deleteShippingForTest(Long orderId);
}

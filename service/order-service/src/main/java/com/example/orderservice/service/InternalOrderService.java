package com.example.orderservice.service;

import com.example.orderservice.dto.request.TestCreateOrderRequest;
import com.example.orderservice.dto.response.TestCreateOrderResponse;

public interface InternalOrderService {

    /**
     * 테스트용 결제 완료 주문 생성
     * PAID 상태의 주문을 생성하고, shipping-service와 payment-service에도
     * 테스트 데이터를 함께 생성합니다.
     *
     * @param request 주문 생성 요청
     * @return 생성된 주문 정보
     */
    TestCreateOrderResponse createOrderForTest(TestCreateOrderRequest request);

    /**
     * 테스트 데이터 삭제
     * 주문 ID에 해당하는 주문 데이터와 shipping-service, payment-service의
     * 관련 테스트 데이터를 함께 삭제합니다.
     *
     * @param orderId 주문 ID
     */
    void deleteOrderForTest(Long orderId);
}

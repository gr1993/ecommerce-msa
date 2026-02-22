package com.example.shippingservice.returns.service;

import com.example.shippingservice.returns.dto.request.InternalCreateReturnRequest;
import com.example.shippingservice.returns.dto.response.InternalCreateReturnResponse;

public interface InternalReturnService {

    /**
     * 반품 레코드를 생성합니다. (order-service에서 Feign으로 호출)
     * 배송 상태가 DELIVERED인 경우에만 반품 신청이 가능하며,
     * 진행 중인 반품/교환 건이 없어야 합니다.
     *
     * @param request 반품 생성 요청 (orderId, userId, reason)
     * @return 생성된 반품 정보
     */
    InternalCreateReturnResponse createReturn(InternalCreateReturnRequest request);
}

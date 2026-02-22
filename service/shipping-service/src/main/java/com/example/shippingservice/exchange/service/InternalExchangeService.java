package com.example.shippingservice.exchange.service;

import com.example.shippingservice.exchange.dto.request.InternalCreateExchangeRequest;
import com.example.shippingservice.exchange.dto.response.InternalCreateExchangeResponse;

public interface InternalExchangeService {

    /**
     * 교환 레코드를 생성합니다. (order-service에서 Feign으로 호출)
     * 배송 상태가 DELIVERED인 경우에만 교환 신청이 가능하며,
     * 진행 중인 반품/교환 건이 없어야 합니다.
     */
    InternalCreateExchangeResponse createExchange(InternalCreateExchangeRequest request);
}

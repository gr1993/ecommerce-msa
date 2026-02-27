package com.example.shippingservice.exchange.service;

import com.example.shippingservice.client.dto.PageResponse;
import com.example.shippingservice.exchange.dto.request.AdminExchangeApproveRequest;
import com.example.shippingservice.exchange.dto.request.AdminExchangeRejectRequest;
import com.example.shippingservice.exchange.dto.request.AdminExchangeShippingRequest;
import com.example.shippingservice.exchange.dto.response.AdminExchangeResponse;
import org.springframework.data.domain.Pageable;

public interface AdminExchangeService {

    /** 교환 목록 조회 (페이징, 필터) */
    PageResponse<AdminExchangeResponse> getExchanges(String exchangeStatus, Long orderId, Pageable pageable);

    /** 교환 상세 조회 */
    AdminExchangeResponse getExchange(Long exchangeId);

    /**
     * 교환 승인: 회수 수거지 정보 저장 + Mock 택배사로 회수 운송장 자동 발급
     * EXCHANGE_REQUESTED → EXCHANGE_COLLECTING
     */
    AdminExchangeResponse approveExchange(Long exchangeId, AdminExchangeApproveRequest request);

    /** 교환 거절: EXCHANGE_REQUESTED → EXCHANGE_REJECTED */
    AdminExchangeResponse rejectExchange(Long exchangeId, AdminExchangeRejectRequest request);

    /**
     * 교환 배송 시작: 원주문 배송지 기반으로 Mock 택배사 운송장 자동 발급
     * EXCHANGE_RETURN_COMPLETED → EXCHANGE_SHIPPING
     */
    AdminExchangeResponse startShipping(Long exchangeId, AdminExchangeShippingRequest request);

}

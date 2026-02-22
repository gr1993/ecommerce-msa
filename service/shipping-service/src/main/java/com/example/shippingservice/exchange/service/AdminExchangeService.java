package com.example.shippingservice.exchange.service;

import com.example.shippingservice.client.dto.PageResponse;
import com.example.shippingservice.exchange.dto.request.AdminExchangeApproveRequest;
import com.example.shippingservice.exchange.dto.request.AdminExchangeRejectRequest;
import com.example.shippingservice.exchange.dto.response.AdminExchangeResponse;
import org.springframework.data.domain.Pageable;

public interface AdminExchangeService {

    /**
     * 교환 목록을 조회합니다. (페이징, 필터)
     */
    PageResponse<AdminExchangeResponse> getExchanges(String exchangeStatus, Long orderId, Pageable pageable);

    /**
     * 교환 상세를 조회합니다.
     */
    AdminExchangeResponse getExchange(Long exchangeId);

    /**
     * 교환을 승인하고 교환품 배송지 설정 + Mock 택배사 API로 송장을 발급합니다.
     */
    AdminExchangeResponse approveExchange(Long exchangeId, AdminExchangeApproveRequest request);

    /**
     * 교환을 거절합니다.
     */
    AdminExchangeResponse rejectExchange(Long exchangeId, AdminExchangeRejectRequest request);

    /**
     * 교환을 완료 처리합니다.
     */
    AdminExchangeResponse completeExchange(Long exchangeId);
}

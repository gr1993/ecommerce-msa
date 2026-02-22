package com.example.shippingservice.returns.service;

import com.example.shippingservice.client.dto.PageResponse;
import com.example.shippingservice.returns.dto.request.ReturnTrackingRequest;
import com.example.shippingservice.returns.dto.response.MarketReturnResponse;

public interface MarketReturnService {

    /**
     * 사용자의 반품 목록을 조회합니다.
     */
    PageResponse<MarketReturnResponse> getMyReturns(Long userId, org.springframework.data.domain.Pageable pageable);

    /**
     * 반품 운송장 정보를 등록합니다.
     * RETURN_APPROVED 상태에서만 가능합니다.
     */
    MarketReturnResponse registerTracking(Long userId, Long returnId, ReturnTrackingRequest request);
}

package com.example.shippingservice.returns.service;

import com.example.shippingservice.client.dto.PageResponse;
import com.example.shippingservice.returns.dto.response.MarketReturnResponse;

public interface MarketReturnService {

    /**
     * 사용자의 반품 목록을 조회합니다.
     */
    PageResponse<MarketReturnResponse> getMyReturns(Long userId, org.springframework.data.domain.Pageable pageable);
}

package com.example.shippingservice.exchange.service;

import com.example.shippingservice.client.dto.PageResponse;
import com.example.shippingservice.exchange.dto.response.MarketExchangeResponse;
import org.springframework.data.domain.Pageable;

public interface MarketExchangeService {

    /**
     * 사용자의 교환 목록을 조회합니다.
     */
    PageResponse<MarketExchangeResponse> getMyExchanges(Long userId, Pageable pageable);
}

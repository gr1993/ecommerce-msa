package com.example.shippingservice.shipping.service;

import com.example.shippingservice.client.dto.PageResponse;
import com.example.shippingservice.shipping.dto.response.MarketShippingResponse;
import org.springframework.data.domain.Pageable;

public interface MarketShippingService {

    PageResponse<MarketShippingResponse> getMyShippings(Long userId, Pageable pageable);
}

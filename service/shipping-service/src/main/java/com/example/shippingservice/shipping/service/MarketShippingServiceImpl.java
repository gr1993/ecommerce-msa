package com.example.shippingservice.shipping.service;

import com.example.shippingservice.client.dto.PageResponse;
import com.example.shippingservice.shipping.dto.response.MarketShippingResponse;
import com.example.shippingservice.shipping.entity.OrderShipping;
import com.example.shippingservice.shipping.repository.OrderShippingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketShippingServiceImpl implements MarketShippingService {

    private final OrderShippingRepository orderShippingRepository;

    @Override
    public PageResponse<MarketShippingResponse> getMyShippings(Long userId, Pageable pageable) {
        Page<OrderShipping> shippingPage = orderShippingRepository.findByUserId(userId, pageable);
        Page<MarketShippingResponse> responsePage = shippingPage.map(MarketShippingResponse::from);
        return PageResponse.from(responsePage);
    }
}

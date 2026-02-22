package com.example.shippingservice.exchange.service;

import com.example.shippingservice.client.dto.PageResponse;
import com.example.shippingservice.exchange.dto.response.MarketExchangeResponse;
import com.example.shippingservice.exchange.entity.OrderExchange;
import com.example.shippingservice.exchange.repository.OrderExchangeRepository;
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
public class MarketExchangeServiceImpl implements MarketExchangeService {

    private final OrderExchangeRepository orderExchangeRepository;

    @Override
    public PageResponse<MarketExchangeResponse> getMyExchanges(Long userId, Pageable pageable) {
        Page<OrderExchange> exchangePage = orderExchangeRepository.findByUserId(userId, pageable);
        Page<MarketExchangeResponse> responsePage = exchangePage.map(MarketExchangeResponse::from);
        return PageResponse.from(responsePage);
    }
}

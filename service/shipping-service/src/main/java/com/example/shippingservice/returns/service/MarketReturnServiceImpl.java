package com.example.shippingservice.returns.service;

import com.example.shippingservice.client.dto.PageResponse;
import com.example.shippingservice.returns.dto.response.MarketReturnResponse;
import com.example.shippingservice.returns.entity.OrderReturn;
import com.example.shippingservice.returns.repository.OrderReturnRepository;
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
public class MarketReturnServiceImpl implements MarketReturnService {

    private final OrderReturnRepository orderReturnRepository;

    @Override
    public PageResponse<MarketReturnResponse> getMyReturns(Long userId, Pageable pageable) {
        Page<OrderReturn> returnPage = orderReturnRepository.findByUserId(userId, pageable);
        Page<MarketReturnResponse> responsePage = returnPage.map(MarketReturnResponse::from);
        return PageResponse.from(responsePage);
    }
}

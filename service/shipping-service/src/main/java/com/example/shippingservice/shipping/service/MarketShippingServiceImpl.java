package com.example.shippingservice.shipping.service;

import com.example.shippingservice.client.dto.PageResponse;
import com.example.shippingservice.exchange.repository.OrderExchangeRepository;
import com.example.shippingservice.returns.repository.OrderReturnRepository;
import com.example.shippingservice.shipping.dto.response.MarketShippingResponse;
import com.example.shippingservice.shipping.entity.OrderShipping;
import com.example.shippingservice.shipping.enums.ShippingStatus;
import com.example.shippingservice.shipping.repository.OrderShippingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketShippingServiceImpl implements MarketShippingService {

    private final OrderShippingRepository orderShippingRepository;
    private final OrderReturnRepository orderReturnRepository;
    private final OrderExchangeRepository orderExchangeRepository;

    @Override
    public PageResponse<MarketShippingResponse> getMyShippings(Long userId, Pageable pageable) {
        Page<OrderShipping> shippingPage = orderShippingRepository.findByUserId(userId, pageable);
        Page<MarketShippingResponse> responsePage = shippingPage.map(MarketShippingResponse::from);
        return PageResponse.from(responsePage);
    }

    @Override
    public List<MarketShippingResponse> getReturnableShippings(Long userId) {
        // 배송 완료(DELIVERED) 상태인 배송 목록 조회
        List<OrderShipping> deliveredShippings = orderShippingRepository
                .findByUserIdAndShippingStatus(userId, ShippingStatus.DELIVERED);

        // 반품/교환 이력이 없는 주문만 필터링
        return deliveredShippings.stream()
                .filter(shipping -> !hasReturnOrExchange(shipping.getOrderId()))
                .map(MarketShippingResponse::from)
                .toList();
    }

    /**
     * 해당 주문에 반품 또는 교환 이력이 있는지 확인
     *
     * 반품/교환 테이블에 해당 orderId가 존재하면 true 반환
     * (진행 중, 완료, 거절 등 모든 상태 포함)
     */
    private boolean hasReturnOrExchange(Long orderId) {
        boolean hasReturn = orderReturnRepository.existsByOrderId(orderId);
        boolean hasExchange = orderExchangeRepository.existsByOrderId(orderId);

        return hasReturn || hasExchange;
    }
}

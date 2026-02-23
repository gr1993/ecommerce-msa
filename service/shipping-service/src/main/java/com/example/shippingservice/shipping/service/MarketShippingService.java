package com.example.shippingservice.shipping.service;

import com.example.shippingservice.client.dto.PageResponse;
import com.example.shippingservice.shipping.dto.response.MarketShippingResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MarketShippingService {

    PageResponse<MarketShippingResponse> getMyShippings(Long userId, Pageable pageable);

    /**
     * 반품 신청 가능한 배송 목록을 조회합니다.
     * 배송 완료(DELIVERED) 상태이며, 진행 중인 반품/교환이 없는 주문만 반환합니다.
     */
    List<MarketShippingResponse> getReturnableShippings(Long userId);
}

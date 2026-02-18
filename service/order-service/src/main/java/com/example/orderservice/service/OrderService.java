package com.example.orderservice.service;

import com.example.orderservice.dto.request.OrderCreateRequest;
import com.example.orderservice.dto.response.MyOrderResponse;
import com.example.orderservice.dto.response.OrderResponse;
import com.example.orderservice.global.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderResponse createOrder(Long userId, OrderCreateRequest request);

    PageResponse<MyOrderResponse> getMyOrders(Long userId, Pageable pageable);
}

package com.example.orderservice.service;

import com.example.orderservice.dto.request.OrderCreateRequest;
import com.example.orderservice.dto.response.OrderResponse;

public interface OrderService {

    OrderResponse createOrder(Long userId, OrderCreateRequest request);
}

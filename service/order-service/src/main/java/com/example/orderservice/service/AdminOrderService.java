package com.example.orderservice.service;

import com.example.orderservice.dto.request.AdminOrderUpdateRequest;
import com.example.orderservice.dto.response.AdminOrderDetailResponse;
import com.example.orderservice.dto.response.AdminOrderResponse;
import com.example.orderservice.global.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface AdminOrderService {

    PageResponse<AdminOrderResponse> getOrders(String orderNumber, String orderStatus, Pageable pageable);

    AdminOrderDetailResponse getOrderDetail(Long orderId);

    AdminOrderDetailResponse updateOrder(Long orderId, AdminOrderUpdateRequest request);
}

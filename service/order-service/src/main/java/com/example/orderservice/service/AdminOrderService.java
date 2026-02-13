package com.example.orderservice.service;

import com.example.orderservice.dto.request.AdminOrderUpdateRequest;
import com.example.orderservice.dto.response.AdminOrderDetailResponse;
import com.example.orderservice.dto.response.AdminOrderResponse;

import java.util.List;

public interface AdminOrderService {

    List<AdminOrderResponse> getOrders(String orderNumber, String orderStatus);

    AdminOrderDetailResponse getOrderDetail(Long orderId);

    AdminOrderDetailResponse updateOrder(Long orderId, AdminOrderUpdateRequest request);
}

package com.example.orderservice.service;

import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderStatus;
import com.example.orderservice.dto.request.AdminOrderUpdateRequest;
import com.example.orderservice.dto.response.AdminOrderDetailResponse;
import com.example.orderservice.dto.response.AdminOrderResponse;
import com.example.orderservice.global.common.dto.PageResponse;
import com.example.orderservice.global.exception.OrderNotFoundException;
import com.example.orderservice.repository.OrderRepository;
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
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderRepository orderRepository;

    @Override
    public PageResponse<AdminOrderResponse> getOrders(String orderNumber, String orderStatus, Pageable pageable) {
        OrderStatus status = parseOrderStatus(orderStatus);

        Page<Order> orderPage = orderRepository.findAllBySearchCondition(
                orderNumber != null && orderNumber.isBlank() ? null : orderNumber,
                status,
                pageable
        );

        Page<AdminOrderResponse> responsePage = orderPage.map(AdminOrderResponse::from);
        return PageResponse.from(responsePage);
    }

    @Override
    public AdminOrderDetailResponse getOrderDetail(Long orderId) {
        Order order = orderRepository.findByIdWithOrderItemsAndDelivery(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        return AdminOrderDetailResponse.from(order);
    }

    @Override
    @Transactional
    public AdminOrderDetailResponse updateOrder(Long orderId, AdminOrderUpdateRequest request) {
        Order order = orderRepository.findByIdWithOrderItemsAndDelivery(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        OrderStatus newStatus = parseOrderStatus(request.getOrderStatus());
        if (newStatus == null) {
            throw new IllegalArgumentException("유효하지 않은 주문 상태입니다: " + request.getOrderStatus());
        }

        order.updateStatus(newStatus);
        order.updateMemo(request.getOrderMemo());

        log.info("관리자 주문 수정: orderId={}, status={}, memo={}", orderId, newStatus, request.getOrderMemo());

        return AdminOrderDetailResponse.from(order);
    }

    private OrderStatus parseOrderStatus(String orderStatus) {
        if (orderStatus == null || orderStatus.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(orderStatus);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 주문 상태입니다: " + orderStatus);
        }
    }
}

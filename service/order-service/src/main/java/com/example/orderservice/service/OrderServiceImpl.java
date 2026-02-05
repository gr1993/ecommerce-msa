package com.example.orderservice.service;

import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderItem;
import com.example.orderservice.domain.entity.OrderStatus;
import com.example.orderservice.dto.request.OrderCreateRequest;
import com.example.orderservice.dto.request.OrderItemRequest;
import com.example.orderservice.dto.response.OrderResponse;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        BigDecimal totalProductAmount = calculateTotalProductAmount(request);
        BigDecimal discountAmount = request.getDiscountAmount() != null
                ? request.getDiscountAmount()
                : BigDecimal.ZERO;
        BigDecimal totalPaymentAmount = totalProductAmount.subtract(discountAmount);

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .userId(request.getUserId())
                .orderStatus(OrderStatus.CREATED)
                .totalProductAmount(totalProductAmount)
                .totalDiscountAmount(discountAmount)
                .totalPaymentAmount(totalPaymentAmount)
                .orderMemo(request.getOrderMemo())
                .build();

        for (OrderItemRequest itemRequest : request.getOrderItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .skuId(itemRequest.getSkuId())
                    .productName(itemRequest.getProductName())
                    .productCode(itemRequest.getProductCode())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(itemRequest.getUnitPrice())
                    .totalPrice(itemRequest.getUnitPrice()
                            .multiply(BigDecimal.valueOf(itemRequest.getQuantity())))
                    .build();
            order.addOrderItem(orderItem);
        }

        Order savedOrder = orderRepository.save(order);
        return OrderResponse.from(savedOrder);
    }

    private BigDecimal calculateTotalProductAmount(OrderCreateRequest request) {
        return request.getOrderItems().stream()
                .map(item -> item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String generateOrderNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uniquePart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD-" + datePart + "-" + uniquePart;
    }
}

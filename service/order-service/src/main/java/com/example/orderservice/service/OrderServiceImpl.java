package com.example.orderservice.service;

import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderDelivery;
import com.example.orderservice.domain.entity.OrderItem;
import com.example.orderservice.domain.entity.OrderStatus;
import com.example.orderservice.dto.request.DeliveryInfoRequest;
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
    public OrderResponse createOrder(Long userId, OrderCreateRequest request) {
        // TODO: product-service에서 상품 정보(productName, unitPrice 등) 조회
        // 현재는 임시값 사용
        BigDecimal totalProductAmount = BigDecimal.ZERO;

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .userId(userId)
                .orderStatus(OrderStatus.CREATED)
                .totalProductAmount(totalProductAmount)
                .totalDiscountAmount(BigDecimal.ZERO)
                .totalPaymentAmount(totalProductAmount)
                .build();

        for (OrderItemRequest itemRequest : request.getOrderItems()) {
            // TODO: product-service에서 상품 정보 조회 후 설정
            BigDecimal unitPrice = BigDecimal.ZERO;
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .skuId(itemRequest.getSkuId())
                    .productName("") // TODO: product-service에서 조회
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .totalPrice(totalPrice)
                    .build();
            order.addOrderItem(orderItem);

            totalProductAmount = totalProductAmount.add(totalPrice);
        }

        DeliveryInfoRequest deliveryInfo = request.getDeliveryInfo();
        OrderDelivery orderDelivery = OrderDelivery.builder()
                .receiverName(deliveryInfo.getReceiverName())
                .receiverPhone(deliveryInfo.getReceiverPhone())
                .zipcode(deliveryInfo.getZipcode())
                .address(deliveryInfo.getAddress())
                .addressDetail(deliveryInfo.getAddressDetail())
                .deliveryMemo(deliveryInfo.getDeliveryMemo())
                .build();
        order.setOrderDelivery(orderDelivery);

        Order savedOrder = orderRepository.save(order);
        return OrderResponse.from(savedOrder);
    }

    private String generateOrderNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uniquePart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD-" + datePart + "-" + uniquePart;
    }
}

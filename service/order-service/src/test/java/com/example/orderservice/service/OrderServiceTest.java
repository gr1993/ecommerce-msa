package com.example.orderservice.service;

import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderItem;
import com.example.orderservice.domain.entity.OrderStatus;
import com.example.orderservice.dto.request.OrderCreateRequest;
import com.example.orderservice.dto.request.OrderItemRequest;
import com.example.orderservice.dto.response.OrderResponse;
import com.example.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private OrderCreateRequest createRequest;
    private Order savedOrder;

    @BeforeEach
    void setUp() {
        OrderItemRequest itemRequest = OrderItemRequest.builder()
                .productId(100L)
                .skuId(1001L)
                .productName("테스트 상품")
                .productCode("PROD-001")
                .quantity(2)
                .unitPrice(new BigDecimal("25000.00"))
                .build();

        createRequest = OrderCreateRequest.builder()
                .userId(1L)
                .orderItems(List.of(itemRequest))
                .discountAmount(new BigDecimal("5000.00"))
                .orderMemo("테스트 주문")
                .build();

        savedOrder = createTestOrder();
    }

    private Order createTestOrder() {
        Order order = Order.builder()
                .orderNumber("ORD-20240101-ABCD1234")
                .userId(1L)
                .orderStatus(OrderStatus.CREATED)
                .totalProductAmount(new BigDecimal("50000.00"))
                .totalDiscountAmount(new BigDecimal("5000.00"))
                .totalPaymentAmount(new BigDecimal("45000.00"))
                .orderMemo("테스트 주문")
                .build();

        OrderItem orderItem = OrderItem.builder()
                .productId(100L)
                .skuId(1001L)
                .productName("테스트 상품")
                .productCode("PROD-001")
                .quantity(2)
                .unitPrice(new BigDecimal("25000.00"))
                .totalPrice(new BigDecimal("50000.00"))
                .build();

        order.addOrderItem(orderItem);
        return order;
    }

    @Test
    @DisplayName("주문 생성 성공")
    void createOrder_Success() {
        // given
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

        // when
        OrderResponse response = orderService.createOrder(createRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(response.getOrderItems()).hasSize(1);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 생성 - 할인 금액 없는 경우")
    void createOrder_WithoutDiscount() {
        // given
        OrderItemRequest itemRequest = OrderItemRequest.builder()
                .productId(100L)
                .skuId(1001L)
                .productName("테스트 상품")
                .productCode("PROD-001")
                .quantity(2)
                .unitPrice(new BigDecimal("25000.00"))
                .build();

        OrderCreateRequest requestWithoutDiscount = OrderCreateRequest.builder()
                .userId(1L)
                .orderItems(List.of(itemRequest))
                .build();

        Order orderWithoutDiscount = Order.builder()
                .orderNumber("ORD-20240101-EFGH5678")
                .userId(1L)
                .orderStatus(OrderStatus.CREATED)
                .totalProductAmount(new BigDecimal("50000.00"))
                .totalDiscountAmount(BigDecimal.ZERO)
                .totalPaymentAmount(new BigDecimal("50000.00"))
                .build();
        orderWithoutDiscount.addOrderItem(OrderItem.builder()
                .productId(100L)
                .skuId(1001L)
                .productName("테스트 상품")
                .productCode("PROD-001")
                .quantity(2)
                .unitPrice(new BigDecimal("25000.00"))
                .totalPrice(new BigDecimal("50000.00"))
                .build());

        given(orderRepository.save(any(Order.class))).willReturn(orderWithoutDiscount);

        // when
        OrderResponse response = orderService.createOrder(requestWithoutDiscount);

        // then
        assertThat(response.getTotalDiscountAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(response.getTotalPaymentAmount()).isEqualTo(new BigDecimal("50000.00"));
    }

    @Test
    @DisplayName("주문 생성 - 여러 상품 주문")
    void createOrder_MultipleItems() {
        // given
        OrderItemRequest item1 = OrderItemRequest.builder()
                .productId(100L)
                .skuId(1001L)
                .productName("상품1")
                .quantity(2)
                .unitPrice(new BigDecimal("10000.00"))
                .build();

        OrderItemRequest item2 = OrderItemRequest.builder()
                .productId(101L)
                .skuId(1002L)
                .productName("상품2")
                .quantity(1)
                .unitPrice(new BigDecimal("30000.00"))
                .build();

        OrderCreateRequest multiItemRequest = OrderCreateRequest.builder()
                .userId(1L)
                .orderItems(List.of(item1, item2))
                .build();

        Order multiItemOrder = Order.builder()
                .orderNumber("ORD-20240101-MULTI123")
                .userId(1L)
                .orderStatus(OrderStatus.CREATED)
                .totalProductAmount(new BigDecimal("50000.00"))
                .totalDiscountAmount(BigDecimal.ZERO)
                .totalPaymentAmount(new BigDecimal("50000.00"))
                .build();
        multiItemOrder.addOrderItem(OrderItem.builder()
                .productId(100L).skuId(1001L).productName("상품1")
                .quantity(2).unitPrice(new BigDecimal("10000.00"))
                .totalPrice(new BigDecimal("20000.00")).build());
        multiItemOrder.addOrderItem(OrderItem.builder()
                .productId(101L).skuId(1002L).productName("상품2")
                .quantity(1).unitPrice(new BigDecimal("30000.00"))
                .totalPrice(new BigDecimal("30000.00")).build());

        given(orderRepository.save(any(Order.class))).willReturn(multiItemOrder);

        // when
        OrderResponse response = orderService.createOrder(multiItemRequest);

        // then
        assertThat(response.getOrderItems()).hasSize(2);
        assertThat(response.getTotalProductAmount()).isEqualTo(new BigDecimal("50000.00"));
    }
}

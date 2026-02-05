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
    private DeliveryInfoRequest deliveryInfoRequest;
    private Order savedOrder;

    @BeforeEach
    void setUp() {
        deliveryInfoRequest = DeliveryInfoRequest.builder()
                .receiverName("홍길동")
                .receiverPhone("010-1234-5678")
                .zipcode("12345")
                .address("서울특별시 강남구 테헤란로 123")
                .addressDetail("아파트 101동 202호")
                .deliveryMemo("문 앞에 놓아주세요.")
                .build();

        OrderItemRequest itemRequest = OrderItemRequest.builder()
                .productId(456L)
                .skuId(789L)
                .quantity(2)
                .build();

        createRequest = OrderCreateRequest.builder()
                .orderItems(List.of(itemRequest))
                .deliveryInfo(deliveryInfoRequest)
                .build();

        savedOrder = createTestOrder();
    }

    private Order createTestOrder() {
        Order order = Order.builder()
                .orderNumber("ORD-20240101-ABCD1234")
                .userId(1L)
                .orderStatus(OrderStatus.CREATED)
                .totalProductAmount(BigDecimal.ZERO)
                .totalDiscountAmount(BigDecimal.ZERO)
                .totalPaymentAmount(BigDecimal.ZERO)
                .build();

        OrderItem orderItem = OrderItem.builder()
                .productId(456L)
                .skuId(789L)
                .productName("")
                .quantity(2)
                .unitPrice(BigDecimal.ZERO)
                .totalPrice(BigDecimal.ZERO)
                .build();

        order.addOrderItem(orderItem);

        OrderDelivery orderDelivery = OrderDelivery.builder()
                .receiverName("홍길동")
                .receiverPhone("010-1234-5678")
                .zipcode("12345")
                .address("서울특별시 강남구 테헤란로 123")
                .addressDetail("아파트 101동 202호")
                .deliveryMemo("문 앞에 놓아주세요.")
                .build();
        order.setOrderDelivery(orderDelivery);

        return order;
    }

    @Test
    @DisplayName("주문 생성 성공")
    void createOrder_Success() {
        // given
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

        // when
        OrderResponse response = orderService.createOrder(1L, createRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getOrderNumber()).isEqualTo("ORD-20240101-ABCD1234");
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.CREATED);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 생성 - 여러 상품 주문")
    void createOrder_MultipleItems() {
        // given
        OrderItemRequest item1 = OrderItemRequest.builder()
                .productId(100L)
                .skuId(1001L)
                .quantity(2)
                .build();

        OrderItemRequest item2 = OrderItemRequest.builder()
                .productId(101L)
                .skuId(1002L)
                .quantity(1)
                .build();

        OrderCreateRequest multiItemRequest = OrderCreateRequest.builder()
                .orderItems(List.of(item1, item2))
                .deliveryInfo(deliveryInfoRequest)
                .build();

        Order multiItemOrder = Order.builder()
                .orderNumber("ORD-20240101-MULTI123")
                .userId(1L)
                .orderStatus(OrderStatus.CREATED)
                .totalProductAmount(BigDecimal.ZERO)
                .totalDiscountAmount(BigDecimal.ZERO)
                .totalPaymentAmount(BigDecimal.ZERO)
                .build();
        multiItemOrder.addOrderItem(OrderItem.builder()
                .productId(100L).skuId(1001L).productName("")
                .quantity(2).unitPrice(BigDecimal.ZERO)
                .totalPrice(BigDecimal.ZERO).build());
        multiItemOrder.addOrderItem(OrderItem.builder()
                .productId(101L).skuId(1002L).productName("")
                .quantity(1).unitPrice(BigDecimal.ZERO)
                .totalPrice(BigDecimal.ZERO).build());

        OrderDelivery orderDelivery = OrderDelivery.builder()
                .receiverName("홍길동")
                .receiverPhone("010-1234-5678")
                .zipcode("12345")
                .address("서울특별시 강남구 테헤란로 123")
                .addressDetail("아파트 101동 202호")
                .deliveryMemo("문 앞에 놓아주세요.")
                .build();
        multiItemOrder.setOrderDelivery(orderDelivery);

        given(orderRepository.save(any(Order.class))).willReturn(multiItemOrder);

        // when
        OrderResponse response = orderService.createOrder(1L, multiItemRequest);

        // then
        assertThat(response.getOrderNumber()).isEqualTo("ORD-20240101-MULTI123");
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.CREATED);
    }
}

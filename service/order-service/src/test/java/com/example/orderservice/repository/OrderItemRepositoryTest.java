package com.example.orderservice.repository;

import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderItem;
import com.example.orderservice.domain.entity.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class OrderItemRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private Order testOrder;
    private OrderItem testOrderItem;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .orderNumber("ORD-20240101-001")
                .userId(1L)
                .orderStatus(OrderStatus.CREATED)
                .totalProductAmount(new BigDecimal("50000.00"))
                .totalDiscountAmount(new BigDecimal("5000.00"))
                .totalPaymentAmount(new BigDecimal("45000.00"))
                .build();

        testOrderItem = OrderItem.builder()
                .productId(100L)
                .skuId(1001L)
                .productName("테스트 상품")
                .productCode("PROD-001")
                .quantity(2)
                .unitPrice(new BigDecimal("25000.00"))
                .totalPrice(new BigDecimal("50000.00"))
                .build();
    }

    @Test
    @DisplayName("주문 상품 저장 테스트")
    void saveOrderItem() {
        // given
        Order savedOrder = orderRepository.save(testOrder);
        savedOrder.addOrderItem(testOrderItem);
        orderRepository.flush();

        // when
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(savedOrder.getId());

        // then
        assertThat(orderItems).hasSize(1);
        assertThat(orderItems.get(0).getProductName()).isEqualTo("테스트 상품");
        assertThat(orderItems.get(0).getQuantity()).isEqualTo(2);
        assertThat(orderItems.get(0).getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("주문 ID로 주문 상품 목록 조회 테스트")
    void findByOrderId() {
        // given
        Order savedOrder = orderRepository.save(testOrder);
        savedOrder.addOrderItem(testOrderItem);

        OrderItem anotherItem = OrderItem.builder()
                .productId(101L)
                .skuId(1002L)
                .productName("다른 상품")
                .productCode("PROD-002")
                .quantity(1)
                .unitPrice(new BigDecimal("10000.00"))
                .totalPrice(new BigDecimal("10000.00"))
                .build();
        savedOrder.addOrderItem(anotherItem);
        orderRepository.flush();

        // when
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(savedOrder.getId());

        // then
        assertThat(orderItems).hasSize(2);
    }

    @Test
    @DisplayName("상품 ID로 주문 상품 목록 조회 테스트")
    void findByProductId() {
        // given
        Order savedOrder = orderRepository.save(testOrder);
        savedOrder.addOrderItem(testOrderItem);
        orderRepository.flush();

        // when
        List<OrderItem> orderItems = orderItemRepository.findByProductId(100L);

        // then
        assertThat(orderItems).hasSize(1);
        assertThat(orderItems.get(0).getProductId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("SKU ID로 주문 상품 목록 조회 테스트")
    void findBySkuId() {
        // given
        Order savedOrder = orderRepository.save(testOrder);
        savedOrder.addOrderItem(testOrderItem);
        orderRepository.flush();

        // when
        List<OrderItem> orderItems = orderItemRepository.findBySkuId(1001L);

        // then
        assertThat(orderItems).hasSize(1);
        assertThat(orderItems.get(0).getSkuId()).isEqualTo(1001L);
    }

    @Test
    @DisplayName("주문 삭제 시 주문 상품도 함께 삭제 테스트 (Cascade)")
    void deleteOrderCascadeOrderItems() {
        // given
        Order savedOrder = orderRepository.save(testOrder);
        savedOrder.addOrderItem(testOrderItem);
        orderRepository.flush();

        Long orderId = savedOrder.getId();

        // when
        orderRepository.deleteById(orderId);
        orderRepository.flush();

        // then
        assertThat(orderItemRepository.findByOrderId(orderId)).isEmpty();
    }
}

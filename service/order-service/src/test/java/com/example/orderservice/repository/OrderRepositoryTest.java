package com.example.orderservice.repository;

import com.example.orderservice.domain.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .orderNumber("ORD-20240101-001")
                .userId(1L)
                .orderStatus(OrderStatus.CREATED)
                .totalProductAmount(new BigDecimal("50000.00"))
                .totalDiscountAmount(new BigDecimal("5000.00"))
                .totalPaymentAmount(new BigDecimal("45000.00"))
                .orderMemo("테스트 주문")
                .build();
    }

    @Test
    @DisplayName("주문 저장 테스트")
    void saveOrder() {
        // when
        Order savedOrder = orderRepository.save(testOrder);

        // then
        assertThat(savedOrder.getId()).isNotNull();
        assertThat(savedOrder.getOrderNumber()).isEqualTo("ORD-20240101-001");
        assertThat(savedOrder.getUserId()).isEqualTo(1L);
        assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(savedOrder.getOrderedAt()).isNotNull();
        assertThat(savedOrder.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("주문번호로 조회 테스트")
    void findByOrderNumber() {
        // given
        orderRepository.save(testOrder);

        // when
        Optional<Order> foundOrder = orderRepository.findByOrderNumber("ORD-20240101-001");

        // then
        assertThat(foundOrder).isPresent();
        assertThat(foundOrder.get().getOrderNumber()).isEqualTo("ORD-20240101-001");
    }

    @Test
    @DisplayName("사용자 ID로 주문 목록 조회 테스트")
    void findByUserId() {
        // given
        orderRepository.save(testOrder);

        Order anotherOrder = Order.builder()
                .orderNumber("ORD-20240101-002")
                .userId(1L)
                .orderStatus(OrderStatus.PAID)
                .totalProductAmount(new BigDecimal("30000.00"))
                .totalDiscountAmount(BigDecimal.ZERO)
                .totalPaymentAmount(new BigDecimal("30000.00"))
                .build();
        orderRepository.save(anotherOrder);

        // when
        List<Order> orders = orderRepository.findByUserId(1L);

        // then
        assertThat(orders).hasSize(2);
    }

    @Test
    @DisplayName("사용자 ID와 주문 상태로 조회 테스트")
    void findByUserIdAndOrderStatus() {
        // given
        orderRepository.save(testOrder);

        // when
        List<Order> orders = orderRepository.findByUserIdAndOrderStatus(1L, OrderStatus.CREATED);

        // then
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getOrderStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    @DisplayName("주문 상태로 조회 테스트")
    void findByOrderStatus() {
        // given
        orderRepository.save(testOrder);

        // when
        List<Order> orders = orderRepository.findByOrderStatus(OrderStatus.CREATED);

        // then
        assertThat(orders).hasSize(1);
    }

    @Test
    @DisplayName("주문번호 존재 여부 확인 테스트")
    void existsByOrderNumber() {
        // given
        orderRepository.save(testOrder);

        // when & then
        assertThat(orderRepository.existsByOrderNumber("ORD-20240101-001")).isTrue();
        assertThat(orderRepository.existsByOrderNumber("ORD-NOT-EXIST")).isFalse();
    }

    @Test
    @DisplayName("주문 상태 업데이트 테스트")
    void updateOrderStatus() {
        // given
        Order savedOrder = orderRepository.save(testOrder);

        // when
        savedOrder.updateStatus(OrderStatus.PAID);
        Order updatedOrder = orderRepository.save(savedOrder);

        // then
        assertThat(updatedOrder.getOrderStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("주문 삭제 테스트")
    void deleteOrder() {
        // given
        Order savedOrder = orderRepository.save(testOrder);
        Long orderId = savedOrder.getId();

        // when
        orderRepository.deleteById(orderId);

        // then
        assertThat(orderRepository.findById(orderId)).isEmpty();
    }
}

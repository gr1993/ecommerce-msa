package com.example.paymentservice.repository;

import com.example.paymentservice.domain.entity.Order;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("주문을 저장하고 orderId로 조회한다")
    void findByOrderId() {
        // given
        Order order = Order.builder()
                .orderId("ORDER-001")
                .orderName("테스트 상품")
                .amount(10000L)
                .status(Order.PaymentStatus.PENDING)
                .customerId("CUSTOMER-001")
                .createdAt(LocalDateTime.now())
                .build();

        orderRepository.save(order);

        // when
        Optional<Order> found = orderRepository.findByOrderId("ORDER-001");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getOrderId()).isEqualTo("ORDER-001");
        assertThat(found.get().getOrderName()).isEqualTo("테스트 상품");
        assertThat(found.get().getAmount()).isEqualTo(10000L);
        assertThat(found.get().getStatus()).isEqualTo(Order.PaymentStatus.PENDING);
        assertThat(found.get().getCustomerId()).isEqualTo("CUSTOMER-001");
    }

    @Test
    @DisplayName("존재하지 않는 orderId로 조회하면 빈 Optional을 반환한다")
    void findByOrderId_notFound() {
        // when
        Optional<Order> found = orderRepository.findByOrderId("NON-EXISTENT");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("주문 상태를 승인으로 변경하고 저장한다")
    void approveOrder() {
        // given
        Order order = Order.builder()
                .orderId("ORDER-002")
                .orderName("승인 테스트 상품")
                .amount(20000L)
                .status(Order.PaymentStatus.PENDING)
                .customerId("CUSTOMER-002")
                .createdAt(LocalDateTime.now())
                .build();

        orderRepository.save(order);

        // when
        Order saved = orderRepository.findByOrderId("ORDER-002").get();
        saved.approve("PAYMENT-KEY-001");
        orderRepository.save(saved);

        // then
        Order approved = orderRepository.findByOrderId("ORDER-002").get();
        assertThat(approved.getStatus()).isEqualTo(Order.PaymentStatus.APPROVED);
        assertThat(approved.getPaymentKey()).isEqualTo("PAYMENT-KEY-001");
        assertThat(approved.getApprovedAt()).isNotNull();
    }

    @Test
    @DisplayName("주문 상태를 실패로 변경하고 저장한다")
    void failOrder() {
        // given
        Order order = Order.builder()
                .orderId("ORDER-003")
                .orderName("실패 테스트 상품")
                .amount(30000L)
                .status(Order.PaymentStatus.PENDING)
                .customerId("CUSTOMER-003")
                .createdAt(LocalDateTime.now())
                .build();

        orderRepository.save(order);

        // when
        Order saved = orderRepository.findByOrderId("ORDER-003").get();
        saved.fail();
        orderRepository.save(saved);

        // then
        Order failed = orderRepository.findByOrderId("ORDER-003").get();
        assertThat(failed.getStatus()).isEqualTo(Order.PaymentStatus.FAILED);
    }
}

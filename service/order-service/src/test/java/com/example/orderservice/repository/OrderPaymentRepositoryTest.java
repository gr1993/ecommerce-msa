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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class OrderPaymentRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderPaymentRepository orderPaymentRepository;

    private Order testOrder;
    private OrderPayment testOrderPayment;

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

        testOrderPayment = OrderPayment.builder()
                .paymentMethod(PaymentMethod.CARD)
                .paymentAmount(new BigDecimal("45000.00"))
                .paymentStatus(PaymentStatus.READY)
                .build();
    }

    @Test
    @DisplayName("주문 결제 저장 테스트")
    void saveOrderPayment() {
        // given
        Order savedOrder = orderRepository.save(testOrder);
        savedOrder.addOrderPayment(testOrderPayment);
        orderRepository.flush();

        // when
        List<OrderPayment> payments = orderPaymentRepository.findByOrderId(savedOrder.getId());

        // then
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(payments.get(0).getPaymentStatus()).isEqualTo(PaymentStatus.READY);
        assertThat(payments.get(0).getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("주문 ID로 결제 목록 조회 테스트")
    void findByOrderId() {
        // given
        Order savedOrder = orderRepository.save(testOrder);
        savedOrder.addOrderPayment(testOrderPayment);
        orderRepository.flush();

        // when
        List<OrderPayment> payments = orderPaymentRepository.findByOrderId(savedOrder.getId());

        // then
        assertThat(payments).hasSize(1);
    }

    @Test
    @DisplayName("결제 상태로 조회 테스트")
    void findByPaymentStatus() {
        // given
        Order savedOrder = orderRepository.save(testOrder);
        savedOrder.addOrderPayment(testOrderPayment);
        orderRepository.flush();

        // when
        List<OrderPayment> readyPayments = orderPaymentRepository.findByPaymentStatus(PaymentStatus.READY);

        // then
        assertThat(readyPayments).hasSize(1);
        assertThat(readyPayments.get(0).getPaymentStatus()).isEqualTo(PaymentStatus.READY);
    }

    @Test
    @DisplayName("결제 상태 업데이트 테스트")
    void updatePaymentStatus() {
        // given
        Order savedOrder = orderRepository.save(testOrder);
        savedOrder.addOrderPayment(testOrderPayment);
        orderRepository.flush();

        List<OrderPayment> payments = orderPaymentRepository.findByOrderId(savedOrder.getId());
        OrderPayment payment = payments.get(0);

        // when
        payment.updateStatus(PaymentStatus.PAID);
        orderPaymentRepository.flush();

        // then
        OrderPayment updatedPayment = orderPaymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(updatedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(updatedPayment.getPaidAt()).isNotNull();
    }

    @Test
    @DisplayName("주문 삭제 시 결제 정보도 함께 삭제 테스트 (Cascade)")
    void deleteOrderCascadePayments() {
        // given
        Order savedOrder = orderRepository.save(testOrder);
        savedOrder.addOrderPayment(testOrderPayment);
        orderRepository.flush();

        Long orderId = savedOrder.getId();

        // when
        orderRepository.deleteById(orderId);
        orderRepository.flush();

        // then
        assertThat(orderPaymentRepository.findByOrderId(orderId)).isEmpty();
    }

    @Test
    @DisplayName("다양한 결제 수단 테스트")
    void testVariousPaymentMethods() {
        // given
        Order savedOrder = orderRepository.save(testOrder);

        OrderPayment cardPayment = OrderPayment.builder()
                .paymentMethod(PaymentMethod.CARD)
                .paymentAmount(new BigDecimal("30000.00"))
                .paymentStatus(PaymentStatus.READY)
                .build();

        OrderPayment kakaoPayment = OrderPayment.builder()
                .paymentMethod(PaymentMethod.KAKAO_PAY)
                .paymentAmount(new BigDecimal("15000.00"))
                .paymentStatus(PaymentStatus.READY)
                .build();

        savedOrder.addOrderPayment(cardPayment);
        savedOrder.addOrderPayment(kakaoPayment);
        orderRepository.flush();

        // when
        List<OrderPayment> payments = orderPaymentRepository.findByOrderId(savedOrder.getId());

        // then
        assertThat(payments).hasSize(2);
        assertThat(payments)
                .extracting(OrderPayment::getPaymentMethod)
                .containsExactlyInAnyOrder(PaymentMethod.CARD, PaymentMethod.KAKAO_PAY);
    }
}

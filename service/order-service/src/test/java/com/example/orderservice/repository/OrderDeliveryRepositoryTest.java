package com.example.orderservice.repository;

import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderDelivery;
import com.example.orderservice.domain.entity.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class OrderDeliveryRepositoryTest {

    @Autowired
    private OrderDeliveryRepository orderDeliveryRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Order savedOrder;

    @BeforeEach
    void setUp() {
        Order order = Order.builder()
                .orderNumber("ORD-20240101-TEST1234")
                .userId(1L)
                .orderStatus(OrderStatus.CREATED)
                .totalProductAmount(BigDecimal.ZERO)
                .totalDiscountAmount(BigDecimal.ZERO)
                .totalPaymentAmount(BigDecimal.ZERO)
                .build();

        OrderDelivery orderDelivery = OrderDelivery.builder()
                .receiverName("홍길동")
                .receiverPhone("010-1234-5678")
                .zipcode("12345")
                .address("서울특별시 강남구 테헤란로 123")
                .addressDetail("아파트 101동 202호")
                .deliveryMemo("문 앞에 놓아주세요.")
                .build();

        order.setOrderDelivery(orderDelivery);
        savedOrder = orderRepository.save(order);
    }

    @Test
    @DisplayName("배송 정보 저장 테스트")
    void saveOrderDelivery() {
        // then
        assertThat(savedOrder.getOrderDelivery()).isNotNull();
        assertThat(savedOrder.getOrderDelivery().getId()).isNotNull();
        assertThat(savedOrder.getOrderDelivery().getReceiverName()).isEqualTo("홍길동");
        assertThat(savedOrder.getOrderDelivery().getReceiverPhone()).isEqualTo("010-1234-5678");
        assertThat(savedOrder.getOrderDelivery().getZipcode()).isEqualTo("12345");
        assertThat(savedOrder.getOrderDelivery().getAddress()).isEqualTo("서울특별시 강남구 테헤란로 123");
        assertThat(savedOrder.getOrderDelivery().getAddressDetail()).isEqualTo("아파트 101동 202호");
        assertThat(savedOrder.getOrderDelivery().getDeliveryMemo()).isEqualTo("문 앞에 놓아주세요.");
    }

    @Test
    @DisplayName("주문 ID로 배송 정보 조회")
    void findByOrderId() {
        // when
        Optional<OrderDelivery> foundDelivery = orderDeliveryRepository.findByOrderId(savedOrder.getId());

        // then
        assertThat(foundDelivery).isPresent();
        assertThat(foundDelivery.get().getReceiverName()).isEqualTo("홍길동");
        assertThat(foundDelivery.get().getOrder().getId()).isEqualTo(savedOrder.getId());
    }

    @Test
    @DisplayName("주문 삭제 시 배송 정보도 함께 삭제 (Cascade)")
    void deleteOrderCascadeDelivery() {
        // given
        Long deliveryId = savedOrder.getOrderDelivery().getId();

        // when
        orderRepository.delete(savedOrder);

        // then
        Optional<OrderDelivery> foundDelivery = orderDeliveryRepository.findById(deliveryId);
        assertThat(foundDelivery).isEmpty();
    }
}

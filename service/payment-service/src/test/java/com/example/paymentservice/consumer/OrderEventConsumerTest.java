package com.example.paymentservice.consumer;

import com.example.paymentservice.consumer.event.OrderCreatedEvent;
import com.example.paymentservice.domain.entity.Order;
import com.example.paymentservice.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

	@InjectMocks
	private OrderEventConsumer orderEventConsumer;

	@Mock
	private OrderRepository orderRepository;

	@Test
	@DisplayName("주문 생성 이벤트 수신 시 Order 저장 성공")
	void consumeOrderCreatedEvent_Success() {
		// given
		OrderCreatedEvent event = createOrderCreatedEvent();

		given(orderRepository.findByOrderNumber(event.getOrderNumber())).willReturn(Optional.empty());
		given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when
		orderEventConsumer.consumeOrderCreatedEvent(event, "order.created", 0L);

		// then
		ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
		verify(orderRepository).save(orderCaptor.capture());

		Order savedOrder = orderCaptor.getValue();
		assertThat(savedOrder.getOrderNumber()).isEqualTo("ORD-20260209-001");
		assertThat(savedOrder.getOrderName()).isEqualTo("테스트 상품 외 1개");
		assertThat(savedOrder.getAmount()).isEqualTo(30000L);
		assertThat(savedOrder.getCustomerId()).isEqualTo("1001");
		assertThat(savedOrder.getStatus()).isEqualTo(Order.PaymentStatus.PENDING);
	}

	@Test
	@DisplayName("이미 존재하는 주문은 저장하지 않음 (멱등성)")
	void consumeOrderCreatedEvent_AlreadyExists() {
		// given
		OrderCreatedEvent event = createOrderCreatedEvent();

		Order existingOrder = Order.builder()
				.orderNumber(event.getOrderNumber())
				.orderName("기존 주문")
				.amount(30000L)
				.status(Order.PaymentStatus.PENDING)
				.build();

		given(orderRepository.findByOrderNumber(event.getOrderNumber())).willReturn(Optional.of(existingOrder));

		// when
		orderEventConsumer.consumeOrderCreatedEvent(event, "order.created", 0L);

		// then
		verify(orderRepository, never()).save(any(Order.class));
	}

	@Test
	@DisplayName("주문 저장 실패 시 예외 발생")
	void consumeOrderCreatedEvent_SaveFailed() {
		// given
		OrderCreatedEvent event = createOrderCreatedEvent();

		given(orderRepository.findByOrderNumber(event.getOrderNumber())).willReturn(Optional.empty());
		given(orderRepository.save(any(Order.class))).willThrow(new RuntimeException("MongoDB 저장 실패"));

		// when & then
		assertThatThrownBy(() -> orderEventConsumer.consumeOrderCreatedEvent(event, "order.created", 0L))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("MongoDB 저장 실패");
	}

	@Test
	@DisplayName("상품이 1개일 때 주문명 생성")
	void consumeOrderCreatedEvent_SingleItem() {
		// given
		OrderCreatedEvent.OrderItemSnapshot item = OrderCreatedEvent.OrderItemSnapshot.builder()
				.orderItemId(1L)
				.productId(100L)
				.productName("단일 상품")
				.quantity(1)
				.unitPrice(BigDecimal.valueOf(10000))
				.totalPrice(BigDecimal.valueOf(10000))
				.build();

		OrderCreatedEvent event = OrderCreatedEvent.builder()
				.orderId(1L)
				.orderNumber("ORD-SINGLE-001")
				.userId(1001L)
				.orderStatus("CREATED")
				.totalProductAmount(BigDecimal.valueOf(10000))
				.totalDiscountAmount(BigDecimal.ZERO)
				.totalPaymentAmount(BigDecimal.valueOf(10000))
				.orderItems(List.of(item))
				.orderedAt(LocalDateTime.now())
				.build();

		given(orderRepository.findByOrderNumber(event.getOrderNumber())).willReturn(Optional.empty());
		given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when
		orderEventConsumer.consumeOrderCreatedEvent(event, "order.created", 0L);

		// then
		ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
		verify(orderRepository).save(orderCaptor.capture());

		Order savedOrder = orderCaptor.getValue();
		assertThat(savedOrder.getOrderName()).isEqualTo("단일 상품");
	}

	@Test
	@DisplayName("orderedAt이 null일 때 현재 시간으로 설정")
	void consumeOrderCreatedEvent_NullOrderedAt() {
		// given
		OrderCreatedEvent event = OrderCreatedEvent.builder()
				.orderId(1L)
				.orderNumber("ORD-NULL-DATE-001")
				.userId(1001L)
				.orderStatus("CREATED")
				.totalPaymentAmount(BigDecimal.valueOf(10000))
				.orderItems(List.of(
						OrderCreatedEvent.OrderItemSnapshot.builder()
								.productName("테스트 상품")
								.build()
				))
				.orderedAt(null)  // null
				.build();

		given(orderRepository.findByOrderNumber(event.getOrderNumber())).willReturn(Optional.empty());
		given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when
		orderEventConsumer.consumeOrderCreatedEvent(event, "order.created", 0L);

		// then
		ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
		verify(orderRepository).save(orderCaptor.capture());

		Order savedOrder = orderCaptor.getValue();
		assertThat(savedOrder.getCreatedAt()).isNotNull();
	}

	private OrderCreatedEvent createOrderCreatedEvent() {
		OrderCreatedEvent.OrderItemSnapshot item1 = OrderCreatedEvent.OrderItemSnapshot.builder()
				.orderItemId(1L)
				.productId(100L)
				.skuId(1000L)
				.productName("테스트 상품")
				.productCode("PROD-001")
				.quantity(2)
				.unitPrice(BigDecimal.valueOf(10000))
				.totalPrice(BigDecimal.valueOf(20000))
				.build();

		OrderCreatedEvent.OrderItemSnapshot item2 = OrderCreatedEvent.OrderItemSnapshot.builder()
				.orderItemId(2L)
				.productId(101L)
				.skuId(1001L)
				.productName("테스트 상품 2")
				.productCode("PROD-002")
				.quantity(1)
				.unitPrice(BigDecimal.valueOf(10000))
				.totalPrice(BigDecimal.valueOf(10000))
				.build();

		OrderCreatedEvent.DeliverySnapshot delivery = OrderCreatedEvent.DeliverySnapshot.builder()
				.receiverName("홍길동")
				.receiverPhone("010-1234-5678")
				.zipcode("12345")
				.address("서울시 강남구")
				.addressDetail("101호")
				.deliveryMemo("부재시 문앞에 놓아주세요")
				.build();

		return OrderCreatedEvent.builder()
				.orderId(1L)
				.orderNumber("ORD-20260209-001")
				.userId(1001L)
				.orderStatus("CREATED")
				.totalProductAmount(BigDecimal.valueOf(30000))
				.totalDiscountAmount(BigDecimal.ZERO)
				.totalPaymentAmount(BigDecimal.valueOf(30000))
				.orderItems(List.of(item1, item2))
				.delivery(delivery)
				.orderedAt(LocalDateTime.of(2026, 2, 9, 10, 0, 0))
				.build();
	}
}

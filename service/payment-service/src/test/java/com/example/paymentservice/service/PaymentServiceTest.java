package com.example.paymentservice.service;

import com.example.paymentservice.client.TossPaymentsClient;
import com.example.paymentservice.client.dto.TossPaymentConfirmRequest;
import com.example.paymentservice.client.dto.TossPaymentResponse;
import com.example.paymentservice.domain.entity.Order;
import com.example.paymentservice.domain.entity.Outbox;
import com.example.paymentservice.dto.request.PaymentConfirmRequest;
import com.example.paymentservice.dto.response.PaymentConfirmResponse;
import com.example.paymentservice.repository.OrderRepository;
import com.example.paymentservice.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

	@InjectMocks
	private PaymentService paymentService;

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private OutboxRepository outboxRepository;

	@Mock
	private TossPaymentsClient tossPaymentsClient;

	@Spy
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		ReflectionTestUtils.setField(paymentService, "objectMapper", objectMapper);
	}

	@Test
	@DisplayName("결제 승인 성공")
	void confirmPayment_Success() {
		// given
		String orderId = "1001";  // 숫자 형식 (Long.parseLong 호환)
		String paymentKey = "test-payment-key";
		Long amount = 10000L;

		PaymentConfirmRequest request = new PaymentConfirmRequest(paymentKey, orderId, amount);

		Order order = Order.builder()
				.orderId(orderId)
				.orderName("테스트 상품")
				.amount(amount)
				.status(Order.PaymentStatus.PENDING)
				.customerId("CUSTOMER-001")
				.createdAt(LocalDateTime.now())
				.build();

		TossPaymentResponse tossResponse = createTossPaymentResponse(orderId, paymentKey, amount);

		given(orderRepository.findByOrderId(orderId)).willReturn(Optional.of(order));
		given(tossPaymentsClient.confirmPayment(any(TossPaymentConfirmRequest.class))).willReturn(tossResponse);
		given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));
		given(outboxRepository.save(any(Outbox.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when
		PaymentConfirmResponse response = paymentService.confirmPayment(request);

		// then
		assertThat(response.getOrderId()).isEqualTo(orderId);
		assertThat(response.getPaymentKey()).isEqualTo(paymentKey);
		assertThat(response.getAmount()).isEqualTo(amount);
		assertThat(response.getStatus()).isEqualTo("DONE");

		verify(orderRepository).findByOrderId(orderId);
		verify(tossPaymentsClient).confirmPayment(any(TossPaymentConfirmRequest.class));
		verify(orderRepository, times(1)).save(any(Order.class));
		verify(outboxRepository).save(any(Outbox.class));
	}

	@Test
	@DisplayName("결제 승인 실패 - 주문을 찾을 수 없음")
	void confirmPayment_OrderNotFound() {
		// given
		String orderId = "9999";  // 숫자 형식
		PaymentConfirmRequest request = new PaymentConfirmRequest("payment-key", orderId, 10000L);

		given(orderRepository.findByOrderId(orderId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> paymentService.confirmPayment(request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("주문을 찾을 수 없습니다");

		verify(orderRepository).findByOrderId(orderId);
		verify(tossPaymentsClient, never()).confirmPayment(any());
	}

	@Test
	@DisplayName("결제 승인 실패 - 금액 불일치로 주문 실패 처리 및 Outbox 저장")
	void confirmPayment_AmountMismatch() {
		// given
		String orderId = "1002";  // 숫자 형식
		Long orderAmount = 10000L;
		Long requestAmount = 99999L;  // 다른 금액

		PaymentConfirmRequest request = new PaymentConfirmRequest("payment-key", orderId, requestAmount);

		Order order = Order.builder()
				.orderId(orderId)
				.orderName("테스트 상품")
				.amount(orderAmount)
				.status(Order.PaymentStatus.PENDING)
				.customerId("CUSTOMER-001")
				.createdAt(LocalDateTime.now())
				.build();

		ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
		ArgumentCaptor<Outbox> outboxCaptor = ArgumentCaptor.forClass(Outbox.class);

		given(orderRepository.findByOrderId(orderId)).willReturn(Optional.of(order));
		given(orderRepository.save(orderCaptor.capture())).willAnswer(invocation -> invocation.getArgument(0));
		given(outboxRepository.save(outboxCaptor.capture())).willAnswer(invocation -> invocation.getArgument(0));

		// when & then
		assertThatThrownBy(() -> paymentService.confirmPayment(request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("결제 금액이 일치하지 않습니다");

		// Order 상태가 FAILED로 변경되었는지 확인
		assertThat(orderCaptor.getValue().getStatus()).isEqualTo(Order.PaymentStatus.FAILED);

		// PaymentCancelledEvent가 Outbox에 저장되었는지 확인
		Outbox savedOutbox = outboxCaptor.getValue();
		assertThat(savedOutbox.getEventType()).isEqualTo("payment.cancelled");
		assertThat(savedOutbox.getAggregateId()).isEqualTo(orderId);

		// TossPayments API가 호출되지 않았는지 확인
		verify(tossPaymentsClient, never()).confirmPayment(any());
	}

	@Test
	@DisplayName("결제 승인 후 Outbox에 결제 완료 이벤트 저장")
	void confirmPayment_SavePaymentConfirmedOutbox() {
		// given
		String orderId = "1003";  // 숫자 형식
		String paymentKey = "test-payment-key";
		Long amount = 10000L;

		PaymentConfirmRequest request = new PaymentConfirmRequest(paymentKey, orderId, amount);

		Order order = Order.builder()
				.orderId(orderId)
				.orderName("테스트 상품")
				.amount(amount)
				.status(Order.PaymentStatus.PENDING)
				.customerId("CUSTOMER-001")
				.createdAt(LocalDateTime.now())
				.build();

		TossPaymentResponse tossResponse = createTossPaymentResponse(orderId, paymentKey, amount);

		given(orderRepository.findByOrderId(orderId)).willReturn(Optional.of(order));
		given(tossPaymentsClient.confirmPayment(any(TossPaymentConfirmRequest.class))).willReturn(tossResponse);
		given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));
		given(outboxRepository.save(any(Outbox.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when
		paymentService.confirmPayment(request);

		// then
		ArgumentCaptor<Outbox> outboxCaptor = ArgumentCaptor.forClass(Outbox.class);
		verify(outboxRepository).save(outboxCaptor.capture());

		Outbox savedOutbox = outboxCaptor.getValue();
		assertThat(savedOutbox.getEventType()).isEqualTo("payment.confirmed");
		assertThat(savedOutbox.getAggregateType()).isEqualTo("Order");
		assertThat(savedOutbox.getAggregateId()).isEqualTo(orderId);
		assertThat(savedOutbox.getPayload()).contains(paymentKey);
	}

	private TossPaymentResponse createTossPaymentResponse(String orderId, String paymentKey, Long amount) {
		TossPaymentResponse response = new TossPaymentResponse();
		ReflectionTestUtils.setField(response, "orderId", orderId);
		ReflectionTestUtils.setField(response, "paymentKey", paymentKey);
		ReflectionTestUtils.setField(response, "totalAmount", amount);
		ReflectionTestUtils.setField(response, "status", "DONE");
		ReflectionTestUtils.setField(response, "method", "카드");
		ReflectionTestUtils.setField(response, "approvedAt", "2026-02-09T10:00:00+09:00");
		return response;
	}
}

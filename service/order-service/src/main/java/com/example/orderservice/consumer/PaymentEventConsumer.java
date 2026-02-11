package com.example.orderservice.consumer;

import com.example.orderservice.consumer.event.PaymentCancelledEvent;
import com.example.orderservice.consumer.event.PaymentConfirmedEvent;
import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderPayment;
import com.example.orderservice.domain.entity.OrderStatus;
import com.example.orderservice.domain.entity.PaymentMethod;
import com.example.orderservice.domain.entity.PaymentStatus;
import com.example.orderservice.repository.OrderRepository;
import io.github.springwolf.bindings.kafka.annotations.KafkaAsyncOperationBinding;
import io.github.springwolf.core.asyncapi.annotations.AsyncListener;
import io.github.springwolf.core.asyncapi.annotations.AsyncMessage;
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Payment 이벤트 컨슈머
 *
 * 재시도 전략:
 * - 총 4회 시도 (원본 1회 + 재시도 3회)
 * - 지수 백오프: 1초 -> 2초 -> 4초
 * - 모든 재시도 실패 시 DLQ(Dead Letter Queue)로 전송
 *
 * 멱등성:
 * - 주문 상태 기반 중복 체크로 이미 결제 완료된 주문에 대해
 *   멱등성을 보장함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

	private final OrderRepository orderRepository;

	@AsyncListener(
			operation = @AsyncOperation(
					channelName = "payment.confirmed",
					description = "결제 확인 이벤트 구독 - 주문 상태를 PAID로 변경",
					message = @AsyncMessage(
							messageId = "paymentConfirmedEvent",
							name = "PaymentConfirmedEvent"
					)
			)
	)
	@KafkaAsyncOperationBinding
	@RetryableTopic(
			attempts = "4",
			backoff = @Backoff(
					delay = 1000,
					multiplier = 2.0,
					maxDelay = 10000
			),
			autoCreateTopics = "false",
			include = {Exception.class},
			topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
			retryTopicSuffix = "-order-retry",
			dltTopicSuffix = "-order-dlt"
	)
	@KafkaListener(topics = "payment.confirmed", groupId = "${spring.kafka.consumer.group-id:order-service}")
	@Transactional
	public void consumePaymentConfirmedEvent(
			@Payload PaymentConfirmedEvent event,
			@Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
			@Header(value = KafkaHeaders.OFFSET, required = false) Long offset
	) {
		log.info("Received payment.confirmed event: orderId={}, paymentKey={}, topic={}, offset={}",
				event.getOrderId(), event.getPaymentKey(), topic, offset);

		try {
			Order order = orderRepository.findById(event.getOrderId())
					.orElse(null);

			if (order == null) {
				log.warn("Order not found for payment confirmation: orderId={}", event.getOrderId());
				return;
			}

			// 이미 결제 완료된 주문인지 확인 (멱등성 보장)
			if (order.getOrderStatus() == OrderStatus.PAID) {
				log.info("Order already paid, skipping: orderId={}", event.getOrderId());
				return;
			}

			// 주문 상태를 PAID로 변경
			order.updateStatus(OrderStatus.PAID);

			// OrderPayment 생성
			LocalDateTime paidAt = parsePaidAt(event.getPaidAt());
			OrderPayment orderPayment = OrderPayment.builder()
					.paymentMethod(parsePaymentMethod(event.getPaymentMethod()))
					.paymentAmount(BigDecimal.valueOf(event.getPaymentAmount()))
					.paymentStatus(PaymentStatus.PAID)
					.paymentKey(event.getPaymentKey())
					.paidAt(paidAt)
					.build();
			order.addOrderPayment(orderPayment);

			orderRepository.save(order);
			log.info("Successfully updated order to PAID: orderId={}, paymentKey={}",
					event.getOrderId(), event.getPaymentKey());
		} catch (Exception e) {
			log.error("Failed to process payment.confirmed event: orderId={}, paymentKey={}",
					event.getOrderId(), event.getPaymentKey(), e);
			throw e;
		}
	}

	private LocalDateTime parsePaidAt(String paidAt) {
		if (paidAt == null || paidAt.isBlank()) {
			return LocalDateTime.now();
		}
		try {
			return LocalDateTime.parse(paidAt, DateTimeFormatter.ISO_DATE_TIME);
		} catch (Exception e) {
			log.warn("Failed to parse paidAt: {}, using current time", paidAt);
			return LocalDateTime.now();
		}
	}

	private PaymentMethod parsePaymentMethod(String method) {
		if (method == null || method.isBlank()) {
			return PaymentMethod.CARD;
		}
		return switch (method) {
			case "카드", "CARD" -> PaymentMethod.CARD;
			case "계좌이체", "BANK_TRANSFER" -> PaymentMethod.BANK_TRANSFER;
			case "카카오페이", "KAKAO_PAY" -> PaymentMethod.KAKAO_PAY;
			case "네이버페이", "NAVER_PAY" -> PaymentMethod.NAVER_PAY;
			case "토스페이", "TOSS_PAY" -> PaymentMethod.TOSS_PAY;
			default -> {
				log.warn("Unknown payment method: {}, defaulting to CARD", method);
				yield PaymentMethod.CARD;
			}
		};
	}

	@AsyncListener(
			operation = @AsyncOperation(
					channelName = "payment.cancelled",
					description = "결제 취소 이벤트 구독 - 주문 상태를 FAILED로 변경",
					message = @AsyncMessage(
							messageId = "paymentCancelledEvent",
							name = "PaymentCancelledEvent"
					)
			)
	)
	@KafkaAsyncOperationBinding
	@RetryableTopic(
			attempts = "4",
			backoff = @Backoff(
					delay = 1000,
					multiplier = 2.0,
					maxDelay = 10000
			),
			autoCreateTopics = "false",
			include = {Exception.class},
			topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
			retryTopicSuffix = "-order-retry",
			dltTopicSuffix = "-order-dlt"
	)
	@KafkaListener(topics = "payment.cancelled", groupId = "${spring.kafka.consumer.group-id:order-service}")
	@Transactional
	public void consumePaymentCancelledEvent(
			@Payload PaymentCancelledEvent event,
			@Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
			@Header(value = KafkaHeaders.OFFSET, required = false) Long offset
	) {
		log.info("Received payment.cancelled event: orderId={}, cancelReason={}, topic={}, offset={}",
				event.getOrderId(), event.getCancelReason(), topic, offset);

		try {
			Order order = orderRepository.findByOrderNumber(event.getOrderId())
					.orElse(null);

			if (order == null) {
				log.warn("Order not found for payment cancellation: orderNumber={}", event.getOrderId());
				return;
			}

			// 이미 실패 처리된 주문인지 확인 (멱등성 보장)
			if (order.getOrderStatus() == OrderStatus.FAILED) {
				log.info("Order already failed, skipping: orderNumber={}", event.getOrderId());
				return;
			}

			// 주문 상태를 FAILED로 변경
			order.updateStatus(OrderStatus.FAILED);

			orderRepository.save(order);
			log.info("Successfully updated order to FAILED: orderNumber={}, cancelReason={}",
					event.getOrderId(), event.getCancelReason());
		} catch (Exception e) {
			log.error("Failed to process payment.cancelled event: orderNumber={}, cancelReason={}",
					event.getOrderId(), event.getCancelReason(), e);
			throw e;
		}
	}

	/**
	 * DLQ(Dead Letter Queue) 핸들러
	 * 모든 재시도가 실패한 후 호출됩니다.
	 */
	@DltHandler
	public void handleDlt(
			@Payload Object payload,
			@Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
			@Header(value = KafkaHeaders.OFFSET, required = false) Long offset,
			@Header(value = KafkaHeaders.ORIGINAL_TOPIC, required = false) String originalTopic,
			@Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String exceptionMessage
	) {
		log.error("""
				========================================
				DLQ 메시지 수신 (재시도 실패)
				========================================
				DLT Topic: {}
				Original Topic: {}
				Offset: {}
				Payload: {}
				Exception: {}
				========================================
				""", topic, originalTopic, offset, payload, exceptionMessage);

		if (payload instanceof PaymentConfirmedEvent event) {
			log.error("DLQ 처리 필요 - payment.confirmed 실패: orderId={}, paymentKey={}",
					event.getOrderId(), event.getPaymentKey());
		} else if (payload instanceof PaymentCancelledEvent event) {
			log.error("DLQ 처리 필요 - payment.cancelled 실패: orderNumber={}, cancelReason={}",
					event.getOrderId(), event.getCancelReason());
		} else {
			log.error("DLQ 알 수 없는 payload 타입: {}", payload.getClass().getName());
		}
	}
}

package com.example.paymentservice.consumer;

import com.example.paymentservice.consumer.event.OrderCreatedEvent;
import com.example.paymentservice.domain.entity.Order;
import com.example.paymentservice.repository.OrderRepository;
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

import java.time.LocalDateTime;

/**
 * Order 이벤트 컨슈머
 *
 * 재시도 전략:
 * - 총 4회 시도 (원본 1회 + 재시도 3회)
 * - 지수 백오프: 1초 -> 2초 -> 4초
 * - 모든 재시도 실패 시 DLQ(Dead Letter Queue)로 전송
 *
 * 멱등성:
 * - MongoDB는 orderNumber 기반 중복 체크로 동일 주문에 대해
 *   멱등성을 보장함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

	private final OrderRepository orderRepository;

	@AsyncListener(
			operation = @AsyncOperation(
					channelName = "order.created",
					description = "주문 생성 이벤트 구독 - 결제 대기 주문 정보 저장",
					message = @AsyncMessage(
							messageId = "orderCreatedEvent",
							name = "OrderCreatedEvent"
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
			retryTopicSuffix = "-payment-retry",
			dltTopicSuffix = "-payment-dlt"
	)
	@KafkaListener(topics = "order.created", groupId = "${spring.kafka.consumer.group-id:payment-service}")
	public void consumeOrderCreatedEvent(
			@Payload OrderCreatedEvent event,
			@Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
			@Header(value = KafkaHeaders.OFFSET, required = false) Long offset
	) {
		log.info("Received order.created event: orderId={}, orderNumber={}, topic={}, offset={}",
				event.getOrderId(), event.getOrderNumber(), topic, offset);

		try {
			// 이미 저장된 주문인지 확인 (멱등성 보장)
			if (orderRepository.findByOrderId(event.getOrderNumber()).isPresent()) {
				log.info("Order already exists, skipping: orderNumber={}", event.getOrderNumber());
				return;
			}

			// 결제 대기 상태로 주문 정보 저장
			Order order = Order.builder()
					.orderId(event.getOrderNumber())
					.orderName(event.generateOrderName())
					.amount(event.getTotalPaymentAmount().longValue())
					.customerId(event.getUserId().toString())
					.status(Order.PaymentStatus.PENDING)
					.createdAt(event.getOrderedAt() != null ? event.getOrderedAt() : LocalDateTime.now())
					.build();

			orderRepository.save(order);
			log.info("Successfully saved order for payment: orderNumber={}, amount={}",
					event.getOrderNumber(), event.getTotalPaymentAmount());
		} catch (Exception e) {
			log.error("Failed to process order.created event: orderId={}, orderNumber={}",
					event.getOrderId(), event.getOrderNumber(), e);
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

		if (payload instanceof OrderCreatedEvent event) {
			log.error("DLQ 처리 필요 - order.created 실패: orderId={}, orderNumber={}",
					event.getOrderId(), event.getOrderNumber());
		} else {
			log.error("DLQ 알 수 없는 payload 타입: {}", payload.getClass().getName());
		}
	}
}

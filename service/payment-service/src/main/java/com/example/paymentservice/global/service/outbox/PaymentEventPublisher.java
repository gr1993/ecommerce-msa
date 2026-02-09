package com.example.paymentservice.global.service.outbox;

import com.example.paymentservice.domain.entity.Outbox;
import com.example.paymentservice.domain.event.PaymentCancelledEvent;
import com.example.paymentservice.domain.event.PaymentConfirmedEvent;
import com.example.paymentservice.global.common.EventTypeConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final ObjectMapper objectMapper;

	public void publishPaymentCancelledEvent(Outbox outbox) {
		String topic = EventTypeConstants.TOPIC_PAYMENT_CANCELLED;
		String key = buildKey(outbox.getAggregateType(), outbox.getAggregateId());

		try {
			PaymentCancelledEvent event = objectMapper.readValue(outbox.getPayload(), PaymentCancelledEvent.class);
			kafkaTemplate.send(topic, key, event).get();
			log.debug("Kafka 메시지 전송 성공: topic={}, key={}", topic, key);
		} catch (JsonProcessingException e) {
			log.error("이벤트 역직렬화 실패: topic={}, key={}", topic, key, e);
			throw new RuntimeException("이벤트 역직렬화 실패", e);
		} catch (Exception e) {
			log.error("Kafka 메시지 전송 실패: topic={}, key={}", topic, key, e);
			throw new RuntimeException("Kafka 메시지 전송 실패", e);
		}
	}

	public void publishPaymentConfirmedEvent(Outbox outbox) {
		String topic = EventTypeConstants.TOPIC_PAYMENT_CONFIRMED;
		String key = buildKey(outbox.getAggregateType(), outbox.getAggregateId());

		try {
			PaymentConfirmedEvent event = objectMapper.readValue(outbox.getPayload(), PaymentConfirmedEvent.class);
			kafkaTemplate.send(topic, key, event).get();
			log.debug("Kafka 메시지 전송 성공: topic={}, key={}", topic, key);
		} catch (JsonProcessingException e) {
			log.error("이벤트 역직렬화 실패: topic={}, key={}", topic, key, e);
			throw new RuntimeException("이벤트 역직렬화 실패", e);
		} catch (Exception e) {
			log.error("Kafka 메시지 전송 실패: topic={}, key={}", topic, key, e);
			throw new RuntimeException("Kafka 메시지 전송 실패", e);
		}
	}

	private String buildKey(String aggregateType, String aggregateId) {
		return aggregateType + "-" + aggregateId;
	}
}

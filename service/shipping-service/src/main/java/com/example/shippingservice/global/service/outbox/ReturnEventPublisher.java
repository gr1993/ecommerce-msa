package com.example.shippingservice.global.service.outbox;

import com.example.shippingservice.domain.entity.Outbox;
import com.example.shippingservice.domain.event.ReturnApprovedEvent;
import com.example.shippingservice.domain.event.ReturnCompletedEvent;
import com.example.shippingservice.global.common.EventTypeConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.springwolf.bindings.kafka.annotations.KafkaAsyncOperationBinding;
import io.github.springwolf.bindings.kafka.annotations.KafkaAsyncOperationBinding.KafkaAsyncKey;
import io.github.springwolf.bindings.kafka.annotations.KafkaAsyncOperationBinding.KafkaAsyncMessageBinding;
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation;
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation.Headers;
import io.github.springwolf.core.asyncapi.annotations.AsyncPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.mapping.AbstractJavaTypeMapper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReturnEventPublisher {

	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final ObjectMapper objectMapper;

	@AsyncPublisher(
		operation = @AsyncOperation(
			channelName = EventTypeConstants.TOPIC_RETURN_APPROVED,
			description = "반품 승인 이벤트 발행",
			payloadType = ReturnApprovedEvent.class,
			headers = @Headers(
				schemaName = "ReturnApprovedEventHeaders",
				values = {
					@Headers.Header(
						name = AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME,
						description = "Spring Kafka 타입 ID 헤더 - Consumer의 TYPE_MAPPINGS와 매핑됨",
						value = EventTypeConstants.TYPE_ID_RETURN_APPROVED
					)
				}
			)
		)
	)
	@KafkaAsyncOperationBinding(
		messageBinding = @KafkaAsyncMessageBinding(
			key = @KafkaAsyncKey(
				description = "집계 타입과 집계 ID를 조합한 키 (형식: {aggregateType}-{aggregateId})",
				example = "Return-123"
			)
		)
	)
	public void publishReturnApprovedEvent(Outbox outbox) {
		String topic = EventTypeConstants.TOPIC_RETURN_APPROVED;
		String key = buildKey(outbox.getAggregateType(), outbox.getAggregateId());

		try {
			ReturnApprovedEvent event = objectMapper.readValue(outbox.getPayload(), ReturnApprovedEvent.class);
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

	@AsyncPublisher(
		operation = @AsyncOperation(
			channelName = EventTypeConstants.TOPIC_RETURN_COMPLETED,
			description = "반품 완료 이벤트 발행",
			payloadType = ReturnCompletedEvent.class,
			headers = @Headers(
				schemaName = "ReturnCompletedEventHeaders",
				values = {
					@Headers.Header(
						name = AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME,
						description = "Spring Kafka 타입 ID 헤더 - Consumer의 TYPE_MAPPINGS와 매핑됨",
						value = EventTypeConstants.TYPE_ID_RETURN_COMPLETED
					)
				}
			)
		)
	)
	@KafkaAsyncOperationBinding(
		messageBinding = @KafkaAsyncMessageBinding(
			key = @KafkaAsyncKey(
				description = "집계 타입과 집계 ID를 조합한 키 (형식: {aggregateType}-{aggregateId})",
				example = "Return-123"
			)
		)
	)
	public void publishReturnCompletedEvent(Outbox outbox) {
		String topic = EventTypeConstants.TOPIC_RETURN_COMPLETED;
		String key = buildKey(outbox.getAggregateType(), outbox.getAggregateId());

		try {
			ReturnCompletedEvent event = objectMapper.readValue(outbox.getPayload(), ReturnCompletedEvent.class);
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

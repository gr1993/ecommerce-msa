package com.example.shippingservice.global.service.outbox;

import com.example.shippingservice.domain.entity.Outbox;
import com.example.shippingservice.domain.event.ExchangeCollectingEvent;
import com.example.shippingservice.domain.event.ExchangeCompletedEvent;
import com.example.shippingservice.domain.event.ExchangeReturnCompletedEvent;
import com.example.shippingservice.domain.event.ExchangeShippingEvent;
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
public class ExchangeEventPublisher {

	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final ObjectMapper objectMapper;

	@AsyncPublisher(
		operation = @AsyncOperation(
			channelName = EventTypeConstants.TOPIC_EXCHANGE_COLLECTING,
			description = "교환 회수 시작 이벤트 발행",
			payloadType = ExchangeCollectingEvent.class,
			headers = @Headers(
				schemaName = "ExchangeCollectingEventHeaders",
				values = {
					@Headers.Header(
						name = AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME,
						description = "Spring Kafka 타입 ID 헤더 - Consumer의 TYPE_MAPPINGS와 매핑됨",
						value = EventTypeConstants.TYPE_ID_EXCHANGE_COLLECTING
					)
				}
			)
		)
	)
	@KafkaAsyncOperationBinding(
		messageBinding = @KafkaAsyncMessageBinding(
			key = @KafkaAsyncKey(
				description = "집계 타입과 집계 ID를 조합한 키 (형식: {aggregateType}-{aggregateId})",
				example = "Exchange-123"
			)
		)
	)
	public void publishExchangeCollectingEvent(Outbox outbox) {
		String topic = EventTypeConstants.TOPIC_EXCHANGE_COLLECTING;
		String key = buildKey(outbox.getAggregateType(), outbox.getAggregateId());

		try {
			ExchangeCollectingEvent event = objectMapper.readValue(outbox.getPayload(), ExchangeCollectingEvent.class);
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
			channelName = EventTypeConstants.TOPIC_EXCHANGE_RETURN_COMPLETED,
			description = "교환 회수 완료 이벤트 발행 (창고 도착)",
			payloadType = ExchangeReturnCompletedEvent.class,
			headers = @Headers(
				schemaName = "ExchangeReturnCompletedEventHeaders",
				values = {
					@Headers.Header(
						name = AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME,
						description = "Spring Kafka 타입 ID 헤더 - Consumer의 TYPE_MAPPINGS와 매핑됨",
						value = EventTypeConstants.TYPE_ID_EXCHANGE_RETURN_COMPLETED
					)
				}
			)
		)
	)
	@KafkaAsyncOperationBinding(
		messageBinding = @KafkaAsyncMessageBinding(
			key = @KafkaAsyncKey(
				description = "집계 타입과 집계 ID를 조합한 키 (형식: {aggregateType}-{aggregateId})",
				example = "Exchange-123"
			)
		)
	)
	public void publishExchangeReturnCompletedEvent(Outbox outbox) {
		String topic = EventTypeConstants.TOPIC_EXCHANGE_RETURN_COMPLETED;
		String key = buildKey(outbox.getAggregateType(), outbox.getAggregateId());

		try {
			ExchangeReturnCompletedEvent event = objectMapper.readValue(outbox.getPayload(), ExchangeReturnCompletedEvent.class);
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
			channelName = EventTypeConstants.TOPIC_EXCHANGE_SHIPPING,
			description = "교환품 배송 시작 이벤트 발행",
			payloadType = ExchangeShippingEvent.class,
			headers = @Headers(
				schemaName = "ExchangeShippingEventHeaders",
				values = {
					@Headers.Header(
						name = AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME,
						description = "Spring Kafka 타입 ID 헤더 - Consumer의 TYPE_MAPPINGS와 매핑됨",
						value = EventTypeConstants.TYPE_ID_EXCHANGE_SHIPPING
					)
				}
			)
		)
	)
	@KafkaAsyncOperationBinding(
		messageBinding = @KafkaAsyncMessageBinding(
			key = @KafkaAsyncKey(
				description = "집계 타입과 집계 ID를 조합한 키 (형식: {aggregateType}-{aggregateId})",
				example = "Exchange-123"
			)
		)
	)
	public void publishExchangeShippingEvent(Outbox outbox) {
		String topic = EventTypeConstants.TOPIC_EXCHANGE_SHIPPING;
		String key = buildKey(outbox.getAggregateType(), outbox.getAggregateId());

		try {
			ExchangeShippingEvent event = objectMapper.readValue(outbox.getPayload(), ExchangeShippingEvent.class);
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
			channelName = EventTypeConstants.TOPIC_EXCHANGE_COMPLETED,
			description = "교환 최종 완료 이벤트 발행",
			payloadType = ExchangeCompletedEvent.class,
			headers = @Headers(
				schemaName = "ExchangeCompletedEventHeaders",
				values = {
					@Headers.Header(
						name = AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME,
						description = "Spring Kafka 타입 ID 헤더 - Consumer의 TYPE_MAPPINGS와 매핑됨",
						value = EventTypeConstants.TYPE_ID_EXCHANGE_COMPLETED
					)
				}
			)
		)
	)
	@KafkaAsyncOperationBinding(
		messageBinding = @KafkaAsyncMessageBinding(
			key = @KafkaAsyncKey(
				description = "집계 타입과 집계 ID를 조합한 키 (형식: {aggregateType}-{aggregateId})",
				example = "Exchange-123"
			)
		)
	)
	public void publishExchangeCompletedEvent(Outbox outbox) {
		String topic = EventTypeConstants.TOPIC_EXCHANGE_COMPLETED;
		String key = buildKey(outbox.getAggregateType(), outbox.getAggregateId());

		try {
			ExchangeCompletedEvent event = objectMapper.readValue(outbox.getPayload(), ExchangeCompletedEvent.class);
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

package com.example.productservice.global.service.outbox;

import com.example.productservice.global.common.EventTypeConstants;
import com.example.productservice.global.domain.Outbox;
import com.example.productservice.product.domain.event.KeywordCreatedEvent;
import com.example.productservice.product.domain.event.KeywordDeletedEvent;
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
public class KeywordEventPublisher {

	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final ObjectMapper objectMapper;

	@AsyncPublisher(
		operation = @AsyncOperation(
			channelName = EventTypeConstants.TOPIC_KEYWORD_CREATED,
			description = "검색 키워드 등록 이벤트 발행",
			payloadType = KeywordCreatedEvent.class,
			headers = @Headers(
				schemaName = "KeywordCreatedEventHeaders",
				values = {
					@Headers.Header(
						name = AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME,
						description = "Spring Kafka 타입 ID 헤더 - Consumer의 TYPE_MAPPINGS와 매핑됨",
						value = EventTypeConstants.TYPE_ID_KEYWORD_CREATED
					)
				}
			)
		)
	)
	@KafkaAsyncOperationBinding(
		messageBinding = @KafkaAsyncMessageBinding(
			key = @KafkaAsyncKey(
				description = "집계 타입과 집계 ID를 조합한 키 (형식: {aggregateType}-{aggregateId})",
				example = "Keyword-1"
			)
		)
	)
	public void publishKeywordCreatedEvent(Outbox outbox) {
		String topic = EventTypeConstants.TOPIC_KEYWORD_CREATED;
		String key = buildKey(outbox.getAggregateType(), outbox.getAggregateId());

		try {
			KeywordCreatedEvent event = objectMapper.readValue(outbox.getPayload(), KeywordCreatedEvent.class);
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
			channelName = EventTypeConstants.TOPIC_KEYWORD_DELETED,
			description = "검색 키워드 삭제 이벤트 발행",
			payloadType = KeywordDeletedEvent.class,
			headers = @Headers(
				schemaName = "KeywordDeletedEventHeaders",
				values = {
					@Headers.Header(
						name = AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME,
						description = "Spring Kafka 타입 ID 헤더 - Consumer의 TYPE_MAPPINGS와 매핑됨",
						value = EventTypeConstants.TYPE_ID_KEYWORD_DELETED
					)
				}
			)
		)
	)
	@KafkaAsyncOperationBinding(
		messageBinding = @KafkaAsyncMessageBinding(
			key = @KafkaAsyncKey(
				description = "집계 타입과 집계 ID를 조합한 키 (형식: {aggregateType}-{aggregateId})",
				example = "Keyword-1"
			)
		)
	)
	public void publishKeywordDeletedEvent(Outbox outbox) {
		String topic = EventTypeConstants.TOPIC_KEYWORD_DELETED;
		String key = buildKey(outbox.getAggregateType(), outbox.getAggregateId());

		try {
			KeywordDeletedEvent event = objectMapper.readValue(outbox.getPayload(), KeywordDeletedEvent.class);
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

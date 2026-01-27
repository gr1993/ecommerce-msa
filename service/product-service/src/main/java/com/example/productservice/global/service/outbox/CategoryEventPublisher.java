package com.example.productservice.global.service.outbox;

import com.example.productservice.category.domain.event.CategoryCreatedEvent;
import com.example.productservice.category.domain.event.CategoryDeletedEvent;
import com.example.productservice.category.domain.event.CategoryUpdatedEvent;
import com.example.productservice.global.common.EventTypeConstants;
import com.example.productservice.global.domain.Outbox;
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
public class CategoryEventPublisher {

	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final ObjectMapper objectMapper;

	@AsyncPublisher(
		operation = @AsyncOperation(
			channelName = EventTypeConstants.TOPIC_CATEGORY_CREATED,
			description = "카테고리 등록 이벤트 발행",
			payloadType = CategoryCreatedEvent.class,
			headers = @Headers(
				schemaName = "CategoryCreatedEventHeaders",
				values = {
					@Headers.Header(
						name = AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME,
						description = "Spring Kafka 타입 ID 헤더 - Consumer의 TYPE_MAPPINGS와 매핑됨",
						value = EventTypeConstants.TYPE_ID_CATEGORY_CREATED
					)
				}
			)
		)
	)
	@KafkaAsyncOperationBinding(
		messageBinding = @KafkaAsyncMessageBinding(
			key = @KafkaAsyncKey(
				description = "집계 타입과 집계 ID를 조합한 키 (형식: {aggregateType}-{aggregateId})",
				example = "Category-1"
			)
		)
	)
	public void publishCategoryCreatedEvent(Outbox outbox) {
		String topic = EventTypeConstants.TOPIC_CATEGORY_CREATED;
		String key = buildKey(outbox.getAggregateType(), outbox.getAggregateId());

		try {
			CategoryCreatedEvent event = objectMapper.readValue(outbox.getPayload(), CategoryCreatedEvent.class);
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
			channelName = EventTypeConstants.TOPIC_CATEGORY_UPDATED,
			description = "카테고리 수정 이벤트 발행",
			payloadType = CategoryUpdatedEvent.class,
			headers = @Headers(
				schemaName = "CategoryUpdatedEventHeaders",
				values = {
					@Headers.Header(
						name = AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME,
						description = "Spring Kafka 타입 ID 헤더 - Consumer의 TYPE_MAPPINGS와 매핑됨",
						value = EventTypeConstants.TYPE_ID_CATEGORY_UPDATED
					)
				}
			)
		)
	)
	@KafkaAsyncOperationBinding(
		messageBinding = @KafkaAsyncMessageBinding(
			key = @KafkaAsyncKey(
				description = "집계 타입과 집계 ID를 조합한 키 (형식: {aggregateType}-{aggregateId})",
				example = "Category-1"
			)
		)
	)
	public void publishCategoryUpdatedEvent(Outbox outbox) {
		String topic = EventTypeConstants.TOPIC_CATEGORY_UPDATED;
		String key = buildKey(outbox.getAggregateType(), outbox.getAggregateId());

		try {
			CategoryUpdatedEvent event = objectMapper.readValue(outbox.getPayload(), CategoryUpdatedEvent.class);
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
			channelName = EventTypeConstants.TOPIC_CATEGORY_DELETED,
			description = "카테고리 삭제 이벤트 발행",
			payloadType = CategoryDeletedEvent.class,
			headers = @Headers(
				schemaName = "CategoryDeletedEventHeaders",
				values = {
					@Headers.Header(
						name = AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME,
						description = "Spring Kafka 타입 ID 헤더 - Consumer의 TYPE_MAPPINGS와 매핑됨",
						value = EventTypeConstants.TYPE_ID_CATEGORY_DELETED
					)
				}
			)
		)
	)
	@KafkaAsyncOperationBinding(
		messageBinding = @KafkaAsyncMessageBinding(
			key = @KafkaAsyncKey(
				description = "집계 타입과 집계 ID를 조합한 키 (형식: {aggregateType}-{aggregateId})",
				example = "Category-1"
			)
		)
	)
	public void publishCategoryDeletedEvent(Outbox outbox) {
		String topic = EventTypeConstants.TOPIC_CATEGORY_DELETED;
		String key = buildKey(outbox.getAggregateType(), outbox.getAggregateId());

		try {
			CategoryDeletedEvent event = objectMapper.readValue(outbox.getPayload(), CategoryDeletedEvent.class);
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

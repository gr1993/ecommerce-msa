package com.example.userservice.service.outbox;

import com.example.userservice.common.EventTypeConstants;
import com.example.userservice.domain.entity.Outbox;
import com.example.userservice.domain.event.UserRegisteredEvent;
import com.example.userservice.repository.OutboxRepository;
import io.github.springwolf.bindings.kafka.annotations.KafkaAsyncOperationBinding;
import io.github.springwolf.bindings.kafka.annotations.KafkaAsyncOperationBinding.KafkaAsyncKey;
import io.github.springwolf.bindings.kafka.annotations.KafkaAsyncOperationBinding.KafkaAsyncMessageBinding;
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation;
import io.github.springwolf.core.asyncapi.annotations.AsyncPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventPublisher {

	private final OutboxRepository outboxRepository;
	private final KafkaTemplate<String, String> kafkaTemplate;

	/**
	 * DB 트랜잭션으로 Outbox 상태를 관리하고 Kafka 트랜잭션은 
	 * DB-Kafka 간 분산 트랜잭션을 만들지 않기 위해 사용하지 않는다.
	 */
	@Transactional
	public void publishPendingEvents() {
		List<Outbox> pendingEvents = outboxRepository.findByStatusOrderByCreatedAtAsc(Outbox.OutboxStatus.PENDING);

		if (pendingEvents.isEmpty()) {
			log.debug("발행할 PENDING 이벤트가 없습니다.");
			return;
		}

		log.info("발행할 PENDING 이벤트 {}개를 찾았습니다.", pendingEvents.size());

		for (Outbox outbox : pendingEvents) {
			try {
				publishEventByType(outbox);
				outbox.markAsPublished();
				outboxRepository.save(outbox);
				log.info("이벤트 발행 성공: id={}, eventType={}, aggregateId={}", 
					outbox.getId(), outbox.getEventType(), outbox.getAggregateId());
			} catch (Exception e) {
				log.error("이벤트 발행 실패: id={}, eventType={}, aggregateId={}", 
					outbox.getId(), outbox.getEventType(), outbox.getAggregateId(), e);
				outbox.markAsFailed();
				outboxRepository.save(outbox);
			}
		}
	}

	private void publishEventByType(Outbox outbox) {
		String eventType = outbox.getEventType();
		
		if (EventTypeConstants.TOPIC_USER_REGISTERED.equals(eventType)) {
			publishUserRegisteredEvent(outbox);
		} else {
			// 알 수 없는 이벤트 타입은 기본 메서드로 처리
			publishEvent(outbox);
		}
	}

	@AsyncPublisher(
		operation = @AsyncOperation(
			channelName = EventTypeConstants.TOPIC_USER_REGISTERED,
			description = "사용자 등록 이벤트 발행",
			payloadType = UserRegisteredEvent.class
		)
	)
	@KafkaAsyncOperationBinding(
		messageBinding = @KafkaAsyncMessageBinding(
			key = @KafkaAsyncKey(
				description = "집계 타입과 집계 ID를 조합한 키 (형식: {aggregateType}-{aggregateId})",
				example = "User-123"
			)
		)
	)
	private void publishUserRegisteredEvent(Outbox outbox) {
		String topic = EventTypeConstants.TOPIC_USER_REGISTERED;
		String key = buildKey(outbox.getAggregateType(), outbox.getAggregateId());
		String payload = outbox.getPayload();

		try {
			kafkaTemplate.send(topic, key, payload).get();
			log.debug("Kafka 메시지 전송 성공: topic={}, key={}", topic, key);
		} catch (Exception e) {
			log.error("Kafka 메시지 전송 실패: topic={}, key={}", topic, key, e);
			throw new RuntimeException("Kafka 메시지 전송 실패", e);
		}
	}

	private void publishEvent(Outbox outbox) {
		String topic = buildTopic(outbox.getEventType());
		String key = buildKey(outbox.getAggregateType(), outbox.getAggregateId());
		String payload = outbox.getPayload();

		try {
			kafkaTemplate.send(topic, key, payload).get();
			log.debug("Kafka 메시지 전송 성공: topic={}, key={}", topic, key);
		} catch (Exception e) {
			log.error("Kafka 메시지 전송 실패: topic={}, key={}", topic, key, e);
			throw new RuntimeException("Kafka 메시지 전송 실패", e);
		}
	}

	private String buildTopic(String eventType) {
		return eventType;
	}

	private String buildKey(String aggregateType, String aggregateId) {
		return aggregateType + "-" + aggregateId;
	}
}


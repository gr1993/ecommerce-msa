package com.example.authservice.consumer;

import com.example.authservice.common.EventTypeConstants;
import com.example.authservice.domain.entity.FailedMessage;
import com.example.authservice.domain.event.UserRegisteredEvent;
import com.example.authservice.repository.FailedMessageRepository;
import com.example.authservice.service.EventProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * UserRegistered 이벤트 컨슈머
 *
 * 재시도 전략:
 * - 총 3회 재시도 (1초, 2초, 4초 간격으로 지수 백오프)
 * - 모든 재시도 실패 시 DLQ(Dead Letter Queue)로 전송
 * - Idempotency 보장으로 중복 처리 방지
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredEventConsumer {

	private final EventProcessingService eventProcessingService;
	private final FailedMessageRepository failedMessageRepository;
	private final ObjectMapper objectMapper; // DLQ 파싱용

	@RetryableTopic(
		attempts = "4", // 원본 1회 + 재시도 3회 = 총 4회
		backoff = @Backoff(
			delay = 1000,        // 첫 재시도 1초 후
			multiplier = 2.0,    // 지수 백오프 (1초 -> 2초 -> 4초)
			maxDelay = 10000     // 최대 10초
		),
		autoCreateTopics = "false", // 토픽 자동 생성 비활성화 (운영 환경에서는 수동 생성 권장)
		include = {Exception.class}, // 모든 예외에 대해 재시도
		topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE // retry-0, retry-1, retry-2
	)
	@KafkaListener(
		topics = EventTypeConstants.TOPIC_USER_REGISTERED,
		groupId = "${spring.kafka.consumer.group-id}"
	)
	public void consume(
		@Payload UserRegisteredEvent event,
		@Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
		@Header(value = KafkaHeaders.OFFSET, required = false) Long offset
	) {
		log.info("UserRegisteredEvent 수신 - topic: {}, offset: {}, userId: {}, email: {}",
			topic, offset, event.getUserId(), event.getEmail());

		try {
			// JsonDeserializer가 자동으로 역직렬화했으므로 바로 사용 가능
			// 추적용 payload는 event 객체를 JSON으로 직렬화하여 전달
			String eventPayload = objectMapper.writeValueAsString(event);

			// EventProcessingService를 통해 사용자 등록 처리 (Idempotency 보장)
			eventProcessingService.processUserRegisteredEvent(event, eventPayload);

			log.info("UserRegisteredEvent 처리 완료 - email: {}, userId: {}", event.getEmail(), event.getUserId());

		} catch (Exception e) {
			log.error("UserRegisteredEvent 처리 실패 - topic: {}, offset: {}, userId: {}, email: {}",
				topic, offset, event.getUserId(), event.getEmail(), e);
			// 예외를 다시 던져서 재시도 메커니즘이 작동하도록 함
			throw new RuntimeException("이벤트 처리 실패: " + e.getMessage(), e);
		}
	}

	/**
	 * DLQ(Dead Letter Queue) 핸들러
	 * 모든 재시도가 실패한 후 호출됩니다.
	 * 여기서 추가 처리 가능:
	 * 1. 별도 DB 테이블에 저장
	 * 2. 알림 발송 (Slack, Email 등)
	 * 3. 외부 모니터링 시스템에 전송
	 * 4. 수동 처리를 위한 대시보드에 표시
	 */
	@DltHandler
	@Transactional
	public void handleDlt(
		@Payload String message,
		@Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
		@Header(KafkaHeaders.OFFSET) Long offset,
		@Header(value = KafkaHeaders.PARTITION, required = false) Integer partition,
		@Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String exceptionMessage,
		@Header(value = KafkaHeaders.EXCEPTION_STACKTRACE, required = false) String stackTrace
	) {
		log.error("""
			========================================
			DLQ 메시지 수신 (재시도 실패)
			========================================
			Topic: {}
			Partition: {}
			Offset: {}
			Message: {}
			Exception: {}
			StackTrace: {}
			========================================
			""", topic, partition, offset, message, exceptionMessage, stackTrace);

		// 이벤트 타입 추출 (파싱 가능한 경우 USER_REGISTERED, 불가능한 경우 PARSE_FAILED)
		String eventType = "USER_REGISTERED";
		try {
			UserRegisteredEvent event = objectMapper.readValue(message, UserRegisteredEvent.class);
			log.error("DLQ 처리 필요 - userId: {}, email: {}", event.getUserId(), event.getEmail());
		} catch (Exception e) {
			log.error("DLQ 메시지 파싱 실패: {}", message, e);
			eventType = "PARSE_FAILED_" + EventTypeConstants.TOPIC_USER_REGISTERED;
		}

		// 실패 메시지를 DB에 저장하여 수동 재처리 및 모니터링 가능하도록 함
		FailedMessage failedMessage = FailedMessage.builder()
			.topic(topic)
			.eventType(eventType)
			.partition(partition)
			.offset(offset)
			.payload(message)
			.exceptionMessage(exceptionMessage)
			.stackTrace(stackTrace != null && stackTrace.length() > 5000
				? stackTrace.substring(0, 5000)
				: stackTrace)
			.build();

		failedMessageRepository.save(failedMessage);
		log.info("DLQ 메시지 저장 완료 - id: {}, topic: {}, eventType: {}",
			failedMessage.getId(), topic, eventType);
	}
}

package com.example.authservice.consumer;

import com.example.authservice.common.EventTypeConstants;
import com.example.authservice.domain.event.UserRegisteredEvent;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredEventConsumer {

	private final EventProcessingService eventProcessingService;
	private final ObjectMapper objectMapper;

	@RetryableTopic(
			attempts = "4",
			backoff = @Backoff(
					delay = 1000,
					multiplier = 2.0,
					maxDelay = 10000
			),
			autoCreateTopics = "false",
			include = {Exception.class},
			topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
	)
	@KafkaListener(
			topics = EventTypeConstants.TOPIC_USER_REGISTERED,
			groupId = "${spring.kafka.consumer.group-id}",
			containerFactory = "kafkaListenerContainerFactory"
	)
	public void consume(
			@Payload String message,
			@Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
			@Header(KafkaHeaders.OFFSET) Long offset
	) {
		log.info("UserRegisteredEvent 수신 - topic: {}, offset: {}, message: {}", topic, offset, message);

		try {
			UserRegisteredEvent event = objectMapper.readValue(message, UserRegisteredEvent.class);
			eventProcessingService.processUserRegisteredEvent(event, message);
			log.info("UserRegisteredEvent 처리 완료 - email: {}, userId: {}", event.getEmail(), event.getUserId());
		} catch (Exception e) {
			log.error("UserRegisteredEvent 처리 실패 - topic: {}, offset: {}, message: {}", topic, offset, message, e);
			throw new RuntimeException("이벤트 처리 실패: " + e.getMessage(), e);
		}
	}

	@DltHandler
	public void handleDlt(
			@Payload String message,
			@Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
			@Header(KafkaHeaders.OFFSET) Long offset,
			@Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String exceptionMessage,
			@Header(value = KafkaHeaders.EXCEPTION_STACKTRACE, required = false) String stackTrace
	) {
		log.error("""
			========================================
			DLQ 메시지 수신 (재시도 실패)
			========================================
			Topic: {}
			Offset: {}
			Message: {}
			Exception: {}
			StackTrace: {}
			========================================
			""", topic, offset, message, exceptionMessage, stackTrace);

		try {
			UserRegisteredEvent event = objectMapper.readValue(message, UserRegisteredEvent.class);
			log.error("DLQ 처리 필요 - userId: {}, email: {}", event.getUserId(), event.getEmail());
		} catch (Exception e) {
			log.error("DLQ 메시지 파싱 실패: {}", message, e);
		}
	}
}

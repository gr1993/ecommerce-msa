package com.example.authservice.consumer;

import com.example.authservice.common.EventTypeConstants;
import com.example.authservice.domain.event.UserRegisteredEvent;
import com.example.authservice.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredEventConsumer {

	private final AuthService authService;
	private final ObjectMapper objectMapper;

	@KafkaListener(
		topics = EventTypeConstants.TOPIC_USER_REGISTERED,
		groupId = "${spring.kafka.consumer.group-id}"
	)
	public void consume(String message) {
		try {
			log.info("UserRegisteredEvent 수신: {}", message);

			// JSON 문자열을 UserRegisteredEvent로 역직렬화
			UserRegisteredEvent event = objectMapper.readValue(message, UserRegisteredEvent.class);

			// AuthService를 통해 사용자 등록 처리
			authService.registerUserFromEvent(event);

			log.info("UserRegisteredEvent 처리 완료: email={}", event.getEmail());
		} catch (Exception e) {
			log.error("UserRegisteredEvent 처리 실패: message={}", message, e);
			// 실패한 메시지는 DLQ(Dead Letter Queue)로 전송하거나 재시도 로직 추가 가능
			// 현재는 로그만 남기고 계속 진행
		}
	}
}

package com.example.paymentservice.global.service.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventScheduler {

	private final OutboxEventPublisher outboxEventPublisher;

	@Scheduled(fixedRate = 1000)
	public void schedulePublishPendingEvents() {
		try {
			outboxEventPublisher.publishPendingEvents();
		} catch (Exception e) {
			log.error("Outbox 이벤트 발행 스케줄러 실행 중 오류 발생", e);
		}
	}
}

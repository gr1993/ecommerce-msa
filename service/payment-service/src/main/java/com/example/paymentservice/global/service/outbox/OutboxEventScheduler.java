package com.example.paymentservice.global.service.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventScheduler {

	private final OutboxEventPublisher outboxEventPublisher;

	/**
	 * Outbox 이벤트 발행 스케줄러 (분산 락 적용)
	 *
	 * <p>다중 서버 환경에서 ShedLock을 사용하여 동시에 하나의 인스턴스만 실행되도록 보장합니다.
	 * 락 정보는 MongoDB의 'shedLock' 컬렉션에 저장됩니다.</p>
	 *
	 * <b>락 동작 방식:</b>
	 * <ul>
	 *   <li>스케줄러 실행 시 MongoDB에서 락 획득 시도</li>
	 *   <li>락 획득 성공: 작업 실행 후 락 해제</li>
	 *   <li>락 획득 실패: 해당 실행 주기 스킵 (다른 인스턴스가 실행 중)</li>
	 * </ul>
	 *
	 * <b>락 설정값:</b>
	 * <ul>
	 *   <li>lockAtMostFor (PT5S): 락 최대 유지 시간 5초. 노드 장애 시에도 이 시간 후 락 자동 해제 (TTL 역할)</li>
	 *   <li>lockAtLeastFor (PT1S): 락 최소 유지 시간 1초. 작업이 빨리 끝나도 1초간 락 유지하여 중복 실행 방지</li>
	 * </ul>
	 */
	@Scheduled(fixedRate = 1000)
	@SchedulerLock(
		name = "payment_outbox_event_publisher_lock",
		lockAtMostFor = "PT5S",
		lockAtLeastFor = "PT1S"
	)
	public void schedulePublishPendingEvents() {
		try {
			outboxEventPublisher.publishPendingEvents();
		} catch (Exception e) {
			log.error("Outbox 이벤트 발행 스케줄러 실행 중 오류 발생", e);
		}
	}
}

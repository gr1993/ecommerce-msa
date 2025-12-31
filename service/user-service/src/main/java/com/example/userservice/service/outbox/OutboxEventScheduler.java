package com.example.userservice.service.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventScheduler {

	private static final String LOCK_NAME = "outbox_event_publisher_lock";
	private static final int LOCK_TIMEOUT_SECONDS = 0; // 즉시 반환 (대기하지 않음)

	private final OutboxEventPublisher outboxEventPublisher;
	private final JdbcTemplate jdbcTemplate;

	/**
	 * 서버를 두 개 이상 실행시키고 한 서버만 실행되는지 확인한다. (실행부분에 sleep을 추가해야 확인 가능)
	 * 테스트 코드 환경은 H2 DB라 분산 락 획득 통합 테스트 코드를 작성할 수 없다.
	 */
	@Scheduled(fixedRate = 1000) // 1초마다 실행
	public void schedulePublishPendingEvents() {
		boolean lockAcquired = false;
		try {
			// MySQL GET_LOCK() 함수로 분산 락 획득 시도
			Integer lockResult = jdbcTemplate.queryForObject(
				"SELECT GET_LOCK(?, ?)", 
				Integer.class, 
				LOCK_NAME, 
				LOCK_TIMEOUT_SECONDS
			);

			// GET_LOCK() 반환값: 1(성공), 0(타임아웃), NULL(에러)
			if (lockResult != null && lockResult == 1) {
				lockAcquired = true;
				log.info("분산 락 획득 성공: {}", LOCK_NAME);
				outboxEventPublisher.publishPendingEvents();
			} else {
				log.info("분산 락 획득 실패: {} (다른 서버에서 실행 중일 수 있음)", LOCK_NAME);
			}
		} catch (Exception e) {
			log.error("Outbox 이벤트 발행 스케줄러 실행 중 오류 발생", e);
		} finally {
			// 락 획득 성공 시에만 락 해제
			if (lockAcquired) {
				try {
					jdbcTemplate.queryForObject(
						"SELECT RELEASE_LOCK(?)", 
						Integer.class, 
						LOCK_NAME
					);
					log.debug("분산 락 해제 완료: {}", LOCK_NAME);
				} catch (Exception e) {
					log.error("분산 락 해제 중 오류 발생: {}", LOCK_NAME, e);
				}
			}
		}
	}
}


package com.example.orderservice.global.service;

import com.example.orderservice.service.OrderCancellationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancellationScheduler {

	private static final String LOCK_NAME = "order_cancellation_scheduler_lock";
	private static final int LOCK_TIMEOUT_SECONDS = 0;

	private final OrderCancellationService orderCancellationService;
	private final JdbcTemplate jdbcTemplate;

	@Scheduled(fixedRate = 60000) // 1분마다 실행
	public void scheduleCancelExpiredOrders() {
		boolean lockAcquired = false;
		try {
			Integer lockResult = jdbcTemplate.queryForObject(
				"SELECT GET_LOCK(?, ?)",
				Integer.class,
				LOCK_NAME,
				LOCK_TIMEOUT_SECONDS
			);

			if (lockResult != null && lockResult == 1) {
				lockAcquired = true;
				log.info("주문 취소 스케줄러 분산 락 획득 성공: {}", LOCK_NAME);
				orderCancellationService.cancelExpiredOrders();
			} else {
				log.debug("주문 취소 스케줄러 분산 락 획득 실패: {} (다른 서버에서 실행 중일 수 있음)", LOCK_NAME);
			}
		} catch (Exception e) {
			log.error("주문 취소 스케줄러 실행 중 오류 발생", e);
		} finally {
			if (lockAcquired) {
				try {
					jdbcTemplate.queryForObject(
						"SELECT RELEASE_LOCK(?)",
						Integer.class,
						LOCK_NAME
					);
					log.debug("주문 취소 스케줄러 분산 락 해제 완료: {}", LOCK_NAME);
				} catch (Exception e) {
					log.error("주문 취소 스케줄러 분산 락 해제 중 오류 발생: {}", LOCK_NAME, e);
				}
			}
		}
	}
}

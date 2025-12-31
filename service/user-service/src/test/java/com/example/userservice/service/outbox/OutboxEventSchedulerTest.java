package com.example.userservice.service.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventSchedulerTest {

	@Mock
	private OutboxEventPublisher outboxEventPublisher;

	@Mock
	private JdbcTemplate jdbcTemplate;

	@InjectMocks
	private OutboxEventScheduler outboxEventScheduler;

	@Test
	@DisplayName("분산 락 획득 성공 시 이벤트 발행 실행")
	void testSchedulePublishPendingEvents_WhenLockAcquired() {
		// given
		when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), any(Integer.class)))
				.thenReturn(1); // GET_LOCK 성공
		when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
				.thenReturn(1); // RELEASE_LOCK 성공

		// when
		outboxEventScheduler.schedulePublishPendingEvents();

		// then
		verify(jdbcTemplate, times(1)).queryForObject(
				eq("SELECT GET_LOCK(?, ?)"), 
				eq(Integer.class), 
				anyString(), 
				any(Integer.class)
		);
		verify(outboxEventPublisher, times(1)).publishPendingEvents();
		verify(jdbcTemplate, times(1)).queryForObject(
				eq("SELECT RELEASE_LOCK(?)"), 
				eq(Integer.class), 
				anyString()
		);
	}

	@Test
	@DisplayName("분산 락 획득 실패 시 이벤트 발행 미실행")
	void testSchedulePublishPendingEvents_WhenLockNotAcquired() {
		// given
		when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), any(Integer.class)))
				.thenReturn(0); // GET_LOCK 실패 (다른 서버에서 락 보유 중)

		// when
		outboxEventScheduler.schedulePublishPendingEvents();

		// then
		verify(jdbcTemplate, times(1)).queryForObject(
				eq("SELECT GET_LOCK(?, ?)"), 
				eq(Integer.class), 
				anyString(), 
				any(Integer.class)
		);
		verify(outboxEventPublisher, never()).publishPendingEvents();
		verify(jdbcTemplate, never()).queryForObject(
				eq("SELECT RELEASE_LOCK(?)"), 
				eq(Integer.class), 
				anyString()
		);
	}

	@Test
	@DisplayName("GET_LOCK 에러 발생 시 이벤트 발행 미실행")
	void testSchedulePublishPendingEvents_WhenLockError() {
		// given
		when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), any(Integer.class)))
				.thenReturn(null); // GET_LOCK 에러

		// when
		outboxEventScheduler.schedulePublishPendingEvents();

		// then
		verify(jdbcTemplate, times(1)).queryForObject(
				eq("SELECT GET_LOCK(?, ?)"), 
				eq(Integer.class), 
				anyString(), 
				any(Integer.class)
		);
		verify(outboxEventPublisher, never()).publishPendingEvents();
		verify(jdbcTemplate, never()).queryForObject(
				eq("SELECT RELEASE_LOCK(?)"), 
				eq(Integer.class), 
				anyString()
		);
	}

	@Test
	@DisplayName("이벤트 발행 중 예외 발생 시에도 락 해제")
	void testSchedulePublishPendingEvents_WhenPublishThrowsException() {
		// given
		when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), any(Integer.class)))
				.thenReturn(1); // GET_LOCK 성공
		doThrow(new RuntimeException("이벤트 발행 실패"))
				.when(outboxEventPublisher).publishPendingEvents();
		when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
				.thenReturn(1); // RELEASE_LOCK 성공

		// when
		outboxEventScheduler.schedulePublishPendingEvents();

		// then
		verify(outboxEventPublisher, times(1)).publishPendingEvents();
		verify(jdbcTemplate, times(1)).queryForObject(
				eq("SELECT RELEASE_LOCK(?)"), 
				eq(Integer.class), 
				anyString()
		);
	}

	@Test
	@DisplayName("RELEASE_LOCK 실패해도 예외 처리")
	void testSchedulePublishPendingEvents_WhenReleaseLockFails() {
		// given
		when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), any(Integer.class)))
				.thenReturn(1); // GET_LOCK 성공
		when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
				.thenThrow(new RuntimeException("RELEASE_LOCK 실패")); // RELEASE_LOCK 실패

		// when
		outboxEventScheduler.schedulePublishPendingEvents();

		// then
		verify(outboxEventPublisher, times(1)).publishPendingEvents();
		verify(jdbcTemplate, times(1)).queryForObject(
				eq("SELECT RELEASE_LOCK(?)"), 
				eq(Integer.class), 
				anyString()
		);
		// 예외가 발생해도 메서드는 정상 종료되어야 함
	}
}


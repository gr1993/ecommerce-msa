package com.example.userservice.service.outbox;

import com.example.userservice.common.EventTypeConstants;
import com.example.userservice.domain.entity.Outbox;
import com.example.userservice.repository.OutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

	@Mock
	private OutboxRepository outboxRepository;

	@Mock
	private KafkaTemplate<String, String> kafkaTemplate;

	@InjectMocks
	private OutboxEventPublisher outboxEventPublisher;

	@Test
	@DisplayName("PENDING 이벤트가 없을 때 아무것도 발행하지 않음")
	void testPublishPendingEvents_WhenNoPendingEvents() {
		// given
		when(outboxRepository.findByStatusOrderByCreatedAtAsc(Outbox.OutboxStatus.PENDING))
				.thenReturn(new ArrayList<>());

		// when
		outboxEventPublisher.publishPendingEvents();

		// then
		verify(outboxRepository, times(1)).findByStatusOrderByCreatedAtAsc(Outbox.OutboxStatus.PENDING);
		verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
		verify(outboxRepository, never()).save(any(Outbox.class));
	}

	@Test
	@DisplayName("PENDING 이벤트 정상 발행 테스트")
	void testPublishPendingEvents_Success() throws Exception {
		// given
		Outbox outbox1 = Outbox.builder()
				.aggregateType("User")
				.aggregateId("1")
				.eventType(EventTypeConstants.TOPIC_USER_REGISTERED)
				.payload("{\"userId\":1,\"email\":\"test@example.com\"}")
				.build();
		ReflectionTestUtils.setField(outbox1, "id", 1L);
		ReflectionTestUtils.setField(outbox1, "createdAt", LocalDateTime.now());

		Outbox outbox2 = Outbox.builder()
				.aggregateType("User")
				.aggregateId("2")
				.eventType(EventTypeConstants.TOPIC_USER_REGISTERED)
				.payload("{\"userId\":2,\"email\":\"test2@example.com\"}")
				.build();
		ReflectionTestUtils.setField(outbox2, "id", 2L);
		ReflectionTestUtils.setField(outbox2, "createdAt", LocalDateTime.now());

		List<Outbox> pendingEvents = List.of(outbox1, outbox2);

		when(outboxRepository.findByStatusOrderByCreatedAtAsc(Outbox.OutboxStatus.PENDING))
				.thenReturn(pendingEvents);

		CompletableFuture<SendResult<String, String>> future1 = CompletableFuture.completedFuture(new SendResult<>(null, null));
		CompletableFuture<SendResult<String, String>> future2 = CompletableFuture.completedFuture(new SendResult<>(null, null));

		when(kafkaTemplate.send(eq(EventTypeConstants.TOPIC_USER_REGISTERED), eq("User-1"), eq(outbox1.getPayload())))
				.thenReturn(future1);
		when(kafkaTemplate.send(eq(EventTypeConstants.TOPIC_USER_REGISTERED), eq("User-2"), eq(outbox2.getPayload())))
				.thenReturn(future2);

		when(outboxRepository.save(any(Outbox.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// when
		outboxEventPublisher.publishPendingEvents();

		// then
		verify(kafkaTemplate, times(1)).send(EventTypeConstants.TOPIC_USER_REGISTERED, "User-1", outbox1.getPayload());
		verify(kafkaTemplate, times(1)).send(EventTypeConstants.TOPIC_USER_REGISTERED, "User-2", outbox2.getPayload());
		
		ArgumentCaptor<Outbox> outboxCaptor = ArgumentCaptor.forClass(Outbox.class);
		verify(outboxRepository, times(2)).save(outboxCaptor.capture());
		
		List<Outbox> savedOutboxes = outboxCaptor.getAllValues();
		assertThat(savedOutboxes).hasSize(2);
		assertThat(savedOutboxes.get(0).getStatus()).isEqualTo(Outbox.OutboxStatus.PUBLISHED);
		assertThat(savedOutboxes.get(0).getPublishedAt()).isNotNull();
		assertThat(savedOutboxes.get(1).getStatus()).isEqualTo(Outbox.OutboxStatus.PUBLISHED);
		assertThat(savedOutboxes.get(1).getPublishedAt()).isNotNull();
	}

	@Test
	@DisplayName("Kafka 전송 실패 시 FAILED 상태로 변경")
	void testPublishPendingEvents_WhenKafkaSendFails() throws Exception {
		// given
		Outbox outbox = Outbox.builder()
				.aggregateType("User")
				.aggregateId("1")
				.eventType(EventTypeConstants.TOPIC_USER_REGISTERED)
				.payload("{\"userId\":1,\"email\":\"test@example.com\"}")
				.build();
		ReflectionTestUtils.setField(outbox, "id", 1L);
		ReflectionTestUtils.setField(outbox, "createdAt", LocalDateTime.now());

		when(outboxRepository.findByStatusOrderByCreatedAtAsc(Outbox.OutboxStatus.PENDING))
				.thenReturn(List.of(outbox));

		CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
		future.completeExceptionally(new ExecutionException("Kafka 전송 실패", new RuntimeException()));

		when(kafkaTemplate.send(eq(EventTypeConstants.TOPIC_USER_REGISTERED), eq("User-1"), eq(outbox.getPayload())))
				.thenReturn(future);

		when(outboxRepository.save(any(Outbox.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// when
		outboxEventPublisher.publishPendingEvents();

		// then
		verify(kafkaTemplate, times(1)).send(EventTypeConstants.TOPIC_USER_REGISTERED, "User-1", outbox.getPayload());
		
		ArgumentCaptor<Outbox> outboxCaptor = ArgumentCaptor.forClass(Outbox.class);
		verify(outboxRepository, times(1)).save(outboxCaptor.capture());
		
		Outbox savedOutbox = outboxCaptor.getValue();
		assertThat(savedOutbox.getStatus()).isEqualTo(Outbox.OutboxStatus.FAILED);
		assertThat(savedOutbox.getPublishedAt()).isNull();
	}

	@Test
	@DisplayName("여러 이벤트 중 일부만 실패해도 나머지는 정상 발행")
	void testPublishPendingEvents_PartialFailure() throws Exception {
		// given
		Outbox outbox1 = Outbox.builder()
				.aggregateType("User")
				.aggregateId("1")
				.eventType(EventTypeConstants.TOPIC_USER_REGISTERED)
				.payload("{\"userId\":1}")
				.build();
		ReflectionTestUtils.setField(outbox1, "id", 1L);

		Outbox outbox2 = Outbox.builder()
				.aggregateType("User")
				.aggregateId("2")
				.eventType(EventTypeConstants.TOPIC_USER_REGISTERED)
				.payload("{\"userId\":2}")
				.build();
		ReflectionTestUtils.setField(outbox2, "id", 2L);

		when(outboxRepository.findByStatusOrderByCreatedAtAsc(Outbox.OutboxStatus.PENDING))
				.thenReturn(List.of(outbox1, outbox2));

		CompletableFuture<SendResult<String, String>> future1 = new CompletableFuture<>();
		future1.completeExceptionally(new ExecutionException("Kafka 전송 실패", new RuntimeException()));
		CompletableFuture<SendResult<String, String>> future2 = CompletableFuture.completedFuture(new SendResult<>(null, null));

		when(kafkaTemplate.send(eq(EventTypeConstants.TOPIC_USER_REGISTERED), eq("User-1"), eq(outbox1.getPayload())))
				.thenReturn(future1);
		when(kafkaTemplate.send(eq(EventTypeConstants.TOPIC_USER_REGISTERED), eq("User-2"), eq(outbox2.getPayload())))
				.thenReturn(future2);

		when(outboxRepository.save(any(Outbox.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// when
		outboxEventPublisher.publishPendingEvents();

		// then
		ArgumentCaptor<Outbox> outboxCaptor = ArgumentCaptor.forClass(Outbox.class);
		verify(outboxRepository, times(2)).save(outboxCaptor.capture());
		
		List<Outbox> savedOutboxes = outboxCaptor.getAllValues();
		assertThat(savedOutboxes.get(0).getStatus()).isEqualTo(Outbox.OutboxStatus.FAILED);
		assertThat(savedOutboxes.get(1).getStatus()).isEqualTo(Outbox.OutboxStatus.PUBLISHED);
	}

	@Test
	@DisplayName("다양한 이벤트 타입에 대한 Topic 생성 검증")
	void testBuildTopic() {
		// given
		Outbox outbox1 = Outbox.builder()
				.aggregateType("User")
				.aggregateId("1")
				.eventType(EventTypeConstants.TOPIC_USER_REGISTERED)
				.payload("{}")
				.build();

		Outbox outbox2 = Outbox.builder()
				.aggregateType("User")
				.aggregateId("2")
				.eventType(EventTypeConstants.TOPIC_USER_UPDATED)
				.payload("{}")
				.build();

		when(outboxRepository.findByStatusOrderByCreatedAtAsc(Outbox.OutboxStatus.PENDING))
				.thenReturn(List.of(outbox1, outbox2));

		CompletableFuture<SendResult<String, String>> future1 = CompletableFuture.completedFuture(new SendResult<>(null, null));
		CompletableFuture<SendResult<String, String>> future2 = CompletableFuture.completedFuture(new SendResult<>(null, null));

		when(kafkaTemplate.send(anyString(), anyString(), anyString()))
				.thenReturn(future1)
				.thenReturn(future2);

		when(outboxRepository.save(any(Outbox.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// when
		outboxEventPublisher.publishPendingEvents();

		// then
		verify(kafkaTemplate).send(eq(EventTypeConstants.TOPIC_USER_REGISTERED), anyString(), anyString());
		verify(kafkaTemplate).send(eq(EventTypeConstants.TOPIC_USER_UPDATED), anyString(), anyString());
	}

	@Test
	@DisplayName("Key 생성 검증")
	void testBuildKey() {
		// given
		Outbox outbox = Outbox.builder()
				.aggregateType("User")
				.aggregateId("123")
				.eventType(EventTypeConstants.TOPIC_USER_REGISTERED)
				.payload("{}")
				.build();

		when(outboxRepository.findByStatusOrderByCreatedAtAsc(Outbox.OutboxStatus.PENDING))
				.thenReturn(List.of(outbox));

		CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(new SendResult<>(null, null));

		when(kafkaTemplate.send(anyString(), eq("User-123"), anyString()))
				.thenReturn(future);

		when(outboxRepository.save(any(Outbox.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// when
		outboxEventPublisher.publishPendingEvents();

		// then
		verify(kafkaTemplate).send(anyString(), eq("User-123"), anyString());
	}
}


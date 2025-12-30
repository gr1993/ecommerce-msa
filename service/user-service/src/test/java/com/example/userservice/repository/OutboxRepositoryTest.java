package com.example.userservice.repository;

import com.example.userservice.domain.entity.Outbox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class OutboxRepositoryTest {

	@Autowired
	private OutboxRepository outboxRepository;

	@Test
	@DisplayName("Outbox 생성 테스트")
	void testCreateOutbox() {
		// given
		Outbox outbox = Outbox.builder()
				.aggregateType("User")
				.aggregateId("1")
				.eventType("UserRegistered")
				.payload("{\"userId\":1,\"email\":\"test@example.com\"}")
				.build();

		// when
		Outbox savedOutbox = outboxRepository.save(outbox);

		// then
		assertThat(savedOutbox.getId()).isNotNull();
		assertThat(savedOutbox.getAggregateType()).isEqualTo("User");
		assertThat(savedOutbox.getAggregateId()).isEqualTo("1");
		assertThat(savedOutbox.getEventType()).isEqualTo("UserRegistered");
		assertThat(savedOutbox.getPayload()).isEqualTo("{\"userId\":1,\"email\":\"test@example.com\"}");
		assertThat(savedOutbox.getStatus()).isEqualTo(Outbox.OutboxStatus.PENDING);
		assertThat(savedOutbox.getCreatedAt()).isNotNull();
		assertThat(savedOutbox.getPublishedAt()).isNull();
	}

	@Test
	@DisplayName("PENDING 상태의 Outbox 조회 테스트")
	void testFindByStatusPending() {
		// given
		Outbox outbox1 = Outbox.builder()
				.aggregateType("User")
				.aggregateId("1")
				.eventType("UserRegistered")
				.payload("{\"userId\":1}")
				.build();

		Outbox outbox2 = Outbox.builder()
				.aggregateType("User")
				.aggregateId("2")
				.eventType("UserRegistered")
				.payload("{\"userId\":2}")
				.build();

		Outbox outbox3 = Outbox.builder()
				.aggregateType("User")
				.aggregateId("3")
				.eventType("UserRegistered")
				.payload("{\"userId\":3}")
				.build();
		outbox3.markAsPublished();

		outboxRepository.save(outbox1);
		outboxRepository.save(outbox2);
		outboxRepository.save(outbox3);

		// when
		List<Outbox> pendingOutboxes = outboxRepository.findByStatusOrderByCreatedAtAsc(Outbox.OutboxStatus.PENDING);

		// then
		assertThat(pendingOutboxes).hasSize(2);
		assertThat(pendingOutboxes).extracting(Outbox::getAggregateId)
				.containsExactly("1", "2");
	}

	@Test
	@DisplayName("PUBLISHED 상태의 Outbox 조회 테스트")
	void testFindByStatusPublished() {
		// given
		Outbox outbox1 = Outbox.builder()
				.aggregateType("User")
				.aggregateId("1")
				.eventType("UserRegistered")
				.payload("{\"userId\":1}")
				.build();
		outbox1.markAsPublished();

		Outbox outbox2 = Outbox.builder()
				.aggregateType("User")
				.aggregateId("2")
				.eventType("UserRegistered")
				.payload("{\"userId\":2}")
				.build();
		outbox2.markAsPublished();

		Outbox outbox3 = Outbox.builder()
				.aggregateType("User")
				.aggregateId("3")
				.eventType("UserRegistered")
				.payload("{\"userId\":3}")
				.build();

		outboxRepository.save(outbox1);
		outboxRepository.save(outbox2);
		outboxRepository.save(outbox3);

		// when
		List<Outbox> publishedOutboxes = outboxRepository.findByStatusOrderByCreatedAtAsc(Outbox.OutboxStatus.PUBLISHED);

		// then
		assertThat(publishedOutboxes).hasSize(2);
		assertThat(publishedOutboxes).extracting(Outbox::getAggregateId)
				.containsExactly("1", "2");
		assertThat(publishedOutboxes).allMatch(outbox -> outbox.getPublishedAt() != null);
	}

	@Test
	@DisplayName("Outbox 상태 변경 테스트 - PENDING to PUBLISHED")
	void testMarkAsPublished() {
		// given
		Outbox outbox = Outbox.builder()
				.aggregateType("User")
				.aggregateId("1")
				.eventType("UserRegistered")
				.payload("{\"userId\":1}")
				.build();
		Outbox savedOutbox = outboxRepository.save(outbox);

		// when
		savedOutbox.markAsPublished();
		Outbox updatedOutbox = outboxRepository.save(savedOutbox);

		// then
		assertThat(updatedOutbox.getStatus()).isEqualTo(Outbox.OutboxStatus.PUBLISHED);
		assertThat(updatedOutbox.getPublishedAt()).isNotNull();
	}

	@Test
	@DisplayName("Outbox 상태 변경 테스트 - PENDING to FAILED")
	void testMarkAsFailed() {
		// given
		Outbox outbox = Outbox.builder()
				.aggregateType("User")
				.aggregateId("1")
				.eventType("UserRegistered")
				.payload("{\"userId\":1}")
				.build();
		Outbox savedOutbox = outboxRepository.save(outbox);

		// when
		savedOutbox.markAsFailed();
		Outbox updatedOutbox = outboxRepository.save(savedOutbox);

		// then
		assertThat(updatedOutbox.getStatus()).isEqualTo(Outbox.OutboxStatus.FAILED);
		assertThat(updatedOutbox.getPublishedAt()).isNull();
	}

	@Test
	@DisplayName("생성 시간 순서로 정렬 테스트")
	void testOrderByCreatedAtAsc() throws InterruptedException {
		// given
		Outbox outbox1 = Outbox.builder()
				.aggregateType("User")
				.aggregateId("1")
				.eventType("UserRegistered")
				.payload("{\"userId\":1}")
				.build();
		outboxRepository.save(outbox1);

		Thread.sleep(10); // 시간 차이를 만들기 위해

		Outbox outbox2 = Outbox.builder()
				.aggregateType("User")
				.aggregateId("2")
				.eventType("UserRegistered")
				.payload("{\"userId\":2}")
				.build();
		outboxRepository.save(outbox2);

		Thread.sleep(10);

		Outbox outbox3 = Outbox.builder()
				.aggregateType("User")
				.aggregateId("3")
				.eventType("UserRegistered")
				.payload("{\"userId\":3}")
				.build();
		outboxRepository.save(outbox3);

		// when
		List<Outbox> pendingOutboxes = outboxRepository.findByStatusOrderByCreatedAtAsc(Outbox.OutboxStatus.PENDING);

		// then
		assertThat(pendingOutboxes).hasSize(3);
		assertThat(pendingOutboxes.get(0).getCreatedAt())
				.isBeforeOrEqualTo(pendingOutboxes.get(1).getCreatedAt());
		assertThat(pendingOutboxes.get(1).getCreatedAt())
				.isBeforeOrEqualTo(pendingOutboxes.get(2).getCreatedAt());
	}

	@Test
	@DisplayName("빈 결과 조회 테스트")
	void testFindByStatusWithNoResults() {
		// when
		List<Outbox> pendingOutboxes = outboxRepository.findByStatusOrderByCreatedAtAsc(Outbox.OutboxStatus.PENDING);
		List<Outbox> failedOutboxes = outboxRepository.findByStatusOrderByCreatedAtAsc(Outbox.OutboxStatus.FAILED);

		// then
		assertThat(pendingOutboxes).isEmpty();
		assertThat(failedOutboxes).isEmpty();
	}
}


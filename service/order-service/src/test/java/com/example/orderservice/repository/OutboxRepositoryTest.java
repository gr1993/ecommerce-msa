package com.example.orderservice.repository;

import com.example.orderservice.domain.entity.Outbox;
import com.example.orderservice.domain.entity.OutboxStatus;
import org.junit.jupiter.api.BeforeEach;
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

    private Outbox testOutbox;

    @BeforeEach
    void setUp() {
        testOutbox = Outbox.builder()
                .aggregateType("Order")
                .aggregateId("1")
                .eventType("OrderCreated")
                .payload("{\"orderId\": 1, \"status\": \"CREATED\"}")
                .build();
    }

    @Test
    @DisplayName("Outbox 저장 테스트")
    void saveOutbox() {
        // when
        Outbox savedOutbox = outboxRepository.save(testOutbox);

        // then
        assertThat(savedOutbox.getId()).isNotNull();
        assertThat(savedOutbox.getAggregateType()).isEqualTo("Order");
        assertThat(savedOutbox.getAggregateId()).isEqualTo("1");
        assertThat(savedOutbox.getEventType()).isEqualTo("OrderCreated");
        assertThat(savedOutbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(savedOutbox.getCreatedAt()).isNotNull();
        assertThat(savedOutbox.getPublishedAt()).isNull();
    }

    @Test
    @DisplayName("상태로 조회 테스트")
    void findByStatus() {
        // given
        outboxRepository.save(testOutbox);

        Outbox publishedOutbox = Outbox.builder()
                .aggregateType("Order")
                .aggregateId("2")
                .eventType("OrderPaid")
                .payload("{\"orderId\": 2, \"status\": \"PAID\"}")
                .build();
        publishedOutbox = outboxRepository.save(publishedOutbox);
        publishedOutbox.markAsPublished();
        outboxRepository.save(publishedOutbox);

        // when
        List<Outbox> pendingList = outboxRepository.findByStatus(OutboxStatus.PENDING);
        List<Outbox> publishedList = outboxRepository.findByStatus(OutboxStatus.PUBLISHED);

        // then
        assertThat(pendingList).hasSize(1);
        assertThat(publishedList).hasSize(1);
    }

    @Test
    @DisplayName("이벤트 타입과 상태로 조회 테스트")
    void findByEventTypeAndStatus() {
        // given
        outboxRepository.save(testOutbox);

        Outbox anotherOutbox = Outbox.builder()
                .aggregateType("Order")
                .aggregateId("2")
                .eventType("OrderPaid")
                .payload("{\"orderId\": 2}")
                .build();
        outboxRepository.save(anotherOutbox);

        // when
        List<Outbox> result = outboxRepository.findByEventTypeAndStatus("OrderCreated", OutboxStatus.PENDING);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEventType()).isEqualTo("OrderCreated");
    }

    @Test
    @DisplayName("Aggregate 타입과 ID로 조회 테스트")
    void findByAggregateTypeAndAggregateId() {
        // given
        outboxRepository.save(testOutbox);

        // when
        List<Outbox> result = outboxRepository.findByAggregateTypeAndAggregateId("Order", "1");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAggregateId()).isEqualTo("1");
    }

    @Test
    @DisplayName("Published 상태 변경 테스트")
    void markAsPublished() {
        // given
        Outbox savedOutbox = outboxRepository.save(testOutbox);

        // when
        savedOutbox.markAsPublished();
        Outbox updatedOutbox = outboxRepository.save(savedOutbox);

        // then
        assertThat(updatedOutbox.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(updatedOutbox.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Failed 상태 변경 테스트")
    void markAsFailed() {
        // given
        Outbox savedOutbox = outboxRepository.save(testOutbox);

        // when
        savedOutbox.markAsFailed();
        Outbox updatedOutbox = outboxRepository.save(savedOutbox);

        // then
        assertThat(updatedOutbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
    }

    @Test
    @DisplayName("Outbox 삭제 테스트")
    void deleteOutbox() {
        // given
        Outbox savedOutbox = outboxRepository.save(testOutbox);
        Long outboxId = savedOutbox.getId();

        // when
        outboxRepository.deleteById(outboxId);

        // then
        assertThat(outboxRepository.findById(outboxId)).isEmpty();
    }
}

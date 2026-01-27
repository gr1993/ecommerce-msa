package com.example.productservice.global.service.outbox;

import com.example.productservice.global.common.EventTypeConstants;
import com.example.productservice.global.domain.Outbox;
import com.example.productservice.global.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxEventPublisher 테스트")
class OutboxEventPublisherTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private ProductEventPublisher productEventPublisher;

    @Mock
    private CategoryEventPublisher categoryEventPublisher;

    @Mock
    private KeywordEventPublisher keywordEventPublisher;

    @InjectMocks
    private OutboxEventPublisher outboxEventPublisher;

    private Outbox productCreatedOutbox;
    private Outbox productUpdatedOutbox;
    private Outbox categoryCreatedOutbox;
    private Outbox categoryUpdatedOutbox;
    private Outbox categoryDeletedOutbox;
    private Outbox keywordCreatedOutbox;
    private Outbox keywordDeletedOutbox;

    @BeforeEach
    void setUp() {
        productCreatedOutbox = Outbox.builder()
                .aggregateType("Product")
                .aggregateId("1")
                .eventType(EventTypeConstants.TOPIC_PRODUCT_CREATED)
                .payload("{\"productId\":1}")
                .build();

        productUpdatedOutbox = Outbox.builder()
                .aggregateType("Product")
                .aggregateId("1")
                .eventType(EventTypeConstants.TOPIC_PRODUCT_UPDATED)
                .payload("{\"productId\":1}")
                .build();

        categoryCreatedOutbox = Outbox.builder()
                .aggregateType("Category")
                .aggregateId("1")
                .eventType(EventTypeConstants.TOPIC_CATEGORY_CREATED)
                .payload("{\"categoryId\":1}")
                .build();

        categoryUpdatedOutbox = Outbox.builder()
                .aggregateType("Category")
                .aggregateId("1")
                .eventType(EventTypeConstants.TOPIC_CATEGORY_UPDATED)
                .payload("{\"categoryId\":1}")
                .build();

        categoryDeletedOutbox = Outbox.builder()
                .aggregateType("Category")
                .aggregateId("1")
                .eventType(EventTypeConstants.TOPIC_CATEGORY_DELETED)
                .payload("{\"categoryId\":1}")
                .build();

        keywordCreatedOutbox = Outbox.builder()
                .aggregateType("Keyword")
                .aggregateId("1")
                .eventType(EventTypeConstants.TOPIC_KEYWORD_CREATED)
                .payload("{\"keywordId\":1}")
                .build();

        keywordDeletedOutbox = Outbox.builder()
                .aggregateType("Keyword")
                .aggregateId("1")
                .eventType(EventTypeConstants.TOPIC_KEYWORD_DELETED)
                .payload("{\"keywordId\":1}")
                .build();
    }

    @Nested
    @DisplayName("publishPendingEvents")
    class PublishPendingEvents {

        @Test
        @DisplayName("성공 - PENDING 이벤트가 없는 경우")
        void publishPendingEvents_noEvents() {
            // given
            when(outboxRepository.findByStatusOrderByCreatedAtAsc(Outbox.OutboxStatus.PENDING))
                    .thenReturn(List.of());

            // when
            outboxEventPublisher.publishPendingEvents();

            // then
            verify(outboxRepository).findByStatusOrderByCreatedAtAsc(Outbox.OutboxStatus.PENDING);
            verifyNoInteractions(productEventPublisher, categoryEventPublisher, keywordEventPublisher);
        }

        @Test
        @DisplayName("성공 - Product 이벤트를 ProductEventPublisher로 위임")
        void publishPendingEvents_delegateToProductPublisher() {
            // given
            when(outboxRepository.findByStatusOrderByCreatedAtAsc(Outbox.OutboxStatus.PENDING))
                    .thenReturn(List.of(productCreatedOutbox, productUpdatedOutbox));

            // when
            outboxEventPublisher.publishPendingEvents();

            // then
            verify(productEventPublisher).publishProductCreatedEvent(productCreatedOutbox);
            verify(productEventPublisher).publishProductUpdatedEvent(productUpdatedOutbox);
            verify(outboxRepository, times(2)).save(any(Outbox.class));
            verifyNoInteractions(categoryEventPublisher, keywordEventPublisher);
        }

        @Test
        @DisplayName("성공 - Category 이벤트를 CategoryEventPublisher로 위임")
        void publishPendingEvents_delegateToCategoryPublisher() {
            // given
            when(outboxRepository.findByStatusOrderByCreatedAtAsc(Outbox.OutboxStatus.PENDING))
                    .thenReturn(List.of(categoryCreatedOutbox, categoryUpdatedOutbox, categoryDeletedOutbox));

            // when
            outboxEventPublisher.publishPendingEvents();

            // then
            verify(categoryEventPublisher).publishCategoryCreatedEvent(categoryCreatedOutbox);
            verify(categoryEventPublisher).publishCategoryUpdatedEvent(categoryUpdatedOutbox);
            verify(categoryEventPublisher).publishCategoryDeletedEvent(categoryDeletedOutbox);
            verify(outboxRepository, times(3)).save(any(Outbox.class));
            verifyNoInteractions(productEventPublisher, keywordEventPublisher);
        }

        @Test
        @DisplayName("성공 - Keyword 이벤트를 KeywordEventPublisher로 위임")
        void publishPendingEvents_delegateToKeywordPublisher() {
            // given
            when(outboxRepository.findByStatusOrderByCreatedAtAsc(Outbox.OutboxStatus.PENDING))
                    .thenReturn(List.of(keywordCreatedOutbox, keywordDeletedOutbox));

            // when
            outboxEventPublisher.publishPendingEvents();

            // then
            verify(keywordEventPublisher).publishKeywordCreatedEvent(keywordCreatedOutbox);
            verify(keywordEventPublisher).publishKeywordDeletedEvent(keywordDeletedOutbox);
            verify(outboxRepository, times(2)).save(any(Outbox.class));
            verifyNoInteractions(productEventPublisher, categoryEventPublisher);
        }

        @Test
        @DisplayName("성공 - 혼합된 이벤트 처리")
        void publishPendingEvents_mixedEvents() {
            // given
            when(outboxRepository.findByStatusOrderByCreatedAtAsc(Outbox.OutboxStatus.PENDING))
                    .thenReturn(List.of(productCreatedOutbox, categoryCreatedOutbox, keywordCreatedOutbox));

            // when
            outboxEventPublisher.publishPendingEvents();

            // then
            verify(productEventPublisher).publishProductCreatedEvent(productCreatedOutbox);
            verify(categoryEventPublisher).publishCategoryCreatedEvent(categoryCreatedOutbox);
            verify(keywordEventPublisher).publishKeywordCreatedEvent(keywordCreatedOutbox);
            verify(outboxRepository, times(3)).save(any(Outbox.class));
        }

        @Test
        @DisplayName("성공 - 이벤트 발행 후 상태가 PUBLISHED로 변경")
        void publishPendingEvents_markAsPublished() {
            // given
            when(outboxRepository.findByStatusOrderByCreatedAtAsc(Outbox.OutboxStatus.PENDING))
                    .thenReturn(List.of(keywordCreatedOutbox));

            // when
            outboxEventPublisher.publishPendingEvents();

            // then
            assertThat(keywordCreatedOutbox.getStatus()).isEqualTo(Outbox.OutboxStatus.PUBLISHED);
            verify(outboxRepository).save(keywordCreatedOutbox);
        }

        @Test
        @DisplayName("실패 - 이벤트 발행 실패 시 상태가 FAILED로 변경")
        void publishPendingEvents_markAsFailed() {
            // given
            when(outboxRepository.findByStatusOrderByCreatedAtAsc(Outbox.OutboxStatus.PENDING))
                    .thenReturn(List.of(keywordCreatedOutbox));
            doThrow(new RuntimeException("Kafka 전송 실패"))
                    .when(keywordEventPublisher).publishKeywordCreatedEvent(any());

            // when
            outboxEventPublisher.publishPendingEvents();

            // then
            assertThat(keywordCreatedOutbox.getStatus()).isEqualTo(Outbox.OutboxStatus.FAILED);
            verify(outboxRepository).save(keywordCreatedOutbox);
        }

        @Test
        @DisplayName("실패 - 알 수 없는 이벤트 타입")
        void publishPendingEvents_unknownEventType() {
            // given
            Outbox unknownOutbox = Outbox.builder()
                    .aggregateType("Unknown")
                    .aggregateId("1")
                    .eventType("unknown.event")
                    .payload("{}")
                    .build();

            when(outboxRepository.findByStatusOrderByCreatedAtAsc(Outbox.OutboxStatus.PENDING))
                    .thenReturn(List.of(unknownOutbox));

            // when
            outboxEventPublisher.publishPendingEvents();

            // then
            assertThat(unknownOutbox.getStatus()).isEqualTo(Outbox.OutboxStatus.FAILED);
            verify(outboxRepository).save(unknownOutbox);
        }
    }
}

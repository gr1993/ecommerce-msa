package com.example.productservice.global.service.outbox;

import com.example.productservice.global.common.EventTypeConstants;
import com.example.productservice.global.domain.Outbox;
import com.example.productservice.product.domain.event.KeywordCreatedEvent;
import com.example.productservice.product.domain.event.KeywordDeletedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KeywordEventPublisher 테스트")
class KeywordEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private ObjectMapper objectMapper;
    private KeywordEventPublisher keywordEventPublisher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        keywordEventPublisher = new KeywordEventPublisher(kafkaTemplate, objectMapper);
    }

    @Nested
    @DisplayName("publishKeywordCreatedEvent")
    class PublishKeywordCreatedEvent {

        @Test
        @DisplayName("성공 - 키워드 생성 이벤트 발행")
        void publishKeywordCreatedEvent_success() throws Exception {
            // given
            KeywordCreatedEvent event = KeywordCreatedEvent.builder()
                    .keywordId(1L)
                    .productId(100L)
                    .keyword("운동화")
                    .createdAt(LocalDateTime.now())
                    .build();

            String payload = objectMapper.writeValueAsString(event);

            Outbox outbox = Outbox.builder()
                    .aggregateType("Keyword")
                    .aggregateId("1")
                    .eventType(EventTypeConstants.TOPIC_KEYWORD_CREATED)
                    .payload(payload)
                    .build();

            CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

            // when
            keywordEventPublisher.publishKeywordCreatedEvent(outbox);

            // then
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<KeywordCreatedEvent> eventCaptor = ArgumentCaptor.forClass(KeywordCreatedEvent.class);

            verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

            assertThat(topicCaptor.getValue()).isEqualTo(EventTypeConstants.TOPIC_KEYWORD_CREATED);
            assertThat(keyCaptor.getValue()).isEqualTo("Keyword-1");
            assertThat(eventCaptor.getValue().getKeywordId()).isEqualTo(1L);
            assertThat(eventCaptor.getValue().getProductId()).isEqualTo(100L);
            assertThat(eventCaptor.getValue().getKeyword()).isEqualTo("운동화");
        }

        @Test
        @DisplayName("실패 - 잘못된 JSON 페이로드")
        void publishKeywordCreatedEvent_invalidJson() {
            // given
            Outbox outbox = Outbox.builder()
                    .aggregateType("Keyword")
                    .aggregateId("1")
                    .eventType(EventTypeConstants.TOPIC_KEYWORD_CREATED)
                    .payload("invalid json")
                    .build();

            // when & then
            assertThatThrownBy(() -> keywordEventPublisher.publishKeywordCreatedEvent(outbox))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("이벤트 역직렬화 실패");

            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        @DisplayName("실패 - Kafka 전송 실패")
        void publishKeywordCreatedEvent_kafkaFailure() throws Exception {
            // given
            KeywordCreatedEvent event = KeywordCreatedEvent.builder()
                    .keywordId(1L)
                    .productId(100L)
                    .keyword("운동화")
                    .createdAt(LocalDateTime.now())
                    .build();

            String payload = objectMapper.writeValueAsString(event);

            Outbox outbox = Outbox.builder()
                    .aggregateType("Keyword")
                    .aggregateId("1")
                    .eventType(EventTypeConstants.TOPIC_KEYWORD_CREATED)
                    .payload(payload)
                    .build();

            CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Kafka 전송 실패"));
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

            // when & then
            assertThatThrownBy(() -> keywordEventPublisher.publishKeywordCreatedEvent(outbox))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Kafka 메시지 전송 실패");
        }
    }

    @Nested
    @DisplayName("publishKeywordDeletedEvent")
    class PublishKeywordDeletedEvent {

        @Test
        @DisplayName("성공 - 키워드 삭제 이벤트 발행")
        void publishKeywordDeletedEvent_success() throws Exception {
            // given
            KeywordDeletedEvent event = KeywordDeletedEvent.builder()
                    .keywordId(1L)
                    .productId(100L)
                    .keyword("운동화")
                    .deletedAt(LocalDateTime.now())
                    .build();

            String payload = objectMapper.writeValueAsString(event);

            Outbox outbox = Outbox.builder()
                    .aggregateType("Keyword")
                    .aggregateId("1")
                    .eventType(EventTypeConstants.TOPIC_KEYWORD_DELETED)
                    .payload(payload)
                    .build();

            CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

            // when
            keywordEventPublisher.publishKeywordDeletedEvent(outbox);

            // then
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<KeywordDeletedEvent> eventCaptor = ArgumentCaptor.forClass(KeywordDeletedEvent.class);

            verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

            assertThat(topicCaptor.getValue()).isEqualTo(EventTypeConstants.TOPIC_KEYWORD_DELETED);
            assertThat(keyCaptor.getValue()).isEqualTo("Keyword-1");
            assertThat(eventCaptor.getValue().getKeywordId()).isEqualTo(1L);
            assertThat(eventCaptor.getValue().getProductId()).isEqualTo(100L);
            assertThat(eventCaptor.getValue().getKeyword()).isEqualTo("운동화");
        }

        @Test
        @DisplayName("실패 - 잘못된 JSON 페이로드")
        void publishKeywordDeletedEvent_invalidJson() {
            // given
            Outbox outbox = Outbox.builder()
                    .aggregateType("Keyword")
                    .aggregateId("1")
                    .eventType(EventTypeConstants.TOPIC_KEYWORD_DELETED)
                    .payload("invalid json")
                    .build();

            // when & then
            assertThatThrownBy(() -> keywordEventPublisher.publishKeywordDeletedEvent(outbox))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("이벤트 역직렬화 실패");

            verifyNoInteractions(kafkaTemplate);
        }
    }
}

package com.example.catalogservice.consumer;

import com.example.catalogservice.consumer.event.KeywordCreatedEvent;
import com.example.catalogservice.consumer.event.KeywordDeletedEvent;
import com.example.catalogservice.service.KeywordSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KeywordEventConsumer 단위 테스트")
class KeywordEventConsumerTest {

    @Mock
    private KeywordSyncService keywordSyncService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private KeywordEventConsumer keywordEventConsumer;

    @Test
    @DisplayName("키워드 생성 이벤트 수신 - 성공")
    void consumeKeywordCreatedEvent_Success() {
        // Given
        KeywordCreatedEvent event = KeywordCreatedEvent.builder()
                .keywordId(100L)
                .productId(1L)
                .keyword("프리미엄")
                .createdAt(LocalDateTime.now())
                .build();

        doNothing().when(keywordSyncService).addKeyword(any(KeywordCreatedEvent.class));

        // When
        keywordEventConsumer.consumeKeywordCreatedEvent(event, "keyword.created", 0L);

        // Then
        verify(keywordSyncService, times(1)).addKeyword(event);
    }

    @Test
    @DisplayName("키워드 생성 이벤트 수신 - KeywordSyncService 예외 발생 시 재전파")
    void consumeKeywordCreatedEvent_ServiceExceptionRethrown() {
        // Given
        KeywordCreatedEvent event = KeywordCreatedEvent.builder()
                .keywordId(100L)
                .productId(1L)
                .keyword("프리미엄")
                .createdAt(LocalDateTime.now())
                .build();

        RuntimeException expectedException = new RuntimeException("Elasticsearch update failed");
        doThrow(expectedException).when(keywordSyncService).addKeyword(any(KeywordCreatedEvent.class));

        // When & Then
        assertThatThrownBy(() -> keywordEventConsumer.consumeKeywordCreatedEvent(event, "keyword.created", 0L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Elasticsearch update failed");

        verify(keywordSyncService, times(1)).addKeyword(event);
    }

    @Test
    @DisplayName("키워드 생성 이벤트 - 상품이 존재하지 않는 경우")
    void consumeKeywordCreatedEvent_ProductNotFound() {
        // Given
        KeywordCreatedEvent event = KeywordCreatedEvent.builder()
                .keywordId(100L)
                .productId(999L)
                .keyword("프리미엄")
                .createdAt(LocalDateTime.now())
                .build();

        IllegalStateException expectedException = new IllegalStateException(
                "Product not found in Elasticsearch: productId=999");
        doThrow(expectedException).when(keywordSyncService).addKeyword(any(KeywordCreatedEvent.class));

        // When & Then
        assertThatThrownBy(() -> keywordEventConsumer.consumeKeywordCreatedEvent(event, "keyword.created", 0L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Product not found in Elasticsearch");

        verify(keywordSyncService, times(1)).addKeyword(event);
    }

    @Test
    @DisplayName("키워드 생성 이벤트 - 최소 필드만 있는 경우")
    void consumeKeywordCreatedEvent_MinimalFields() {
        // Given
        KeywordCreatedEvent event = KeywordCreatedEvent.builder()
                .keywordId(100L)
                .productId(1L)
                .keyword("키워드")
                .build();

        doNothing().when(keywordSyncService).addKeyword(any(KeywordCreatedEvent.class));

        // When
        keywordEventConsumer.consumeKeywordCreatedEvent(event, "keyword.created", 0L);

        // Then
        verify(keywordSyncService, times(1)).addKeyword(event);
    }

    @Test
    @DisplayName("키워드 생성 이벤트 - 특수문자가 포함된 키워드")
    void consumeKeywordCreatedEvent_SpecialCharacters() {
        // Given
        KeywordCreatedEvent event = KeywordCreatedEvent.builder()
                .keywordId(100L)
                .productId(1L)
                .keyword("프리미엄+최고급")
                .createdAt(LocalDateTime.now())
                .build();

        doNothing().when(keywordSyncService).addKeyword(any(KeywordCreatedEvent.class));

        // When
        keywordEventConsumer.consumeKeywordCreatedEvent(event, "keyword.created", 0L);

        // Then
        verify(keywordSyncService, times(1)).addKeyword(event);
    }

    @Test
    @DisplayName("키워드 생성 이벤트 - 영문 키워드")
    void consumeKeywordCreatedEvent_EnglishKeyword() {
        // Given
        KeywordCreatedEvent event = KeywordCreatedEvent.builder()
                .keywordId(100L)
                .productId(1L)
                .keyword("Premium")
                .createdAt(LocalDateTime.now())
                .build();

        doNothing().when(keywordSyncService).addKeyword(any(KeywordCreatedEvent.class));

        // When
        keywordEventConsumer.consumeKeywordCreatedEvent(event, "keyword.created", 0L);

        // Then
        verify(keywordSyncService, times(1)).addKeyword(event);
    }

    @Test
    @DisplayName("키워드 삭제 이벤트 수신 - 성공")
    void consumeKeywordDeletedEvent_Success() {
        // Given
        KeywordDeletedEvent event = KeywordDeletedEvent.builder()
                .keywordId(100L)
                .productId(1L)
                .keyword("프리미엄")
                .deletedAt(LocalDateTime.now())
                .build();

        doNothing().when(keywordSyncService).removeKeyword(any(KeywordDeletedEvent.class));

        // When
        keywordEventConsumer.consumeKeywordDeletedEvent(event, "keyword.deleted", 0L);

        // Then
        verify(keywordSyncService, times(1)).removeKeyword(event);
    }

    @Test
    @DisplayName("키워드 삭제 이벤트 수신 - KeywordSyncService 예외 발생 시 재전파")
    void consumeKeywordDeletedEvent_ServiceExceptionRethrown() {
        // Given
        KeywordDeletedEvent event = KeywordDeletedEvent.builder()
                .keywordId(100L)
                .productId(1L)
                .keyword("프리미엄")
                .deletedAt(LocalDateTime.now())
                .build();

        RuntimeException expectedException = new RuntimeException("Elasticsearch update failed");
        doThrow(expectedException).when(keywordSyncService).removeKeyword(any(KeywordDeletedEvent.class));

        // When & Then
        assertThatThrownBy(() -> keywordEventConsumer.consumeKeywordDeletedEvent(event, "keyword.deleted", 0L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Elasticsearch update failed");

        verify(keywordSyncService, times(1)).removeKeyword(event);
    }

    @Test
    @DisplayName("키워드 삭제 이벤트 - 상품이 존재하지 않는 경우")
    void consumeKeywordDeletedEvent_ProductNotFound() {
        // Given
        KeywordDeletedEvent event = KeywordDeletedEvent.builder()
                .keywordId(100L)
                .productId(999L)
                .keyword("프리미엄")
                .deletedAt(LocalDateTime.now())
                .build();

        IllegalStateException expectedException = new IllegalStateException(
                "Product not found in Elasticsearch: productId=999");
        doThrow(expectedException).when(keywordSyncService).removeKeyword(any(KeywordDeletedEvent.class));

        // When & Then
        assertThatThrownBy(() -> keywordEventConsumer.consumeKeywordDeletedEvent(event, "keyword.deleted", 0L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Product not found in Elasticsearch");

        verify(keywordSyncService, times(1)).removeKeyword(event);
    }

    @Test
    @DisplayName("키워드 삭제 이벤트 - 최소 필드만 있는 경우")
    void consumeKeywordDeletedEvent_MinimalFields() {
        // Given
        KeywordDeletedEvent event = KeywordDeletedEvent.builder()
                .keywordId(100L)
                .productId(1L)
                .keyword("키워드")
                .build();

        doNothing().when(keywordSyncService).removeKeyword(any(KeywordDeletedEvent.class));

        // When
        keywordEventConsumer.consumeKeywordDeletedEvent(event, "keyword.deleted", 0L);

        // Then
        verify(keywordSyncService, times(1)).removeKeyword(event);
    }

    @Test
    @DisplayName("키워드 삭제 이벤트 - 이미 삭제된 키워드 (멱등성 보장)")
    void consumeKeywordDeletedEvent_IdempotentOperation() {
        // Given
        KeywordDeletedEvent event = KeywordDeletedEvent.builder()
                .keywordId(100L)
                .productId(1L)
                .keyword("이미삭제됨")
                .deletedAt(LocalDateTime.now())
                .build();

        doNothing().when(keywordSyncService).removeKeyword(any(KeywordDeletedEvent.class));

        // When
        keywordEventConsumer.consumeKeywordDeletedEvent(event, "keyword.deleted", 0L);

        // Then
        verify(keywordSyncService, times(1)).removeKeyword(event);
    }

    @Test
    @DisplayName("DLQ 핸들러 - keyword.created 이벤트 처리")
    void handleDlt_KeywordCreatedEvent() throws Exception {
        // Given
        String message = """
                {"keywordId":100,"productId":1,"keyword":"프리미엄","createdAt":"2024-01-01T00:00:00"}
                """;

        // When - DLQ 핸들러는 예외를 던지지 않고 로그만 남김
        keywordEventConsumer.handleDlt(
                message,
                "keyword.created-dlt",
                100L,
                "keyword.created",
                "Elasticsearch connection refused"
        );

        // Then - 로그만 남기므로 예외 없이 정상 종료되면 성공
    }

    @Test
    @DisplayName("DLQ 핸들러 - keyword.deleted 이벤트 처리")
    void handleDlt_KeywordDeletedEvent() throws Exception {
        // Given
        String message = """
                {"keywordId":100,"productId":1,"keyword":"프리미엄","deletedAt":"2024-01-01T00:00:00"}
                """;

        // When
        keywordEventConsumer.handleDlt(
                message,
                "keyword.deleted-dlt",
                200L,
                "keyword.deleted",
                "Elasticsearch timeout"
        );

        // Then - 로그만 남기므로 예외 없이 정상 종료되면 성공
    }

    @Test
    @DisplayName("DLQ 핸들러 - 파싱 실패 시에도 예외 없이 처리")
    void handleDlt_ParsingFailure() {
        // Given
        String invalidMessage = "invalid json message";

        // When & Then - 파싱 실패해도 예외 없이 로그만 남김
        keywordEventConsumer.handleDlt(
                invalidMessage,
                "keyword.created-dlt",
                300L,
                "keyword.created",
                "Original exception"
        );
    }

    @Test
    @DisplayName("DLQ 핸들러 - originalTopic이 null인 경우")
    void handleDlt_NullOriginalTopic() {
        // Given
        String message = """
                {"keywordId":100,"productId":1,"keyword":"프리미엄"}
                """;

        // When & Then
        keywordEventConsumer.handleDlt(
                message,
                "unknown-dlt",
                400L,
                null,
                "Unknown error"
        );
    }

    @Test
    @DisplayName("DLQ 핸들러 - 빈 메시지 처리")
    void handleDlt_EmptyMessage() {
        // Given
        String emptyMessage = "";

        // When & Then - 빈 메시지도 예외 없이 로그만 남김
        keywordEventConsumer.handleDlt(
                emptyMessage,
                "keyword.created-dlt",
                500L,
                "keyword.created",
                "Empty message"
        );
    }

    @Test
    @DisplayName("DLQ 핸들러 - originalTopic에 키워드 이벤트 관련 문자열이 없는 경우")
    void handleDlt_UnknownOriginalTopic() {
        // Given
        String message = """
                {"keywordId":100,"productId":1,"keyword":"프리미엄"}
                """;

        // When & Then
        keywordEventConsumer.handleDlt(
                message,
                "some.other.topic-dlt",
                600L,
                "some.other.topic",
                "Unknown topic error"
        );
    }
}

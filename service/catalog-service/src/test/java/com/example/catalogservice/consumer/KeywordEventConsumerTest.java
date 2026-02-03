package com.example.catalogservice.consumer;

import com.example.catalogservice.consumer.event.KeywordCreatedEvent;
import com.example.catalogservice.consumer.event.KeywordDeletedEvent;
import com.example.catalogservice.service.KeywordSyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
    void handleDlt_KeywordCreatedEvent() {
        // Given
        KeywordCreatedEvent event = KeywordCreatedEvent.builder()
                .keywordId(100L)
                .productId(1L)
                .keyword("프리미엄")
                .createdAt(LocalDateTime.now())
                .build();

        // When - DLQ 핸들러는 예외를 던지지 않고 로그만 남김
        keywordEventConsumer.handleDlt(
                event,
                "keyword.created-dlt",
                100L,
                "keyword.created",
                "Elasticsearch connection refused"
        );

        // Then - 로그만 남기므로 예외 없이 정상 종료되면 성공
    }

    @Test
    @DisplayName("DLQ 핸들러 - keyword.deleted 이벤트 처리")
    void handleDlt_KeywordDeletedEvent() {
        // Given
        KeywordDeletedEvent event = KeywordDeletedEvent.builder()
                .keywordId(100L)
                .productId(1L)
                .keyword("프리미엄")
                .deletedAt(LocalDateTime.now())
                .build();

        // When
        keywordEventConsumer.handleDlt(
                event,
                "keyword.deleted-dlt",
                200L,
                "keyword.deleted",
                "Elasticsearch timeout"
        );

        // Then - 로그만 남기므로 예외 없이 정상 종료되면 성공
    }

    @Test
    @DisplayName("DLQ 핸들러 - 알 수 없는 payload 타입")
    void handleDlt_UnknownPayloadType() {
        // Given
        Object unknownPayload = new Object();

        // When & Then - 알 수 없는 타입도 예외 없이 로그만 남김
        keywordEventConsumer.handleDlt(
                unknownPayload,
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
        KeywordCreatedEvent event = KeywordCreatedEvent.builder()
                .keywordId(100L)
                .productId(1L)
                .keyword("프리미엄")
                .createdAt(LocalDateTime.now())
                .build();

        // When & Then
        keywordEventConsumer.handleDlt(
                event,
                "unknown-dlt",
                400L,
                null,
                "Unknown error"
        );
    }

    @Test
    @DisplayName("DLQ 핸들러 - null payload 처리")
    void handleDlt_NullPayload() {
        // Given & When & Then - null payload도 예외 없이 로그만 남김
        keywordEventConsumer.handleDlt(
                null,
                "keyword.created-dlt",
                500L,
                "keyword.created",
                "Null payload"
        );
    }

    @Test
    @DisplayName("DLQ 핸들러 - String payload 처리 (역호환성)")
    void handleDlt_StringPayload() {
        // Given
        String message = "Some legacy string message";

        // When & Then - String도 알 수 없는 타입으로 처리
        keywordEventConsumer.handleDlt(
                message,
                "keyword.created-dlt",
                600L,
                "keyword.created",
                "Legacy message format"
        );
    }
}

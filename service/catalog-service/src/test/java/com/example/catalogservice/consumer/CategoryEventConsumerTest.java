package com.example.catalogservice.consumer;

import com.example.catalogservice.consumer.event.CategoryCreatedEvent;
import com.example.catalogservice.consumer.event.CategoryDeletedEvent;
import com.example.catalogservice.consumer.event.CategoryUpdatedEvent;
import com.example.catalogservice.service.CategorySyncService;
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
@DisplayName("CategoryEventConsumer 단위 테스트")
class CategoryEventConsumerTest {

    @Mock
    private CategorySyncService categorySyncService;

    @InjectMocks
    private CategoryEventConsumer categoryEventConsumer;

    @Test
    @DisplayName("카테고리 생성 이벤트 수신 - 성공")
    void consumeCategoryCreatedEvent_Success() {
        // Given
        CategoryCreatedEvent event = CategoryCreatedEvent.builder()
                .categoryId(1L)
                .categoryName("전자제품")
                .parentId(null)
                .displayOrder(1)
                .isDisplayed(true)
                .createdAt(LocalDateTime.now())
                .build();

        doNothing().when(categorySyncService).syncCategory(any(CategoryCreatedEvent.class));

        // When
        categoryEventConsumer.consumeCategoryCreatedEvent(event, "category.created", 0L);

        // Then
        verify(categorySyncService, times(1)).syncCategory(event);
    }

    @Test
    @DisplayName("카테고리 생성 이벤트 수신 - CategorySyncService 예외 발생 시 재전파")
    void consumeCategoryCreatedEvent_ServiceExceptionRethrown() {
        // Given
        CategoryCreatedEvent event = CategoryCreatedEvent.builder()
                .categoryId(1L)
                .categoryName("전자제품")
                .parentId(null)
                .displayOrder(1)
                .isDisplayed(true)
                .createdAt(LocalDateTime.now())
                .build();

        RuntimeException expectedException = new RuntimeException("Redis save failed");
        doThrow(expectedException).when(categorySyncService).syncCategory(any(CategoryCreatedEvent.class));

        // When & Then
        assertThatThrownBy(() -> categoryEventConsumer.consumeCategoryCreatedEvent(event, "category.created", 0L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Redis save failed");

        verify(categorySyncService, times(1)).syncCategory(event);
    }

    @Test
    @DisplayName("카테고리 생성 이벤트 - 하위 카테고리")
    void consumeCategoryCreatedEvent_SubCategory() {
        // Given
        CategoryCreatedEvent event = CategoryCreatedEvent.builder()
                .categoryId(10L)
                .categoryName("스마트폰")
                .parentId(1L)
                .displayOrder(1)
                .isDisplayed(true)
                .createdAt(LocalDateTime.now())
                .build();

        doNothing().when(categorySyncService).syncCategory(any(CategoryCreatedEvent.class));

        // When
        categoryEventConsumer.consumeCategoryCreatedEvent(event, "category.created", 0L);

        // Then
        verify(categorySyncService, times(1)).syncCategory(event);
    }

    @Test
    @DisplayName("카테고리 생성 이벤트 - 최소 필드만 있는 경우")
    void consumeCategoryCreatedEvent_MinimalFields() {
        // Given
        CategoryCreatedEvent event = CategoryCreatedEvent.builder()
                .categoryId(1L)
                .categoryName("최소 필드 카테고리")
                .createdAt(LocalDateTime.now())
                .build();

        doNothing().when(categorySyncService).syncCategory(any(CategoryCreatedEvent.class));

        // When
        categoryEventConsumer.consumeCategoryCreatedEvent(event, "category.created", 0L);

        // Then
        verify(categorySyncService, times(1)).syncCategory(event);
    }

    @Test
    @DisplayName("카테고리 수정 이벤트 수신 - 성공")
    void consumeCategoryUpdatedEvent_Success() {
        // Given
        CategoryUpdatedEvent event = CategoryUpdatedEvent.builder()
                .categoryId(1L)
                .categoryName("전자제품 (수정됨)")
                .parentId(null)
                .displayOrder(1)
                .isDisplayed(true)
                .updatedAt(LocalDateTime.now())
                .build();

        doNothing().when(categorySyncService).updateCategory(any(CategoryUpdatedEvent.class));

        // When
        categoryEventConsumer.consumeCategoryUpdatedEvent(event, "category.updated", 0L);

        // Then
        verify(categorySyncService, times(1)).updateCategory(event);
    }

    @Test
    @DisplayName("카테고리 수정 이벤트 수신 - CategorySyncService 예외 발생 시 재전파")
    void consumeCategoryUpdatedEvent_ServiceExceptionRethrown() {
        // Given
        CategoryUpdatedEvent event = CategoryUpdatedEvent.builder()
                .categoryId(1L)
                .categoryName("전자제품 (수정됨)")
                .parentId(null)
                .displayOrder(1)
                .isDisplayed(true)
                .updatedAt(LocalDateTime.now())
                .build();

        RuntimeException expectedException = new RuntimeException("Redis update failed");
        doThrow(expectedException).when(categorySyncService).updateCategory(any(CategoryUpdatedEvent.class));

        // When & Then
        assertThatThrownBy(() -> categoryEventConsumer.consumeCategoryUpdatedEvent(event, "category.updated", 0L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Redis update failed");

        verify(categorySyncService, times(1)).updateCategory(event);
    }

    @Test
    @DisplayName("카테고리 수정 이벤트 - 부모 카테고리 변경")
    void consumeCategoryUpdatedEvent_ParentChanged() {
        // Given
        CategoryUpdatedEvent event = CategoryUpdatedEvent.builder()
                .categoryId(10L)
                .categoryName("스마트폰")
                .parentId(2L)  // 부모 카테고리 변경
                .displayOrder(1)
                .isDisplayed(true)
                .updatedAt(LocalDateTime.now())
                .build();

        doNothing().when(categorySyncService).updateCategory(any(CategoryUpdatedEvent.class));

        // When
        categoryEventConsumer.consumeCategoryUpdatedEvent(event, "category.updated", 0L);

        // Then
        verify(categorySyncService, times(1)).updateCategory(event);
    }

    @Test
    @DisplayName("카테고리 수정 이벤트 - 노출 여부 변경")
    void consumeCategoryUpdatedEvent_DisplayStatusChanged() {
        // Given
        CategoryUpdatedEvent event = CategoryUpdatedEvent.builder()
                .categoryId(1L)
                .categoryName("전자제품")
                .parentId(null)
                .displayOrder(1)
                .isDisplayed(false)  // 노출 중지
                .updatedAt(LocalDateTime.now())
                .build();

        doNothing().when(categorySyncService).updateCategory(any(CategoryUpdatedEvent.class));

        // When
        categoryEventConsumer.consumeCategoryUpdatedEvent(event, "category.updated", 0L);

        // Then
        verify(categorySyncService, times(1)).updateCategory(event);
    }

    @Test
    @DisplayName("카테고리 삭제 이벤트 수신 - 성공")
    void consumeCategoryDeletedEvent_Success() {
        // Given
        CategoryDeletedEvent event = CategoryDeletedEvent.builder()
                .categoryId(1L)
                .deletedAt(LocalDateTime.now())
                .build();

        doNothing().when(categorySyncService).deleteCategory(any(CategoryDeletedEvent.class));

        // When
        categoryEventConsumer.consumeCategoryDeletedEvent(event, "category.deleted", 0L);

        // Then
        verify(categorySyncService, times(1)).deleteCategory(event);
    }

    @Test
    @DisplayName("카테고리 삭제 이벤트 수신 - CategorySyncService 예외 발생 시 재전파")
    void consumeCategoryDeletedEvent_ServiceExceptionRethrown() {
        // Given
        CategoryDeletedEvent event = CategoryDeletedEvent.builder()
                .categoryId(1L)
                .deletedAt(LocalDateTime.now())
                .build();

        RuntimeException expectedException = new RuntimeException("Redis delete failed");
        doThrow(expectedException).when(categorySyncService).deleteCategory(any(CategoryDeletedEvent.class));

        // When & Then
        assertThatThrownBy(() -> categoryEventConsumer.consumeCategoryDeletedEvent(event, "category.deleted", 0L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Redis delete failed");

        verify(categorySyncService, times(1)).deleteCategory(event);
    }

    @Test
    @DisplayName("DLQ 핸들러 - category.created 이벤트 처리")
    void handleDlt_CategoryCreatedEvent() {
        // Given
        CategoryCreatedEvent event = CategoryCreatedEvent.builder()
                .categoryId(1L)
                .categoryName("전자제품")
                .parentId(null)
                .displayOrder(1)
                .isDisplayed(true)
                .createdAt(LocalDateTime.now())
                .build();

        // When - DLQ 핸들러는 예외를 던지지 않고 로그만 남김
        categoryEventConsumer.handleDlt(
                event,
                "category.created-dlt",
                100L,
                "category.created",
                "Redis connection refused"
        );

        // Then - 로그만 남기므로 예외 없이 정상 종료되면 성공
    }

    @Test
    @DisplayName("DLQ 핸들러 - category.updated 이벤트 처리")
    void handleDlt_CategoryUpdatedEvent() {
        // Given
        CategoryUpdatedEvent event = CategoryUpdatedEvent.builder()
                .categoryId(1L)
                .categoryName("전자제품 (수정됨)")
                .parentId(null)
                .displayOrder(1)
                .isDisplayed(true)
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        categoryEventConsumer.handleDlt(
                event,
                "category.updated-dlt",
                200L,
                "category.updated",
                "Redis timeout"
        );

        // Then - 로그만 남기므로 예외 없이 정상 종료되면 성공
    }

    @Test
    @DisplayName("DLQ 핸들러 - category.deleted 이벤트 처리")
    void handleDlt_CategoryDeletedEvent() {
        // Given
        CategoryDeletedEvent event = CategoryDeletedEvent.builder()
                .categoryId(1L)
                .deletedAt(LocalDateTime.now())
                .build();

        // When
        categoryEventConsumer.handleDlt(
                event,
                "category.deleted-dlt",
                300L,
                "category.deleted",
                "Redis error"
        );

        // Then - 로그만 남기므로 예외 없이 정상 종료되면 성공
    }

    @Test
    @DisplayName("DLQ 핸들러 - 알 수 없는 payload 타입")
    void handleDlt_UnknownPayloadType() {
        // Given
        Object unknownPayload = new Object();

        // When & Then - 알 수 없는 타입도 예외 없이 로그만 남김
        categoryEventConsumer.handleDlt(
                unknownPayload,
                "category.created-dlt",
                400L,
                "category.created",
                "Original exception"
        );
    }

    @Test
    @DisplayName("DLQ 핸들러 - originalTopic이 null인 경우")
    void handleDlt_NullOriginalTopic() {
        // Given
        CategoryCreatedEvent event = CategoryCreatedEvent.builder()
                .categoryId(1L)
                .categoryName("전자제품")
                .createdAt(LocalDateTime.now())
                .build();

        // When & Then
        categoryEventConsumer.handleDlt(
                event,
                "unknown-dlt",
                500L,
                null,
                "Unknown error"
        );
    }
}

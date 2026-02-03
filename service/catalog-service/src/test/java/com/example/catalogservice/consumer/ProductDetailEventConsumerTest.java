package com.example.catalogservice.consumer;

import com.example.catalogservice.consumer.event.ProductCreatedEvent;
import com.example.catalogservice.consumer.event.ProductUpdatedEvent;
import com.example.catalogservice.service.ProductDetailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductDetailEventConsumer 단위 테스트")
class ProductDetailEventConsumerTest {

    @Mock
    private ProductDetailService productDetailService;

    @InjectMocks
    private ProductDetailEventConsumer productDetailEventConsumer;

    @Test
    @DisplayName("product.created 이벤트 수신 - refreshCache 호출 성공")
    void consumeProductCreatedEvent_Success() {
        // Given
        ProductCreatedEvent event = ProductCreatedEvent.builder()
                .productId(1L)
                .productCode("P001")
                .productName("테스트 상품")
                .description("테스트 설명")
                .basePrice(BigDecimal.valueOf(10000))
                .salePrice(BigDecimal.valueOf(8000))
                .status("ACTIVE")
                .isDisplayed(true)
                .categoryIds(List.of(1L, 2L, 3L))
                .createdAt(LocalDateTime.now())
                .build();

        doNothing().when(productDetailService).refreshCache(any(Long.class));

        // When
        productDetailEventConsumer.consumeProductCreatedEvent(event, "product.created", 0L);

        // Then
        verify(productDetailService, times(1)).refreshCache(1L);
    }

    @Test
    @DisplayName("product.created 이벤트 수신 - refreshCache 실패 시 예외 전파")
    void consumeProductCreatedEvent_RefreshCacheFailure_ExceptionRethrown() {
        // Given
        ProductCreatedEvent event = ProductCreatedEvent.builder()
                .productId(1L)
                .productCode("P001")
                .productName("테스트 상품")
                .description("테스트 설명")
                .basePrice(BigDecimal.valueOf(10000))
                .salePrice(BigDecimal.valueOf(8000))
                .status("ACTIVE")
                .isDisplayed(true)
                .categoryIds(List.of(1L, 2L, 3L))
                .createdAt(LocalDateTime.now())
                .build();

        RuntimeException expectedException = new RuntimeException("Redis connection failed");
        doThrow(expectedException).when(productDetailService).refreshCache(any(Long.class));

        // When & Then
        assertThatThrownBy(() -> productDetailEventConsumer.consumeProductCreatedEvent(event, "product.created", 0L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Redis connection failed");

        verify(productDetailService, times(1)).refreshCache(1L);
    }

    @Test
    @DisplayName("product.updated 이벤트 수신 - refreshCache 호출 성공")
    void consumeProductUpdatedEvent_Success() {
        // Given
        ProductUpdatedEvent event = ProductUpdatedEvent.builder()
                .productId(2L)
                .productCode("P002")
                .productName("수정된 상품")
                .description("수정된 설명")
                .basePrice(BigDecimal.valueOf(15000))
                .salePrice(BigDecimal.valueOf(12000))
                .status("ON_SALE")
                .isDisplayed(true)
                .categoryIds(List.of(1L, 2L))
                .updatedAt(LocalDateTime.now())
                .build();

        doNothing().when(productDetailService).refreshCache(any(Long.class));

        // When
        productDetailEventConsumer.consumeProductUpdatedEvent(event, "product.updated", 0L);

        // Then
        verify(productDetailService, times(1)).refreshCache(2L);
    }

    @Test
    @DisplayName("product.updated 이벤트 수신 - refreshCache 실패 시 예외 전파")
    void consumeProductUpdatedEvent_RefreshCacheFailure_ExceptionRethrown() {
        // Given
        ProductUpdatedEvent event = ProductUpdatedEvent.builder()
                .productId(2L)
                .productCode("P002")
                .productName("수정된 상품")
                .description("수정된 설명")
                .basePrice(BigDecimal.valueOf(15000))
                .salePrice(BigDecimal.valueOf(12000))
                .status("ON_SALE")
                .isDisplayed(true)
                .categoryIds(List.of(1L, 2L))
                .updatedAt(LocalDateTime.now())
                .build();

        RuntimeException expectedException = new RuntimeException("Feign client error");
        doThrow(expectedException).when(productDetailService).refreshCache(any(Long.class));

        // When & Then
        assertThatThrownBy(() -> productDetailEventConsumer.consumeProductUpdatedEvent(event, "product.updated", 0L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Feign client error");

        verify(productDetailService, times(1)).refreshCache(2L);
    }

    @Test
    @DisplayName("DLT 핸들러 - product.created 이벤트 처리")
    void handleDlt_ProductCreatedEvent() {
        // Given
        ProductCreatedEvent event = ProductCreatedEvent.builder()
                .productId(1L)
                .productCode("P001")
                .productName("테스트 상품")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        // When - DLQ 핸들러는 예외를 던지지 않고 로그만 남김
        productDetailEventConsumer.handleDlt(
                event,
                "product.created-detail-dlt",
                100L,
                "product.created",
                "Redis connection timeout"
        );

        // Then - 로그만 남기므로 예외 없이 정상 종료되면 성공
    }

    @Test
    @DisplayName("DLT 핸들러 - product.updated 이벤트 처리")
    void handleDlt_ProductUpdatedEvent() {
        // Given
        ProductUpdatedEvent event = ProductUpdatedEvent.builder()
                .productId(2L)
                .productCode("P002")
                .productName("수정된 상품")
                .status("ON_SALE")
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        productDetailEventConsumer.handleDlt(
                event,
                "product.updated-detail-dlt",
                200L,
                "product.updated",
                "Product service unavailable"
        );

        // Then - 로그만 남기므로 예외 없이 정상 종료되면 성공
    }

    @Test
    @DisplayName("DLT 핸들러 - 알 수 없는 payload 타입")
    void handleDlt_UnknownPayloadType() {
        // Given
        Object unknownPayload = new Object();

        // When & Then - 알 수 없는 타입도 예외 없이 로그만 남김
        productDetailEventConsumer.handleDlt(
                unknownPayload,
                "product.created-detail-dlt",
                300L,
                "product.created",
                "Original exception"
        );
    }

    @Test
    @DisplayName("DLT 핸들러 - originalTopic이 null인 경우")
    void handleDlt_NullOriginalTopic() {
        // Given
        ProductCreatedEvent event = ProductCreatedEvent.builder()
                .productId(1L)
                .productCode("P001")
                .productName("테스트 상품")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        // When & Then
        productDetailEventConsumer.handleDlt(
                event,
                "unknown-dlt",
                400L,
                null,
                "Unknown error"
        );
    }

    @Test
    @DisplayName("product.created 이벤트 - 최소 필드만 있는 경우")
    void consumeProductCreatedEvent_MinimalFields() {
        // Given
        ProductCreatedEvent event = ProductCreatedEvent.builder()
                .productId(3L)
                .productCode("P003")
                .productName("최소 필드 상품")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        doNothing().when(productDetailService).refreshCache(any(Long.class));

        // When
        productDetailEventConsumer.consumeProductCreatedEvent(event, "product.created", 0L);

        // Then
        verify(productDetailService, times(1)).refreshCache(3L);
    }

    @Test
    @DisplayName("product.updated 이벤트 - 카테고리가 비어있는 경우")
    void consumeProductUpdatedEvent_EmptyCategoryIds() {
        // Given
        ProductUpdatedEvent event = ProductUpdatedEvent.builder()
                .productId(4L)
                .productCode("P004")
                .productName("카테고리 없는 상품")
                .description("카테고리 제거됨")
                .basePrice(BigDecimal.valueOf(10000))
                .salePrice(BigDecimal.valueOf(8000))
                .status("ACTIVE")
                .isDisplayed(true)
                .categoryIds(List.of())
                .updatedAt(LocalDateTime.now())
                .build();

        doNothing().when(productDetailService).refreshCache(any(Long.class));

        // When
        productDetailEventConsumer.consumeProductUpdatedEvent(event, "product.updated", 0L);

        // Then
        verify(productDetailService, times(1)).refreshCache(4L);
    }
}

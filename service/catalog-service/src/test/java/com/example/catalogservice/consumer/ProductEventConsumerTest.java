package com.example.catalogservice.consumer;

import com.example.catalogservice.consumer.event.ProductCreatedEvent;
import com.example.catalogservice.consumer.event.ProductUpdatedEvent;
import com.example.catalogservice.service.ProductSyncService;
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
@DisplayName("ProductEventConsumer 단위 테스트")
class ProductEventConsumerTest {

    @Mock
    private ProductSyncService productSyncService;

    @InjectMocks
    private ProductEventConsumer productEventConsumer;

    @Test
    @DisplayName("상품 생성 이벤트 수신 - 성공")
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

        doNothing().when(productSyncService).indexProduct(any(ProductCreatedEvent.class));

        // When
        productEventConsumer.consumeProductCreatedEvent(event, "product.created", 0L);

        // Then
        verify(productSyncService, times(1)).indexProduct(event);
    }

    @Test
    @DisplayName("상품 생성 이벤트 수신 - ProductSyncService 예외 발생 시 재전파")
    void consumeProductCreatedEvent_ServiceExceptionRethrown() {
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

        RuntimeException expectedException = new RuntimeException("Elasticsearch indexing failed");
        doThrow(expectedException).when(productSyncService).indexProduct(any(ProductCreatedEvent.class));

        // When & Then
        assertThatThrownBy(() -> productEventConsumer.consumeProductCreatedEvent(event, "product.created", 0L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Elasticsearch indexing failed");

        verify(productSyncService, times(1)).indexProduct(event);
    }

    @Test
    @DisplayName("상품 수정 이벤트 수신 - 성공")
    void consumeProductUpdatedEvent_Success() {
        // Given
        ProductUpdatedEvent event = ProductUpdatedEvent.builder()
                .productId(1L)
                .productCode("P001")
                .productName("수정된 상품")
                .description("수정된 설명")
                .basePrice(BigDecimal.valueOf(15000))
                .salePrice(BigDecimal.valueOf(12000))
                .status("ON_SALE")
                .isDisplayed(true)
                .categoryIds(List.of(1L, 2L))
                .updatedAt(LocalDateTime.now())
                .build();

        doNothing().when(productSyncService).updateProduct(any(ProductUpdatedEvent.class));

        // When
        productEventConsumer.consumeProductUpdatedEvent(event, "product.updated", 0L);

        // Then
        verify(productSyncService, times(1)).updateProduct(event);
    }

    @Test
    @DisplayName("상품 수정 이벤트 수신 - ProductSyncService 예외 발생 시 재전파")
    void consumeProductUpdatedEvent_ServiceExceptionRethrown() {
        // Given
        ProductUpdatedEvent event = ProductUpdatedEvent.builder()
                .productId(1L)
                .productCode("P001")
                .productName("수정된 상품")
                .description("수정된 설명")
                .basePrice(BigDecimal.valueOf(15000))
                .salePrice(BigDecimal.valueOf(12000))
                .status("ON_SALE")
                .isDisplayed(true)
                .categoryIds(List.of(1L, 2L))
                .updatedAt(LocalDateTime.now())
                .build();

        RuntimeException expectedException = new RuntimeException("Elasticsearch update failed");
        doThrow(expectedException).when(productSyncService).updateProduct(any(ProductUpdatedEvent.class));

        // When & Then
        assertThatThrownBy(() -> productEventConsumer.consumeProductUpdatedEvent(event, "product.updated", 0L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Elasticsearch update failed");

        verify(productSyncService, times(1)).updateProduct(event);
    }

    @Test
    @DisplayName("상품 생성 이벤트 - 최소 필드만 있는 경우")
    void consumeProductCreatedEvent_MinimalFields() {
        // Given
        ProductCreatedEvent event = ProductCreatedEvent.builder()
                .productId(1L)
                .productCode("P001")
                .productName("최소 필드 상품")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        doNothing().when(productSyncService).indexProduct(any(ProductCreatedEvent.class));

        // When
        productEventConsumer.consumeProductCreatedEvent(event, "product.created", 0L);

        // Then
        verify(productSyncService, times(1)).indexProduct(event);
    }

    @Test
    @DisplayName("상품 수정 이벤트 - 카테고리가 비어있는 경우")
    void consumeProductUpdatedEvent_EmptyCategoryIds() {
        // Given
        ProductUpdatedEvent event = ProductUpdatedEvent.builder()
                .productId(1L)
                .productCode("P001")
                .productName("카테고리 없는 상품")
                .description("카테고리 제거됨")
                .basePrice(BigDecimal.valueOf(10000))
                .salePrice(BigDecimal.valueOf(8000))
                .status("ACTIVE")
                .isDisplayed(true)
                .categoryIds(List.of())
                .updatedAt(LocalDateTime.now())
                .build();

        doNothing().when(productSyncService).updateProduct(any(ProductUpdatedEvent.class));

        // When
        productEventConsumer.consumeProductUpdatedEvent(event, "product.updated", 0L);

        // Then
        verify(productSyncService, times(1)).updateProduct(event);
    }

    @Test
    @DisplayName("상품 생성 이벤트 - 판매가가 null인 경우")
    void consumeProductCreatedEvent_NullSalePrice() {
        // Given
        ProductCreatedEvent event = ProductCreatedEvent.builder()
                .productId(1L)
                .productCode("P001")
                .productName("정상가 상품")
                .description("할인 없음")
                .basePrice(BigDecimal.valueOf(10000))
                .salePrice(null)
                .status("ACTIVE")
                .isDisplayed(true)
                .categoryIds(List.of(1L))
                .createdAt(LocalDateTime.now())
                .build();

        doNothing().when(productSyncService).indexProduct(any(ProductCreatedEvent.class));

        // When
        productEventConsumer.consumeProductCreatedEvent(event, "product.created", 0L);

        // Then
        verify(productSyncService, times(1)).indexProduct(event);
    }

    @Test
    @DisplayName("DLQ 핸들러 - product.created 이벤트 처리")
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
        productEventConsumer.handleDlt(
                event,
                "product.created-dlt",
                100L,
                "product.created",
                "Elasticsearch connection refused"
        );

        // Then - 로그만 남기므로 예외 없이 정상 종료되면 성공
    }

    @Test
    @DisplayName("DLQ 핸들러 - product.updated 이벤트 처리")
    void handleDlt_ProductUpdatedEvent() {
        // Given
        ProductUpdatedEvent event = ProductUpdatedEvent.builder()
                .productId(1L)
                .productCode("P001")
                .productName("수정된 상품")
                .status("ON_SALE")
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        productEventConsumer.handleDlt(
                event,
                "product.updated-dlt",
                200L,
                "product.updated",
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
        productEventConsumer.handleDlt(
                unknownPayload,
                "product.created-dlt",
                300L,
                "product.created",
                "Original exception"
        );
    }

    @Test
    @DisplayName("DLQ 핸들러 - originalTopic이 null인 경우")
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
        productEventConsumer.handleDlt(
                event,
                "unknown-dlt",
                400L,
                null,
                "Unknown error"
        );
    }
}

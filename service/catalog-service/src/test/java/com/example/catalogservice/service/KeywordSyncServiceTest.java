package com.example.catalogservice.service;

import com.example.catalogservice.consumer.event.KeywordCreatedEvent;
import com.example.catalogservice.consumer.event.KeywordDeletedEvent;
import com.example.catalogservice.domain.document.ProductDocument;
import com.example.catalogservice.repository.ProductSearchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KeywordSyncService 단위 테스트")
class KeywordSyncServiceTest {

    @Mock
    private ProductSearchRepository productSearchRepository;

    @InjectMocks
    private KeywordSyncService keywordSyncService;

    @Test
    @DisplayName("키워드 추가 - 성공 (기존 키워드가 없는 경우)")
    void addKeyword_Success_NoExistingKeywords() {
        // Given
        Long productId = 1L;
        String keyword = "프리미엄";

        KeywordCreatedEvent event = KeywordCreatedEvent.builder()
                .keywordId(100L)
                .productId(productId)
                .keyword(keyword)
                .createdAt(LocalDateTime.now())
                .build();

        ProductDocument existingDocument = ProductDocument.builder()
                .productId(String.valueOf(productId))
                .productName("테스트 상품")
                .description("테스트 설명")
                .basePrice(10000L)
                .salePrice(8000L)
                .status("ACTIVE")
                .primaryImageUrl("http://example.com/image.jpg")
                .categoryIds(List.of(1L, 2L))
                .searchKeywords(null)  // 키워드 없음
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(productSearchRepository.findById(String.valueOf(productId)))
                .thenReturn(Optional.of(existingDocument));
        when(productSearchRepository.save(any(ProductDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        keywordSyncService.addKeyword(event);

        // Then
        ArgumentCaptor<ProductDocument> captor = ArgumentCaptor.forClass(ProductDocument.class);
        verify(productSearchRepository, times(1)).findById(String.valueOf(productId));
        verify(productSearchRepository, times(1)).save(captor.capture());

        ProductDocument savedDocument = captor.getValue();
        assertThat(savedDocument.getSearchKeywords()).hasSize(1);
        assertThat(savedDocument.getSearchKeywords()).contains(keyword);
    }

    @Test
    @DisplayName("키워드 추가 - 성공 (기존 키워드가 있는 경우)")
    void addKeyword_Success_WithExistingKeywords() {
        // Given
        Long productId = 1L;
        String newKeyword = "신상품";
        List<String> existingKeywords = new ArrayList<>(List.of("프리미엄", "베스트"));

        KeywordCreatedEvent event = KeywordCreatedEvent.builder()
                .keywordId(100L)
                .productId(productId)
                .keyword(newKeyword)
                .createdAt(LocalDateTime.now())
                .build();

        ProductDocument existingDocument = ProductDocument.builder()
                .productId(String.valueOf(productId))
                .productName("테스트 상품")
                .description("테스트 설명")
                .basePrice(10000L)
                .salePrice(8000L)
                .status("ACTIVE")
                .primaryImageUrl("http://example.com/image.jpg")
                .categoryIds(List.of(1L, 2L))
                .searchKeywords(existingKeywords)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(productSearchRepository.findById(String.valueOf(productId)))
                .thenReturn(Optional.of(existingDocument));
        when(productSearchRepository.save(any(ProductDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        keywordSyncService.addKeyword(event);

        // Then
        ArgumentCaptor<ProductDocument> captor = ArgumentCaptor.forClass(ProductDocument.class);
        verify(productSearchRepository, times(1)).save(captor.capture());

        ProductDocument savedDocument = captor.getValue();
        assertThat(savedDocument.getSearchKeywords()).hasSize(3);
        assertThat(savedDocument.getSearchKeywords()).containsExactlyInAnyOrder("프리미엄", "베스트", "신상품");
    }

    @Test
    @DisplayName("키워드 추가 - 중복 키워드는 추가하지 않음")
    void addKeyword_DuplicateKeyword_NotAdded() {
        // Given
        Long productId = 1L;
        String duplicateKeyword = "프리미엄";
        List<String> existingKeywords = new ArrayList<>(List.of("프리미엄", "베스트"));

        KeywordCreatedEvent event = KeywordCreatedEvent.builder()
                .keywordId(100L)
                .productId(productId)
                .keyword(duplicateKeyword)
                .createdAt(LocalDateTime.now())
                .build();

        ProductDocument existingDocument = ProductDocument.builder()
                .productId(String.valueOf(productId))
                .productName("테스트 상품")
                .description("테스트 설명")
                .basePrice(10000L)
                .salePrice(8000L)
                .status("ACTIVE")
                .primaryImageUrl("http://example.com/image.jpg")
                .categoryIds(List.of(1L, 2L))
                .searchKeywords(existingKeywords)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(productSearchRepository.findById(String.valueOf(productId)))
                .thenReturn(Optional.of(existingDocument));
        when(productSearchRepository.save(any(ProductDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        keywordSyncService.addKeyword(event);

        // Then
        ArgumentCaptor<ProductDocument> captor = ArgumentCaptor.forClass(ProductDocument.class);
        verify(productSearchRepository, times(1)).save(captor.capture());

        ProductDocument savedDocument = captor.getValue();
        assertThat(savedDocument.getSearchKeywords()).hasSize(2);
        assertThat(savedDocument.getSearchKeywords()).containsExactlyInAnyOrder("프리미엄", "베스트");
    }

    @Test
    @DisplayName("키워드 추가 - 상품이 Elasticsearch에 존재하지 않으면 예외 발생")
    void addKeyword_ProductNotFound_ThrowsException() {
        // Given
        Long productId = 999L;
        String keyword = "프리미엄";

        KeywordCreatedEvent event = KeywordCreatedEvent.builder()
                .keywordId(100L)
                .productId(productId)
                .keyword(keyword)
                .createdAt(LocalDateTime.now())
                .build();

        when(productSearchRepository.findById(String.valueOf(productId)))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> keywordSyncService.addKeyword(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Product not found in Elasticsearch")
                .hasMessageContaining("productId=" + productId);

        verify(productSearchRepository, times(1)).findById(String.valueOf(productId));
        verify(productSearchRepository, never()).save(any(ProductDocument.class));
    }

    @Test
    @DisplayName("키워드 추가 - 모든 필드가 정상적으로 유지됨")
    void addKeyword_AllFieldsPreserved() {
        // Given
        Long productId = 1L;
        String keyword = "신상품";
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 15, 0, 0);

        KeywordCreatedEvent event = KeywordCreatedEvent.builder()
                .keywordId(100L)
                .productId(productId)
                .keyword(keyword)
                .createdAt(LocalDateTime.now())
                .build();

        ProductDocument existingDocument = ProductDocument.builder()
                .productId(String.valueOf(productId))
                .productName("원본 상품명")
                .description("원본 설명")
                .basePrice(20000L)
                .salePrice(15000L)
                .status("ON_SALE")
                .primaryImageUrl("http://example.com/original.jpg")
                .categoryIds(List.of(1L, 2L, 3L))
                .searchKeywords(List.of("기존키워드"))
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        when(productSearchRepository.findById(String.valueOf(productId)))
                .thenReturn(Optional.of(existingDocument));
        when(productSearchRepository.save(any(ProductDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        keywordSyncService.addKeyword(event);

        // Then
        ArgumentCaptor<ProductDocument> captor = ArgumentCaptor.forClass(ProductDocument.class);
        verify(productSearchRepository, times(1)).save(captor.capture());

        ProductDocument savedDocument = captor.getValue();
        assertThat(savedDocument.getProductId()).isEqualTo(String.valueOf(productId));
        assertThat(savedDocument.getProductName()).isEqualTo("원본 상품명");
        assertThat(savedDocument.getDescription()).isEqualTo("원본 설명");
        assertThat(savedDocument.getBasePrice()).isEqualTo(20000L);
        assertThat(savedDocument.getSalePrice()).isEqualTo(15000L);
        assertThat(savedDocument.getStatus()).isEqualTo("ON_SALE");
        assertThat(savedDocument.getPrimaryImageUrl()).isEqualTo("http://example.com/original.jpg");
        assertThat(savedDocument.getCategoryIds()).containsExactly(1L, 2L, 3L);
        assertThat(savedDocument.getCreatedAt()).isEqualTo(createdAt);
        assertThat(savedDocument.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(savedDocument.getSearchKeywords()).containsExactly("기존키워드", "신상품");
    }

    @Test
    @DisplayName("키워드 삭제 - 성공")
    void removeKeyword_Success() {
        // Given
        Long productId = 1L;
        String keywordToRemove = "프리미엄";
        List<String> existingKeywords = new ArrayList<>(List.of("프리미엄", "베스트", "신상품"));

        KeywordDeletedEvent event = KeywordDeletedEvent.builder()
                .keywordId(100L)
                .productId(productId)
                .keyword(keywordToRemove)
                .deletedAt(LocalDateTime.now())
                .build();

        ProductDocument existingDocument = ProductDocument.builder()
                .productId(String.valueOf(productId))
                .productName("테스트 상품")
                .description("테스트 설명")
                .basePrice(10000L)
                .salePrice(8000L)
                .status("ACTIVE")
                .primaryImageUrl("http://example.com/image.jpg")
                .categoryIds(List.of(1L, 2L))
                .searchKeywords(existingKeywords)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(productSearchRepository.findById(String.valueOf(productId)))
                .thenReturn(Optional.of(existingDocument));
        when(productSearchRepository.save(any(ProductDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        keywordSyncService.removeKeyword(event);

        // Then
        ArgumentCaptor<ProductDocument> captor = ArgumentCaptor.forClass(ProductDocument.class);
        verify(productSearchRepository, times(1)).findById(String.valueOf(productId));
        verify(productSearchRepository, times(1)).save(captor.capture());

        ProductDocument savedDocument = captor.getValue();
        assertThat(savedDocument.getSearchKeywords()).hasSize(2);
        assertThat(savedDocument.getSearchKeywords()).containsExactlyInAnyOrder("베스트", "신상품");
        assertThat(savedDocument.getSearchKeywords()).doesNotContain(keywordToRemove);
    }

    @Test
    @DisplayName("키워드 삭제 - 키워드가 없는 경우 (빈 배열)")
    void removeKeyword_EmptyKeywords() {
        // Given
        Long productId = 1L;
        String keywordToRemove = "프리미엄";

        KeywordDeletedEvent event = KeywordDeletedEvent.builder()
                .keywordId(100L)
                .productId(productId)
                .keyword(keywordToRemove)
                .deletedAt(LocalDateTime.now())
                .build();

        ProductDocument existingDocument = ProductDocument.builder()
                .productId(String.valueOf(productId))
                .productName("테스트 상품")
                .description("테스트 설명")
                .basePrice(10000L)
                .salePrice(8000L)
                .status("ACTIVE")
                .primaryImageUrl("http://example.com/image.jpg")
                .categoryIds(List.of(1L, 2L))
                .searchKeywords(new ArrayList<>())  // 빈 배열
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(productSearchRepository.findById(String.valueOf(productId)))
                .thenReturn(Optional.of(existingDocument));
        when(productSearchRepository.save(any(ProductDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        keywordSyncService.removeKeyword(event);

        // Then
        ArgumentCaptor<ProductDocument> captor = ArgumentCaptor.forClass(ProductDocument.class);
        verify(productSearchRepository, times(1)).save(captor.capture());

        ProductDocument savedDocument = captor.getValue();
        assertThat(savedDocument.getSearchKeywords()).isEmpty();
    }

    @Test
    @DisplayName("키워드 삭제 - 키워드가 null인 경우")
    void removeKeyword_NullKeywords() {
        // Given
        Long productId = 1L;
        String keywordToRemove = "프리미엄";

        KeywordDeletedEvent event = KeywordDeletedEvent.builder()
                .keywordId(100L)
                .productId(productId)
                .keyword(keywordToRemove)
                .deletedAt(LocalDateTime.now())
                .build();

        ProductDocument existingDocument = ProductDocument.builder()
                .productId(String.valueOf(productId))
                .productName("테스트 상품")
                .description("테스트 설명")
                .basePrice(10000L)
                .salePrice(8000L)
                .status("ACTIVE")
                .primaryImageUrl("http://example.com/image.jpg")
                .categoryIds(List.of(1L, 2L))
                .searchKeywords(null)  // null
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(productSearchRepository.findById(String.valueOf(productId)))
                .thenReturn(Optional.of(existingDocument));
        when(productSearchRepository.save(any(ProductDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        keywordSyncService.removeKeyword(event);

        // Then
        ArgumentCaptor<ProductDocument> captor = ArgumentCaptor.forClass(ProductDocument.class);
        verify(productSearchRepository, times(1)).save(captor.capture());

        ProductDocument savedDocument = captor.getValue();
        assertThat(savedDocument.getSearchKeywords()).isEmpty();
    }

    @Test
    @DisplayName("키워드 삭제 - 존재하지 않는 키워드 삭제 시도 (무해함)")
    void removeKeyword_NonExistentKeyword() {
        // Given
        Long productId = 1L;
        String keywordToRemove = "없는키워드";
        List<String> existingKeywords = new ArrayList<>(List.of("프리미엄", "베스트"));

        KeywordDeletedEvent event = KeywordDeletedEvent.builder()
                .keywordId(100L)
                .productId(productId)
                .keyword(keywordToRemove)
                .deletedAt(LocalDateTime.now())
                .build();

        ProductDocument existingDocument = ProductDocument.builder()
                .productId(String.valueOf(productId))
                .productName("테스트 상품")
                .description("테스트 설명")
                .basePrice(10000L)
                .salePrice(8000L)
                .status("ACTIVE")
                .primaryImageUrl("http://example.com/image.jpg")
                .categoryIds(List.of(1L, 2L))
                .searchKeywords(existingKeywords)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(productSearchRepository.findById(String.valueOf(productId)))
                .thenReturn(Optional.of(existingDocument));
        when(productSearchRepository.save(any(ProductDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        keywordSyncService.removeKeyword(event);

        // Then
        ArgumentCaptor<ProductDocument> captor = ArgumentCaptor.forClass(ProductDocument.class);
        verify(productSearchRepository, times(1)).save(captor.capture());

        ProductDocument savedDocument = captor.getValue();
        assertThat(savedDocument.getSearchKeywords()).hasSize(2);
        assertThat(savedDocument.getSearchKeywords()).containsExactlyInAnyOrder("프리미엄", "베스트");
    }

    @Test
    @DisplayName("키워드 삭제 - 상품이 Elasticsearch에 존재하지 않으면 예외 발생")
    void removeKeyword_ProductNotFound_ThrowsException() {
        // Given
        Long productId = 999L;
        String keyword = "프리미엄";

        KeywordDeletedEvent event = KeywordDeletedEvent.builder()
                .keywordId(100L)
                .productId(productId)
                .keyword(keyword)
                .deletedAt(LocalDateTime.now())
                .build();

        when(productSearchRepository.findById(String.valueOf(productId)))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> keywordSyncService.removeKeyword(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Product not found in Elasticsearch")
                .hasMessageContaining("productId=" + productId);

        verify(productSearchRepository, times(1)).findById(String.valueOf(productId));
        verify(productSearchRepository, never()).save(any(ProductDocument.class));
    }

    @Test
    @DisplayName("키워드 삭제 - 모든 필드가 정상적으로 유지됨")
    void removeKeyword_AllFieldsPreserved() {
        // Given
        Long productId = 1L;
        String keywordToRemove = "프리미엄";
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 15, 0, 0);

        KeywordDeletedEvent event = KeywordDeletedEvent.builder()
                .keywordId(100L)
                .productId(productId)
                .keyword(keywordToRemove)
                .deletedAt(LocalDateTime.now())
                .build();

        ProductDocument existingDocument = ProductDocument.builder()
                .productId(String.valueOf(productId))
                .productName("원본 상품명")
                .description("원본 설명")
                .basePrice(20000L)
                .salePrice(15000L)
                .status("ON_SALE")
                .primaryImageUrl("http://example.com/original.jpg")
                .categoryIds(List.of(1L, 2L, 3L))
                .searchKeywords(new ArrayList<>(List.of("프리미엄", "베스트")))
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        when(productSearchRepository.findById(String.valueOf(productId)))
                .thenReturn(Optional.of(existingDocument));
        when(productSearchRepository.save(any(ProductDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        keywordSyncService.removeKeyword(event);

        // Then
        ArgumentCaptor<ProductDocument> captor = ArgumentCaptor.forClass(ProductDocument.class);
        verify(productSearchRepository, times(1)).save(captor.capture());

        ProductDocument savedDocument = captor.getValue();
        assertThat(savedDocument.getProductId()).isEqualTo(String.valueOf(productId));
        assertThat(savedDocument.getProductName()).isEqualTo("원본 상품명");
        assertThat(savedDocument.getDescription()).isEqualTo("원본 설명");
        assertThat(savedDocument.getBasePrice()).isEqualTo(20000L);
        assertThat(savedDocument.getSalePrice()).isEqualTo(15000L);
        assertThat(savedDocument.getStatus()).isEqualTo("ON_SALE");
        assertThat(savedDocument.getPrimaryImageUrl()).isEqualTo("http://example.com/original.jpg");
        assertThat(savedDocument.getCategoryIds()).containsExactly(1L, 2L, 3L);
        assertThat(savedDocument.getCreatedAt()).isEqualTo(createdAt);
        assertThat(savedDocument.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(savedDocument.getSearchKeywords()).containsExactly("베스트");
    }
}

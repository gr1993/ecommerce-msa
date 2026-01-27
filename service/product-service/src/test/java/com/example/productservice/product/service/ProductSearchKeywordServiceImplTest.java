package com.example.productservice.product.service;

import com.example.productservice.global.common.EventTypeConstants;
import com.example.productservice.global.domain.Outbox;
import com.example.productservice.global.repository.OutboxRepository;
import com.example.productservice.product.domain.Product;
import com.example.productservice.product.domain.ProductSearchKeyword;
import com.example.productservice.product.dto.SearchKeywordRequest;
import com.example.productservice.product.dto.SearchKeywordResponse;
import com.example.productservice.product.repository.ProductRepository;
import com.example.productservice.product.repository.ProductSearchKeywordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductSearchKeywordService 테스트")
class ProductSearchKeywordServiceImplTest {

    @Mock
    private ProductSearchKeywordRepository keywordRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OutboxRepository outboxRepository;

    private ObjectMapper objectMapper;

    private ProductSearchKeywordServiceImpl keywordService;

    private Product product;
    private ProductSearchKeyword keyword1;
    private ProductSearchKeyword keyword2;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        keywordService = new ProductSearchKeywordServiceImpl(keywordRepository, productRepository, outboxRepository, objectMapper);

        product = Product.builder()
                .productId(1L)
                .productName("나이키 에어맥스")
                .productCode("NIKE-001")
                .basePrice(new BigDecimal("150000"))
                .status("ACTIVE")
                .isDisplayed(true)
                .searchKeywords(new ArrayList<>())
                .build();

        keyword1 = ProductSearchKeyword.builder()
                .keywordId(1L)
                .product(product)
                .keyword("운동화")
                .createdAt(LocalDateTime.now())
                .build();

        keyword2 = ProductSearchKeyword.builder()
                .keywordId(2L)
                .product(product)
                .keyword("나이키")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("키워드 목록 조회")
    class GetKeywordsByProductId {

        @Test
        @DisplayName("성공 - 상품의 키워드 목록 조회")
        void getKeywordsByProductId_success() {
            // given
            when(productRepository.existsById(1L)).thenReturn(true);
            when(keywordRepository.findByProductProductIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(keyword1, keyword2));

            // when
            List<SearchKeywordResponse> result = keywordService.getKeywordsByProductId(1L);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getKeyword()).isEqualTo("운동화");
            assertThat(result.get(1).getKeyword()).isEqualTo("나이키");

            verify(productRepository).existsById(1L);
            verify(keywordRepository).findByProductProductIdOrderByCreatedAtDesc(1L);
        }

        @Test
        @DisplayName("성공 - 키워드가 없는 상품")
        void getKeywordsByProductId_emptyResult() {
            // given
            when(productRepository.existsById(1L)).thenReturn(true);
            when(keywordRepository.findByProductProductIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of());

            // when
            List<SearchKeywordResponse> result = keywordService.getKeywordsByProductId(1L);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 상품")
        void getKeywordsByProductId_productNotFound() {
            // given
            when(productRepository.existsById(999L)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> keywordService.getKeywordsByProductId(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("상품을 찾을 수 없습니다");

            verify(keywordRepository, never()).findByProductProductIdOrderByCreatedAtDesc(any());
        }
    }

    @Nested
    @DisplayName("키워드 등록")
    class AddKeyword {

        @Test
        @DisplayName("성공 - 키워드 등록 및 이벤트 발행")
        void addKeyword_success() {
            // given
            SearchKeywordRequest request = new SearchKeywordRequest();
            request.setKeyword("에어맥스");

            ProductSearchKeyword savedKeyword = ProductSearchKeyword.builder()
                    .keywordId(3L)
                    .product(product)
                    .keyword("에어맥스")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(keywordRepository.existsByProductProductIdAndKeyword(1L, "에어맥스")).thenReturn(false);
            when(keywordRepository.save(any(ProductSearchKeyword.class))).thenReturn(savedKeyword);
            when(outboxRepository.save(any(Outbox.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            SearchKeywordResponse result = keywordService.addKeyword(1L, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getKeywordId()).isEqualTo(3L);
            assertThat(result.getKeyword()).isEqualTo("에어맥스");

            // Outbox 이벤트 저장 검증
            ArgumentCaptor<Outbox> outboxCaptor = ArgumentCaptor.forClass(Outbox.class);
            verify(outboxRepository).save(outboxCaptor.capture());

            Outbox savedOutbox = outboxCaptor.getValue();
            assertThat(savedOutbox.getAggregateType()).isEqualTo("Keyword");
            assertThat(savedOutbox.getAggregateId()).isEqualTo("3");
            assertThat(savedOutbox.getEventType()).isEqualTo(EventTypeConstants.TOPIC_KEYWORD_CREATED);
            assertThat(savedOutbox.getPayload()).contains("에어맥스");
        }

        @Test
        @DisplayName("성공 - 키워드 앞뒤 공백 제거")
        void addKeyword_trimWhitespace() {
            // given
            SearchKeywordRequest request = new SearchKeywordRequest();
            request.setKeyword("  에어맥스  ");

            ProductSearchKeyword savedKeyword = ProductSearchKeyword.builder()
                    .keywordId(3L)
                    .product(product)
                    .keyword("에어맥스")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(keywordRepository.existsByProductProductIdAndKeyword(1L, "에어맥스")).thenReturn(false);
            when(keywordRepository.save(any(ProductSearchKeyword.class))).thenReturn(savedKeyword);
            when(outboxRepository.save(any(Outbox.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            SearchKeywordResponse result = keywordService.addKeyword(1L, request);

            // then
            assertThat(result.getKeyword()).isEqualTo("에어맥스");
            verify(keywordRepository).existsByProductProductIdAndKeyword(1L, "에어맥스");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 상품")
        void addKeyword_productNotFound() {
            // given
            SearchKeywordRequest request = new SearchKeywordRequest();
            request.setKeyword("에어맥스");

            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> keywordService.addKeyword(999L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("상품을 찾을 수 없습니다");

            verify(keywordRepository, never()).save(any());
            verify(outboxRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패 - 중복 키워드")
        void addKeyword_duplicateKeyword() {
            // given
            SearchKeywordRequest request = new SearchKeywordRequest();
            request.setKeyword("운동화");

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(keywordRepository.existsByProductProductIdAndKeyword(1L, "운동화")).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> keywordService.addKeyword(1L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미 등록된 키워드입니다");

            verify(keywordRepository, never()).save(any());
            verify(outboxRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("키워드 삭제")
    class DeleteKeyword {

        @Test
        @DisplayName("성공 - 키워드 삭제 및 이벤트 발행")
        void deleteKeyword_success() {
            // given
            when(keywordRepository.findById(1L)).thenReturn(Optional.of(keyword1));
            when(outboxRepository.save(any(Outbox.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            keywordService.deleteKeyword(1L, 1L);

            // then
            verify(keywordRepository).delete(keyword1);

            // Outbox 이벤트 저장 검증
            ArgumentCaptor<Outbox> outboxCaptor = ArgumentCaptor.forClass(Outbox.class);
            verify(outboxRepository).save(outboxCaptor.capture());

            Outbox savedOutbox = outboxCaptor.getValue();
            assertThat(savedOutbox.getAggregateType()).isEqualTo("Keyword");
            assertThat(savedOutbox.getAggregateId()).isEqualTo("1");
            assertThat(savedOutbox.getEventType()).isEqualTo(EventTypeConstants.TOPIC_KEYWORD_DELETED);
            assertThat(savedOutbox.getPayload()).contains("운동화");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 키워드")
        void deleteKeyword_keywordNotFound() {
            // given
            when(keywordRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> keywordService.deleteKeyword(1L, 999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("키워드를 찾을 수 없습니다");

            verify(keywordRepository, never()).delete(any());
            verify(outboxRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패 - 상품 ID 불일치")
        void deleteKeyword_productMismatch() {
            // given
            when(keywordRepository.findById(1L)).thenReturn(Optional.of(keyword1));

            // when & then
            assertThatThrownBy(() -> keywordService.deleteKeyword(999L, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("해당 상품의 키워드가 아닙니다");

            verify(keywordRepository, never()).delete(any());
            verify(outboxRepository, never()).save(any());
        }
    }
}

package com.example.catalogservice.service;

import com.example.catalogservice.client.ProductServiceClient;
import com.example.catalogservice.controller.dto.ProductDetailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ProductDetailServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ProductServiceClient productServiceClient;

    @InjectMocks
    private ProductDetailService productDetailService;

    @Test
    @DisplayName("캐시 히트 - Redis에서 상품 상세 정보 조회 성공")
    void getProductDetail_cacheHit() {
        // given
        Long productId = 1L;
        String cacheKey = "product:detail:" + productId;
        ProductDetailResponse cachedResponse = createProductDetailResponse(productId, "맥북 프로");

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(cacheKey)).willReturn(cachedResponse);

        // when
        ProductDetailResponse result = productDetailService.getProductDetail(productId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(productId);
        assertThat(result.getProductName()).isEqualTo("맥북 프로");

        then(redisTemplate).should(times(1)).opsForValue();
        then(valueOperations).should(times(1)).get(cacheKey);
        then(productServiceClient).should(never()).getProductDetail(any());
        then(valueOperations).should(never()).set(any(), any(), any(Duration.class));
    }

    @Test
    @DisplayName("캐시 미스 - product-service 호출 후 Redis에 저장")
    void getProductDetail_cacheMiss_thenFetchAndCache() {
        // given
        Long productId = 2L;
        String cacheKey = "product:detail:" + productId;
        ProductDetailResponse serviceResponse = createProductDetailResponse(productId, "갤럭시 S24");

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(cacheKey)).willReturn(null);
        given(productServiceClient.getProductDetail(productId)).willReturn(serviceResponse);

        // when
        ProductDetailResponse result = productDetailService.getProductDetail(productId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(productId);
        assertThat(result.getProductName()).isEqualTo("갤럭시 S24");

        then(redisTemplate).should(times(2)).opsForValue(); // get, set 2번
        then(valueOperations).should(times(1)).get(cacheKey);
        then(productServiceClient).should(times(1)).getProductDetail(productId);
        then(valueOperations).should(times(1)).set(eq(cacheKey), eq(serviceResponse), eq(Duration.ofMinutes(30)));
    }

    @Test
    @DisplayName("캐시 미스 + product-service에서 null 반환 - Redis 저장하지 않음")
    void getProductDetail_cacheMiss_serviceReturnsNull() {
        // given
        Long productId = 999L;
        String cacheKey = "product:detail:" + productId;

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(cacheKey)).willReturn(null);
        given(productServiceClient.getProductDetail(productId)).willReturn(null);

        // when
        ProductDetailResponse result = productDetailService.getProductDetail(productId);

        // then
        assertThat(result).isNull();

        then(redisTemplate).should(times(1)).opsForValue(); // get만 호출
        then(valueOperations).should(times(1)).get(cacheKey);
        then(productServiceClient).should(times(1)).getProductDetail(productId);
        then(valueOperations).should(never()).set(any(), any(), any(Duration.class));
    }

    @Test
    @DisplayName("캐시 타입 불일치 - 잘못된 타입이 캐시되어 있으면 product-service 호출")
    void getProductDetail_cacheTypeMismatch() {
        // given
        Long productId = 3L;
        String cacheKey = "product:detail:" + productId;
        String wrongTypeCache = "this is not ProductDetailResponse";
        ProductDetailResponse serviceResponse = createProductDetailResponse(productId, "아이폰 15");

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(cacheKey)).willReturn(wrongTypeCache);
        given(productServiceClient.getProductDetail(productId)).willReturn(serviceResponse);

        // when
        ProductDetailResponse result = productDetailService.getProductDetail(productId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(productId);
        assertThat(result.getProductName()).isEqualTo("아이폰 15");

        then(redisTemplate).should(times(2)).opsForValue();
        then(valueOperations).should(times(1)).get(cacheKey);
        then(productServiceClient).should(times(1)).getProductDetail(productId);
        then(valueOperations).should(times(1)).set(eq(cacheKey), eq(serviceResponse), eq(Duration.ofMinutes(30)));
    }

    @Test
    @DisplayName("상품 상세 정보 - 모든 필드 포함")
    void getProductDetail_withAllFields() {
        // given
        Long productId = 10L;
        String cacheKey = "product:detail:" + productId;

        ProductDetailResponse fullResponse = ProductDetailResponse.builder()
                .productId(productId)
                .productName("맥북 프로 16인치")
                .productCode("PROD-001")
                .description("M3 Max 칩이 탑재된 맥북 프로")
                .basePrice(4500000L)
                .salePrice(4300000L)
                .status("ACTIVE")
                .isDisplayed(true)
                .optionGroups(List.of(
                        ProductDetailResponse.OptionGroupResponse.builder()
                                .id(1L)
                                .optionGroupName("색상")
                                .displayOrder(1)
                                .optionValues(List.of(
                                        ProductDetailResponse.OptionValueResponse.builder()
                                                .id(1L)
                                                .optionValueName("스페이스 그레이")
                                                .displayOrder(1)
                                                .build()
                                ))
                                .build()
                ))
                .skus(List.of(
                        ProductDetailResponse.SkuResponse.builder()
                                .id(1L)
                                .skuCode("MACBOOK-PRO-16-GRAY")
                                .price(4300000L)
                                .stockQty(10)
                                .status("ACTIVE")
                                .optionValueIds(List.of(1L))
                                .build()
                ))
                .images(List.of(
                        ProductDetailResponse.ImageResponse.builder()
                                .id(1L)
                                .fileId(100L)
                                .imageUrl("https://example.com/macbook.jpg")
                                .isPrimary(true)
                                .displayOrder(1)
                                .build()
                ))
                .categories(List.of(
                        ProductDetailResponse.CategoryResponse.builder()
                                .categoryId(1L)
                                .categoryName("전자제품")
                                .displayOrder(1)
                                .build()
                ))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(cacheKey)).willReturn(null);
        given(productServiceClient.getProductDetail(productId)).willReturn(fullResponse);

        // when
        ProductDetailResponse result = productDetailService.getProductDetail(productId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(productId);
        assertThat(result.getProductName()).isEqualTo("맥북 프로 16인치");
        assertThat(result.getProductCode()).isEqualTo("PROD-001");
        assertThat(result.getBasePrice()).isEqualTo(4500000L);
        assertThat(result.getSalePrice()).isEqualTo(4300000L);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getIsDisplayed()).isTrue();
        assertThat(result.getOptionGroups()).hasSize(1);
        assertThat(result.getSkus()).hasSize(1);
        assertThat(result.getImages()).hasSize(1);
        assertThat(result.getCategories()).hasSize(1);

        then(productServiceClient).should(times(1)).getProductDetail(productId);
        then(valueOperations).should(times(1)).set(eq(cacheKey), eq(fullResponse), eq(Duration.ofMinutes(30)));
    }

    @Test
    @DisplayName("refreshCache - 정상 캐싱")
    void refreshCache_success() {
        // given
        Long productId = 1L;
        String cacheKey = "product:detail:" + productId;
        ProductDetailResponse serviceResponse = createProductDetailResponse(productId, "맥북 프로");

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(productServiceClient.getProductDetail(productId)).willReturn(serviceResponse);

        // when
        productDetailService.refreshCache(productId);

        // then
        then(productServiceClient).should(times(1)).getProductDetail(productId);
        then(redisTemplate).should(times(1)).opsForValue();
        then(valueOperations).should(times(1)).set(eq(cacheKey), eq(serviceResponse), eq(Duration.ofMinutes(30)));
    }

    @Test
    @DisplayName("refreshCache - product-service가 null 반환 시 캐싱하지 않음")
    void refreshCache_serviceReturnsNull_notCached() {
        // given
        Long productId = 999L;

        given(productServiceClient.getProductDetail(productId)).willReturn(null);

        // when
        productDetailService.refreshCache(productId);

        // then
        then(productServiceClient).should(times(1)).getProductDetail(productId);
        then(redisTemplate).should(never()).opsForValue();
    }

    private ProductDetailResponse createProductDetailResponse(Long productId, String productName) {
        return ProductDetailResponse.builder()
                .productId(productId)
                .productName(productName)
                .productCode("PROD-" + productId)
                .description("상품 설명")
                .basePrice(1000000L)
                .salePrice(900000L)
                .status("ACTIVE")
                .isDisplayed(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}

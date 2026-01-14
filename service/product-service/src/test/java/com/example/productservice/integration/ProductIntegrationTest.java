package com.example.productservice.integration;

import com.example.productservice.domain.entity.Product;
import com.example.productservice.domain.repository.ProductRepository;
import com.example.productservice.dto.FileUploadResponse;
import com.example.productservice.dto.OptionGroupRequest;
import com.example.productservice.dto.OptionValueRequest;
import com.example.productservice.dto.ProductCreateRequest;
import com.example.productservice.dto.ProductDetailResponse;
import com.example.productservice.dto.ProductImageRequest;
import com.example.productservice.dto.ProductResponse;
import com.example.productservice.dto.SkuRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("상품 통합 테스트")
class ProductIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("파일 업로드 후 상품 등록 - 성공")
    void createProductWithFileUpload_success() throws Exception {
        // 1. 파일 업로드
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "product-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/admin/products/files/upload")
                        .file(file))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileId").exists())
                .andExpect(jsonPath("$.url").exists())
                .andReturn();

        // 업로드된 파일 정보 추출
        String uploadResponse = uploadResult.getResponse().getContentAsString();
        FileUploadResponse fileUploadResponse = objectMapper.readValue(uploadResponse, FileUploadResponse.class);

        // 2. 상품 등록 요청 생성
        ProductCreateRequest request = createSampleProductRequest(fileUploadResponse);

        // 3. 상품 등록
        MvcResult createResult = mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").exists())
                .andExpect(jsonPath("$.productName").value("강림 상품"))
                .andExpect(jsonPath("$.basePrice").value(5000))
                .andExpect(jsonPath("$.salePrice").value(4000))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        // 4. DB 검증
        String createResponse = createResult.getResponse().getContentAsString();
        ProductResponse productResponse = objectMapper.readValue(createResponse, ProductResponse.class);

        Product savedProduct = productRepository.findById(productResponse.getProductId()).orElse(null);
        assertThat(savedProduct).isNotNull();
        assertThat(savedProduct.getProductName()).isEqualTo("강림 상품");
        assertThat(savedProduct.getOptionGroups()).hasSize(1);
        assertThat(savedProduct.getOptionGroups().get(0).getOptionValues()).hasSize(2);
        assertThat(savedProduct.getSkus()).hasSize(2);
        assertThat(savedProduct.getImages()).hasSize(1);
    }

    @Test
    @DisplayName("상품 등록 - 이미지 없이 등록 (images가 빈 배열)")
    void createProductWithoutImages_emptyArray() throws Exception {
        // given
        ProductCreateRequest request = ProductCreateRequest.builder()
                .productName("강림 상품")
                .productCode("PRODUCT-001")
                .description("강림 상품입니다.")
                .basePrice(new BigDecimal("5000"))
                .salePrice(new BigDecimal("4000"))
                .status("ACTIVE")
                .isDisplayed(true)
                .optionGroups(createOptionGroups())
                .skus(createSkus())
                .images(new ArrayList<>()) // 빈 배열
                .build();

        // when & then
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").exists())
                .andExpect(jsonPath("$.productName").value("강림 상품"));

        // DB 검증
        List<Product> products = productRepository.findAll();
        assertThat(products).hasSize(1);
        assertThat(products.get(0).getImages()).isEmpty();
    }

    @Test
    @DisplayName("상품 등록 실패 - 필수 필드 누락 (productName)")
    void createProductFailure_missingProductName() throws Exception {
        // given
        ProductCreateRequest request = ProductCreateRequest.builder()
                .productName(null) // 필수 필드 누락
                .productCode("PRODUCT-001")
                .description("강림 상품입니다.")
                .basePrice(new BigDecimal("5000"))
                .status("ACTIVE")
                .optionGroups(createOptionGroups())
                .skus(createSkus())
                .images(new ArrayList<>())
                .build();

        // when & then
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    private ProductCreateRequest createSampleProductRequest(FileUploadResponse fileUploadResponse) {
        return ProductCreateRequest.builder()
                .productName("강림 상품")
                .productCode("PRODUCT-001")
                .description("강림 상품입니다.")
                .basePrice(new BigDecimal("5000"))
                .salePrice(new BigDecimal("4000"))
                .status("ACTIVE")
                .isDisplayed(true)
                .optionGroups(createOptionGroups())
                .skus(createSkus())
                .images(createImages(fileUploadResponse))
                .build();
    }

    private List<OptionGroupRequest> createOptionGroups() {
        OptionValueRequest value1 = OptionValueRequest.builder()
                .id("value_1768284326458")
                .optionValueName("빨강")
                .displayOrder(0)
                .build();

        OptionValueRequest value2 = OptionValueRequest.builder()
                .id("value_1768284330098")
                .optionValueName("파랑")
                .displayOrder(1)
                .build();

        OptionGroupRequest group = OptionGroupRequest.builder()
                .optionGroupName("색상")
                .displayOrder(0)
                .optionValues(List.of(value1, value2))
                .build();

        return List.of(group);
    }

    private List<SkuRequest> createSkus() {
        SkuRequest sku1 = SkuRequest.builder()
                .skuCode("SKU-1768284335299-0")
                .price(new BigDecimal("5000"))
                .stockQty(15)
                .status("ACTIVE")
                .optionValueIds(List.of("value_1768284326458"))
                .build();

        SkuRequest sku2 = SkuRequest.builder()
                .skuCode("SKU-1768284335299-1")
                .price(new BigDecimal("5000"))
                .stockQty(10)
                .status("ACTIVE")
                .optionValueIds(List.of("value_1768284330098"))
                .build();

        return List.of(sku1, sku2);
    }

    private List<ProductImageRequest> createImages(FileUploadResponse fileUploadResponse) {
        ProductImageRequest image = ProductImageRequest.builder()
                .fileId(fileUploadResponse.getFileId())
                .isPrimary(true)
                .displayOrder(0)
                .build();

        return List.of(image);
    }

    // ==================== 상품 상세 조회 테스트 ====================

    @Test
    @DisplayName("상품 상세 조회 - 성공")
    void getProductDetail_success() throws Exception {
        // given - 상품 등록
        ProductCreateRequest createRequest = ProductCreateRequest.builder()
                .productName("테스트 노트북")
                .productCode("LAPTOP-001")
                .description("고성능 테스트 노트북입니다.")
                .basePrice(new BigDecimal("1200000"))
                .salePrice(new BigDecimal("1000000"))
                .status("ACTIVE")
                .isDisplayed(true)
                .optionGroups(createOptionGroupsForDetail())
                .skus(createSkusForDetail())
                .images(new ArrayList<>())
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ProductResponse createdProduct = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), ProductResponse.class);
        Long productId = createdProduct.getProductId();

        // when & then - 상품 상세 조회
        mockMvc.perform(get("/api/admin/products/{productId}", productId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.productName").value("테스트 노트북"))
                .andExpect(jsonPath("$.productCode").value("LAPTOP-001"))
                .andExpect(jsonPath("$.description").value("고성능 테스트 노트북입니다."))
                .andExpect(jsonPath("$.basePrice").value(1200000))
                .andExpect(jsonPath("$.salePrice").value(1000000))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.isDisplayed").value(true))
                // 옵션 그룹 검증
                .andExpect(jsonPath("$.optionGroups").isArray())
                .andExpect(jsonPath("$.optionGroups.length()").value(2))
                .andExpect(jsonPath("$.optionGroups[0].optionGroupName").value("색상"))
                .andExpect(jsonPath("$.optionGroups[0].optionValues.length()").value(2))
                .andExpect(jsonPath("$.optionGroups[1].optionGroupName").value("용량"))
                // SKU 검증
                .andExpect(jsonPath("$.skus").isArray())
                .andExpect(jsonPath("$.skus.length()").value(2))
                .andExpect(jsonPath("$.skus[0].skuCode").value("SKU-DETAIL-001"))
                .andExpect(jsonPath("$.skus[0].stockQty").value(10))
                // 이미지 검증
                .andExpect(jsonPath("$.images").isArray());
    }

    @Test
    @DisplayName("상품 상세 조회 - 이미지 포함")
    void getProductDetail_withImages() throws Exception {
        // given - 파일 업로드
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "detail-test.jpg",
                "image/jpeg",
                "test image for detail".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/admin/products/files/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andReturn();

        FileUploadResponse fileUploadResponse = objectMapper.readValue(
                uploadResult.getResponse().getContentAsString(), FileUploadResponse.class);

        // 상품 등록
        ProductCreateRequest createRequest = ProductCreateRequest.builder()
                .productName("이미지 포함 상품")
                .productCode("IMG-PRODUCT-001")
                .description("이미지가 포함된 상품입니다.")
                .basePrice(new BigDecimal("50000"))
                .salePrice(new BigDecimal("45000"))
                .status("ACTIVE")
                .isDisplayed(true)
                .optionGroups(createOptionGroups())
                .skus(createSkus())
                .images(createImages(fileUploadResponse))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ProductResponse createdProduct = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), ProductResponse.class);
        Long productId = createdProduct.getProductId();

        // when & then - 상품 상세 조회
        mockMvc.perform(get("/api/admin/products/{productId}", productId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.productName").value("이미지 포함 상품"))
                .andExpect(jsonPath("$.images").isArray())
                .andExpect(jsonPath("$.images.length()").value(1))
                .andExpect(jsonPath("$.images[0].fileId").value(fileUploadResponse.getFileId()));
    }

    @Test
    @DisplayName("상품 상세 조회 - 존재하지 않는 상품 ID")
    void getProductDetail_notFound() throws Exception {
        // given
        Long nonExistentProductId = 999999L;

        // when & then
        mockMvc.perform(get("/api/admin/products/{productId}", nonExistentProductId))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("상품 상세 조회 - SKU의 옵션 값 ID 매핑 검증")
    void getProductDetail_skuOptionValueMapping() throws Exception {
        // given - 상품 등록
        ProductCreateRequest createRequest = ProductCreateRequest.builder()
                .productName("SKU 매핑 테스트 상품")
                .productCode("SKU-MAPPING-001")
                .description("SKU와 옵션 값 매핑을 테스트합니다.")
                .basePrice(new BigDecimal("100000"))
                .salePrice(new BigDecimal("90000"))
                .status("ACTIVE")
                .isDisplayed(true)
                .optionGroups(createOptionGroupsForDetail())
                .skus(createSkusForDetail())
                .images(new ArrayList<>())
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ProductResponse createdProduct = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), ProductResponse.class);
        Long productId = createdProduct.getProductId();

        // when - 상품 상세 조회
        MvcResult detailResult = mockMvc.perform(get("/api/admin/products/{productId}", productId))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // then - SKU의 optionValueIds 검증
        String responseJson = detailResult.getResponse().getContentAsString();
        ProductDetailResponse detailResponse = objectMapper.readValue(responseJson, ProductDetailResponse.class);

        assertThat(detailResponse.getSkus()).hasSize(2);

        // 각 SKU가 optionValueIds를 가지고 있는지 검증
        for (ProductDetailResponse.SkuResponse sku : detailResponse.getSkus()) {
            assertThat(sku.getOptionValueIds()).isNotEmpty();
            // 옵션 값 ID가 실제 옵션 그룹의 옵션 값 ID와 일치하는지 검증
            List<Long> allOptionValueIds = detailResponse.getOptionGroups().stream()
                    .flatMap(og -> og.getOptionValues().stream())
                    .map(ProductDetailResponse.OptionValueResponse::getId)
                    .toList();

            for (Long optionValueId : sku.getOptionValueIds()) {
                assertThat(allOptionValueIds).contains(optionValueId);
            }
        }
    }

    // ==================== 상품 수정 통합 테스트 ====================

    @Test
    @DisplayName("상품 수정 - 성공")
    void updateProduct_success() throws Exception {
        // given - 상품 등록
        ProductCreateRequest createRequest = ProductCreateRequest.builder()
                .productName("원본 상품")
                .productCode("ORIGINAL-001")
                .description("원본 설명")
                .basePrice(new BigDecimal("100000"))
                .salePrice(new BigDecimal("90000"))
                .status("ACTIVE")
                .isDisplayed(true)
                .optionGroups(createOptionGroups())
                .skus(createSkus())
                .images(new ArrayList<>())
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ProductResponse createdProduct = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), ProductResponse.class);
        Long productId = createdProduct.getProductId();

        // when - 상품 수정
        ProductCreateRequest updateRequest = ProductCreateRequest.builder()
                .productName("수정된 상품")
                .productCode("UPDATED-001")
                .description("수정된 설명")
                .basePrice(new BigDecimal("150000"))
                .salePrice(new BigDecimal("130000"))
                .status("INACTIVE")
                .isDisplayed(false)
                .optionGroups(new ArrayList<>())
                .skus(new ArrayList<>())
                .images(new ArrayList<>())
                .build();

        // then
        mockMvc.perform(put("/api/admin/products/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.productName").value("수정된 상품"))
                .andExpect(jsonPath("$.productCode").value("UPDATED-001"))
                .andExpect(jsonPath("$.basePrice").value(150000))
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.isDisplayed").value(false));

        // DB 검증
        Product updatedProduct = productRepository.findById(productId).orElse(null);
        assertThat(updatedProduct).isNotNull();
        assertThat(updatedProduct.getProductName()).isEqualTo("수정된 상품");
        assertThat(updatedProduct.getOptionGroups()).isEmpty();
        assertThat(updatedProduct.getSkus()).isEmpty();
    }

    @Test
    @DisplayName("상품 수정 - 존재하지 않는 상품")
    void updateProduct_notFound() throws Exception {
        // given
        ProductCreateRequest updateRequest = ProductCreateRequest.builder()
                .productName("수정된 상품")
                .basePrice(new BigDecimal("150000"))
                .status("ACTIVE")
                .optionGroups(new ArrayList<>())
                .skus(new ArrayList<>())
                .images(new ArrayList<>())
                .build();

        // when & then
        mockMvc.perform(put("/api/admin/products/{productId}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    private List<OptionGroupRequest> createOptionGroupsForDetail() {
        OptionValueRequest colorValue1 = OptionValueRequest.builder()
                .id("detail_color_1")
                .optionValueName("Black")
                .displayOrder(0)
                .build();

        OptionValueRequest colorValue2 = OptionValueRequest.builder()
                .id("detail_color_2")
                .optionValueName("White")
                .displayOrder(1)
                .build();

        OptionGroupRequest colorGroup = OptionGroupRequest.builder()
                .optionGroupName("색상")
                .displayOrder(0)
                .optionValues(List.of(colorValue1, colorValue2))
                .build();

        OptionValueRequest sizeValue1 = OptionValueRequest.builder()
                .id("detail_size_1")
                .optionValueName("256GB")
                .displayOrder(0)
                .build();

        OptionValueRequest sizeValue2 = OptionValueRequest.builder()
                .id("detail_size_2")
                .optionValueName("512GB")
                .displayOrder(1)
                .build();

        OptionGroupRequest sizeGroup = OptionGroupRequest.builder()
                .optionGroupName("용량")
                .displayOrder(1)
                .optionValues(List.of(sizeValue1, sizeValue2))
                .build();

        return List.of(colorGroup, sizeGroup);
    }

    private List<SkuRequest> createSkusForDetail() {
        SkuRequest sku1 = SkuRequest.builder()
                .skuCode("SKU-DETAIL-001")
                .price(new BigDecimal("1200000"))
                .stockQty(10)
                .status("ACTIVE")
                .optionValueIds(List.of("detail_color_1", "detail_size_1"))
                .build();

        SkuRequest sku2 = SkuRequest.builder()
                .skuCode("SKU-DETAIL-002")
                .price(new BigDecimal("1300000"))
                .stockQty(5)
                .status("ACTIVE")
                .optionValueIds(List.of("detail_color_2", "detail_size_2"))
                .build();

        return List.of(sku1, sku2);
    }
}

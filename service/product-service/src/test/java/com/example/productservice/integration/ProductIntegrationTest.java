package com.example.productservice.integration;

import com.example.productservice.domain.entity.Product;
import com.example.productservice.domain.repository.ProductRepository;
import com.example.productservice.dto.*;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("상품 등록 통합 테스트")
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
    @DisplayName("상품 등록 실패 - 이미지 URL 누락 (imageUrl이 null)")
    void createProductFailure_missingImageUrl() throws Exception {
        // given - imageUrl이 null인 이미지
        ProductImageRequest invalidImage = ProductImageRequest.builder()
                .id("img_1")
                .fileId(1L)
                .imageUrl(null) // @NotBlank이므로 validation 오류
                .isPrimary(true)
                .displayOrder(0)
                .build();

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
                .images(List.of(invalidImage))
                .build();

        // when & then
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest()); // Validation 오류
    }

    @Test
    @DisplayName("상품 등록 실패 - 이미지 URL이 빈 문자열")
    void createProductFailure_emptyImageUrl() throws Exception {
        // given - imageUrl이 빈 문자열
        ProductImageRequest invalidImage = ProductImageRequest.builder()
                .id("img_1")
                .fileId(1L)
                .imageUrl("") // @NotBlank이므로 validation 오류
                .isPrimary(true)
                .displayOrder(0)
                .build();

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
                .images(List.of(invalidImage))
                .build();

        // when & then
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest()); // Validation 오류
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

    @Test
    @DisplayName("상품 등록 - fileId 없이 imageUrl만으로 등록")
    void createProductWithImageUrlOnly() throws Exception {
        // given - fileId는 없고 imageUrl만 있는 경우
        ProductImageRequest imageWithUrlOnly = ProductImageRequest.builder()
                .id("img_1")
                .fileId(null) // fileId 없음
                .imageUrl("/files/temp/2024/01/15/test.jpg") // URL만 있음
                .isPrimary(true)
                .displayOrder(0)
                .build();

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
                .images(List.of(imageWithUrlOnly))
                .build();

        // when & then
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").exists());

        // DB 검증
        List<Product> products = productRepository.findAll();
        assertThat(products).hasSize(1);
        assertThat(products.get(0).getImages()).hasSize(1);
        assertThat(products.get(0).getImages().get(0).getImageUrl()).isEqualTo("/files/temp/2024/01/15/test.jpg");
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
                .id("group_1768284322698")
                .optionGroupName("색상")
                .displayOrder(0)
                .optionValues(List.of(value1, value2))
                .build();

        return List.of(group);
    }

    private List<SkuRequest> createSkus() {
        SkuRequest sku1 = SkuRequest.builder()
                .id("sku_1768284335299_0")
                .skuCode("SKU-1768284335299-0")
                .price(new BigDecimal("5000"))
                .stockQty(15)
                .status("ACTIVE")
                .optionValueIds(List.of("value_1768284326458"))
                .build();

        SkuRequest sku2 = SkuRequest.builder()
                .id("sku_1768284335299_1")
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
                .id("img_" + System.currentTimeMillis())
                .fileId(fileUploadResponse.getFileId())
                .imageUrl(fileUploadResponse.getUrl())
                .isPrimary(true)
                .displayOrder(0)
                .build();

        return List.of(image);
    }
}

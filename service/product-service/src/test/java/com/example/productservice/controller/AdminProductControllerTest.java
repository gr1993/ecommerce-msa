package com.example.productservice.controller;

import com.example.productservice.service.FileStorageService;
import com.example.productservice.service.ProductService;
import com.example.productservice.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.mock.web.MockMultipartFile;

@WebMvcTest(AdminProductController.class)
@DisplayName("AdminProductController 테스트")
@ActiveProfiles("test")
class AdminProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private FileStorageService fileStorageService;

    private ProductResponse productResponse1;
    private ProductResponse productResponse2;
    private PageResponse<ProductResponse> pageResponse;

    @BeforeEach
    void setUp() {
        productResponse1 = ProductResponse.builder()
                .productId(1L)
                .productName("나이키 에어맥스")
                .productCode("NIKE-001")
                .description("편안한 운동화")
                .basePrice(new BigDecimal("150000"))
                .salePrice(new BigDecimal("120000"))
                .status("ACTIVE")
                .isDisplayed(true)
                .primaryImageUrl("https://example.com/nike1.jpg")
                .totalStockQty(50)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        productResponse2 = ProductResponse.builder()
                .productId(2L)
                .productName("아디다스 울트라부스트")
                .productCode("ADIDAS-001")
                .description("러닝화")
                .basePrice(new BigDecimal("200000"))
                .salePrice(new BigDecimal("180000"))
                .status("ACTIVE")
                .isDisplayed(true)
                .primaryImageUrl("https://example.com/adidas1.jpg")
                .totalStockQty(30)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        pageResponse = PageResponse.<ProductResponse>builder()
                .content(List.of(productResponse1, productResponse2))
                .page(0)
                .size(10)
                .totalElements(2)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();
    }

    @Test
    @DisplayName("상품 목록 조회 - 성공")
    void searchProducts_success() throws Exception {
        // given
        when(productService.searchProducts(any(ProductSearchRequest.class)))
                .thenReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/admin/products")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].productName").value("나이키 에어맥스"))
                .andExpect(jsonPath("$.content[0].productCode").value("NIKE-001"))
                .andExpect(jsonPath("$.content[0].basePrice").value(150000))
                .andExpect(jsonPath("$.content[1].productName").value("아디다스 울트라부스트"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));

        verify(productService, times(1)).searchProducts(any(ProductSearchRequest.class));
    }

    @Test
    @DisplayName("상품 목록 조회 - 상품명 검색")
    void searchProducts_byProductName() throws Exception {
        // given
        PageResponse<ProductResponse> filteredResponse = PageResponse.<ProductResponse>builder()
                .content(List.of(productResponse1))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(productService.searchProducts(any(ProductSearchRequest.class)))
                .thenReturn(filteredResponse);

        // when & then
        mockMvc.perform(get("/api/admin/products")
                        .param("productName", "나이키")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].productName").value("나이키 에어맥스"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(productService, times(1)).searchProducts(any(ProductSearchRequest.class));
    }

    @Test
    @DisplayName("상품 목록 조회 - 상태 필터")
    void searchProducts_byStatus() throws Exception {
        // given
        when(productService.searchProducts(any(ProductSearchRequest.class)))
                .thenReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/admin/products")
                        .param("status", "ACTIVE")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2));

        verify(productService, times(1)).searchProducts(any(ProductSearchRequest.class));
    }

    @Test
    @DisplayName("상품 목록 조회 - 진열 여부 필터")
    void searchProducts_byIsDisplayed() throws Exception {
        // given
        when(productService.searchProducts(any(ProductSearchRequest.class)))
                .thenReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/admin/products")
                        .param("isDisplayed", "true")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2));

        verify(productService, times(1)).searchProducts(any(ProductSearchRequest.class));
    }

    @Test
    @DisplayName("상품 목록 조회 - 가격 범위 검색")
    void searchProducts_byPriceRange() throws Exception {
        // given
        when(productService.searchProducts(any(ProductSearchRequest.class)))
                .thenReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/admin/products")
                        .param("minPrice", "100000")
                        .param("maxPrice", "200000")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2));

        verify(productService, times(1)).searchProducts(any(ProductSearchRequest.class));
    }

    @Test
    @DisplayName("상품 목록 조회 - 정렬 적용")
    void searchProducts_withSorting() throws Exception {
        // given
        when(productService.searchProducts(any(ProductSearchRequest.class)))
                .thenReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/admin/products")
                        .param("sort", "basePrice,desc")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2));

        verify(productService, times(1)).searchProducts(any(ProductSearchRequest.class));
    }

    @Test
    @DisplayName("상품 목록 조회 - 복합 필터")
    void searchProducts_withMultipleFilters() throws Exception {
        // given
        PageResponse<ProductResponse> filteredResponse = PageResponse.<ProductResponse>builder()
                .content(List.of(productResponse1))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(productService.searchProducts(any(ProductSearchRequest.class)))
                .thenReturn(filteredResponse);

        // when & then
        mockMvc.perform(get("/api/admin/products")
                        .param("productName", "나이키")
                        .param("status", "ACTIVE")
                        .param("isDisplayed", "true")
                        .param("minPrice", "100000")
                        .param("maxPrice", "160000")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(productService, times(1)).searchProducts(any(ProductSearchRequest.class));
    }

    @Test
    @DisplayName("상품 목록 조회 - 빈 결과")
    void searchProducts_emptyResult() throws Exception {
        // given
        PageResponse<ProductResponse> emptyResponse = PageResponse.<ProductResponse>builder()
                .content(List.of())
                .page(0)
                .size(10)
                .totalElements(0)
                .totalPages(0)
                .first(true)
                .last(true)
                .build();

        when(productService.searchProducts(any(ProductSearchRequest.class)))
                .thenReturn(emptyResponse);

        // when & then
        mockMvc.perform(get("/api/admin/products")
                        .param("productName", "존재하지않는상품")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(productService, times(1)).searchProducts(any(ProductSearchRequest.class));
    }

    @Test
    @DisplayName("상품 목록 조회 - 기본 페이지네이션 파라미터")
    void searchProducts_defaultParameters() throws Exception {
        // given
        when(productService.searchProducts(any(ProductSearchRequest.class)))
                .thenReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(productService, times(1)).searchProducts(any(ProductSearchRequest.class));
    }

    @Test
    @DisplayName("상품 등록 - 성공")
    void createProduct_success() throws Exception {
        // given
        OptionValueRequest optionValue1 = OptionValueRequest.builder()
                .id("value_1")
                .optionValueName("Red")
                .displayOrder(0)
                .build();

        OptionGroupRequest optionGroup = OptionGroupRequest.builder()
                .id("group_1")
                .optionGroupName("색상")
                .displayOrder(0)
                .optionValues(List.of(optionValue1))
                .build();

        SkuRequest sku = SkuRequest.builder()
                .id("sku_1")
                .skuCode("SKU-001-RED")
                .price(new BigDecimal("120000"))
                .stockQty(50)
                .status("ACTIVE")
                .optionValueIds(List.of("value_1"))
                .build();

        ProductImageRequest image = ProductImageRequest.builder()
                .id("img_1")
                .imageUrl("https://example.com/image.jpg")
                .isPrimary(true)
                .displayOrder(0)
                .build();

        ProductCreateRequest request = ProductCreateRequest.builder()
                .productName("나이키 에어맥스")
                .productCode("NIKE-001")
                .description("편안한 운동화")
                .basePrice(new BigDecimal("150000"))
                .salePrice(new BigDecimal("120000"))
                .status("ACTIVE")
                .isDisplayed(true)
                .optionGroups(List.of(optionGroup))
                .skus(List.of(sku))
                .images(List.of(image))
                .build();

        ProductResponse response = ProductResponse.builder()
                .productId(1L)
                .productName("나이키 에어맥스")
                .productCode("NIKE-001")
                .basePrice(new BigDecimal("150000"))
                .salePrice(new BigDecimal("120000"))
                .status("ACTIVE")
                .isDisplayed(true)
                .totalStockQty(50)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(productService.createProduct(any(ProductCreateRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productName").value("나이키 에어맥스"))
                .andExpect(jsonPath("$.productCode").value("NIKE-001"))
                .andExpect(jsonPath("$.basePrice").value(150000))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(productService, times(1)).createProduct(any(ProductCreateRequest.class));
    }

    @Test
    @DisplayName("상품 등록 - 필수값 누락")
    void createProduct_missingRequired() throws Exception {
        // given - productName 누락
        ProductCreateRequest request = ProductCreateRequest.builder()
                .productCode("NIKE-001")
                .basePrice(new BigDecimal("150000"))
                .status("ACTIVE")
                .build();

        // when & then
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(productService, never()).createProduct(any(ProductCreateRequest.class));
    }

    @Test
    @DisplayName("상품 등록 - 옵션 없이 등록")
    void createProduct_withoutOptions() throws Exception {
        // given
        ProductCreateRequest request = ProductCreateRequest.builder()
                .productName("나이키 에어맥스")
                .productCode("NIKE-001")
                .description("편안한 운동화")
                .basePrice(new BigDecimal("150000"))
                .status("ACTIVE")
                .isDisplayed(true)
                .optionGroups(List.of())
                .skus(List.of())
                .images(List.of())
                .build();

        ProductResponse response = ProductResponse.builder()
                .productId(1L)
                .productName("나이키 에어맥스")
                .productCode("NIKE-001")
                .basePrice(new BigDecimal("150000"))
                .status("ACTIVE")
                .isDisplayed(true)
                .totalStockQty(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(productService.createProduct(any(ProductCreateRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productName").value("나이키 에어맥스"));

        verify(productService, times(1)).createProduct(any(ProductCreateRequest.class));
    }

    @Test
    @DisplayName("파일 업로드 - 성공")
    void uploadFile_success() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        FileUploadResponse response = FileUploadResponse.builder()
                .fileId(1L)
                .originalFilename("test-image.jpg")
                .url("/files/temp/2024/01/15/uuid-test.jpg")
                .fileSize(18L)
                .contentType("image/jpeg")
                .status("TEMP")
                .uploadedAt(LocalDateTime.now())
                .build();

        when(fileStorageService.uploadFile(any())).thenReturn(response);

        // when & then
        mockMvc.perform(multipart("/api/admin/products/files/upload")
                        .file(file))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileId").value(1))
                .andExpect(jsonPath("$.originalFilename").value("test-image.jpg"))
                .andExpect(jsonPath("$.url").value("/files/temp/2024/01/15/uuid-test.jpg"))
                .andExpect(jsonPath("$.fileSize").value(18))
                .andExpect(jsonPath("$.contentType").value("image/jpeg"))
                .andExpect(jsonPath("$.status").value("TEMP"));

        verify(fileStorageService, times(1)).uploadFile(any());
    }

    @Test
    @DisplayName("파일 업로드 - 빈 파일")
    void uploadFile_emptyFile() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        when(fileStorageService.uploadFile(any()))
                .thenThrow(new IllegalArgumentException("파일이 비어있습니다"));

        // when & then
        mockMvc.perform(multipart("/api/admin/products/files/upload")
                        .file(file))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(fileStorageService, times(1)).uploadFile(any());
    }

    @Test
    @DisplayName("파일 업로드 - 허용되지 않은 파일 형식")
    void uploadFile_invalidFileType() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "test content".getBytes()
        );

        when(fileStorageService.uploadFile(any()))
                .thenThrow(new IllegalArgumentException("허용되지 않은 파일 형식입니다: txt"));

        // when & then
        mockMvc.perform(multipart("/api/admin/products/files/upload")
                        .file(file))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(fileStorageService, times(1)).uploadFile(any());
    }
}

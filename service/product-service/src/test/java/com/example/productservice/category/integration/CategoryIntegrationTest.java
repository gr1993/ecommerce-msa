package com.example.productservice.category.integration;

import com.example.productservice.category.domain.Category;
import com.example.productservice.category.dto.CategoryCreateRequest;
import com.example.productservice.category.dto.CategoryResponse;
import com.example.productservice.category.repository.CategoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("카테고리 통합 테스트")
class CategoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();
    }

    @Test
    @DisplayName("최상위 카테고리 등록 - 성공")
    void createRootCategory_success() throws Exception {
        // given
        CategoryCreateRequest request = CategoryCreateRequest.builder()
                .categoryName("의류")
                .displayOrder(1)
                .isDisplayed(true)
                .build();

        // when
        MvcResult result = mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").exists())
                .andExpect(jsonPath("$.categoryName").value("의류"))
                .andExpect(jsonPath("$.displayOrder").value(1))
                .andExpect(jsonPath("$.isDisplayed").value(true))
                .andExpect(jsonPath("$.parentId").doesNotExist())
                .andReturn();

        // then - DB 검증
        String response = result.getResponse().getContentAsString();
        CategoryResponse categoryResponse = objectMapper.readValue(response, CategoryResponse.class);

        Category savedCategory = categoryRepository.findById(categoryResponse.getCategoryId()).orElse(null);
        assertThat(savedCategory).isNotNull();
        assertThat(savedCategory.getCategoryName()).isEqualTo("의류");
        assertThat(savedCategory.getDisplayOrder()).isEqualTo(1);
        assertThat(savedCategory.getIsDisplayed()).isTrue();
        assertThat(savedCategory.getParent()).isNull();
    }

    @Test
    @DisplayName("하위 카테고리 등록 - 성공")
    void createChildCategory_success() throws Exception {
        // given - 상위 카테고리 먼저 등록
        CategoryCreateRequest parentRequest = CategoryCreateRequest.builder()
                .categoryName("의류")
                .displayOrder(1)
                .isDisplayed(true)
                .build();

        MvcResult parentResult = mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parentRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        CategoryResponse parentResponse = objectMapper.readValue(
                parentResult.getResponse().getContentAsString(),
                CategoryResponse.class
        );

        // when - 하위 카테고리 등록
        CategoryCreateRequest childRequest = CategoryCreateRequest.builder()
                .parentId(parentResponse.getCategoryId())
                .categoryName("상의")
                .displayOrder(1)
                .isDisplayed(true)
                .build();

        MvcResult childResult = mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(childRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").exists())
                .andExpect(jsonPath("$.categoryName").value("상의"))
                .andExpect(jsonPath("$.parentId").value(parentResponse.getCategoryId()))
                .andReturn();

        // then - DB 검증
        CategoryResponse childResponse = objectMapper.readValue(
                childResult.getResponse().getContentAsString(),
                CategoryResponse.class
        );

        Category savedChild = categoryRepository.findById(childResponse.getCategoryId()).orElse(null);
        assertThat(savedChild).isNotNull();
        assertThat(savedChild.getCategoryName()).isEqualTo("상의");
        assertThat(savedChild.getParent()).isNotNull();
        assertThat(savedChild.getParent().getCategoryId()).isEqualTo(parentResponse.getCategoryId());
    }

    @Test
    @DisplayName("계층 구조 카테고리 등록 - 3단계 카테고리")
    void createThreeLevelCategories_success() throws Exception {
        // 1단계: 최상위 카테고리
        CategoryCreateRequest level1Request = CategoryCreateRequest.builder()
                .categoryName("의류")
                .displayOrder(1)
                .isDisplayed(true)
                .build();

        MvcResult level1Result = mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(level1Request)))
                .andExpect(status().isCreated())
                .andReturn();

        CategoryResponse level1Response = objectMapper.readValue(
                level1Result.getResponse().getContentAsString(),
                CategoryResponse.class
        );

        // 2단계: 중간 카테고리
        CategoryCreateRequest level2Request = CategoryCreateRequest.builder()
                .parentId(level1Response.getCategoryId())
                .categoryName("상의")
                .displayOrder(1)
                .isDisplayed(true)
                .build();

        MvcResult level2Result = mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(level2Request)))
                .andExpect(status().isCreated())
                .andReturn();

        CategoryResponse level2Response = objectMapper.readValue(
                level2Result.getResponse().getContentAsString(),
                CategoryResponse.class
        );

        // 3단계: 최하위 카테고리
        CategoryCreateRequest level3Request = CategoryCreateRequest.builder()
                .parentId(level2Response.getCategoryId())
                .categoryName("티셔츠")
                .displayOrder(1)
                .isDisplayed(true)
                .build();

        MvcResult level3Result = mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(level3Request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryName").value("티셔츠"))
                .andExpect(jsonPath("$.parentId").value(level2Response.getCategoryId()))
                .andReturn();

        // DB 검증 - 계층 구조 확인
        List<Category> rootCategories = categoryRepository.findByParentIsNullOrderByDisplayOrderAsc();
        assertThat(rootCategories).hasSize(1);
        assertThat(rootCategories.get(0).getCategoryName()).isEqualTo("의류");

        List<Category> level2Categories = categoryRepository.findByParentCategoryIdOrderByDisplayOrderAsc(
                level1Response.getCategoryId()
        );
        assertThat(level2Categories).hasSize(1);
        assertThat(level2Categories.get(0).getCategoryName()).isEqualTo("상의");

        List<Category> level3Categories = categoryRepository.findByParentCategoryIdOrderByDisplayOrderAsc(
                level2Response.getCategoryId()
        );
        assertThat(level3Categories).hasSize(1);
        assertThat(level3Categories.get(0).getCategoryName()).isEqualTo("티셔츠");
    }

    @Test
    @DisplayName("카테고리 등록 실패 - 필수 필드 누락 (categoryName)")
    void createCategory_missingCategoryName() throws Exception {
        // given
        CategoryCreateRequest request = CategoryCreateRequest.builder()
                .displayOrder(1)
                .isDisplayed(true)
                .build();

        // when & then
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // DB 검증 - 저장되지 않음
        List<Category> categories = categoryRepository.findAll();
        assertThat(categories).isEmpty();
    }

    @Test
    @DisplayName("카테고리 등록 실패 - 존재하지 않는 상위 카테고리")
    void createCategory_parentNotFound() throws Exception {
        // given
        CategoryCreateRequest request = CategoryCreateRequest.builder()
                .parentId(999L)
                .categoryName("상의")
                .displayOrder(1)
                .isDisplayed(true)
                .build();

        // when & then
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // DB 검증 - 저장되지 않음
        List<Category> categories = categoryRepository.findAll();
        assertThat(categories).isEmpty();
    }

    @Test
    @DisplayName("여러 카테고리 등록 - 전시 순서 확인")
    void createMultipleCategories_displayOrder() throws Exception {
        // given & when - 여러 카테고리 등록
        for (int i = 3; i >= 1; i--) {
            CategoryCreateRequest request = CategoryCreateRequest.builder()
                    .categoryName("카테고리" + i)
                    .displayOrder(i)
                    .isDisplayed(true)
                    .build();

            mockMvc.perform(post("/api/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // then - 전시 순서대로 조회
        List<Category> categories = categoryRepository.findByParentIsNullOrderByDisplayOrderAsc();
        assertThat(categories).hasSize(3);
        assertThat(categories.get(0).getCategoryName()).isEqualTo("카테고리1");
        assertThat(categories.get(1).getCategoryName()).isEqualTo("카테고리2");
        assertThat(categories.get(2).getCategoryName()).isEqualTo("카테고리3");
    }

    @Test
    @DisplayName("카테고리 등록 - 기본값 적용 확인")
    void createCategory_defaultValues() throws Exception {
        // given - categoryName만 설정
        String jsonRequest = "{\"categoryName\": \"액세서리\"}";

        // when
        MvcResult result = mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn();

        // then
        CategoryResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CategoryResponse.class
        );

        Category savedCategory = categoryRepository.findById(response.getCategoryId()).orElse(null);
        assertThat(savedCategory).isNotNull();
        assertThat(savedCategory.getDisplayOrder()).isEqualTo(0);
        assertThat(savedCategory.getIsDisplayed()).isTrue();
    }

    @Test
    @DisplayName("카테고리 등록 - 전시 안함으로 등록")
    void createCategory_notDisplayed() throws Exception {
        // given
        CategoryCreateRequest request = CategoryCreateRequest.builder()
                .categoryName("비공개 카테고리")
                .displayOrder(99)
                .isDisplayed(false)
                .build();

        // when
        MvcResult result = mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isDisplayed").value(false))
                .andReturn();

        // then
        CategoryResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CategoryResponse.class
        );

        Category savedCategory = categoryRepository.findById(response.getCategoryId()).orElse(null);
        assertThat(savedCategory).isNotNull();
        assertThat(savedCategory.getIsDisplayed()).isFalse();
    }
}

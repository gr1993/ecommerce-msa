package com.example.productservice.category.service;

import com.example.productservice.category.domain.Category;
import com.example.productservice.category.dto.CategoryCreateRequest;
import com.example.productservice.category.dto.CategoryResponse;
import com.example.productservice.category.dto.CategoryTreeResponse;
import com.example.productservice.category.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService 테스트")
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category parentCategory;
    private Category childCategory;

    @BeforeEach
    void setUp() {
        parentCategory = Category.builder()
                .categoryId(1L)
                .categoryName("의류")
                .displayOrder(1)
                .isDisplayed(true)
                .children(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        childCategory = Category.builder()
                .categoryId(2L)
                .parent(parentCategory)
                .categoryName("상의")
                .displayOrder(1)
                .isDisplayed(true)
                .children(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("카테고리 등록 - 최상위 카테고리 성공")
    void createCategory_rootCategory_success() {
        // given
        CategoryCreateRequest request = CategoryCreateRequest.builder()
                .categoryName("신발")
                .displayOrder(2)
                .isDisplayed(true)
                .build();

        Category savedCategory = Category.builder()
                .categoryId(3L)
                .categoryName("신발")
                .displayOrder(2)
                .isDisplayed(true)
                .children(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        // when
        CategoryResponse response = categoryService.createCategory(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getCategoryId()).isEqualTo(3L);
        assertThat(response.getCategoryName()).isEqualTo("신발");
        assertThat(response.getDisplayOrder()).isEqualTo(2);
        assertThat(response.getIsDisplayed()).isTrue();
        assertThat(response.getParentId()).isNull();

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository, times(1)).save(captor.capture());

        Category captured = captor.getValue();
        assertThat(captured.getCategoryName()).isEqualTo("신발");
        assertThat(captured.getParent()).isNull();
    }

    @Test
    @DisplayName("카테고리 등록 - 하위 카테고리 성공")
    void createCategory_childCategory_success() {
        // given
        CategoryCreateRequest request = CategoryCreateRequest.builder()
                .parentId(1L)
                .categoryName("하의")
                .displayOrder(2)
                .isDisplayed(true)
                .build();

        Category savedCategory = Category.builder()
                .categoryId(3L)
                .parent(parentCategory)
                .categoryName("하의")
                .displayOrder(2)
                .isDisplayed(true)
                .children(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parentCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        // when
        CategoryResponse response = categoryService.createCategory(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getCategoryId()).isEqualTo(3L);
        assertThat(response.getCategoryName()).isEqualTo("하의");
        assertThat(response.getParentId()).isEqualTo(1L);

        verify(categoryRepository, times(1)).findById(1L);
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("카테고리 등록 - 존재하지 않는 상위 카테고리")
    void createCategory_parentNotFound() {
        // given
        CategoryCreateRequest request = CategoryCreateRequest.builder()
                .parentId(999L)
                .categoryName("하의")
                .displayOrder(1)
                .isDisplayed(true)
                .build();

        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("상위 카테고리를 찾을 수 없습니다");

        verify(categoryRepository, times(1)).findById(999L);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("카테고리 등록 - 기본값 적용")
    void createCategory_withDefaultValues() {
        // given
        CategoryCreateRequest request = CategoryCreateRequest.builder()
                .categoryName("액세서리")
                .build();

        Category savedCategory = Category.builder()
                .categoryId(4L)
                .categoryName("액세서리")
                .displayOrder(0)
                .isDisplayed(true)
                .children(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        // when
        CategoryResponse response = categoryService.createCategory(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getDisplayOrder()).isEqualTo(0);
        assertThat(response.getIsDisplayed()).isTrue();

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository, times(1)).save(captor.capture());

        Category captured = captor.getValue();
        assertThat(captured.getDisplayOrder()).isEqualTo(0);
        assertThat(captured.getIsDisplayed()).isTrue();
    }

    @Test
    @DisplayName("카테고리 등록 - 전시 안함으로 설정")
    void createCategory_notDisplayed() {
        // given
        CategoryCreateRequest request = CategoryCreateRequest.builder()
                .categoryName("비공개 카테고리")
                .displayOrder(99)
                .isDisplayed(false)
                .build();

        Category savedCategory = Category.builder()
                .categoryId(5L)
                .categoryName("비공개 카테고리")
                .displayOrder(99)
                .isDisplayed(false)
                .children(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        // when
        CategoryResponse response = categoryService.createCategory(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getIsDisplayed()).isFalse();
        assertThat(response.getDisplayOrder()).isEqualTo(99);
    }

    @Test
    @DisplayName("CategoryResponse 변환 - 상위 카테고리 있는 경우")
    void categoryResponse_withParent() {
        // given
        CategoryCreateRequest request = CategoryCreateRequest.builder()
                .parentId(1L)
                .categoryName("티셔츠")
                .displayOrder(1)
                .isDisplayed(true)
                .build();

        Category savedCategory = Category.builder()
                .categoryId(10L)
                .parent(parentCategory)
                .categoryName("티셔츠")
                .displayOrder(1)
                .isDisplayed(true)
                .children(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parentCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        // when
        CategoryResponse response = categoryService.createCategory(request);

        // then
        assertThat(response.getParentId()).isEqualTo(1L);
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("카테고리 트리 조회 - 빈 목록")
    void getCategoryTree_empty() {
        // given
        when(categoryRepository.findByParentIsNullOrderByDisplayOrderAsc())
                .thenReturn(Collections.emptyList());

        // when
        List<CategoryTreeResponse> result = categoryService.getCategoryTree();

        // then
        assertThat(result).isEmpty();
        verify(categoryRepository, times(1)).findByParentIsNullOrderByDisplayOrderAsc();
    }

    @Test
    @DisplayName("카테고리 트리 조회 - 최상위 카테고리만 존재")
    void getCategoryTree_onlyRootCategories() {
        // given
        Category root1 = Category.builder()
                .categoryId(1L)
                .categoryName("의류")
                .displayOrder(1)
                .isDisplayed(true)
                .children(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Category root2 = Category.builder()
                .categoryId(2L)
                .categoryName("신발")
                .displayOrder(2)
                .isDisplayed(true)
                .children(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(categoryRepository.findByParentIsNullOrderByDisplayOrderAsc())
                .thenReturn(Arrays.asList(root1, root2));

        // when
        List<CategoryTreeResponse> result = categoryService.getCategoryTree();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCategoryId()).isEqualTo(1L);
        assertThat(result.get(0).getCategoryName()).isEqualTo("의류");
        assertThat(result.get(0).getDepth()).isEqualTo(1);
        assertThat(result.get(0).getChildren()).isNull();
        assertThat(result.get(1).getCategoryId()).isEqualTo(2L);
        assertThat(result.get(1).getCategoryName()).isEqualTo("신발");
    }

    @Test
    @DisplayName("카테고리 트리 조회 - 계층 구조 포함")
    void getCategoryTree_withHierarchy() {
        // given
        Category child1 = Category.builder()
                .categoryId(2L)
                .categoryName("상의")
                .displayOrder(1)
                .isDisplayed(true)
                .children(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Category child2 = Category.builder()
                .categoryId(3L)
                .categoryName("하의")
                .displayOrder(2)
                .isDisplayed(true)
                .children(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Category root = Category.builder()
                .categoryId(1L)
                .categoryName("의류")
                .displayOrder(1)
                .isDisplayed(true)
                .children(Arrays.asList(child1, child2))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        child1.setParent(root);
        child2.setParent(root);

        when(categoryRepository.findByParentIsNullOrderByDisplayOrderAsc())
                .thenReturn(Collections.singletonList(root));

        // when
        List<CategoryTreeResponse> result = categoryService.getCategoryTree();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategoryId()).isEqualTo(1L);
        assertThat(result.get(0).getDepth()).isEqualTo(1);
        assertThat(result.get(0).getChildren()).hasSize(2);
        assertThat(result.get(0).getChildren().get(0).getCategoryName()).isEqualTo("상의");
        assertThat(result.get(0).getChildren().get(0).getDepth()).isEqualTo(2);
        assertThat(result.get(0).getChildren().get(0).getParentId()).isEqualTo(1L);
        assertThat(result.get(0).getChildren().get(1).getCategoryName()).isEqualTo("하의");
    }

    @Test
    @DisplayName("카테고리 트리 조회 - 3단계 계층 구조")
    void getCategoryTree_threeLevel() {
        // given
        Category grandChild = Category.builder()
                .categoryId(3L)
                .categoryName("티셔츠")
                .displayOrder(1)
                .isDisplayed(true)
                .children(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Category child = Category.builder()
                .categoryId(2L)
                .categoryName("상의")
                .displayOrder(1)
                .isDisplayed(true)
                .children(Collections.singletonList(grandChild))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        grandChild.setParent(child);

        Category root = Category.builder()
                .categoryId(1L)
                .categoryName("의류")
                .displayOrder(1)
                .isDisplayed(true)
                .children(Collections.singletonList(child))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        child.setParent(root);

        when(categoryRepository.findByParentIsNullOrderByDisplayOrderAsc())
                .thenReturn(Collections.singletonList(root));

        // when
        List<CategoryTreeResponse> result = categoryService.getCategoryTree();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDepth()).isEqualTo(1);
        assertThat(result.get(0).getChildren().get(0).getDepth()).isEqualTo(2);
        assertThat(result.get(0).getChildren().get(0).getChildren().get(0).getDepth()).isEqualTo(3);
        assertThat(result.get(0).getChildren().get(0).getChildren().get(0).getCategoryName()).isEqualTo("티셔츠");
    }

    @Test
    @DisplayName("카테고리 상세 조회 - 성공")
    void getCategory_success() {
        // given
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parentCategory));

        // when
        CategoryResponse response = categoryService.getCategory(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getCategoryId()).isEqualTo(1L);
        assertThat(response.getCategoryName()).isEqualTo("의류");
        assertThat(response.getDisplayOrder()).isEqualTo(1);
        assertThat(response.getIsDisplayed()).isTrue();
        verify(categoryRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("카테고리 상세 조회 - 하위 카테고리 조회")
    void getCategory_childCategory() {
        // given
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(childCategory));

        // when
        CategoryResponse response = categoryService.getCategory(2L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getCategoryId()).isEqualTo(2L);
        assertThat(response.getCategoryName()).isEqualTo("상의");
        assertThat(response.getParentId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("카테고리 상세 조회 - 존재하지 않는 카테고리")
    void getCategory_notFound() {
        // given
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> categoryService.getCategory(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("카테고리를 찾을 수 없습니다");

        verify(categoryRepository, times(1)).findById(999L);
    }
}

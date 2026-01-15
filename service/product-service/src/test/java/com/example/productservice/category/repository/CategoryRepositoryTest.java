package com.example.productservice.category.repository;

import com.example.productservice.category.domain.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("CategoryRepository 테스트")
@ActiveProfiles("test")
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    private Category parentCategory;
    private Category childCategory1;
    private Category childCategory2;

    @BeforeEach
    void setUp() {
        // 상위 카테고리 생성
        parentCategory = Category.builder()
                .categoryName("의류")
                .displayOrder(1)
                .isDisplayed(true)
                .build();
        categoryRepository.save(parentCategory);

        // 하위 카테고리 생성
        childCategory1 = Category.builder()
                .parent(parentCategory)
                .categoryName("상의")
                .displayOrder(1)
                .isDisplayed(true)
                .build();

        childCategory2 = Category.builder()
                .parent(parentCategory)
                .categoryName("하의")
                .displayOrder(2)
                .isDisplayed(false)
                .build();

        categoryRepository.save(childCategory1);
        categoryRepository.save(childCategory2);

        // 다른 상위 카테고리
        Category anotherParent = Category.builder()
                .categoryName("신발")
                .displayOrder(2)
                .isDisplayed(true)
                .build();
        categoryRepository.save(anotherParent);
    }

    @Test
    @DisplayName("카테고리 저장 - 성공")
    void save_success() {
        // given
        Category category = Category.builder()
                .categoryName("액세서리")
                .displayOrder(3)
                .isDisplayed(true)
                .build();

        // when
        Category saved = categoryRepository.save(category);

        // then
        assertThat(saved.getCategoryId()).isNotNull();
        assertThat(saved.getCategoryName()).isEqualTo("액세서리");
        assertThat(saved.getDisplayOrder()).isEqualTo(3);
        assertThat(saved.getIsDisplayed()).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("카테고리 ID로 조회")
    void findById_success() {
        // when
        Optional<Category> found = categoryRepository.findById(parentCategory.getCategoryId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getCategoryName()).isEqualTo("의류");
    }

    @Test
    @DisplayName("최상위 카테고리 목록 조회 - 전시순서로 정렬")
    void findByParentIsNull_orderedByDisplayOrder() {
        // when
        List<Category> rootCategories = categoryRepository.findByParentIsNullOrderByDisplayOrderAsc();

        // then
        assertThat(rootCategories).hasSize(2);
        assertThat(rootCategories.get(0).getCategoryName()).isEqualTo("의류");
        assertThat(rootCategories.get(1).getCategoryName()).isEqualTo("신발");
    }

    @Test
    @DisplayName("상위 카테고리 ID로 하위 카테고리 목록 조회")
    void findByParentCategoryId_success() {
        // when
        List<Category> children = categoryRepository.findByParentCategoryIdOrderByDisplayOrderAsc(
                parentCategory.getCategoryId()
        );

        // then
        assertThat(children).hasSize(2);
        assertThat(children.get(0).getCategoryName()).isEqualTo("상의");
        assertThat(children.get(1).getCategoryName()).isEqualTo("하의");
    }

    @Test
    @DisplayName("카테고리명 존재 여부 확인 - 존재하는 경우")
    void existsByCategoryName_exists() {
        // when
        boolean exists = categoryRepository.existsByCategoryName("의류");

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("카테고리명 존재 여부 확인 - 존재하지 않는 경우")
    void existsByCategoryName_notExists() {
        // when
        boolean exists = categoryRepository.existsByCategoryName("존재하지않는카테고리");

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("하위 카테고리가 있는 상위 카테고리 조회")
    void findParentWithChildren() {
        // when
        Optional<Category> found = categoryRepository.findById(parentCategory.getCategoryId());
        List<Category> children = categoryRepository.findByParentCategoryIdOrderByDisplayOrderAsc(
                parentCategory.getCategoryId()
        );

        // then
        assertThat(found).isPresent();
        assertThat(children).hasSize(2);
    }

    @Test
    @DisplayName("카테고리 수정")
    void update_success() {
        // given
        parentCategory.setCategoryName("수정된 의류");
        parentCategory.setDisplayOrder(10);
        parentCategory.setIsDisplayed(false);

        // when
        Category updated = categoryRepository.save(parentCategory);

        // then
        assertThat(updated.getCategoryName()).isEqualTo("수정된 의류");
        assertThat(updated.getDisplayOrder()).isEqualTo(10);
        assertThat(updated.getIsDisplayed()).isFalse();
    }

    @Test
    @DisplayName("카테고리 삭제")
    void delete_success() {
        // given
        Long categoryId = childCategory1.getCategoryId();

        // when
        categoryRepository.delete(childCategory1);

        // then
        Optional<Category> deleted = categoryRepository.findById(categoryId);
        assertThat(deleted).isEmpty();
    }
}

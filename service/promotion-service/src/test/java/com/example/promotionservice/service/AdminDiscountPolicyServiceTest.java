package com.example.promotionservice.service;

import com.example.promotionservice.domain.entity.DiscountPolicy;
import com.example.promotionservice.domain.entity.DiscountPolicyStatus;
import com.example.promotionservice.domain.entity.DiscountTargetType;
import com.example.promotionservice.domain.entity.DiscountType;
import com.example.promotionservice.dto.request.AdminDiscountPolicyCreateRequest;
import com.example.promotionservice.dto.request.AdminDiscountPolicyUpdateRequest;
import com.example.promotionservice.dto.response.AdminDiscountPolicyDetailResponse;
import com.example.promotionservice.dto.response.AdminDiscountPolicyResponse;
import com.example.promotionservice.global.common.dto.PageResponse;
import com.example.promotionservice.global.exception.DiscountPolicyNotFoundException;
import com.example.promotionservice.repository.DiscountPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminDiscountPolicyServiceTest {

    @Mock
    private DiscountPolicyRepository discountPolicyRepository;

    @InjectMocks
    private AdminDiscountPolicyServiceImpl adminDiscountPolicyService;

    private DiscountPolicy testPolicy;
    private AdminDiscountPolicyCreateRequest createRequest;
    private AdminDiscountPolicyUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        testPolicy = DiscountPolicy.builder()
                .discountName("신상품 10% 할인")
                .discountType(DiscountType.RATE)
                .discountValue(new BigDecimal("10.00"))
                .targetType(DiscountTargetType.PRODUCT)
                .targetId(1L)
                .minOrderAmount(BigDecimal.ZERO)
                .maxDiscountAmount(new BigDecimal("50000.00"))
                .validFrom(LocalDateTime.of(2024, 1, 1, 0, 0))
                .validTo(LocalDateTime.of(2024, 12, 31, 23, 59))
                .status(DiscountPolicyStatus.ACTIVE)
                .build();

        createRequest = AdminDiscountPolicyCreateRequest.builder()
                .discountName("여름 특가 20% 할인")
                .discountType("RATE")
                .discountValue(new BigDecimal("20.00"))
                .targetType("ORDER")
                .targetId(null)
                .minOrderAmount(new BigDecimal("100000.00"))
                .maxDiscountAmount(new BigDecimal("30000.00"))
                .validFrom(LocalDateTime.of(2024, 6, 1, 0, 0))
                .validTo(LocalDateTime.of(2024, 8, 31, 23, 59))
                .status("ACTIVE")
                .build();

        updateRequest = AdminDiscountPolicyUpdateRequest.builder()
                .discountName("신상품 15% 할인 (수정)")
                .discountType("RATE")
                .discountValue(new BigDecimal("15.00"))
                .targetType("PRODUCT")
                .targetId(1L)
                .minOrderAmount(new BigDecimal("10000.00"))
                .maxDiscountAmount(new BigDecimal("30000.00"))
                .validFrom(LocalDateTime.of(2024, 1, 1, 0, 0))
                .validTo(LocalDateTime.of(2025, 6, 30, 23, 59))
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("할인 정책 목록 조회 성공")
    void getDiscountPolicies_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Page<DiscountPolicy> policyPage = new PageImpl<>(List.of(testPolicy), pageable, 1);
        given(discountPolicyRepository.findAllBySearchCondition(eq(null), eq(null), any(Pageable.class)))
                .willReturn(policyPage);

        // when
        PageResponse<AdminDiscountPolicyResponse> response = adminDiscountPolicyService.getDiscountPolicies(null, null, pageable);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getDiscountName()).isEqualTo("신상품 10% 할인");
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("할인 정책 목록 조회 - 키워드 필터링")
    void getDiscountPolicies_WithKeyword() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Page<DiscountPolicy> policyPage = new PageImpl<>(List.of(testPolicy), pageable, 1);
        given(discountPolicyRepository.findAllBySearchCondition(eq("신상품"), eq(null), any(Pageable.class)))
                .willReturn(policyPage);

        // when
        PageResponse<AdminDiscountPolicyResponse> response = adminDiscountPolicyService.getDiscountPolicies("신상품", null, pageable);

        // then
        assertThat(response.getContent()).hasSize(1);
        verify(discountPolicyRepository).findAllBySearchCondition(eq("신상품"), eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("할인 정책 목록 조회 - 상태 필터링")
    void getDiscountPolicies_WithStatus() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Page<DiscountPolicy> policyPage = new PageImpl<>(List.of(testPolicy), pageable, 1);
        given(discountPolicyRepository.findAllBySearchCondition(eq(null), eq(DiscountPolicyStatus.ACTIVE), any(Pageable.class)))
                .willReturn(policyPage);

        // when
        PageResponse<AdminDiscountPolicyResponse> response = adminDiscountPolicyService.getDiscountPolicies(null, "ACTIVE", pageable);

        // then
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getStatus()).isEqualTo(DiscountPolicyStatus.ACTIVE);
    }

    @Test
    @DisplayName("할인 정책 상세 조회 성공")
    void getDiscountPolicyDetail_Success() {
        // given
        given(discountPolicyRepository.findById(1L)).willReturn(Optional.of(testPolicy));

        // when
        AdminDiscountPolicyDetailResponse response = adminDiscountPolicyService.getDiscountPolicyDetail(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getDiscountName()).isEqualTo("신상품 10% 할인");
        assertThat(response.getDiscountType()).isEqualTo(DiscountType.RATE);
        assertThat(response.getTargetType()).isEqualTo(DiscountTargetType.PRODUCT);
    }

    @Test
    @DisplayName("할인 정책 상세 조회 실패 - 존재하지 않는 정책")
    void getDiscountPolicyDetail_NotFound() {
        // given
        given(discountPolicyRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminDiscountPolicyService.getDiscountPolicyDetail(999L))
                .isInstanceOf(DiscountPolicyNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("할인 정책 등록 성공")
    void createDiscountPolicy_Success() {
        // given
        given(discountPolicyRepository.save(any(DiscountPolicy.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        AdminDiscountPolicyDetailResponse response = adminDiscountPolicyService.createDiscountPolicy(createRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getDiscountName()).isEqualTo("여름 특가 20% 할인");
        assertThat(response.getDiscountType()).isEqualTo(DiscountType.RATE);
        assertThat(response.getTargetType()).isEqualTo(DiscountTargetType.ORDER);
        assertThat(response.getStatus()).isEqualTo(DiscountPolicyStatus.ACTIVE);
        verify(discountPolicyRepository).save(any(DiscountPolicy.class));
    }

    @Test
    @DisplayName("할인 정책 수정 성공")
    void updateDiscountPolicy_Success() {
        // given
        given(discountPolicyRepository.findById(1L)).willReturn(Optional.of(testPolicy));

        // when
        AdminDiscountPolicyDetailResponse response = adminDiscountPolicyService.updateDiscountPolicy(1L, updateRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getDiscountName()).isEqualTo("신상품 15% 할인 (수정)");
        assertThat(response.getDiscountValue()).isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(response.getMaxDiscountAmount()).isEqualByComparingTo(new BigDecimal("30000.00"));
    }

    @Test
    @DisplayName("할인 정책 수정 실패 - 존재하지 않는 정책")
    void updateDiscountPolicy_NotFound() {
        // given
        given(discountPolicyRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminDiscountPolicyService.updateDiscountPolicy(999L, updateRequest))
                .isInstanceOf(DiscountPolicyNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("할인 정책 등록 실패 - 유효하지 않은 할인 유형")
    void createDiscountPolicy_InvalidDiscountType() {
        // given
        AdminDiscountPolicyCreateRequest invalidRequest = AdminDiscountPolicyCreateRequest.builder()
                .discountName("테스트 할인")
                .discountType("INVALID")
                .discountValue(new BigDecimal("10.00"))
                .targetType("ORDER")
                .validFrom(LocalDateTime.of(2024, 1, 1, 0, 0))
                .validTo(LocalDateTime.of(2024, 12, 31, 23, 59))
                .status("ACTIVE")
                .build();

        // when & then
        assertThatThrownBy(() -> adminDiscountPolicyService.createDiscountPolicy(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은 할인 유형");
    }

    @Test
    @DisplayName("할인 정책 등록 실패 - 유효하지 않은 적용 대상")
    void createDiscountPolicy_InvalidTargetType() {
        // given
        AdminDiscountPolicyCreateRequest invalidRequest = AdminDiscountPolicyCreateRequest.builder()
                .discountName("테스트 할인")
                .discountType("FIXED")
                .discountValue(new BigDecimal("5000.00"))
                .targetType("INVALID")
                .validFrom(LocalDateTime.of(2024, 1, 1, 0, 0))
                .validTo(LocalDateTime.of(2024, 12, 31, 23, 59))
                .status("ACTIVE")
                .build();

        // when & then
        assertThatThrownBy(() -> adminDiscountPolicyService.createDiscountPolicy(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은 적용 대상");
    }

    @Test
    @DisplayName("할인 정책 등록 실패 - 유효하지 않은 상태")
    void createDiscountPolicy_InvalidStatus() {
        // given
        AdminDiscountPolicyCreateRequest invalidRequest = AdminDiscountPolicyCreateRequest.builder()
                .discountName("테스트 할인")
                .discountType("FIXED")
                .discountValue(new BigDecimal("5000.00"))
                .targetType("ORDER")
                .validFrom(LocalDateTime.of(2024, 1, 1, 0, 0))
                .validTo(LocalDateTime.of(2024, 12, 31, 23, 59))
                .status("INVALID_STATUS")
                .build();

        // when & then
        assertThatThrownBy(() -> adminDiscountPolicyService.createDiscountPolicy(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은 할인 상태");
    }
}

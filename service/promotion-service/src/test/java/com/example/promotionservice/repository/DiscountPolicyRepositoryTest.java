package com.example.promotionservice.repository;

import com.example.promotionservice.domain.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class DiscountPolicyRepositoryTest {

    @Autowired
    private DiscountPolicyRepository discountPolicyRepository;

    private DiscountPolicy testDiscountPolicy;

    @BeforeEach
    void setUp() {
        testDiscountPolicy = DiscountPolicy.builder()
                .discountName("여름 세일 할인")
                .discountType(DiscountType.RATE)
                .discountValue(new BigDecimal("15.00"))
                .targetType(DiscountTargetType.PRODUCT)
                .targetId(100L)
                .minOrderAmount(new BigDecimal("10000.00"))
                .maxDiscountAmount(new BigDecimal("50000.00"))
                .validFrom(LocalDateTime.of(2024, 6, 1, 0, 0))
                .validTo(LocalDateTime.of(2024, 8, 31, 23, 59))
                .status(DiscountPolicyStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("할인 정책 저장 테스트")
    void saveDiscountPolicy() {
        // when
        DiscountPolicy savedPolicy = discountPolicyRepository.save(testDiscountPolicy);

        // then
        assertThat(savedPolicy.getId()).isNotNull();
        assertThat(savedPolicy.getDiscountName()).isEqualTo("여름 세일 할인");
        assertThat(savedPolicy.getDiscountType()).isEqualTo(DiscountType.RATE);
        assertThat(savedPolicy.getTargetType()).isEqualTo(DiscountTargetType.PRODUCT);
        assertThat(savedPolicy.getCreatedAt()).isNotNull();
        assertThat(savedPolicy.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("할인 상태로 목록 조회 테스트")
    void findByStatus() {
        // given
        discountPolicyRepository.save(testDiscountPolicy);

        DiscountPolicy inactivePolicy = DiscountPolicy.builder()
                .discountName("비활성 할인")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("3000.00"))
                .targetType(DiscountTargetType.ORDER)
                .minOrderAmount(BigDecimal.ZERO)
                .validFrom(LocalDateTime.of(2024, 1, 1, 0, 0))
                .validTo(LocalDateTime.of(2024, 12, 31, 23, 59))
                .status(DiscountPolicyStatus.INACTIVE)
                .build();
        discountPolicyRepository.save(inactivePolicy);

        // when
        List<DiscountPolicy> activePolicies = discountPolicyRepository.findByStatus(
                DiscountPolicyStatus.ACTIVE);

        // then
        assertThat(activePolicies).hasSize(1);
        assertThat(activePolicies.get(0).getStatus()).isEqualTo(DiscountPolicyStatus.ACTIVE);
    }

    @Test
    @DisplayName("대상 유형과 대상 ID로 조회 테스트")
    void findByTargetTypeAndTargetId() {
        // given
        discountPolicyRepository.save(testDiscountPolicy);

        // when
        List<DiscountPolicy> policies = discountPolicyRepository.findByTargetTypeAndTargetId(
                DiscountTargetType.PRODUCT, 100L);

        // then
        assertThat(policies).hasSize(1);
        assertThat(policies.get(0).getTargetId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("대상 유형과 상태로 조회 테스트")
    void findByTargetTypeAndStatus() {
        // given
        discountPolicyRepository.save(testDiscountPolicy);

        DiscountPolicy categoryPolicy = DiscountPolicy.builder()
                .discountName("카테고리 할인")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("2000.00"))
                .targetType(DiscountTargetType.CATEGORY)
                .targetId(10L)
                .minOrderAmount(BigDecimal.ZERO)
                .validFrom(LocalDateTime.of(2024, 1, 1, 0, 0))
                .validTo(LocalDateTime.of(2024, 12, 31, 23, 59))
                .status(DiscountPolicyStatus.ACTIVE)
                .build();
        discountPolicyRepository.save(categoryPolicy);

        // when
        List<DiscountPolicy> productPolicies = discountPolicyRepository.findByTargetTypeAndStatus(
                DiscountTargetType.PRODUCT, DiscountPolicyStatus.ACTIVE);

        // then
        assertThat(productPolicies).hasSize(1);
        assertThat(productPolicies.get(0).getTargetType()).isEqualTo(DiscountTargetType.PRODUCT);
    }

    @Test
    @DisplayName("만료된 할인 정책 조회 테스트")
    void findByValidToBeforeAndStatus() {
        // given
        discountPolicyRepository.save(testDiscountPolicy);

        // when
        List<DiscountPolicy> expiredPolicies = discountPolicyRepository.findByValidToBeforeAndStatus(
                LocalDateTime.of(2025, 1, 1, 0, 0), DiscountPolicyStatus.ACTIVE);

        // then
        assertThat(expiredPolicies).hasSize(1);
        assertThat(expiredPolicies.get(0).getDiscountName()).isEqualTo("여름 세일 할인");
    }

    @Test
    @DisplayName("할인 정책 상태 업데이트 테스트")
    void updateDiscountPolicyStatus() {
        // given
        DiscountPolicy savedPolicy = discountPolicyRepository.save(testDiscountPolicy);

        // when
        savedPolicy.updateStatus(DiscountPolicyStatus.EXPIRED);
        DiscountPolicy updatedPolicy = discountPolicyRepository.save(savedPolicy);

        // then
        assertThat(updatedPolicy.getStatus()).isEqualTo(DiscountPolicyStatus.EXPIRED);
    }

    @Test
    @DisplayName("할인 정책 삭제 테스트")
    void deleteDiscountPolicy() {
        // given
        DiscountPolicy savedPolicy = discountPolicyRepository.save(testDiscountPolicy);
        Long policyId = savedPolicy.getId();

        // when
        discountPolicyRepository.deleteById(policyId);

        // then
        assertThat(discountPolicyRepository.findById(policyId)).isEmpty();
    }
}

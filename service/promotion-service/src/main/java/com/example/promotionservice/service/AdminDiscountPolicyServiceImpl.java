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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDiscountPolicyServiceImpl implements AdminDiscountPolicyService {

    private final DiscountPolicyRepository discountPolicyRepository;

    @Override
    public PageResponse<AdminDiscountPolicyResponse> getDiscountPolicies(String keyword, String status, Pageable pageable) {
        DiscountPolicyStatus policyStatus = parseDiscountPolicyStatus(status);

        Page<DiscountPolicy> policyPage = discountPolicyRepository.findAllBySearchCondition(
                keyword != null && keyword.isBlank() ? null : keyword,
                policyStatus,
                pageable
        );

        Page<AdminDiscountPolicyResponse> responsePage = policyPage.map(AdminDiscountPolicyResponse::from);
        return PageResponse.from(responsePage);
    }

    @Override
    public AdminDiscountPolicyDetailResponse getDiscountPolicyDetail(Long discountId) {
        DiscountPolicy policy = discountPolicyRepository.findById(discountId)
                .orElseThrow(() -> new DiscountPolicyNotFoundException(discountId));

        return AdminDiscountPolicyDetailResponse.from(policy);
    }

    @Override
    @Transactional
    public AdminDiscountPolicyDetailResponse createDiscountPolicy(AdminDiscountPolicyCreateRequest request) {
        DiscountType discountType = parseDiscountType(request.getDiscountType());
        DiscountTargetType targetType = parseDiscountTargetType(request.getTargetType());
        DiscountPolicyStatus policyStatus = parseDiscountPolicyStatus(request.getStatus());
        if (policyStatus == null) {
            throw new IllegalArgumentException("유효하지 않은 할인 상태입니다: " + request.getStatus());
        }

        DiscountPolicy policy = DiscountPolicy.builder()
                .discountName(request.getDiscountName())
                .discountType(discountType)
                .discountValue(request.getDiscountValue())
                .targetType(targetType)
                .targetId(request.getTargetId())
                .minOrderAmount(request.getMinOrderAmount())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .status(policyStatus)
                .build();

        DiscountPolicy savedPolicy = discountPolicyRepository.save(policy);
        log.info("관리자 할인 정책 등록: discountId={}, discountName={}", savedPolicy.getId(), savedPolicy.getDiscountName());

        return AdminDiscountPolicyDetailResponse.from(savedPolicy);
    }

    @Override
    @Transactional
    public AdminDiscountPolicyDetailResponse updateDiscountPolicy(Long discountId, AdminDiscountPolicyUpdateRequest request) {
        DiscountPolicy policy = discountPolicyRepository.findById(discountId)
                .orElseThrow(() -> new DiscountPolicyNotFoundException(discountId));

        DiscountType discountType = parseDiscountType(request.getDiscountType());
        DiscountTargetType targetType = parseDiscountTargetType(request.getTargetType());
        DiscountPolicyStatus newStatus = parseDiscountPolicyStatus(request.getStatus());
        if (newStatus == null) {
            throw new IllegalArgumentException("유효하지 않은 할인 상태입니다: " + request.getStatus());
        }

        policy.update(
                request.getDiscountName(),
                discountType,
                request.getDiscountValue(),
                targetType,
                request.getTargetId(),
                request.getMinOrderAmount(),
                request.getMaxDiscountAmount(),
                request.getValidFrom(),
                request.getValidTo(),
                newStatus
        );

        log.info("관리자 할인 정책 수정: discountId={}, discountName={}", discountId, request.getDiscountName());

        return AdminDiscountPolicyDetailResponse.from(policy);
    }

    private DiscountPolicyStatus parseDiscountPolicyStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return DiscountPolicyStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 할인 상태입니다: " + status);
        }
    }

    private DiscountType parseDiscountType(String discountType) {
        if (discountType == null || discountType.isBlank()) {
            throw new IllegalArgumentException("할인 유형은 필수입니다");
        }
        try {
            return DiscountType.valueOf(discountType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 할인 유형입니다: " + discountType);
        }
    }

    private DiscountTargetType parseDiscountTargetType(String targetType) {
        if (targetType == null || targetType.isBlank()) {
            throw new IllegalArgumentException("적용 대상은 필수입니다");
        }
        try {
            return DiscountTargetType.valueOf(targetType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 적용 대상입니다: " + targetType);
        }
    }
}

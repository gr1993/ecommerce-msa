package com.example.promotionservice.service;

import com.example.promotionservice.domain.entity.Coupon;
import com.example.promotionservice.domain.entity.CouponStatus;
import com.example.promotionservice.domain.entity.DiscountType;
import com.example.promotionservice.dto.request.AdminCouponCreateRequest;
import com.example.promotionservice.dto.request.AdminCouponUpdateRequest;
import com.example.promotionservice.dto.response.AdminCouponDetailResponse;
import com.example.promotionservice.dto.response.AdminCouponResponse;
import com.example.promotionservice.global.common.dto.PageResponse;
import com.example.promotionservice.global.exception.CouponNotFoundException;
import com.example.promotionservice.repository.CouponRepository;
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
public class AdminCouponServiceImpl implements AdminCouponService {

    private final CouponRepository couponRepository;

    @Override
    public PageResponse<AdminCouponResponse> getCoupons(String keyword, String status, Pageable pageable) {
        CouponStatus couponStatus = parseCouponStatus(status);

        Page<Coupon> couponPage = couponRepository.findAllBySearchCondition(
                keyword != null && keyword.isBlank() ? null : keyword,
                couponStatus,
                pageable
        );

        Page<AdminCouponResponse> responsePage = couponPage.map(AdminCouponResponse::from);
        return PageResponse.from(responsePage);
    }

    @Override
    public AdminCouponDetailResponse getCouponDetail(Long couponId) {
        Coupon coupon = couponRepository.findByIdWithUserCoupons(couponId)
                .orElseThrow(() -> new CouponNotFoundException(couponId));

        return AdminCouponDetailResponse.from(coupon);
    }

    @Override
    @Transactional
    public AdminCouponDetailResponse createCoupon(AdminCouponCreateRequest request) {
        if (couponRepository.existsByCouponCode(request.getCouponCode())) {
            throw new IllegalArgumentException("이미 존재하는 쿠폰 코드입니다: " + request.getCouponCode());
        }

        DiscountType discountType = parseDiscountType(request.getDiscountType());
        CouponStatus couponStatus = parseCouponStatus(request.getStatus());
        if (couponStatus == null) {
            throw new IllegalArgumentException("유효하지 않은 쿠폰 상태입니다: " + request.getStatus());
        }

        Coupon coupon = Coupon.builder()
                .couponCode(request.getCouponCode())
                .couponName(request.getCouponName())
                .discountType(discountType)
                .discountValue(request.getDiscountValue())
                .minOrderAmount(request.getMinOrderAmount())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .status(couponStatus)
                .build();

        Coupon savedCoupon = couponRepository.save(coupon);
        log.info("관리자 쿠폰 등록: couponId={}, couponCode={}", savedCoupon.getId(), savedCoupon.getCouponCode());

        return AdminCouponDetailResponse.from(savedCoupon);
    }

    @Override
    @Transactional
    public AdminCouponDetailResponse updateCoupon(Long couponId, AdminCouponUpdateRequest request) {
        Coupon coupon = couponRepository.findByIdWithUserCoupons(couponId)
                .orElseThrow(() -> new CouponNotFoundException(couponId));

        DiscountType discountType = parseDiscountType(request.getDiscountType());
        CouponStatus newStatus = parseCouponStatus(request.getStatus());
        if (newStatus == null) {
            throw new IllegalArgumentException("유효하지 않은 쿠폰 상태입니다: " + request.getStatus());
        }

        coupon.update(
                request.getCouponCode(),
                request.getCouponName(),
                discountType,
                request.getDiscountValue(),
                request.getMinOrderAmount(),
                request.getMaxDiscountAmount(),
                request.getValidFrom(),
                request.getValidTo(),
                newStatus
        );

        log.info("관리자 쿠폰 수정: couponId={}, couponCode={}", couponId, request.getCouponCode());

        return AdminCouponDetailResponse.from(coupon);
    }

    private CouponStatus parseCouponStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return CouponStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 쿠폰 상태입니다: " + status);
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
}

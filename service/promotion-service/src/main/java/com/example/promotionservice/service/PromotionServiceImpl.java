package com.example.promotionservice.service;

import com.example.promotionservice.domain.entity.*;
import com.example.promotionservice.dto.request.CouponClaimRequest;
import com.example.promotionservice.dto.response.CouponClaimResponse;
import com.example.promotionservice.dto.response.UserCouponResponse;
import com.example.promotionservice.global.exception.CouponNotFoundException;
import com.example.promotionservice.repository.CouponRepository;
import com.example.promotionservice.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromotionServiceImpl implements PromotionService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    @Override
    @Transactional
    public CouponClaimResponse claimCoupon(Long userId, CouponClaimRequest request) {
        Coupon coupon = couponRepository.findByCouponCode(request.getCouponCode())
                .orElseThrow(() -> new CouponNotFoundException("존재하지 않는 쿠폰 코드입니다: " + request.getCouponCode()));

        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            throw new IllegalArgumentException("현재 사용할 수 없는 쿠폰입니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidTo())) {
            throw new IllegalArgumentException("쿠폰 유효 기간이 아닙니다.");
        }

        userCouponRepository.findByUserIdAndCouponId(userId, coupon.getId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("이미 등록된 쿠폰입니다.");
                });

        UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .coupon(coupon)
                .couponStatus(UserCouponStatus.ISSUED)
                .build();

        UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);
        log.info("쿠폰 등록 완료: userId={}, couponCode={}, userCouponId={}",
                userId, coupon.getCouponCode(), savedUserCoupon.getId());

        return CouponClaimResponse.from(savedUserCoupon);
    }

    @Override
    public List<UserCouponResponse> getUserCoupons(Long userId) {
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);
        return userCoupons.stream()
                .map(UserCouponResponse::from)
                .toList();
    }
}

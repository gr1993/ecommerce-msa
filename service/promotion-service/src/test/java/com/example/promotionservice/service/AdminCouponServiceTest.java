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
class AdminCouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private AdminCouponServiceImpl adminCouponService;

    private Coupon testCoupon;
    private AdminCouponCreateRequest createRequest;
    private AdminCouponUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        testCoupon = Coupon.builder()
                .couponCode("WELCOME10")
                .couponName("신규 가입 환영 쿠폰")
                .discountType(DiscountType.RATE)
                .discountValue(new BigDecimal("10.00"))
                .minOrderAmount(new BigDecimal("10000.00"))
                .maxDiscountAmount(new BigDecimal("5000.00"))
                .validFrom(LocalDateTime.of(2024, 1, 1, 0, 0))
                .validTo(LocalDateTime.of(2024, 12, 31, 23, 59))
                .status(CouponStatus.ACTIVE)
                .build();

        createRequest = AdminCouponCreateRequest.builder()
                .couponCode("SUMMER20")
                .couponName("여름 특가 쿠폰")
                .discountType("RATE")
                .discountValue(new BigDecimal("20.00"))
                .minOrderAmount(new BigDecimal("50000.00"))
                .maxDiscountAmount(new BigDecimal("20000.00"))
                .validFrom(LocalDateTime.of(2024, 6, 1, 0, 0))
                .validTo(LocalDateTime.of(2024, 8, 31, 23, 59))
                .status("ACTIVE")
                .build();

        updateRequest = AdminCouponUpdateRequest.builder()
                .couponCode("WELCOME10")
                .couponName("신규 가입 환영 쿠폰 (수정)")
                .discountType("FIXED")
                .discountValue(new BigDecimal("5000.00"))
                .minOrderAmount(new BigDecimal("20000.00"))
                .maxDiscountAmount(null)
                .validFrom(LocalDateTime.of(2024, 1, 1, 0, 0))
                .validTo(LocalDateTime.of(2025, 6, 30, 23, 59))
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("쿠폰 목록 조회 성공")
    void getCoupons_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Page<Coupon> couponPage = new PageImpl<>(List.of(testCoupon), pageable, 1);
        given(couponRepository.findAllBySearchCondition(eq(null), eq(null), any(Pageable.class)))
                .willReturn(couponPage);

        // when
        PageResponse<AdminCouponResponse> response = adminCouponService.getCoupons(null, null, pageable);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getCouponCode()).isEqualTo("WELCOME10");
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("쿠폰 목록 조회 - 키워드 필터링")
    void getCoupons_WithKeyword() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Page<Coupon> couponPage = new PageImpl<>(List.of(testCoupon), pageable, 1);
        given(couponRepository.findAllBySearchCondition(eq("WELCOME"), eq(null), any(Pageable.class)))
                .willReturn(couponPage);

        // when
        PageResponse<AdminCouponResponse> response = adminCouponService.getCoupons("WELCOME", null, pageable);

        // then
        assertThat(response.getContent()).hasSize(1);
        verify(couponRepository).findAllBySearchCondition(eq("WELCOME"), eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("쿠폰 목록 조회 - 상태 필터링")
    void getCoupons_WithStatus() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Page<Coupon> couponPage = new PageImpl<>(List.of(testCoupon), pageable, 1);
        given(couponRepository.findAllBySearchCondition(eq(null), eq(CouponStatus.ACTIVE), any(Pageable.class)))
                .willReturn(couponPage);

        // when
        PageResponse<AdminCouponResponse> response = adminCouponService.getCoupons(null, "ACTIVE", pageable);

        // then
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getStatus()).isEqualTo(CouponStatus.ACTIVE);
    }

    @Test
    @DisplayName("쿠폰 상세 조회 성공")
    void getCouponDetail_Success() {
        // given
        given(couponRepository.findByIdWithUserCoupons(1L)).willReturn(Optional.of(testCoupon));

        // when
        AdminCouponDetailResponse response = adminCouponService.getCouponDetail(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getCouponCode()).isEqualTo("WELCOME10");
        assertThat(response.getCouponName()).isEqualTo("신규 가입 환영 쿠폰");
        assertThat(response.getDiscountType()).isEqualTo(DiscountType.RATE);
    }

    @Test
    @DisplayName("쿠폰 상세 조회 실패 - 존재하지 않는 쿠폰")
    void getCouponDetail_NotFound() {
        // given
        given(couponRepository.findByIdWithUserCoupons(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminCouponService.getCouponDetail(999L))
                .isInstanceOf(CouponNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("쿠폰 등록 성공")
    void createCoupon_Success() {
        // given
        given(couponRepository.existsByCouponCode("SUMMER20")).willReturn(false);
        given(couponRepository.save(any(Coupon.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        AdminCouponDetailResponse response = adminCouponService.createCoupon(createRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getCouponCode()).isEqualTo("SUMMER20");
        assertThat(response.getCouponName()).isEqualTo("여름 특가 쿠폰");
        assertThat(response.getDiscountType()).isEqualTo(DiscountType.RATE);
        assertThat(response.getStatus()).isEqualTo(CouponStatus.ACTIVE);
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    @DisplayName("쿠폰 등록 실패 - 중복 쿠폰 코드")
    void createCoupon_DuplicateCode() {
        // given
        given(couponRepository.existsByCouponCode("SUMMER20")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> adminCouponService.createCoupon(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 존재하는 쿠폰 코드입니다");
    }

    @Test
    @DisplayName("쿠폰 수정 성공")
    void updateCoupon_Success() {
        // given
        given(couponRepository.findByIdWithUserCoupons(1L)).willReturn(Optional.of(testCoupon));

        // when
        AdminCouponDetailResponse response = adminCouponService.updateCoupon(1L, updateRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getCouponName()).isEqualTo("신규 가입 환영 쿠폰 (수정)");
        assertThat(response.getDiscountType()).isEqualTo(DiscountType.FIXED);
        assertThat(response.getDiscountValue()).isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    @DisplayName("쿠폰 수정 실패 - 존재하지 않는 쿠폰")
    void updateCoupon_NotFound() {
        // given
        given(couponRepository.findByIdWithUserCoupons(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminCouponService.updateCoupon(999L, updateRequest))
                .isInstanceOf(CouponNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("쿠폰 등록 실패 - 유효하지 않은 할인 유형")
    void createCoupon_InvalidDiscountType() {
        // given
        AdminCouponCreateRequest invalidRequest = AdminCouponCreateRequest.builder()
                .couponCode("TEST01")
                .couponName("테스트 쿠폰")
                .discountType("INVALID")
                .discountValue(new BigDecimal("10.00"))
                .validFrom(LocalDateTime.of(2024, 1, 1, 0, 0))
                .validTo(LocalDateTime.of(2024, 12, 31, 23, 59))
                .status("ACTIVE")
                .build();
        given(couponRepository.existsByCouponCode("TEST01")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> adminCouponService.createCoupon(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은 할인 유형");
    }

    @Test
    @DisplayName("쿠폰 등록 실패 - 유효하지 않은 상태")
    void createCoupon_InvalidStatus() {
        // given
        AdminCouponCreateRequest invalidRequest = AdminCouponCreateRequest.builder()
                .couponCode("TEST02")
                .couponName("테스트 쿠폰")
                .discountType("FIXED")
                .discountValue(new BigDecimal("5000.00"))
                .validFrom(LocalDateTime.of(2024, 1, 1, 0, 0))
                .validTo(LocalDateTime.of(2024, 12, 31, 23, 59))
                .status("INVALID_STATUS")
                .build();
        given(couponRepository.existsByCouponCode("TEST02")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> adminCouponService.createCoupon(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은 쿠폰 상태");
    }
}

package com.example.promotionservice.repository;

import com.example.promotionservice.domain.entity.Coupon;
import com.example.promotionservice.domain.entity.CouponStatus;
import com.example.promotionservice.domain.entity.DiscountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CouponRepositoryTest {

    @Autowired
    private CouponRepository couponRepository;

    private Coupon testCoupon;

    @BeforeEach
    void setUp() {
        testCoupon = Coupon.builder()
                .couponCode("WELCOME-2024")
                .couponName("신규 가입 쿠폰")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("5000.00"))
                .minOrderAmount(new BigDecimal("20000.00"))
                .maxDiscountAmount(null)
                .validFrom(LocalDateTime.of(2024, 1, 1, 0, 0))
                .validTo(LocalDateTime.of(2024, 12, 31, 23, 59))
                .status(CouponStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("쿠폰 저장 테스트")
    void saveCoupon() {
        // when
        Coupon savedCoupon = couponRepository.save(testCoupon);

        // then
        assertThat(savedCoupon.getId()).isNotNull();
        assertThat(savedCoupon.getCouponCode()).isEqualTo("WELCOME-2024");
        assertThat(savedCoupon.getCouponName()).isEqualTo("신규 가입 쿠폰");
        assertThat(savedCoupon.getDiscountType()).isEqualTo(DiscountType.FIXED);
        assertThat(savedCoupon.getCreatedAt()).isNotNull();
        assertThat(savedCoupon.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("쿠폰 코드로 조회 테스트")
    void findByCouponCode() {
        // given
        couponRepository.save(testCoupon);

        // when
        Optional<Coupon> foundCoupon = couponRepository.findByCouponCode("WELCOME-2024");

        // then
        assertThat(foundCoupon).isPresent();
        assertThat(foundCoupon.get().getCouponCode()).isEqualTo("WELCOME-2024");
    }

    @Test
    @DisplayName("쿠폰 상태로 목록 조회 테스트")
    void findByStatus() {
        // given
        couponRepository.save(testCoupon);

        Coupon inactiveCoupon = Coupon.builder()
                .couponCode("INACTIVE-001")
                .couponName("비활성 쿠폰")
                .discountType(DiscountType.RATE)
                .discountValue(new BigDecimal("10.00"))
                .minOrderAmount(BigDecimal.ZERO)
                .validFrom(LocalDateTime.of(2024, 1, 1, 0, 0))
                .validTo(LocalDateTime.of(2024, 12, 31, 23, 59))
                .status(CouponStatus.INACTIVE)
                .build();
        couponRepository.save(inactiveCoupon);

        // when
        List<Coupon> activeCoupons = couponRepository.findByStatus(CouponStatus.ACTIVE);

        // then
        assertThat(activeCoupons).hasSize(1);
        assertThat(activeCoupons.get(0).getStatus()).isEqualTo(CouponStatus.ACTIVE);
    }

    @Test
    @DisplayName("쿠폰 코드 존재 여부 확인 테스트")
    void existsByCouponCode() {
        // given
        couponRepository.save(testCoupon);

        // when & then
        assertThat(couponRepository.existsByCouponCode("WELCOME-2024")).isTrue();
        assertThat(couponRepository.existsByCouponCode("NOT-EXIST")).isFalse();
    }

    @Test
    @DisplayName("만료된 쿠폰 조회 테스트")
    void findByValidToBeforeAndStatus() {
        // given
        couponRepository.save(testCoupon);

        // when
        List<Coupon> expiredCoupons = couponRepository.findByValidToBeforeAndStatus(
                LocalDateTime.of(2025, 1, 1, 0, 0), CouponStatus.ACTIVE);

        // then
        assertThat(expiredCoupons).hasSize(1);
        assertThat(expiredCoupons.get(0).getCouponCode()).isEqualTo("WELCOME-2024");
    }

    @Test
    @DisplayName("쿠폰 상태 업데이트 테스트")
    void updateCouponStatus() {
        // given
        Coupon savedCoupon = couponRepository.save(testCoupon);

        // when
        savedCoupon.updateStatus(CouponStatus.EXPIRED);
        Coupon updatedCoupon = couponRepository.save(savedCoupon);

        // then
        assertThat(updatedCoupon.getStatus()).isEqualTo(CouponStatus.EXPIRED);
    }

    @Test
    @DisplayName("쿠폰 삭제 테스트")
    void deleteCoupon() {
        // given
        Coupon savedCoupon = couponRepository.save(testCoupon);
        Long couponId = savedCoupon.getId();

        // when
        couponRepository.deleteById(couponId);

        // then
        assertThat(couponRepository.findById(couponId)).isEmpty();
    }
}

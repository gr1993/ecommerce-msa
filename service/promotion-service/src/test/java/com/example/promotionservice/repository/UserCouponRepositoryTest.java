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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserCouponRepositoryTest {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    private Coupon testCoupon;
    private UserCoupon testUserCoupon;

    @BeforeEach
    void setUp() {
        testCoupon = Coupon.builder()
                .couponCode("WELCOME-2024")
                .couponName("신규 가입 쿠폰")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("5000.00"))
                .minOrderAmount(new BigDecimal("20000.00"))
                .validFrom(LocalDateTime.of(2024, 1, 1, 0, 0))
                .validTo(LocalDateTime.of(2024, 12, 31, 23, 59))
                .status(CouponStatus.ACTIVE)
                .build();

        testUserCoupon = UserCoupon.builder()
                .userId(1L)
                .coupon(testCoupon)
                .couponStatus(UserCouponStatus.ISSUED)
                .build();
    }

    @Test
    @DisplayName("사용자 쿠폰 저장 테스트")
    void saveUserCoupon() {
        // given
        Coupon savedCoupon = couponRepository.save(testCoupon);
        savedCoupon.addUserCoupon(testUserCoupon);
        couponRepository.flush();

        // when
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(1L);

        // then
        assertThat(userCoupons).hasSize(1);
        assertThat(userCoupons.get(0).getUserId()).isEqualTo(1L);
        assertThat(userCoupons.get(0).getCouponStatus()).isEqualTo(UserCouponStatus.ISSUED);
        assertThat(userCoupons.get(0).getIssuedAt()).isNotNull();
    }

    @Test
    @DisplayName("사용자 ID로 쿠폰 목록 조회 테스트")
    void findByUserId() {
        // given
        Coupon savedCoupon = couponRepository.save(testCoupon);
        savedCoupon.addUserCoupon(testUserCoupon);

        UserCoupon anotherUserCoupon = UserCoupon.builder()
                .userId(1L)
                .couponStatus(UserCouponStatus.USED)
                .build();
        savedCoupon.addUserCoupon(anotherUserCoupon);
        couponRepository.flush();

        // when
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(1L);

        // then
        assertThat(userCoupons).hasSize(2);
    }

    @Test
    @DisplayName("사용자 ID와 쿠폰 상태로 조회 테스트")
    void findByUserIdAndCouponStatus() {
        // given
        Coupon savedCoupon = couponRepository.save(testCoupon);
        savedCoupon.addUserCoupon(testUserCoupon);
        couponRepository.flush();

        // when
        List<UserCoupon> issuedCoupons = userCouponRepository.findByUserIdAndCouponStatus(
                1L, UserCouponStatus.ISSUED);

        // then
        assertThat(issuedCoupons).hasSize(1);
        assertThat(issuedCoupons.get(0).getCouponStatus()).isEqualTo(UserCouponStatus.ISSUED);
    }

    @Test
    @DisplayName("쿠폰 ID로 발급 목록 조회 테스트")
    void findByCouponId() {
        // given
        Coupon savedCoupon = couponRepository.save(testCoupon);
        savedCoupon.addUserCoupon(testUserCoupon);

        UserCoupon anotherUserCoupon = UserCoupon.builder()
                .userId(2L)
                .couponStatus(UserCouponStatus.ISSUED)
                .build();
        savedCoupon.addUserCoupon(anotherUserCoupon);
        couponRepository.flush();

        // when
        List<UserCoupon> userCoupons = userCouponRepository.findByCouponId(savedCoupon.getId());

        // then
        assertThat(userCoupons).hasSize(2);
    }

    @Test
    @DisplayName("사용자 ID와 쿠폰 ID로 조회 테스트")
    void findByUserIdAndCouponId() {
        // given
        Coupon savedCoupon = couponRepository.save(testCoupon);
        savedCoupon.addUserCoupon(testUserCoupon);
        couponRepository.flush();

        // when
        Optional<UserCoupon> found = userCouponRepository.findByUserIdAndCouponId(
                1L, savedCoupon.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("쿠폰 사용 처리 테스트")
    void useUserCoupon() {
        // given
        Coupon savedCoupon = couponRepository.save(testCoupon);
        savedCoupon.addUserCoupon(testUserCoupon);
        couponRepository.flush();

        // when
        UserCoupon userCoupon = userCouponRepository.findByUserId(1L).get(0);
        userCoupon.use();
        userCouponRepository.flush();

        // then
        UserCoupon usedCoupon = userCouponRepository.findById(userCoupon.getId()).get();
        assertThat(usedCoupon.getCouponStatus()).isEqualTo(UserCouponStatus.USED);
        assertThat(usedCoupon.getUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("쿠폰 삭제 시 사용자 쿠폰도 함께 삭제 테스트 (Cascade)")
    void deleteCouponCascadeUserCoupons() {
        // given
        Coupon savedCoupon = couponRepository.save(testCoupon);
        savedCoupon.addUserCoupon(testUserCoupon);
        couponRepository.flush();

        Long couponId = savedCoupon.getId();

        // when
        couponRepository.deleteById(couponId);
        couponRepository.flush();

        // then
        assertThat(userCouponRepository.findByCouponId(couponId)).isEmpty();
    }
}

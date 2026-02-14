package com.example.promotionservice.repository;

import com.example.promotionservice.domain.entity.UserCoupon;
import com.example.promotionservice.domain.entity.UserCouponStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    List<UserCoupon> findByUserId(Long userId);

    List<UserCoupon> findByUserIdAndCouponStatus(Long userId, UserCouponStatus couponStatus);

    List<UserCoupon> findByCouponId(Long couponId);

    Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId);
}

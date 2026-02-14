package com.example.promotionservice.repository;

import com.example.promotionservice.domain.entity.Coupon;
import com.example.promotionservice.domain.entity.CouponStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCouponCode(String couponCode);

    List<Coupon> findByStatus(CouponStatus status);

    boolean existsByCouponCode(String couponCode);

    @Query("SELECT c FROM Coupon c WHERE c.validTo < :now AND c.status = :status")
    List<Coupon> findByValidToBeforeAndStatus(@Param("now") LocalDateTime now,
                                              @Param("status") CouponStatus status);

    @Query("SELECT c FROM Coupon c LEFT JOIN FETCH c.userCoupons WHERE c.id = :couponId")
    Optional<Coupon> findByIdWithUserCoupons(@Param("couponId") Long couponId);
}

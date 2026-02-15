package com.example.promotionservice.repository;

import com.example.promotionservice.domain.entity.DiscountPolicy;
import com.example.promotionservice.domain.entity.DiscountPolicyStatus;
import com.example.promotionservice.domain.entity.DiscountTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DiscountPolicyRepository extends JpaRepository<DiscountPolicy, Long> {

    List<DiscountPolicy> findByStatus(DiscountPolicyStatus status);

    List<DiscountPolicy> findByTargetTypeAndTargetId(DiscountTargetType targetType, Long targetId);

    List<DiscountPolicy> findByTargetTypeAndStatus(DiscountTargetType targetType, DiscountPolicyStatus status);

    @Query("SELECT d FROM DiscountPolicy d WHERE d.validTo < :now AND d.status = :status")
    List<DiscountPolicy> findByValidToBeforeAndStatus(@Param("now") LocalDateTime now,
                                                      @Param("status") DiscountPolicyStatus status);

    @Query("SELECT d FROM DiscountPolicy d WHERE " +
            "(:keyword IS NULL OR d.discountName LIKE %:keyword%) AND " +
            "(:status IS NULL OR d.status = :status)")
    Page<DiscountPolicy> findAllBySearchCondition(@Param("keyword") String keyword,
                                                   @Param("status") DiscountPolicyStatus status,
                                                   Pageable pageable);

    @Query("SELECT d FROM DiscountPolicy d WHERE d.status = :status " +
            "AND d.validFrom <= :now AND d.validTo >= :now " +
            "AND (d.targetType = 'ORDER' OR (d.targetType = 'PRODUCT' AND d.targetId IN :productIds))")
    List<DiscountPolicy> findApplicablePolicies(@Param("status") DiscountPolicyStatus status,
                                                 @Param("now") LocalDateTime now,
                                                 @Param("productIds") List<Long> productIds);
}

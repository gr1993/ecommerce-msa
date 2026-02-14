package com.example.promotionservice.repository;

import com.example.promotionservice.domain.entity.DiscountPolicy;
import com.example.promotionservice.domain.entity.DiscountPolicyStatus;
import com.example.promotionservice.domain.entity.DiscountTargetType;
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
}

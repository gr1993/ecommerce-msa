package com.example.promotionservice.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "discount_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiscountPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "discount_id")
    private Long id;

    @Column(name = "discount_name", nullable = false, length = 100)
    private String discountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private DiscountTargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "min_order_amount", precision = 12, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "max_discount_amount", precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDateTime validTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DiscountPolicyStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public DiscountPolicy(String discountName, DiscountType discountType,
                          BigDecimal discountValue, DiscountTargetType targetType,
                          Long targetId, BigDecimal minOrderAmount, BigDecimal maxDiscountAmount,
                          LocalDateTime validFrom, LocalDateTime validTo, DiscountPolicyStatus status) {
        this.discountName = discountName;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.targetType = targetType;
        this.targetId = targetId;
        this.minOrderAmount = minOrderAmount != null ? minOrderAmount : BigDecimal.ZERO;
        this.maxDiscountAmount = maxDiscountAmount;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.status = status;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updateStatus(DiscountPolicyStatus status) {
        this.status = status;
    }
}

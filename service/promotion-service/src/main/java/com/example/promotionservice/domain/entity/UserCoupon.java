package com.example.promotionservice.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_coupon")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_coupon_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_status", nullable = false, length = 20)
    private UserCouponStatus couponStatus;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @Builder
    public UserCoupon(Long userId, Coupon coupon, UserCouponStatus couponStatus) {
        this.userId = userId;
        this.coupon = coupon;
        this.couponStatus = couponStatus;
    }

    @PrePersist
    protected void onCreate() {
        this.issuedAt = LocalDateTime.now();
    }

    public void use() {
        this.couponStatus = UserCouponStatus.USED;
        this.usedAt = LocalDateTime.now();
    }

    public void restore() {
        this.couponStatus = UserCouponStatus.RESTORED;
    }

    public void expire() {
        this.couponStatus = UserCouponStatus.EXPIRED;
    }

    void setCoupon(Coupon coupon) {
        this.coupon = coupon;
    }
}

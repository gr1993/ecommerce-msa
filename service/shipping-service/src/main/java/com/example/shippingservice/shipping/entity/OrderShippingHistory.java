package com.example.shippingservice.shipping.entity;

import com.example.shippingservice.shipping.enums.ShippingStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_shipping_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderShippingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shipping_history_id")
    private Long shippingHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_id", nullable = false)
    private OrderShipping orderShipping;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30)
    private ShippingStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 30)
    private ShippingStatus newStatus;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "remark", length = 200)
    private String remark;

    @Column(name = "tracking_kind", length = 30)
    private String trackingKind;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "changed_by", length = 50)
    private String changedBy;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @Builder
    public OrderShippingHistory(OrderShipping orderShipping, ShippingStatus previousStatus,
                                 ShippingStatus newStatus, String location, String remark,
                                 String trackingKind, String trackingNumber, String changedBy) {
        this.orderShipping = orderShipping;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.location = location;
        this.remark = remark;
        this.trackingKind = trackingKind;
        this.trackingNumber = trackingNumber;
        this.changedBy = changedBy;
    }
}

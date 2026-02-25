package com.example.shippingservice.exchange.entity;

import com.example.shippingservice.exchange.enums.ExchangeStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_exchange_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderExchangeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exchange_history_id")
    private Long exchangeHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_id", nullable = false)
    private OrderExchange orderExchange;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30)
    private ExchangeStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 30)
    private ExchangeStatus newStatus;

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
    public OrderExchangeHistory(OrderExchange orderExchange, ExchangeStatus previousStatus,
                                ExchangeStatus newStatus, String location, String remark,
                                String trackingKind, String trackingNumber, String changedBy) {
        this.orderExchange = orderExchange;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.location = location;
        this.remark = remark;
        this.trackingKind = trackingKind;
        this.trackingNumber = trackingNumber;
        this.changedBy = changedBy;
    }
}

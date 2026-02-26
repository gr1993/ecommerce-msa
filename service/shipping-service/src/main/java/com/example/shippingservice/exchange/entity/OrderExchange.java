package com.example.shippingservice.exchange.entity;

import com.example.shippingservice.exchange.enums.ExchangeStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order_exchange")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderExchange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exchange_id")
    private Long exchangeId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "exchange_status", nullable = false, length = 30)
    private ExchangeStatus exchangeStatus;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    // 회수 정보 (교환 승인 시 입력)
    @Column(name = "collect_courier", length = 50)
    private String collectCourier;

    @Column(name = "collect_tracking_number", length = 50)
    private String collectTrackingNumber;

    @Column(name = "collect_receiver_name", length = 100)
    private String collectReceiverName;

    @Column(name = "collect_receiver_phone", length = 20)
    private String collectReceiverPhone;

    @Column(name = "collect_address", length = 500)
    private String collectAddress;

    @Column(name = "collect_postal_code", length = 20)
    private String collectPostalCode;

    // 교환 배송 정보 (새 물품 발송 시 입력)
    @Column(name = "tracking_number", length = 50)
    private String trackingNumber;

    @Column(name = "courier", length = 50)
    private String courier;

    @Column(name = "receiver_name", length = 100)
    private String receiverName;

    @Column(name = "receiver_phone", length = 20)
    private String receiverPhone;

    @Column(name = "exchange_address", length = 500)
    private String exchangeAddress;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @CreationTimestamp
    @Column(name = "requested_at", updatable = false)
    private LocalDateTime requestedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "orderExchange", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderExchangeHistory> exchangeHistories = new ArrayList<>();

    @OneToMany(mappedBy = "orderExchange", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderExchangeItem> exchangeItems = new ArrayList<>();

    @Builder
    public OrderExchange(Long orderId, Long userId, ExchangeStatus exchangeStatus, String reason) {
        this.orderId = orderId;
        this.userId = userId;
        this.exchangeStatus = exchangeStatus;
        this.reason = reason;
    }

    public void updateExchangeStatus(ExchangeStatus exchangeStatus) {
        this.exchangeStatus = exchangeStatus;
    }

    public void reject(String rejectReason) {
        this.exchangeStatus = ExchangeStatus.EXCHANGE_REJECTED;
        this.rejectReason = rejectReason;
    }

    /** 교환 승인 시: 회수용 수거지 정보 저장 */
    public void updateCollectInfo(String collectReceiverName, String collectReceiverPhone,
                                  String collectAddress, String collectPostalCode) {
        this.collectReceiverName = collectReceiverName;
        this.collectReceiverPhone = collectReceiverPhone;
        this.collectAddress = collectAddress;
        this.collectPostalCode = collectPostalCode;
    }

    /** 교환 승인 시: 회수 운송장 자동 발급 후 저장 */
    public void updateCollectTrackingInfo(String collectCourier, String collectTrackingNumber) {
        this.collectCourier = collectCourier;
        this.collectTrackingNumber = collectTrackingNumber;
    }

    /** 교환 배송 시작 시: 교환품 배송지 정보 저장 */
    public void updateExchangeAddress(String receiverName, String receiverPhone,
                                      String exchangeAddress, String postalCode) {
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.exchangeAddress = exchangeAddress;
        this.postalCode = postalCode;
    }

    /** 교환 배송 시작 시: 교환품 배송 운송장 자동 발급 후 저장 */
    public void updateTrackingInfo(String courier, String trackingNumber) {
        this.courier = courier;
        this.trackingNumber = trackingNumber;
    }

    public void addExchangeHistory(ExchangeStatus previousStatus, ExchangeStatus newStatus, String location,
                                   String remark, String trackingKind, String changedBy) {
        OrderExchangeHistory history = OrderExchangeHistory.builder()
                .orderExchange(this)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .location(location)
                .remark(remark)
                .trackingKind(trackingKind)
                .trackingNumber(this.trackingNumber)
                .changedBy(changedBy)
                .build();
        this.exchangeHistories.add(history);
    }

    public void addExchangeItem(Long orderItemId, Long originalOptionId, Long newOptionId, Integer quantity) {
        OrderExchangeItem item = OrderExchangeItem.builder()
                .orderExchange(this)
                .orderItemId(orderItemId)
                .originalOptionId(originalOptionId)
                .newOptionId(newOptionId)
                .quantity(quantity)
                .build();
        this.exchangeItems.add(item);
    }
}

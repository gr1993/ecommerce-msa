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

    @Enumerated(EnumType.STRING)
    @Column(name = "exchange_status", nullable = false, length = 30)
    private ExchangeStatus exchangeStatus;

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

    @Builder
    public OrderExchange(Long orderId, ExchangeStatus exchangeStatus, String trackingNumber,
                         String courier, String receiverName, String receiverPhone,
                         String exchangeAddress, String postalCode) {
        this.orderId = orderId;
        this.exchangeStatus = exchangeStatus;
        this.trackingNumber = trackingNumber;
        this.courier = courier;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.exchangeAddress = exchangeAddress;
        this.postalCode = postalCode;
    }

    public void updateExchangeStatus(ExchangeStatus exchangeStatus) {
        this.exchangeStatus = exchangeStatus;
    }

    public void updateTrackingInfo(String courier, String trackingNumber) {
        this.courier = courier;
        this.trackingNumber = trackingNumber;
    }

    public void updateExchangeAddress(String receiverName, String receiverPhone,
                                      String exchangeAddress, String postalCode) {
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.exchangeAddress = exchangeAddress;
        this.postalCode = postalCode;
    }
}

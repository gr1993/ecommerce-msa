package com.example.shippingservice.returns.entity;

import com.example.shippingservice.returns.enums.ReturnStatus;
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
@Table(name = "order_return")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_id")
    private Long returnId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_status", nullable = false, length = 30)
    private ReturnStatus returnStatus;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @Column(name = "tracking_number", length = 50)
    private String trackingNumber;

    @Column(name = "courier", length = 50)
    private String courier;

    @Column(name = "receiver_name", length = 100)
    private String receiverName;

    @Column(name = "receiver_phone", length = 20)
    private String receiverPhone;

    @Column(name = "return_address", length = 500)
    private String returnAddress;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @CreationTimestamp
    @Column(name = "requested_at", updatable = false)
    private LocalDateTime requestedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "orderReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderReturnHistory> returnHistories = new ArrayList<>();

    @Builder
    public OrderReturn(Long orderId, Long userId, ReturnStatus returnStatus, String reason,
                       String trackingNumber, String courier, String receiverName,
                       String receiverPhone, String returnAddress, String postalCode) {
        this.orderId = orderId;
        this.userId = userId;
        this.returnStatus = returnStatus;
        this.reason = reason;
        this.trackingNumber = trackingNumber;
        this.courier = courier;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.returnAddress = returnAddress;
        this.postalCode = postalCode;
    }

    public void updateReturnStatus(ReturnStatus returnStatus) {
        this.returnStatus = returnStatus;
    }

    public void reject(String rejectReason) {
        this.returnStatus = ReturnStatus.RETURN_REJECTED;
        this.rejectReason = rejectReason;
    }

    public void updateTrackingInfo(String courier, String trackingNumber) {
        this.courier = courier;
        this.trackingNumber = trackingNumber;
    }

    public void updateReturnAddress(String receiverName, String receiverPhone,
                                    String returnAddress, String postalCode) {
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.returnAddress = returnAddress;
        this.postalCode = postalCode;
    }

    public void addReturnHistory(ReturnStatus previousStatus, ReturnStatus newStatus, String location,
                                 String remark, String trackingKind, String changedBy) {
        OrderReturnHistory history = OrderReturnHistory.builder()
                .orderReturn(this)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .location(location)
                .remark(remark)
                .trackingKind(trackingKind)
                .trackingNumber(this.trackingNumber)
                .changedBy(changedBy)
                .build();
        this.returnHistories.add(history);
    }
}

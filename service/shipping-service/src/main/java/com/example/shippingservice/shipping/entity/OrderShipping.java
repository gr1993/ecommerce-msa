package com.example.shippingservice.shipping.entity;

import com.example.shippingservice.shipping.enums.DeliveryServiceStatus;
import com.example.shippingservice.shipping.enums.ShippingStatus;
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
@Table(name = "order_shipping")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderShipping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shipping_id")
    private Long shippingId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "receiver_name", nullable = false, length = 100)
    private String receiverName;

    @Column(name = "receiver_phone", nullable = false, length = 20)
    private String receiverPhone;

    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_status", nullable = false, length = 30)
    private ShippingStatus shippingStatus;

    @Column(name = "shipping_company", length = 100)
    private String shippingCompany;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_service_status", length = 30)
    private DeliveryServiceStatus deliveryServiceStatus;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "orderShipping", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderShippingHistory> shippingHistories = new ArrayList<>();

    @Builder
    public OrderShipping(Long orderId, String receiverName, String receiverPhone,
                         String address, String postalCode, ShippingStatus shippingStatus,
                         String shippingCompany, String trackingNumber,
                         DeliveryServiceStatus deliveryServiceStatus) {
        this.orderId = orderId;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.address = address;
        this.postalCode = postalCode;
        this.shippingStatus = shippingStatus;
        this.shippingCompany = shippingCompany;
        this.trackingNumber = trackingNumber;
        this.deliveryServiceStatus = deliveryServiceStatus;
    }

    public void updateShippingStatus(ShippingStatus newStatus, String changedBy) {
        updateShippingStatusWithDetail(newStatus, null, null, null, changedBy);
    }

    public void updateShippingStatusWithDetail(ShippingStatus newStatus, String location,
                                                String remark, String trackingKind, String changedBy) {
        ShippingStatus previousStatus = this.shippingStatus;
        this.shippingStatus = newStatus;

        OrderShippingHistory history = OrderShippingHistory.builder()
                .orderShipping(this)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .location(location)
                .remark(remark)
                .trackingKind(trackingKind)
                .changedBy(changedBy)
                .build();
        this.shippingHistories.add(history);
    }

    public void addTrackingDetail(String location, String remark, String trackingKind) {
        OrderShippingHistory history = OrderShippingHistory.builder()
                .orderShipping(this)
                .previousStatus(this.shippingStatus)
                .newStatus(this.shippingStatus)
                .location(location)
                .remark(remark)
                .trackingKind(trackingKind)
                .changedBy("DELIVERY_API")
                .build();
        this.shippingHistories.add(history);
    }

    public void updateTrackingInfo(String shippingCompany, String trackingNumber) {
        this.shippingCompany = shippingCompany;
        this.trackingNumber = trackingNumber;
    }

    public void updateDeliveryServiceStatus(DeliveryServiceStatus deliveryServiceStatus) {
        this.deliveryServiceStatus = deliveryServiceStatus;
    }
}

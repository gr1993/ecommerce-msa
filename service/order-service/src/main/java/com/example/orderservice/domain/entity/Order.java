package com.example.orderservice.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 30)
    private OrderStatus orderStatus;

    @Column(name = "total_product_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalProductAmount;

    @Column(name = "total_discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalDiscountAmount;

    @Column(name = "total_payment_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPaymentAmount;

    @Column(name = "order_memo", columnDefinition = "TEXT")
    private String orderMemo;

    @Column(name = "ordered_at", nullable = false, updatable = false)
    private LocalDateTime orderedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderPayment> orderPayments = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private OrderDelivery orderDelivery;

    @Builder
    public Order(String orderNumber, Long userId, OrderStatus orderStatus,
                 BigDecimal totalProductAmount, BigDecimal totalDiscountAmount,
                 BigDecimal totalPaymentAmount, String orderMemo) {
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.orderStatus = orderStatus;
        this.totalProductAmount = totalProductAmount;
        this.totalDiscountAmount = totalDiscountAmount != null ? totalDiscountAmount : BigDecimal.ZERO;
        this.totalPaymentAmount = totalPaymentAmount;
        this.orderMemo = orderMemo;
    }

    @PrePersist
    protected void onCreate() {
        this.orderedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updateStatus(OrderStatus status) {
        this.orderStatus = status;
    }

    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    public void addOrderPayment(OrderPayment orderPayment) {
        this.orderPayments.add(orderPayment);
        orderPayment.setOrder(this);
    }

    public void setOrderDelivery(OrderDelivery orderDelivery) {
        this.orderDelivery = orderDelivery;
        orderDelivery.setOrder(this);
    }

    public void updateTotalAmounts(BigDecimal totalProductAmount, BigDecimal totalDiscountAmount) {
        this.totalProductAmount = totalProductAmount;
        this.totalDiscountAmount = totalDiscountAmount != null ? totalDiscountAmount : BigDecimal.ZERO;
        this.totalPaymentAmount = totalProductAmount.subtract(this.totalDiscountAmount);
    }
}

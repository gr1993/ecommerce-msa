package com.example.orderservice.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_discount")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderDiscount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "order_discount_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@Enumerated(EnumType.STRING)
	@Column(name = "discount_type", nullable = false, length = 30)
	private DiscountType discountType;

	@Column(name = "reference_id")
	private Long referenceId;

	@Column(name = "discount_name", nullable = false, length = 100)
	private String discountName;

	@Column(name = "discount_amount", nullable = false)
	private Long discountAmount;

	@Column(name = "discount_rate", precision = 5, scale = 2)
	private BigDecimal discountRate;

	@Column(name = "description")
	private String description;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Builder
	public OrderDiscount(DiscountType discountType, Long referenceId, String discountName,
						 Long discountAmount, BigDecimal discountRate, String description) {
		this.discountType = discountType;
		this.referenceId = referenceId;
		this.discountName = discountName;
		this.discountAmount = discountAmount;
		this.discountRate = discountRate;
		this.description = description;
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

	void setOrder(Order order) {
		this.order = order;
	}
}

package com.example.shippingservice.exchange.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_exchange_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderExchangeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exchange_item_id")
    private Long exchangeItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_id", nullable = false)
    private OrderExchange orderExchange;

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;

    @Column(name = "original_option_id", nullable = false)
    private Long originalOptionId;

    @Column(name = "new_option_id", nullable = false)
    private Long newOptionId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Builder
    public OrderExchangeItem(OrderExchange orderExchange, Long orderItemId,
                             Long originalOptionId, Long newOptionId, Integer quantity) {
        this.orderExchange = orderExchange;
        this.orderItemId = orderItemId;
        this.originalOptionId = originalOptionId;
        this.newOptionId = newOptionId;
        this.quantity = quantity != null ? quantity : 1;
    }

    void setOrderExchange(OrderExchange orderExchange) {
        this.orderExchange = orderExchange;
    }
}

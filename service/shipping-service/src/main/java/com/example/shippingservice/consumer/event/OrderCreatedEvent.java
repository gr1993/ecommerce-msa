package com.example.shippingservice.consumer.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class OrderCreatedEvent {

    private Long orderId;
    private String orderNumber;
    private Long userId;
    private String orderStatus;
    private BigDecimal totalProductAmount;
    private BigDecimal totalDiscountAmount;
    private BigDecimal totalPaymentAmount;
    private List<OrderItemSnapshot> orderItems;
    private DeliverySnapshot delivery;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime orderedAt;

    @Getter
    @NoArgsConstructor
    public static class OrderItemSnapshot {
        private Long orderItemId;
        private Long productId;
        private Long skuId;
        private String productName;
        private String productCode;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }

    @Getter
    @NoArgsConstructor
    public static class DeliverySnapshot {
        private String receiverName;
        private String receiverPhone;
        private String zipcode;
        private String address;
        private String addressDetail;
        private String deliveryMemo;
    }
}

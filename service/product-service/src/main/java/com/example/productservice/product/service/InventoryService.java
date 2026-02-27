package com.example.productservice.product.service;

import com.example.productservice.consumer.event.InventoryDecreaseEvent;
import com.example.productservice.consumer.event.InventoryIncreaseEvent;
import com.example.productservice.consumer.event.OrderCancelledEvent;
import com.example.productservice.consumer.event.OrderCreatedEvent;
import com.example.productservice.consumer.event.PaymentCancelledEvent;

public interface InventoryService {

    /**
     * 주문 생성 이벤트에 따른 재고 선차감
     *
     * @param event 주문 생성 이벤트
     */
    void decreaseStock(OrderCreatedEvent event);

    /**
     * 주문 취소 이벤트에 따른 재고 복구 (보상 트랜잭션)
     *
     * @param event 주문 취소 이벤트
     */
    void restoreStockForOrderCancelled(OrderCancelledEvent event);

    /**
     * 결제 취소 이벤트에 따른 재고 복구 (보상 트랜잭션)
     *
     * @param event 결제 취소 이벤트
     */
    void restoreStockForPaymentCancelled(PaymentCancelledEvent event);

    /**
     * 교환 승인 이벤트에 따른 신규 SKU 재고 차감
     *
     * @param event 재고 차감 이벤트 (inventory.decrease)
     */
    void decreaseStockForExchangeApproved(InventoryDecreaseEvent event);

    /**
     * 교환 회수 완료 이벤트에 따른 원래 SKU 재고 증가
     *
     * @param event 재고 증가 이벤트 (inventory.increase)
     */
    void increaseStockForExchangeReturnCompleted(InventoryIncreaseEvent event);
}

package com.example.productservice.product.service;

import com.example.productservice.consumer.event.OrderCreatedEvent;

public interface InventoryService {

    /**
     * 주문 생성 이벤트에 따른 재고 선차감
     *
     * @param event 주문 생성 이벤트
     */
    void decreaseStock(OrderCreatedEvent event);
}

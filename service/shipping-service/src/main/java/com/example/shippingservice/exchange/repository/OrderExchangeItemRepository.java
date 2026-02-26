package com.example.shippingservice.exchange.repository;

import com.example.shippingservice.exchange.entity.OrderExchangeItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderExchangeItemRepository extends JpaRepository<OrderExchangeItem, Long> {

    List<OrderExchangeItem> findByOrderExchange_ExchangeId(Long exchangeId);
}

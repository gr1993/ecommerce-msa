package com.example.shippingservice.exchange.repository;

import com.example.shippingservice.exchange.entity.OrderExchange;
import com.example.shippingservice.exchange.enums.ExchangeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderExchangeRepository extends JpaRepository<OrderExchange, Long> {

    Optional<OrderExchange> findByOrderId(Long orderId);

    List<OrderExchange> findByExchangeStatus(ExchangeStatus exchangeStatus);

    List<OrderExchange> findByTrackingNumber(String trackingNumber);

    boolean existsByOrderId(Long orderId);
}

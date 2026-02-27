package com.example.shippingservice.exchange.repository;

import com.example.shippingservice.exchange.entity.OrderExchangeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderExchangeHistoryRepository extends JpaRepository<OrderExchangeHistory, Long> {

    List<OrderExchangeHistory> findByOrderExchange_ExchangeIdOrderByChangedAtDesc(Long exchangeId);

    @Query("SELECT h.trackingKind FROM OrderExchangeHistory h " +
            "WHERE h.orderExchange.exchangeId = :exchangeId AND h.trackingKind IS NOT NULL " +
            "ORDER BY h.changedAt DESC LIMIT 1")
    Optional<String> findLastTrackingKindByExchangeId(@Param("exchangeId") Long exchangeId);

    @Query("SELECT h.trackingKind FROM OrderExchangeHistory h " +
            "WHERE h.orderExchange.exchangeId = :exchangeId AND h.trackingNumber = :trackingNumber " +
            "AND h.trackingKind IS NOT NULL ORDER BY h.changedAt DESC LIMIT 1")
    Optional<String> findLastTrackingKindByExchangeIdAndTrackingNumber(
            @Param("exchangeId") Long exchangeId,
            @Param("trackingNumber") String trackingNumber);
}

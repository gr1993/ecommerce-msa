package com.example.shippingservice.shipping.repository;

import com.example.shippingservice.shipping.entity.OrderShippingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderShippingHistoryRepository extends JpaRepository<OrderShippingHistory, Long> {

    List<OrderShippingHistory> findByOrderShipping_ShippingIdOrderByChangedAtDesc(Long shippingId);

    @Query("SELECT h.trackingKind FROM OrderShippingHistory h " +
            "WHERE h.orderShipping.shippingId = :shippingId AND h.trackingKind IS NOT NULL " +
            "ORDER BY h.changedAt DESC LIMIT 1")
    Optional<String> findLastTrackingKindByShippingId(@Param("shippingId") Long shippingId);
}

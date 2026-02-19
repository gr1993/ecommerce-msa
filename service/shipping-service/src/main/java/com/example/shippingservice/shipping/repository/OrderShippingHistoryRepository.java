package com.example.shippingservice.shipping.repository;

import com.example.shippingservice.shipping.entity.OrderShippingHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderShippingHistoryRepository extends JpaRepository<OrderShippingHistory, Long> {

    List<OrderShippingHistory> findByOrderShipping_ShippingIdOrderByChangedAtDesc(Long shippingId);
}

package com.example.orderservice.repository;

import com.example.orderservice.domain.entity.OrderDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderDeliveryRepository extends JpaRepository<OrderDelivery, Long> {

    Optional<OrderDelivery> findByOrderId(Long orderId);
}

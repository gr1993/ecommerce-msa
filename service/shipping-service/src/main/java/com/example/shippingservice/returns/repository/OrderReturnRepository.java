package com.example.shippingservice.returns.repository;

import com.example.shippingservice.returns.entity.OrderReturn;
import com.example.shippingservice.returns.enums.ReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderReturnRepository extends JpaRepository<OrderReturn, Long> {

    Optional<OrderReturn> findByOrderId(Long orderId);

    List<OrderReturn> findByReturnStatus(ReturnStatus returnStatus);

    List<OrderReturn> findByTrackingNumber(String trackingNumber);

    boolean existsByOrderId(Long orderId);
}

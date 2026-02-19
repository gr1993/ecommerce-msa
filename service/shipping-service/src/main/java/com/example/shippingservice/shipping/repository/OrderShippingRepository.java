package com.example.shippingservice.shipping.repository;

import com.example.shippingservice.shipping.entity.OrderShipping;
import com.example.shippingservice.shipping.enums.DeliveryServiceStatus;
import com.example.shippingservice.shipping.enums.ShippingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderShippingRepository extends JpaRepository<OrderShipping, Long> {

    Optional<OrderShipping> findByOrderId(Long orderId);

    List<OrderShipping> findByShippingStatus(ShippingStatus shippingStatus);

    List<OrderShipping> findByDeliveryServiceStatus(DeliveryServiceStatus deliveryServiceStatus);

    List<OrderShipping> findByTrackingNumber(String trackingNumber);

    boolean existsByOrderId(Long orderId);
}

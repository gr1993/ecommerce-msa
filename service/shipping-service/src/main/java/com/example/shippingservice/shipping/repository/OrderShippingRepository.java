package com.example.shippingservice.shipping.repository;

import com.example.shippingservice.shipping.entity.OrderShipping;
import com.example.shippingservice.shipping.enums.DeliveryServiceStatus;
import com.example.shippingservice.shipping.enums.ShippingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderShippingRepository extends JpaRepository<OrderShipping, Long> {

    Optional<OrderShipping> findByOrderId(Long orderId);

    List<OrderShipping> findByShippingStatus(ShippingStatus shippingStatus);

    List<OrderShipping> findByDeliveryServiceStatus(DeliveryServiceStatus deliveryServiceStatus);

    List<OrderShipping> findByTrackingNumber(String trackingNumber);

    boolean existsByOrderId(Long orderId);

    @Query("SELECT s FROM OrderShipping s WHERE " +
            "(:shippingStatus IS NULL OR s.shippingStatus = :shippingStatus) AND " +
            "(:trackingNumber IS NULL OR s.trackingNumber LIKE %:trackingNumber%)")
    Page<OrderShipping> findAllBySearchCondition(@Param("shippingStatus") ShippingStatus shippingStatus,
                                                  @Param("trackingNumber") String trackingNumber,
                                                  Pageable pageable);
}

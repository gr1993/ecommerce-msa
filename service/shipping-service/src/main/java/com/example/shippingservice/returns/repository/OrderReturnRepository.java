package com.example.shippingservice.returns.repository;

import com.example.shippingservice.returns.entity.OrderReturn;
import com.example.shippingservice.returns.enums.ReturnStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderReturnRepository extends JpaRepository<OrderReturn, Long> {

    Optional<OrderReturn> findByOrderId(Long orderId);

    List<OrderReturn> findByReturnStatus(ReturnStatus returnStatus);

    List<OrderReturn> findByTrackingNumber(String trackingNumber);

    boolean existsByOrderId(Long orderId);

    boolean existsByOrderIdAndReturnStatusIn(Long orderId, List<ReturnStatus> statuses);

    Page<OrderReturn> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT r FROM OrderReturn r WHERE " +
            "(:returnStatus IS NULL OR r.returnStatus = :returnStatus) AND " +
            "(:orderId IS NULL OR r.orderId = :orderId)")
    Page<OrderReturn> findAllBySearchCondition(@Param("returnStatus") ReturnStatus returnStatus,
                                                @Param("orderId") Long orderId,
                                                Pageable pageable);
}

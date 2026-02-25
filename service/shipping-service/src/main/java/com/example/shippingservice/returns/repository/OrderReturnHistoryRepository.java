package com.example.shippingservice.returns.repository;

import com.example.shippingservice.returns.entity.OrderReturnHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderReturnHistoryRepository extends JpaRepository<OrderReturnHistory, Long> {

    List<OrderReturnHistory> findByOrderReturn_ReturnIdOrderByChangedAtDesc(Long returnId);

    @Query("SELECT h.trackingKind FROM OrderReturnHistory h " +
            "WHERE h.orderReturn.returnId = :returnId AND h.trackingKind IS NOT NULL " +
            "ORDER BY h.changedAt DESC LIMIT 1")
    Optional<String> findLastTrackingKindByReturnId(@Param("returnId") Long returnId);
}

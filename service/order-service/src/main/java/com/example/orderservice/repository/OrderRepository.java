package com.example.orderservice.repository;

import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByUserId(Long userId);

    List<Order> findByUserIdAndOrderStatus(Long userId, OrderStatus orderStatus);

    List<Order> findByOrderStatus(OrderStatus orderStatus);

    @Query("SELECT o FROM Order o WHERE o.orderedAt BETWEEN :startDate AND :endDate")
    List<Order> findByOrderedAtBetween(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.id = :orderId")
    Optional<Order> findByIdWithOrderItems(@Param("orderId") Long orderId);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderPayments WHERE o.id = :orderId")
    Optional<Order> findByIdWithOrderPayments(@Param("orderId") Long orderId);

    boolean existsByOrderNumber(String orderNumber);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.orderStatus = :status AND o.orderedAt < :expiredBefore")
    List<Order> findExpiredOrdersByStatusWithItems(@Param("status") OrderStatus status,
                                                    @Param("expiredBefore") LocalDateTime expiredBefore);
}

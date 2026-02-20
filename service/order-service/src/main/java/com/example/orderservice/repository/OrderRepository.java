package com.example.orderservice.repository;

import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    @Query("SELECT o FROM Order o WHERE " +
            "(:orderNumber IS NULL OR o.orderNumber LIKE %:orderNumber%) AND " +
            "(:orderStatus IS NULL OR o.orderStatus = :orderStatus)")
    Page<Order> findAllBySearchCondition(@Param("orderNumber") String orderNumber,
                                         @Param("orderStatus") OrderStatus orderStatus,
                                         Pageable pageable);

    @Query(value = "SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.userId = :userId",
            countQuery = "SELECT COUNT(o) FROM Order o WHERE o.userId = :userId")
    Page<Order> findByUserIdWithItems(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.orderItems " +
            "LEFT JOIN FETCH o.orderDelivery " +
            "WHERE o.id = :orderId")
    Optional<Order> findByIdWithOrderItemsAndDelivery(@Param("orderId") Long orderId);

    @Query(value = "SELECT o FROM Order o " +
            "JOIN FETCH o.orderDelivery " +
            "WHERE o.orderStatus = :status " +
            "ORDER BY o.orderedAt ASC",
            countQuery = "SELECT COUNT(o) FROM Order o WHERE o.orderStatus = :status")
    Page<Order> findByOrderStatusWithDelivery(@Param("status") OrderStatus status, Pageable pageable);
}

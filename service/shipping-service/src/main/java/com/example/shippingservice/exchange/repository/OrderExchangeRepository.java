package com.example.shippingservice.exchange.repository;

import com.example.shippingservice.exchange.entity.OrderExchange;
import com.example.shippingservice.exchange.enums.ExchangeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderExchangeRepository extends JpaRepository<OrderExchange, Long> {

    Optional<OrderExchange> findByOrderId(Long orderId);

    List<OrderExchange> findByExchangeStatus(ExchangeStatus exchangeStatus);

    List<OrderExchange> findByTrackingNumber(String trackingNumber);

    boolean existsByOrderId(Long orderId);

    boolean existsByOrderIdAndExchangeStatusIn(Long orderId, List<ExchangeStatus> statuses);

    Page<OrderExchange> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT e FROM OrderExchange e WHERE " +
            "(:exchangeStatus IS NULL OR e.exchangeStatus = :exchangeStatus) AND " +
            "(:orderId IS NULL OR e.orderId = :orderId)")
    Page<OrderExchange> findAllBySearchCondition(@Param("exchangeStatus") ExchangeStatus exchangeStatus,
                                                  @Param("orderId") Long orderId,
                                                  Pageable pageable);
}

package com.example.orderservice.repository;

import com.example.orderservice.domain.entity.OrderPayment;
import com.example.orderservice.domain.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderPaymentRepository extends JpaRepository<OrderPayment, Long> {

    List<OrderPayment> findByOrderId(Long orderId);

    List<OrderPayment> findByPaymentStatus(PaymentStatus paymentStatus);
}

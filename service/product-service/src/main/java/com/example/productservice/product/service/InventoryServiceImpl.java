package com.example.productservice.product.service;

import com.example.productservice.consumer.domain.ProcessedEvent;
import com.example.productservice.consumer.event.OrderCancelledEvent;
import com.example.productservice.consumer.event.OrderCreatedEvent;
import com.example.productservice.consumer.event.PaymentCancelledEvent;
import com.example.productservice.consumer.repository.ProcessedEventRepository;
import com.example.productservice.product.domain.ProductSku;
import com.example.productservice.product.repository.ProductSkuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private static final String EVENT_TYPE_ORDER_CREATED = "ORDER_CREATED";
    private static final String EVENT_TYPE_ORDER_CANCELLED = "ORDER_CANCELLED";
    private static final String EVENT_TYPE_PAYMENT_CANCELLED = "PAYMENT_CANCELLED";

    private final ProductSkuRepository productSkuRepository;
    private final ProductSkuHistoryService productSkuHistoryService;
    private final ProcessedEventRepository processedEventRepository;

    @Override
    @Transactional
    public void decreaseStock(OrderCreatedEvent event) {
        log.info("Starting stock decrease for orderId={}, orderNumber={}",
                event.getOrderId(), event.getOrderNumber());

        String aggregateId = event.getOrderId().toString();

        // 멱등성 체크: 이미 처리된 주문인지 확인
        if (processedEventRepository.existsByEventTypeAndAggregateId(EVENT_TYPE_ORDER_CREATED, aggregateId)) {
            log.warn("Order already processed (idempotency check): eventType={}, orderId={}, orderNumber={} - skipping",
                    EVENT_TYPE_ORDER_CREATED, event.getOrderId(), event.getOrderNumber());
            return;
        }

        for (OrderCreatedEvent.OrderItemSnapshot item : event.getOrderItems()) {
            ProductSku sku = productSkuRepository.findById(item.getSkuId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "SKU not found: skuId=" + item.getSkuId()));

            int currentStock = sku.getStockQty();
            int requestedQty = item.getQuantity();

            if (currentStock < requestedQty) {
                throw new IllegalStateException(String.format(
                        "Insufficient stock: skuId=%d, currentStock=%d, requestedQty=%d",
                        item.getSkuId(), currentStock, requestedQty));
            }

            int newStock = currentStock - requestedQty;
            sku.setStockQty(newStock);
            productSkuRepository.save(sku);

            // 재고 차감 이력 기록
            productSkuHistoryService.recordDeduction(sku, event.getOrderNumber(), requestedQty, newStock);

            log.info("Stock decreased: skuId={}, productName={}, before={}, after={}, quantity={}",
                    item.getSkuId(), item.getProductName(), currentStock,
                    newStock, requestedQty);
        }

        // 처리 완료 기록 (멱등성 보장)
        ProcessedEvent processedEvent = ProcessedEvent.ofOrderCreated(
                event.getOrderId(),
                event.getOrderNumber()
        );
        processedEventRepository.save(processedEvent);

        log.info("Completed stock decrease for orderId={}, itemCount={} - marked as processed",
                event.getOrderId(), event.getOrderItems().size());
    }

    @Override
    @Transactional
    public void restoreStockForOrderCancelled(OrderCancelledEvent event) {
        log.info("Starting stock restore (compensation) for order.cancelled: orderId={}, orderNumber={}, reason={}",
                event.getOrderId(), event.getOrderNumber(), event.getCancellationReason());

        String aggregateId = event.getOrderId().toString();

        // 멱등성 체크: 이미 처리된 주문 취소 이벤트인지 확인
        if (processedEventRepository.existsByEventTypeAndAggregateId(EVENT_TYPE_ORDER_CANCELLED, aggregateId)) {
            log.warn("Order cancellation already processed (idempotency check): eventType={}, orderId={}, orderNumber={} - skipping",
                    EVENT_TYPE_ORDER_CANCELLED, event.getOrderId(), event.getOrderNumber());
            return;
        }

        for (OrderCancelledEvent.CancelledOrderItem item : event.getCancelledItems()) {
            ProductSku sku = productSkuRepository.findById(item.getSkuId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "SKU not found: skuId=" + item.getSkuId()));

            int currentStock = sku.getStockQty();
            int recoveryQty = item.getQuantity();
            int newStock = currentStock + recoveryQty;

            sku.setStockQty(newStock);
            productSkuRepository.save(sku);

            // 재고 복구 이력 기록
            productSkuHistoryService.recordRestore(sku, event.getOrderNumber(), recoveryQty, newStock, event.getCancellationReason());

            log.info("Stock restored (order.cancelled): skuId={}, productName={}, before={}, after={}, recoveryQty={}, reason={}",
                    item.getSkuId(), item.getProductName(), currentStock,
                    newStock, recoveryQty, event.getCancellationReason());
        }

        // 처리 완료 기록 (멱등성 보장)
        ProcessedEvent processedEvent = ProcessedEvent.of(
                EVENT_TYPE_ORDER_CANCELLED,
                event.getOrderId().toString(),
                String.format("{\"orderId\":%d,\"reason\":\"%s\"}", event.getOrderId(), event.getCancellationReason())
        );
        processedEventRepository.save(processedEvent);

        log.info("Completed stock restore (order.cancelled) for orderId={}, itemCount={} - marked as processed",
                event.getOrderId(), event.getCancelledItems().size());
    }

    @Override
    @Transactional
    public void restoreStockForPaymentCancelled(PaymentCancelledEvent event) {
        log.info("Starting stock restore (compensation) for payment.cancelled: orderId={}, orderNumber={}, paymentId={}, reason={}",
                event.getOrderId(), event.getOrderNumber(), event.getPaymentId(), event.getCancellationReason());

        String aggregateId = event.getOrderId().toString();

        // 멱등성 체크: 이미 처리된 결제 취소 이벤트인지 확인
        if (processedEventRepository.existsByEventTypeAndAggregateId(EVENT_TYPE_PAYMENT_CANCELLED, aggregateId)) {
            log.warn("Payment cancellation already processed (idempotency check): eventType={}, orderId={}, orderNumber={} - skipping",
                    EVENT_TYPE_PAYMENT_CANCELLED, event.getOrderId(), event.getOrderNumber());
            return;
        }

        for (PaymentCancelledEvent.PaymentItem item : event.getItems()) {
            ProductSku sku = productSkuRepository.findById(item.getSkuId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "SKU not found: skuId=" + item.getSkuId()));

            int currentStock = sku.getStockQty();
            int recoveryQty = item.getQuantity();
            int newStock = currentStock + recoveryQty;

            sku.setStockQty(newStock);
            productSkuRepository.save(sku);

            // 재고 복구 이력 기록
            productSkuHistoryService.recordRestore(sku, event.getOrderNumber(), recoveryQty, newStock, event.getCancellationReason());

            log.info("Stock restored (payment.cancelled): skuId={}, productName={}, before={}, after={}, recoveryQty={}, reason={}",
                    item.getSkuId(), item.getProductName(), currentStock,
                    newStock, recoveryQty, event.getCancellationReason());
        }

        // 처리 완료 기록 (멱등성 보장)
        ProcessedEvent processedEvent = ProcessedEvent.of(
                EVENT_TYPE_PAYMENT_CANCELLED,
                event.getOrderId().toString(),
                String.format("{\"orderId\":%d,\"paymentId\":%d,\"reason\":\"%s\"}",
                        event.getOrderId(), event.getPaymentId(), event.getCancellationReason())
        );
        processedEventRepository.save(processedEvent);

        log.info("Completed stock restore (payment.cancelled) for orderId={}, itemCount={} - marked as processed",
                event.getOrderId(), event.getItems().size());
    }
}

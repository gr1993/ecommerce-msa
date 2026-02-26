package com.example.productservice.product.service;

import com.example.productservice.consumer.domain.ProcessedEvent;
import com.example.productservice.consumer.event.InventoryDecreaseEvent;
import com.example.productservice.consumer.event.OrderCancelledEvent;
import com.example.productservice.consumer.event.OrderCreatedEvent;
import com.example.productservice.consumer.event.PaymentCancelledEvent;
import com.example.productservice.consumer.repository.ProcessedEventRepository;
import com.example.productservice.global.common.EventTypeConstants;
import com.example.productservice.global.domain.Outbox;
import com.example.productservice.global.repository.OutboxRepository;
import com.example.productservice.product.domain.ProductSku;
import com.example.productservice.product.domain.event.StockRejectedEvent;
import com.example.productservice.product.repository.ProductSkuRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private static final String EVENT_TYPE_ORDER_CREATED = "ORDER_CREATED";
    private static final String EVENT_TYPE_ORDER_CANCELLED = "ORDER_CANCELLED";
    private static final String EVENT_TYPE_PAYMENT_CANCELLED = "PAYMENT_CANCELLED";
    private static final String EVENT_TYPE_INVENTORY_DECREASE = "INVENTORY_DECREASE";

    private final ProductSkuRepository productSkuRepository;
    private final ProductSkuHistoryService productSkuHistoryService;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

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

        // 재고 부족 항목 수집
        List<StockRejectedEvent.RejectedItem> rejectedItems = new ArrayList<>();

        for (OrderCreatedEvent.OrderItemSnapshot item : event.getOrderItems()) {
            ProductSku sku = productSkuRepository.findByIdForUpdate(item.getSkuId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "SKU not found: skuId=" + item.getSkuId()));

            int currentStock = sku.getStockQty();
            int requestedQty = item.getQuantity();

            // 재고 부족 시 거부 항목에 추가
            if (currentStock < requestedQty) {
                log.warn("Insufficient stock detected: skuId={}, currentStock={}, requestedQty={}",
                        item.getSkuId(), currentStock, requestedQty);

                rejectedItems.add(StockRejectedEvent.RejectedItem.builder()
                        .orderItemId(item.getOrderItemId())
                        .productId(item.getProductId())
                        .skuId(item.getSkuId())
                        .productName(item.getProductName())
                        .productCode(item.getProductCode())
                        .requestedQuantity(requestedQty)
                        .availableStock(currentStock)
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())
                        .build());
                continue;
            }

            // 재고 충분: 차감 처리
            int newStock = currentStock - requestedQty;
            sku.setStockQty(newStock);
            productSkuRepository.save(sku);

            // 재고 차감 이력 기록
            productSkuHistoryService.recordDeduction(sku, event.getOrderNumber(), requestedQty, newStock);

            log.info("Stock decreased: skuId={}, productName={}, before={}, after={}, quantity={}",
                    item.getSkuId(), item.getProductName(), currentStock,
                    newStock, requestedQty);
        }

        // 재고 부족 항목이 있으면 stock.rejected 이벤트 발행
        if (!rejectedItems.isEmpty()) {
            publishStockRejectedEvent(event, rejectedItems);
            log.warn("Stock rejected for orderId={}, orderNumber={}, rejectedItemCount={}",
                    event.getOrderId(), event.getOrderNumber(), rejectedItems.size());
            return; // 재고 부족이므로 처리 중단 (ProcessedEvent 저장 안 함)
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
            ProductSku sku = productSkuRepository.findByIdForUpdate(item.getSkuId())
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
            ProductSku sku = productSkuRepository.findByIdForUpdate(item.getSkuId())
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

    @Override
    @Transactional
    public void decreaseStockForExchangeApproved(InventoryDecreaseEvent event) {
        log.info("Starting stock decrease for exchange approved: orderId={}, exchangeId={}, reason={}, itemCount={}",
                event.getOrderId(), event.getExchangeId(), event.getReason(), event.getItems().size());

        String aggregateId = event.getExchangeId().toString();

        // 멱등성 체크: 이미 처리된 교환 승인 이벤트인지 확인
        if (processedEventRepository.existsByEventTypeAndAggregateId(EVENT_TYPE_INVENTORY_DECREASE, aggregateId)) {
            log.warn("Inventory decrease already processed (idempotency check): eventType={}, exchangeId={} - skipping",
                    EVENT_TYPE_INVENTORY_DECREASE, event.getExchangeId());
            return;
        }

        String orderRef = event.getOrderNumber();

        for (InventoryDecreaseEvent.DecreaseItem item : event.getItems()) {
            ProductSku sku = productSkuRepository.findByIdForUpdate(item.getSkuId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "SKU not found: skuId=" + item.getSkuId()));

            int currentStock = sku.getStockQty();
            int requestedQty = item.getQuantity();

            if (currentStock < requestedQty) {
                log.error("Insufficient stock for exchange: skuId={}, currentStock={}, requestedQty={}, exchangeId={}",
                        item.getSkuId(), currentStock, requestedQty, event.getExchangeId());
                throw new IllegalStateException(
                        String.format("Insufficient stock for exchange: skuId=%d, available=%d, requested=%d",
                                item.getSkuId(), currentStock, requestedQty));
            }

            int newStock = currentStock - requestedQty;
            sku.setStockQty(newStock);
            productSkuRepository.save(sku);

            // 재고 차감 이력 기록
            productSkuHistoryService.recordDeduction(sku, orderRef, requestedQty, newStock);

            log.info("Stock decreased for exchange: skuId={}, before={}, after={}, quantity={}, exchangeId={}",
                    item.getSkuId(), currentStock, newStock, requestedQty, event.getExchangeId());
        }

        // 처리 완료 기록 (멱등성 보장)
        ProcessedEvent processedEvent = ProcessedEvent.ofInventoryDecrease(
                event.getExchangeId(),
                event.getReason()
        );
        processedEventRepository.save(processedEvent);

        log.info("Completed stock decrease for exchange: exchangeId={}, orderId={}, itemCount={} - marked as processed",
                event.getExchangeId(), event.getOrderId(), event.getItems().size());
    }

    /**
     * 재고 부족 이벤트 발행 (Outbox 패턴)
     */
    private void publishStockRejectedEvent(OrderCreatedEvent orderEvent, List<StockRejectedEvent.RejectedItem> rejectedItems) {
        try {
            StockRejectedEvent stockRejectedEvent = StockRejectedEvent.builder()
                    .orderId(orderEvent.getOrderId())
                    .orderNumber(orderEvent.getOrderNumber())
                    .rejectionReason("INSUFFICIENT_STOCK")
                    .userId(orderEvent.getUserId())
                    .rejectedItems(rejectedItems)
                    .rejectedAt(LocalDateTime.now())
                    .build();

            String payload = objectMapper.writeValueAsString(stockRejectedEvent);

            Outbox outbox = Outbox.builder()
                    .aggregateType("Order")
                    .aggregateId(orderEvent.getOrderId().toString())
                    .eventType(EventTypeConstants.TOPIC_STOCK_REJECTED)
                    .payload(payload)
                    .build();

            outboxRepository.save(outbox);

            log.info("StockRejectedEvent saved to outbox: orderId={}, orderNumber={}, rejectedItemCount={}",
                    orderEvent.getOrderId(), orderEvent.getOrderNumber(), rejectedItems.size());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize StockRejectedEvent: orderId={}, orderNumber={}",
                    orderEvent.getOrderId(), orderEvent.getOrderNumber(), e);
            throw new RuntimeException("Failed to publish stock.rejected event", e);
        }
    }
}

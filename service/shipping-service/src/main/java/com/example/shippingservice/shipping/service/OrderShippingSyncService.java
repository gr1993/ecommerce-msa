package com.example.shippingservice.shipping.service;

import com.example.shippingservice.client.OrderServiceClient;
import com.example.shippingservice.client.dto.PageResponse;
import com.example.shippingservice.client.dto.ShippingSyncOrderResponse;
import com.example.shippingservice.shipping.entity.OrderShipping;
import com.example.shippingservice.shipping.enums.DeliveryServiceStatus;
import com.example.shippingservice.shipping.enums.ShippingStatus;
import com.example.shippingservice.shipping.repository.OrderShippingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderShippingSyncService {

    private final OrderServiceClient orderServiceClient;
    private final OrderShippingRepository orderShippingRepository;

    private static final int DEFAULT_PAGE_SIZE = 100;

    /**
     * order-service의 배송 대상 주문(PAID 상태)을 조회하여
     * shipping-service의 order_shipping 테이블에 동기화합니다.
     * 이미 존재하는 orderId는 건너뜁니다.
     */
    @Transactional
    public int fullSync() {
        log.info("Starting full sync of shipping orders from order-service");

        int page = 0;
        int totalSynced = 0;
        int totalSkipped = 0;

        while (true) {
            PageResponse<ShippingSyncOrderResponse> response = orderServiceClient.getOrdersForSync(page, DEFAULT_PAGE_SIZE);

            if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
                log.info("No more orders to sync at page {}", page);
                break;
            }

            for (ShippingSyncOrderResponse orderData : response.getContent()) {
                if (orderShippingRepository.existsByOrderId(orderData.getOrderId())) {
                    log.debug("Order already exists in shipping table, skipping: orderId={}", orderData.getOrderId());
                    totalSkipped++;
                    continue;
                }

                OrderShipping orderShipping = toOrderShipping(orderData);
                orderShippingRepository.save(orderShipping);
                totalSynced++;
            }

            log.info("Processed page {} with {} orders (synced: {}, skipped: {})",
                    page, response.getContent().size(), totalSynced, totalSkipped);

            if (response.isLast()) {
                break;
            }

            page++;
        }

        log.info("Full sync completed. Total synced: {}, Total skipped: {}", totalSynced, totalSkipped);
        return totalSynced;
    }

    private OrderShipping toOrderShipping(ShippingSyncOrderResponse orderData) {
        return OrderShipping.builder()
                .orderId(orderData.getOrderId())
                .orderNumber(orderData.getOrderNumber())
                .receiverName(orderData.getReceiverName())
                .receiverPhone(orderData.getReceiverPhone())
                .address(orderData.getAddress())
                .postalCode(orderData.getPostalCode())
                .shippingStatus(ShippingStatus.READY)
                .deliveryServiceStatus(DeliveryServiceStatus.NOT_SENT)
                .build();
    }
}

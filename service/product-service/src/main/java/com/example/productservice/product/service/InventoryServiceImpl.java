package com.example.productservice.product.service;

import com.example.productservice.consumer.event.OrderCreatedEvent;
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

    private final ProductSkuRepository productSkuRepository;

    @Override
    @Transactional
    public void decreaseStock(OrderCreatedEvent event) {
        log.info("Starting stock decrease for orderId={}, orderNumber={}",
                event.getOrderId(), event.getOrderNumber());

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

            sku.setStockQty(currentStock - requestedQty);
            productSkuRepository.save(sku);

            log.info("Stock decreased: skuId={}, productName={}, before={}, after={}, quantity={}",
                    item.getSkuId(), item.getProductName(), currentStock,
                    sku.getStockQty(), requestedQty);
        }

        log.info("Completed stock decrease for orderId={}, itemCount={}",
                event.getOrderId(), event.getOrderItems().size());
    }
}

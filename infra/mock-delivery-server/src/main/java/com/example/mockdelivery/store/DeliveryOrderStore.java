package com.example.mockdelivery.store;

import com.example.mockdelivery.entity.DeliveryOrder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class DeliveryOrderStore {

    private final Map<String, DeliveryOrder> orders = new ConcurrentHashMap<>();
    private final AtomicLong trackingNumberSequence = new AtomicLong(100000000000L);

    public String generateTrackingNumber() {
        return String.valueOf(trackingNumberSequence.getAndIncrement());
    }

    public void save(DeliveryOrder order) {
        orders.put(order.getTrackingNumber(), order);
    }

    public Optional<DeliveryOrder> findByTrackingNumber(String trackingNumber) {
        return Optional.ofNullable(orders.get(trackingNumber));
    }

    public boolean exists(String trackingNumber) {
        return orders.containsKey(trackingNumber);
    }

    public boolean remove(String trackingNumber) {
        return orders.remove(trackingNumber) != null;
    }

    public void clear() {
        orders.clear();
    }

    public int size() {
        return orders.size();
    }
}

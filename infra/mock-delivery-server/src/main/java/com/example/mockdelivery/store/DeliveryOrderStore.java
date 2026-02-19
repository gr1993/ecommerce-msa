package com.example.mockdelivery.store;

import com.example.mockdelivery.entity.DeliveryOrder;
import com.example.mockdelivery.entity.DeliveryStatus;
import org.springframework.stereotype.Component;

import java.util.List;
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

    public List<DeliveryOrder> findByStatus(DeliveryStatus status) {
        return orders.values().stream()
                .filter(order -> order.getStatus() == status)
                .toList();
    }

    public List<DeliveryOrder> findReadyForProgress(long secondsThreshold) {
        return orders.values().stream()
                .filter(order -> order.isReadyForNextStatus(secondsThreshold))
                .toList();
    }
}

package com.example.mockdelivery.scheduler;

import com.example.mockdelivery.entity.DeliveryOrder;
import com.example.mockdelivery.entity.DeliveryStatus;
import com.example.mockdelivery.store.DeliveryOrderStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryStatusScheduler {

    private static final long STATUS_CHANGE_INTERVAL_SECONDS = 10;

    private final DeliveryOrderStore deliveryOrderStore;

    private static final Map<DeliveryStatus, String> LOCATION_BY_STATUS = Map.of(
            DeliveryStatus.ACCEPTED, "○○허브",
            DeliveryStatus.PICKED_UP, "○○허브",
            DeliveryStatus.IN_TRANSIT, "△△허브",
            DeliveryStatus.AT_DESTINATION, "배송지 인근"
    );

    @Scheduled(fixedRate = 1000)
    public void updateDeliveryStatus() {
        List<DeliveryOrder> readyOrders = deliveryOrderStore.findReadyForProgress(STATUS_CHANGE_INTERVAL_SECONDS);

        for (DeliveryOrder order : readyOrders) {
            DeliveryStatus currentStatus = order.getStatus();
            String location = getLocationForStatus(order, currentStatus);

            order.progressStatus(location);
            deliveryOrderStore.save(order);

            log.info("송장 {} 상태 변경: {} -> {}",
                    order.getTrackingNumber(),
                    currentStatus.name(),
                    order.getStatus().name());
        }
    }

    private String getLocationForStatus(DeliveryOrder order, DeliveryStatus status) {
        if (status == DeliveryStatus.OUT_FOR_DELIVERY) {
            return order.getReceiverAddress();
        }
        return LOCATION_BY_STATUS.getOrDefault(status, "물류센터");
    }
}

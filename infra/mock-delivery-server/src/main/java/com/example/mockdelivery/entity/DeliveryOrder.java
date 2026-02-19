package com.example.mockdelivery.entity;

import com.example.mockdelivery.dto.tracking.TrackingDetail;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DeliveryOrder {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String trackingNumber;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String goodsName;
    private Integer goodsQty;
    private String senderName;
    private String orderNumber;
    private DeliveryStatus status;
    private List<TrackingDetail> trackingDetails;
    private LocalDateTime createdAt;
    private LocalDateTime lastStatusChangedAt;

    public DeliveryOrder() {
        this.trackingDetails = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.lastStatusChangedAt = LocalDateTime.now();
        this.status = DeliveryStatus.ACCEPTED;
        this.senderName = "샘플 판매자";
        this.orderNumber = "ORD-" + String.format("%04d", (int) (Math.random() * 10000));
    }

    public void addTrackingDetail(String where, String remark, String kind) {
        TrackingDetail detail = new TrackingDetail(
                LocalDateTime.now().format(FORMATTER),
                where,
                remark,
                kind
        );
        this.trackingDetails.add(detail);
    }

    public void progressStatus(String location) {
        if (!status.canProgress()) {
            return;
        }

        DeliveryStatus nextStatus = status.next();
        if (nextStatus == null) {
            return;
        }

        this.status = nextStatus;
        this.lastStatusChangedAt = LocalDateTime.now();

        String remark = nextStatus.getDescription();
        String kind = nextStatus.name();

        addTrackingDetail(location, remark, kind);
    }

    public TrackingDetail getLastDetail() {
        if (trackingDetails.isEmpty()) {
            return new TrackingDetail(
                    createdAt.format(FORMATTER),
                    "접수",
                    status.getDescription(),
                    status.name()
            );
        }
        return trackingDetails.get(trackingDetails.size() - 1);
    }

    public boolean isReadyForNextStatus(long secondsThreshold) {
        if (!status.canProgress()) {
            return false;
        }
        return lastStatusChangedAt.plusSeconds(secondsThreshold).isBefore(LocalDateTime.now());
    }
}

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
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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

    public DeliveryOrder() {
        this.trackingDetails = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.status = DeliveryStatus.PREPARING;
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

    public TrackingDetail getLastDetail() {
        if (trackingDetails.isEmpty()) {
            return new TrackingDetail(
                    createdAt.format(FORMATTER),
                    "접수",
                    "배송 준비중",
                    "접수"
            );
        }
        return trackingDetails.get(trackingDetails.size() - 1);
    }
}

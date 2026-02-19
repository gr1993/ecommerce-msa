package com.example.mockdelivery.controller;

import com.example.mockdelivery.config.ApiKeyProperties;
import com.example.mockdelivery.dto.tracking.TrackingDetail;
import com.example.mockdelivery.dto.tracking.TrackingInfoRequest;
import com.example.mockdelivery.dto.tracking.TrackingInfoResponse;
import com.example.mockdelivery.entity.DeliveryOrder;
import com.example.mockdelivery.store.DeliveryOrderStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Tracking", description = "배송 조회 API")
public class TrackingController {

    private final ApiKeyProperties apiKeyProperties;
    private final DeliveryOrderStore deliveryOrderStore;

    public TrackingController(ApiKeyProperties apiKeyProperties, DeliveryOrderStore deliveryOrderStore) {
        this.apiKeyProperties = apiKeyProperties;
        this.deliveryOrderStore = deliveryOrderStore;
    }

    @Operation(summary = "배송 조회", description = "운송장 번호로 배송 상태를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = TrackingInfoResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "invoiceNo": "1234567890",
                                      "orderNumber": "ORD-0001",
                                      "itemName": "애플망고 3kg",
                                      "receiverName": "홍길동",
                                      "receiverAddr": "서울시 강남구 테헤란로 123",
                                      "senderName": "샘플 판매자",
                                      "lastDetail": {
                                        "timeString": "2026-02-19 10:00",
                                        "where": "서울 강남",
                                        "remark": "배송 준비중",
                                        "kind": "배송중"
                                      },
                                      "trackingDetails": [
                                        {
                                          "timeString": "2026-02-19 10:00",
                                          "where": "서울 강남",
                                          "remark": "배송 준비중",
                                          "kind": "배송중"
                                        },
                                        {
                                          "timeString": "2026-02-20 14:00",
                                          "where": "서울 강남",
                                          "remark": "배송 완료",
                                          "kind": "배송완료"
                                        }
                                      ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(examples = @ExampleObject(value = """
                            {"error": "Invalid smart delivery API key"}
                            """))),
            @ApiResponse(responseCode = "404", description = "송장 정보 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {"error": "Tracking information not found"}
                            """)))
    })
    @PostMapping("/trackingInfo")
    public ResponseEntity<?> getTrackingInfo(@RequestBody TrackingInfoRequest request) {
        if (!isValidSmartDeliveryApiKey(request.getTKey())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid smart delivery API key"));
        }

        String trackingNumber = request.getTInvoice();
        if (trackingNumber == null || trackingNumber.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Tracking number is required"));
        }

        var orderOpt = deliveryOrderStore.findByTrackingNumber(trackingNumber);

        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Tracking information not found"));
        }

        DeliveryOrder order = orderOpt.get();

        List<TrackingDetail> trackingDetails = order.getTrackingDetails();
        TrackingDetail lastDetail = order.getLastDetail();

        TrackingInfoResponse response = new TrackingInfoResponse(
                order.getTrackingNumber(),
                order.getOrderNumber(),
                order.getGoodsName(),
                order.getReceiverName(),
                order.getReceiverAddress(),
                order.getSenderName(),
                lastDetail,
                trackingDetails
        );

        return ResponseEntity.ok(response);
    }

    private boolean isValidSmartDeliveryApiKey(String key) {
        return apiKeyProperties.getSmartDelivery().getApiKey().equals(key);
    }
}

package com.example.mockdelivery.controller;

import com.example.mockdelivery.config.ApiKeyProperties;
import com.example.mockdelivery.dto.courier.*;
import com.example.mockdelivery.entity.DeliveryOrder;
import com.example.mockdelivery.entity.DeliveryStatus;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/courier/orders")
@Tag(name = "Courier", description = "송장 발급/취소 API")
public class CourierController {

    private final ApiKeyProperties apiKeyProperties;
    private final DeliveryOrderStore deliveryOrderStore;

    public CourierController(ApiKeyProperties apiKeyProperties, DeliveryOrderStore deliveryOrderStore) {
        this.apiKeyProperties = apiKeyProperties;
        this.deliveryOrderStore = deliveryOrderStore;
    }

    @Operation(summary = "송장 일괄 발급", description = "여러 건의 송장을 일괄 발급합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "발급 성공",
                    content = @Content(schema = @Schema(implementation = BulkUploadResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": true,
                                      "summary": {
                                        "total": 2,
                                        "success": 2,
                                        "failed": 0
                                      },
                                      "results": [
                                        {
                                          "index": 0,
                                          "isSuccess": true,
                                          "trackingNumber": "123456789012"
                                        },
                                        {
                                          "index": 1,
                                          "isSuccess": true,
                                          "trackingNumber": "210987654321"
                                        }
                                      ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(examples = @ExampleObject(value = """
                            {"error": "Invalid courier account key"}
                            """)))
    })
    @PostMapping("/bulk-upload")
    public ResponseEntity<?> bulkUpload(@RequestBody BulkUploadRequest request) {
        if (!isValidCourierAccountKey(request.getCourierAccountKey())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid courier account key"));
        }

        List<BulkUploadItem> items = request.getItems();
        if (items == null || items.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No items provided"));
        }

        List<BulkUploadResult> results = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;

        for (int i = 0; i < items.size(); i++) {
            BulkUploadItem item = items.get(i);

            try {
                String trackingNumber = deliveryOrderStore.generateTrackingNumber();

                DeliveryOrder order = new DeliveryOrder();
                order.setTrackingNumber(trackingNumber);
                order.setReceiverName(item.getReceiverName());
                order.setReceiverPhone(item.getReceiverPhone1());
                order.setReceiverAddress(item.getReceiverAddress());
                order.setGoodsName(item.getGoodsName());
                order.setGoodsQty(item.getGoodsQty());
                order.addTrackingDetail("접수", "접수완료", DeliveryStatus.ACCEPTED.name());

                deliveryOrderStore.save(order);

                results.add(new BulkUploadResult(i, true, trackingNumber));
                successCount++;
            } catch (Exception e) {
                results.add(new BulkUploadResult(i, false, null));
                failedCount++;
            }
        }

        BulkUploadSummary summary = new BulkUploadSummary(items.size(), successCount, failedCount);
        BulkUploadResponse response = new BulkUploadResponse(failedCount == 0, summary, results);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "송장 일괄 취소", description = "여러 건의 송장을 일괄 취소합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "취소 처리 완료",
                    content = @Content(schema = @Schema(implementation = BulkCancelResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": true,
                                      "summary": {
                                        "total": 2,
                                        "success": 2,
                                        "failed": 0
                                      },
                                      "results": [
                                        {
                                          "index": 0,
                                          "isSuccess": true,
                                          "trackingNumber": "123456789012",
                                          "message": "취소 완료"
                                        },
                                        {
                                          "index": 1,
                                          "isSuccess": true,
                                          "trackingNumber": "210987654321",
                                          "message": "취소 완료"
                                        }
                                      ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(examples = @ExampleObject(value = """
                            {"error": "Invalid courier account key"}
                            """)))
    })
    @PostMapping("/bulk-cancel")
    public ResponseEntity<?> bulkCancel(@RequestBody BulkCancelRequest request) {
        if (!isValidCourierAccountKey(request.getCourierAccountKey())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid courier account key"));
        }

        List<BulkCancelItem> items = request.getItems();
        if (items == null || items.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No items provided"));
        }

        List<BulkCancelResult> results = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;

        for (int i = 0; i < items.size(); i++) {
            BulkCancelItem item = items.get(i);
            String trackingNumber = item.getTrackingNumber();

            var orderOpt = deliveryOrderStore.findByTrackingNumber(trackingNumber);

            if (orderOpt.isEmpty()) {
                results.add(new BulkCancelResult(i, false, trackingNumber, "송장을 찾을 수 없습니다"));
                failedCount++;
            } else {
                DeliveryOrder order = orderOpt.get();

                if (order.getStatus() == DeliveryStatus.DELIVERED) {
                    results.add(new BulkCancelResult(i, false, trackingNumber, "이미 배송 완료된 송장은 취소할 수 없습니다"));
                    failedCount++;
                } else if (order.getStatus() == DeliveryStatus.CANCELLED) {
                    results.add(new BulkCancelResult(i, false, trackingNumber, "이미 취소된 송장입니다"));
                    failedCount++;
                } else {
                    order.setStatus(DeliveryStatus.CANCELLED);
                    order.addTrackingDetail("시스템", "취소됨", DeliveryStatus.CANCELLED.name());
                    deliveryOrderStore.save(order);

                    results.add(new BulkCancelResult(i, true, trackingNumber, "취소 완료"));
                    successCount++;
                }
            }
        }

        BulkCancelSummary summary = new BulkCancelSummary(items.size(), successCount, failedCount);
        BulkCancelResponse response = new BulkCancelResponse(failedCount == 0, summary, results);

        return ResponseEntity.ok(response);
    }

    private boolean isValidCourierAccountKey(String key) {
        return apiKeyProperties.getCourier().getAccountKey().equals(key);
    }
}

package com.example.shippingservice.shipping.service;

import com.example.shippingservice.client.MockDeliveryClient;
import com.example.shippingservice.client.dto.*;
import com.example.shippingservice.shipping.entity.OrderShipping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Mock Delivery Server(외부 택배사 API) 연동 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockDeliveryService {

    private final MockDeliveryClient mockDeliveryClient;

    @Value("${mock-delivery-server.courier-account-key}")
    private String courierAccountKey;

    @Value("${mock-delivery-server.tracking-api-key}")
    private String trackingApiKey;

    @Value("${mock-delivery-server.courier-code:04}")
    private String defaultCourierCode;

    /**
     * 배송 조회
     *
     * @param trackingNumber 운송장 번호
     * @return 배송 조회 응답
     */
    public TrackingInfoResponse getTrackingInfo(String trackingNumber) {
        return getTrackingInfo(defaultCourierCode, trackingNumber);
    }

    /**
     * 배송 조회 (택배사 코드 지정)
     *
     * @param courierCode    택배사 코드
     * @param trackingNumber 운송장 번호
     * @return 배송 조회 응답
     */
    public TrackingInfoResponse getTrackingInfo(String courierCode, String trackingNumber) {
        log.info("배송 조회 요청 - courierCode: {}, trackingNumber: {}", courierCode, trackingNumber);

        TrackingInfoRequest request = TrackingInfoRequest.builder()
                .code(courierCode)
                .invoice(trackingNumber)
                .key(trackingApiKey)
                .build();

        TrackingInfoResponse response = mockDeliveryClient.getTrackingInfo(request);

        log.info("배송 조회 응답 - invoiceNo: {}, lastStatus: {}",
                response.getInvoiceNo(),
                response.getLastDetail() != null ? response.getLastDetail().getKind() : "N/A");

        return response;
    }

    /**
     * 송장 일괄 발급
     *
     * @param items 발급할 송장 항목 목록
     * @return 송장 발급 응답
     */
    public BulkUploadResponse bulkUpload(List<BulkUploadItem> items) {
        log.info("송장 일괄 발급 요청 - itemCount: {}", items.size());

        BulkUploadRequest request = BulkUploadRequest.builder()
                .courierAccountKey(courierAccountKey)
                .items(items)
                .build();

        BulkUploadResponse response = mockDeliveryClient.bulkUpload(request);

        log.info("송장 일괄 발급 응답 - isSuccess: {}, total: {}, success: {}, failed: {}",
                response.getIsSuccess(),
                response.getSummary().getTotal(),
                response.getSummary().getSuccess(),
                response.getSummary().getFailed());

        return response;
    }

    /**
     * 단일 송장 발급
     *
     * @param orderShipping 배송 정보
     * @return 발급된 운송장 번호 (실패 시 null)
     */
    public String issueSingleTrackingNumber(OrderShipping orderShipping) {
        BulkUploadItem item = BulkUploadItem.builder()
                .receiverName(orderShipping.getReceiverName())
                .receiverPhone1(orderShipping.getReceiverPhone())
                .receiverAddress(orderShipping.getAddress())
                .goodsName("주문상품")
                .goodsQty(1)
                .build();

        BulkUploadResponse response = bulkUpload(List.of(item));

        if (response.getIsSuccess() && !response.getResults().isEmpty()) {
            BulkUploadResult result = response.getResults().get(0);
            if (result.getIsSuccess()) {
                return result.getTrackingNumber();
            }
        }

        log.warn("송장 발급 실패 - orderId: {}", orderShipping.getOrderId());
        return null;
    }

    /**
     * 송장 일괄 취소
     *
     * @param trackingNumbers 취소할 운송장 번호 목록
     * @return 송장 취소 응답
     */
    public BulkCancelResponse bulkCancel(List<String> trackingNumbers) {
        log.info("송장 일괄 취소 요청 - trackingNumberCount: {}", trackingNumbers.size());

        List<BulkCancelItem> items = trackingNumbers.stream()
                .map(tn -> BulkCancelItem.builder().trackingNumber(tn).build())
                .toList();

        BulkCancelRequest request = BulkCancelRequest.builder()
                .courierAccountKey(courierAccountKey)
                .items(items)
                .build();

        BulkCancelResponse response = mockDeliveryClient.bulkCancel(request);

        log.info("송장 일괄 취소 응답 - isSuccess: {}, total: {}, success: {}, failed: {}",
                response.getIsSuccess(),
                response.getSummary().getTotal(),
                response.getSummary().getSuccess(),
                response.getSummary().getFailed());

        return response;
    }

    /**
     * 단일 송장 취소
     *
     * @param trackingNumber 취소할 운송장 번호
     * @return 취소 성공 여부
     */
    public boolean cancelSingleTrackingNumber(String trackingNumber) {
        BulkCancelResponse response = bulkCancel(List.of(trackingNumber));

        if (response.getIsSuccess() && !response.getResults().isEmpty()) {
            return response.getResults().get(0).getIsSuccess();
        }

        return false;
    }
}

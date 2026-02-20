package com.example.shippingservice.client;

import com.example.shippingservice.client.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Mock Delivery Server(외부 택배사 API) 연동 클라이언트
 */
@FeignClient(name = "mock-delivery-client", url = "${mock-delivery-server.url}")
public interface MockDeliveryClient {

    /**
     * 배송 조회
     * 운송장 번호로 배송 상태를 조회합니다.
     *
     * @param request 배송 조회 요청 (택배사 코드, 운송장 번호, API 키)
     * @return 배송 조회 응답 (배송 이력 포함)
     */
    @PostMapping("/api/v1/trackingInfo")
    TrackingInfoResponse getTrackingInfo(@RequestBody TrackingInfoRequest request);

    /**
     * 송장 일괄 발급
     * 여러 건의 송장을 일괄 발급합니다.
     *
     * @param request 송장 발급 요청 (택배사 계정 키, 발급 항목 목록)
     * @return 송장 발급 응답 (발급 결과 목록)
     */
    @PostMapping("/api/v1/courier/orders/bulk-upload")
    BulkUploadResponse bulkUpload(@RequestBody BulkUploadRequest request);

    /**
     * 송장 일괄 취소
     * 여러 건의 송장을 일괄 취소합니다.
     *
     * @param request 송장 취소 요청 (택배사 계정 키, 취소 항목 목록)
     * @return 송장 취소 응답 (취소 결과 목록)
     */
    @PostMapping("/api/v1/courier/orders/bulk-cancel")
    BulkCancelResponse bulkCancel(@RequestBody BulkCancelRequest request);
}

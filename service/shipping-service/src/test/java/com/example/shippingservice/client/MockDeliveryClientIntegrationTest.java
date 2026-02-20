package com.example.shippingservice.client;

import com.example.shippingservice.client.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mock Delivery API 연동 통합 테스트
 *
 * 실행 전 mock-delivery-server가 localhost:9082에서 실행 중이어야 합니다.
 */
@SpringBootTest
@ActiveProfiles("local")
class MockDeliveryClientIntegrationTest {

    @Autowired
    private MockDeliveryClient mockDeliveryClient;

    @Test
    void 송장_일괄_발급_테스트() {
        // given
        BulkUploadItem item = BulkUploadItem.builder()
                .receiverName("홍길동")
                .receiverPhone1("01012345678")
                .receiverAddress("서울시 강남구 테헤란로 123")
                .goodsName("테스트 상품")
                .goodsQty(1)
                .build();

        BulkUploadRequest request = BulkUploadRequest.builder()
                .courierAccountKey("CJ_COURIER_ACCOUNT_KEY_2024")
                .items(List.of(item))
                .build();

        // when
        BulkUploadResponse response = mockDeliveryClient.bulkUpload(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getIsSuccess()).isTrue();
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getTrackingNumber()).isNotBlank();

        System.out.println("발급된 운송장 번호: " + response.getResults().get(0).getTrackingNumber());
    }

    @Test
    void 배송_조회_테스트() {
        // given
        TrackingInfoRequest request = TrackingInfoRequest.builder()
                .code("04")
                .invoice("100000000000")
                .key("SMART_DELIVERY_API_KEY_2024")
                .build();

        // when
        TrackingInfoResponse response = mockDeliveryClient.getTrackingInfo(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getInvoiceNo()).isEqualTo("100000000000");
        assertThat(response.getTrackingDetails()).isNotEmpty();

        System.out.println("운송장 번호: " + response.getInvoiceNo());
        System.out.println("최근 상태: " + response.getLastDetail().getKind());
        System.out.println("배송 이력 수: " + response.getTrackingDetails().size());
    }

    @Test
    void 송장_일괄_취소_테스트() {
        // given
        BulkCancelItem item = BulkCancelItem.builder()
                .trackingNumber("100000000000")
                .build();

        BulkCancelRequest request = BulkCancelRequest.builder()
                .courierAccountKey("CJ_COURIER_ACCOUNT_KEY_2024")
                .items(List.of(item))
                .build();

        // when
        BulkCancelResponse response = mockDeliveryClient.bulkCancel(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getIsSuccess()).isTrue();
        assertThat(response.getResults()).hasSize(1);

        System.out.println("취소 결과: " + response.getResults().get(0).getMessage());
    }
}

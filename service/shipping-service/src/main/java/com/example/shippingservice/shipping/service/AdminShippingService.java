package com.example.shippingservice.shipping.service;

import com.example.shippingservice.client.dto.PageResponse;
import com.example.shippingservice.shipping.dto.request.RegisterTrackingRequest;
import com.example.shippingservice.shipping.dto.response.AdminShippingResponse;
import org.springframework.data.domain.Pageable;

public interface AdminShippingService {

    PageResponse<AdminShippingResponse> getShippings(String shippingStatus, String trackingNumber, Pageable pageable);

    AdminShippingResponse registerTracking(Long shippingId, RegisterTrackingRequest request);
}

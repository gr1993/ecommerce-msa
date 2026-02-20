package com.example.shippingservice.client;

import com.example.shippingservice.client.dto.PageResponse;
import com.example.shippingservice.client.dto.ShippingSyncOrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service", url = "${order-service.url}")
public interface OrderServiceClient {

    @GetMapping("/api/internal/orders/sync")
    PageResponse<ShippingSyncOrderResponse> getOrdersForSync(
            @RequestParam("page") int page,
            @RequestParam("size") int size
    );
}

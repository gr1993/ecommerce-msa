package com.example.orderservice.service;

import com.example.orderservice.client.ProductServiceClient;
import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderDelivery;
import com.example.orderservice.domain.entity.OrderItem;
import com.example.orderservice.domain.entity.OrderStatus;
import com.example.orderservice.dto.request.DeliveryInfoRequest;
import com.example.orderservice.dto.request.OrderCreateRequest;
import com.example.orderservice.dto.request.OrderItemRequest;
import com.example.orderservice.dto.response.OrderResponse;
import com.example.orderservice.client.dto.ProductDetailResponse;
import com.example.orderservice.client.dto.ProductDetailResponse.OptionValueResponse;
import com.example.orderservice.client.dto.ProductDetailResponse.SkuResponse;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;

    @Override
    @Transactional
    public OrderResponse createOrder(Long userId, OrderCreateRequest request) {
        BigDecimal totalProductAmount = BigDecimal.ZERO;

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .userId(userId)
                .orderStatus(OrderStatus.CREATED)
                .totalProductAmount(BigDecimal.ZERO)
                .totalDiscountAmount(BigDecimal.ZERO)
                .totalPaymentAmount(BigDecimal.ZERO)
                .build();

        for (OrderItemRequest itemRequest : request.getOrderItems()) {
            ProductDetailResponse product = productServiceClient.getProductDetail(itemRequest.getProductId());

            SkuResponse sku = findSkuById(product.getSkus(), itemRequest.getSkuId());
            String productNameWithOptions = buildProductNameWithOptions(product, sku);
            BigDecimal unitPrice = BigDecimal.valueOf(sku.getPrice());
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .skuId(itemRequest.getSkuId())
                    .productName(productNameWithOptions)
                    .productCode(product.getProductCode())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .totalPrice(totalPrice)
                    .build();
            order.addOrderItem(orderItem);

            totalProductAmount = totalProductAmount.add(totalPrice);
        }

        order.updateTotalAmounts(totalProductAmount, BigDecimal.ZERO);

        DeliveryInfoRequest deliveryInfo = request.getDeliveryInfo();
        OrderDelivery orderDelivery = OrderDelivery.builder()
                .receiverName(deliveryInfo.getReceiverName())
                .receiverPhone(deliveryInfo.getReceiverPhone())
                .zipcode(deliveryInfo.getZipcode())
                .address(deliveryInfo.getAddress())
                .addressDetail(deliveryInfo.getAddressDetail())
                .deliveryMemo(deliveryInfo.getDeliveryMemo())
                .build();
        order.setOrderDelivery(orderDelivery);

        Order savedOrder = orderRepository.save(order);
        return OrderResponse.from(savedOrder);
    }

    private SkuResponse findSkuById(List<SkuResponse> skus, Long skuId) {
        return skus.stream()
                .filter(sku -> sku.getId().equals(skuId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("SKU를 찾을 수 없습니다: " + skuId));
    }

    private String buildProductNameWithOptions(ProductDetailResponse product, SkuResponse sku) {
        if (sku.getOptionValueIds() == null || sku.getOptionValueIds().isEmpty()) {
            return product.getProductName();
        }

        Map<Long, String> optionValueNameMap = product.getOptionGroups().stream()
                .flatMap(group -> group.getOptionValues().stream())
                .collect(Collectors.toMap(OptionValueResponse::getId, OptionValueResponse::getOptionValueName));

        List<String> optionNames = sku.getOptionValueIds().stream()
                .map(optionValueNameMap::get)
                .filter(name -> name != null)
                .toList();

        if (optionNames.isEmpty()) {
            return product.getProductName();
        }

        return product.getProductName() + "(" + String.join(", ", optionNames) + ")";
    }

    private String generateOrderNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uniquePart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD-" + datePart + "-" + uniquePart;
    }
}

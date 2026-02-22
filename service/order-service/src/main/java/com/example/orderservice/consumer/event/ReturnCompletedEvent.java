package com.example.orderservice.consumer.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnCompletedEvent {

    private Long returnId;
    private Long orderId;
    private Long userId;
    private String reason;
    private LocalDateTime completedAt;
}

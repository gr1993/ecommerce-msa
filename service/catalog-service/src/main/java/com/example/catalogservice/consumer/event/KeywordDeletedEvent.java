package com.example.catalogservice.consumer.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeywordDeletedEvent {

    private Long keywordId;
    private Long productId;
    private String keyword;
    private LocalDateTime deletedAt;
}

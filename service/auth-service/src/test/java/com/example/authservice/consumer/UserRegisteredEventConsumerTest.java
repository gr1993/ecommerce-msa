package com.example.authservice.consumer;

import com.example.authservice.domain.event.UserRegisteredEvent;
import com.example.authservice.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRegisteredEventConsumer 테스트")
class UserRegisteredEventConsumerTest {

    @Mock
    private AuthService authService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private UserRegisteredEventConsumer consumer;

    private String validJsonMessage;
    private UserRegisteredEvent validEvent;

    @BeforeEach
    void setUp() {
        validEvent = new UserRegisteredEvent(
                1L,
                "test@example.com",
                "홍길동",
                "010-1234-5678",
                "$2a$10$hashedPassword",
                LocalDateTime.now()
        );

        validJsonMessage = "{\"userId\":1,\"email\":\"test@example.com\",\"name\":\"홍길동\",\"phone\":\"010-1234-5678\",\"hashedPassword\":\"$2a$10$hashedPassword\",\"registeredAt\":\"2024-01-01T10:00:00\"}";
    }

    @Test
    @DisplayName("메시지 수신 및 처리 성공")
    void consume_Success() throws Exception {
        // given
        given(objectMapper.readValue(validJsonMessage, UserRegisteredEvent.class))
                .willReturn(validEvent);
        doNothing().when(authService).registerUserFromEvent(any(UserRegisteredEvent.class));

        // when
        consumer.consume(validJsonMessage);

        // then
        verify(objectMapper, times(1)).readValue(validJsonMessage, UserRegisteredEvent.class);
        verify(authService, times(1)).registerUserFromEvent(any(UserRegisteredEvent.class));
    }

    @Test
    @DisplayName("JSON 역직렬화 실패 - 예외 처리")
    void consume_JsonProcessingException() throws Exception {
        // given
        String invalidJsonMessage = "{invalid json}";
        given(objectMapper.readValue(invalidJsonMessage, UserRegisteredEvent.class))
                .willThrow(new RuntimeException("JSON parsing error"));

        // when
        consumer.consume(invalidJsonMessage);

        // then
        verify(objectMapper, times(1)).readValue(invalidJsonMessage, UserRegisteredEvent.class);
        verify(authService, never()).registerUserFromEvent(any(UserRegisteredEvent.class));
    }

    @Test
    @DisplayName("서비스 처리 중 예외 발생")
    void consume_ServiceException() throws Exception {
        // given
        given(objectMapper.readValue(validJsonMessage, UserRegisteredEvent.class))
                .willReturn(validEvent);
        doThrow(new RuntimeException("Service error"))
                .when(authService).registerUserFromEvent(any(UserRegisteredEvent.class));

        // when
        consumer.consume(validJsonMessage);

        // then
        verify(objectMapper, times(1)).readValue(validJsonMessage, UserRegisteredEvent.class);
        verify(authService, times(1)).registerUserFromEvent(any(UserRegisteredEvent.class));
    }
}

package com.example.authservice.consumer;

import com.example.authservice.domain.entity.FailedMessage;
import com.example.authservice.domain.event.UserRegisteredEvent;
import com.example.authservice.repository.FailedMessageRepository;
import com.example.authservice.service.EventProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRegisteredEventConsumer 테스트")
class UserRegisteredEventConsumerTest {

    @Mock
    private EventProcessingService eventProcessingService;

    @Mock
    private FailedMessageRepository failedMessageRepository;

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
        doNothing().when(eventProcessingService)
                .processUserRegisteredEvent(any(UserRegisteredEvent.class), anyString());

        // when
        consumer.consume(validJsonMessage, "user-registered", 0L);

        // then
        verify(objectMapper, times(1)).readValue(validJsonMessage, UserRegisteredEvent.class);
        verify(eventProcessingService, times(1))
                .processUserRegisteredEvent(validEvent, validJsonMessage);
    }

    @Test
    @DisplayName("JSON 역직렬화 실패 - 예외 전파")
    void consume_JsonProcessingException() throws Exception {
        // given
        String invalidJsonMessage = "{invalid json}";
        given(objectMapper.readValue(invalidJsonMessage, UserRegisteredEvent.class))
                .willThrow(new RuntimeException("JSON parsing error"));

        // when & then
        assertThatThrownBy(() ->
                consumer.consume(invalidJsonMessage, "user-registered", 0L)
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이벤트 처리 실패");

        verify(objectMapper, times(1)).readValue(invalidJsonMessage, UserRegisteredEvent.class);
        verify(eventProcessingService, never())
                .processUserRegisteredEvent(any(UserRegisteredEvent.class), anyString());
    }

    @Test
    @DisplayName("서비스 처리 중 예외 발생 - 예외 전파 (재시도 트리거)")
    void consume_ServiceException() throws Exception {
        // given
        given(objectMapper.readValue(validJsonMessage, UserRegisteredEvent.class))
                .willReturn(validEvent);
        doThrow(new IllegalStateException("DB 연결 실패"))
                .when(eventProcessingService)
                .processUserRegisteredEvent(any(UserRegisteredEvent.class), anyString());

        // when & then
        assertThatThrownBy(() ->
                consumer.consume(validJsonMessage, "user-registered", 0L)
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이벤트 처리 실패");

        verify(objectMapper, times(1)).readValue(validJsonMessage, UserRegisteredEvent.class);
        verify(eventProcessingService, times(1))
                .processUserRegisteredEvent(validEvent, validJsonMessage);
    }

    @Test
    @DisplayName("DLQ 핸들러 - 정상 메시지")
    void handleDlt_ValidMessage() throws Exception {
        // given
        given(objectMapper.readValue(validJsonMessage, UserRegisteredEvent.class))
                .willReturn(validEvent);
        given(failedMessageRepository.save(any(FailedMessage.class)))
                .willAnswer(invocation -> {
                    FailedMessage fm = invocation.getArgument(0);
                    // Simulate ID assignment
                    return fm;
                });

        // when
        consumer.handleDlt(
                validJsonMessage,
                "user-registered-dlt",
                0L,
                0,  // partition
                "처리 실패",
                "stacktrace..."
        );

        // then
        verify(objectMapper, times(1)).readValue(validJsonMessage, UserRegisteredEvent.class);
        verify(failedMessageRepository, times(1)).save(any(FailedMessage.class));
    }

    @Test
    @DisplayName("DLQ 핸들러 - 파싱 실패 메시지")
    void handleDlt_InvalidMessage() throws Exception {
        // given
        String invalidMessage = "{invalid}";
        given(objectMapper.readValue(invalidMessage, UserRegisteredEvent.class))
                .willThrow(new RuntimeException("JSON parsing error"));
        given(failedMessageRepository.save(any(FailedMessage.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        consumer.handleDlt(
                invalidMessage,
                "user-registered-dlt",
                0L,
                0,  // partition
                "처리 실패",
                "stacktrace..."
        );

        // then
        verify(objectMapper, times(1)).readValue(invalidMessage, UserRegisteredEvent.class);
        verify(failedMessageRepository, times(1)).save(any(FailedMessage.class));
    }
}

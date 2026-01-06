package com.example.authservice.service;

import com.example.authservice.domain.entity.AuthUser;
import com.example.authservice.domain.entity.ProcessedEvent;
import com.example.authservice.domain.event.UserRegisteredEvent;
import com.example.authservice.repository.AuthUserRepository;
import com.example.authservice.repository.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventProcessingService 테스트")
class EventProcessingServiceTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private EventProcessingService eventProcessingService;

    @Test
    @DisplayName("정상적인 회원 등록 이벤트 처리")
    void processUserRegisteredEvent_Success() {
        // given
        UserRegisteredEvent event = new UserRegisteredEvent(
            1L,
            "test@example.com",
            "테스트사용자",
            "010-1234-5678",
            "$2a$10$hashedPassword",
            LocalDateTime.now()
        );
        String eventPayload = "{\"userId\":1,\"email\":\"test@example.com\"}";

        given(processedEventRepository.existsByEventTypeAndEventKey(anyString(), anyString()))
            .willReturn(false);
        given(authUserRepository.existsByEmail(anyString()))
            .willReturn(false);
        given(authUserRepository.save(any(AuthUser.class)))
            .willAnswer(invocation -> {
                AuthUser user = invocation.getArgument(0);
                return user;
            });
        given(processedEventRepository.save(any(ProcessedEvent.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        // when
        eventProcessingService.processUserRegisteredEvent(event, eventPayload);

        // then
        then(processedEventRepository).should().existsByEventTypeAndEventKey("USER_REGISTERED", "1");
        then(authUserRepository).should().existsByEmail("test@example.com");
        then(authUserRepository).should().save(any(AuthUser.class));
        then(processedEventRepository).should().save(argThat(processed ->
            processed.getEventType().equals("USER_REGISTERED") &&
            processed.getEventKey().equals("1") &&
            processed.getStatus() == ProcessedEvent.ProcessStatus.SUCCESS
        ));
    }

    @Test
    @DisplayName("이미 처리된 이벤트 - 중복 처리 방지")
    void processUserRegisteredEvent_AlreadyProcessed() {
        // given
        UserRegisteredEvent event = new UserRegisteredEvent(
            1L,
            "test@example.com",
            "테스트사용자",
            "010-1234-5678",
            "$2a$10$hashedPassword",
            LocalDateTime.now()
        );
        String eventPayload = "{\"userId\":1,\"email\":\"test@example.com\"}";

        ProcessedEvent existingProcessed = ProcessedEvent.builder()
            .eventType("USER_REGISTERED")
            .eventKey("1")
            .payload(eventPayload)
            .status(ProcessedEvent.ProcessStatus.SUCCESS)
            .build();

        given(processedEventRepository.existsByEventTypeAndEventKey("USER_REGISTERED", "1"))
            .willReturn(true);
        given(processedEventRepository.findByEventTypeAndEventKey("USER_REGISTERED", "1"))
            .willReturn(Optional.of(existingProcessed));
        given(processedEventRepository.save(any(ProcessedEvent.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        // when
        eventProcessingService.processUserRegisteredEvent(event, eventPayload);

        // then
        then(processedEventRepository).should().existsByEventTypeAndEventKey("USER_REGISTERED", "1");
        then(authUserRepository).should(never()).save(any(AuthUser.class));
        then(processedEventRepository).should().save(argThat(processed ->
            processed.getStatus() == ProcessedEvent.ProcessStatus.DUPLICATE
        ));
    }

    @Test
    @DisplayName("이메일 중복 - 비즈니스 검증 실패")
    void processUserRegisteredEvent_DuplicateEmail() {
        // given
        UserRegisteredEvent event = new UserRegisteredEvent(
            1L,
            "duplicate@example.com",
            "테스트사용자",
            "010-1234-5678",
            "$2a$10$hashedPassword",
            LocalDateTime.now()
        );
        String eventPayload = "{\"userId\":1,\"email\":\"test@example.com\"}";

        given(processedEventRepository.existsByEventTypeAndEventKey(anyString(), anyString()))
            .willReturn(false);
        given(authUserRepository.existsByEmail("duplicate@example.com"))
            .willReturn(true);
        given(processedEventRepository.save(any(ProcessedEvent.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        // when
        eventProcessingService.processUserRegisteredEvent(event, eventPayload);

        // then
        then(authUserRepository).should(never()).save(any(AuthUser.class));
        then(processedEventRepository).should().save(argThat(processed ->
            processed.getStatus() == ProcessedEvent.ProcessStatus.DUPLICATE &&
            processed.getResultMessage().contains("이메일 중복")
        ));
    }

    @Test
    @DisplayName("처리 중 예외 발생 - 실패 이력 기록 및 예외 전파")
    void processUserRegisteredEvent_Exception() {
        // given
        UserRegisteredEvent event = new UserRegisteredEvent(
            1L,
            "test@example.com",
            "테스트사용자",
            "010-1234-5678",
            "$2a$10$hashedPassword",
            LocalDateTime.now()
        );
        String eventPayload = "{\"userId\":1,\"email\":\"test@example.com\"}";

        given(processedEventRepository.existsByEventTypeAndEventKey(anyString(), anyString()))
            .willReturn(false);
        given(authUserRepository.existsByEmail(anyString()))
            .willReturn(false);
        given(authUserRepository.save(any(AuthUser.class)))
            .willThrow(new RuntimeException("DB 연결 실패"));
        given(processedEventRepository.save(any(ProcessedEvent.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        // when & then
        assertThatThrownBy(() ->
            eventProcessingService.processUserRegisteredEvent(event, eventPayload)
        )
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("이벤트 처리 실패");

        then(processedEventRepository).should().save(argThat(processed ->
            processed.getStatus() == ProcessedEvent.ProcessStatus.FAILED &&
            processed.getResultMessage().contains("처리 실패")
        ));
    }

    @Test
    @DisplayName("AuthUser 생성 검증")
    void processUserRegisteredEvent_AuthUserCreation() {
        // given
        UserRegisteredEvent event = new UserRegisteredEvent(
            1L,
            "test@example.com",
            "테스트사용자",
            "010-1234-5678",
            "$2a$10$hashedPassword",
            LocalDateTime.now()
        );
        String eventPayload = "{\"userId\":1,\"email\":\"test@example.com\"}";

        given(processedEventRepository.existsByEventTypeAndEventKey(anyString(), anyString()))
            .willReturn(false);
        given(authUserRepository.existsByEmail(anyString()))
            .willReturn(false);
        given(authUserRepository.save(any(AuthUser.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        given(processedEventRepository.save(any(ProcessedEvent.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        // when
        eventProcessingService.processUserRegisteredEvent(event, eventPayload);

        // then
        then(authUserRepository).should().save(argThat(authUser ->
            authUser.getEmail().equals("test@example.com") &&
            authUser.getPassword().equals("$2a$10$hashedPassword") &&
            authUser.getStatus() == AuthUser.UserStatus.ACTIVE
        ));
    }
}

package com.example.authservice.consumer;

import com.example.authservice.common.EventTypeConstants;
import com.example.authservice.domain.entity.AuthUser;
import com.example.authservice.domain.entity.FailedMessage;
import com.example.authservice.domain.entity.ProcessedEvent;
import com.example.authservice.repository.AuthUserRepository;
import com.example.authservice.repository.FailedMessageRepository;
import com.example.authservice.repository.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * UserRegisteredEventConsumer 통합 테스트
 *
 * EmbeddedKafka를 사용하여 실제 Kafka 메시지 전송부터
 * Consumer 처리, DB 저장까지 전체 플로우를 테스트합니다.
 */
@SpringBootTest
@EmbeddedKafka(
    partitions = 3,
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9099",
        "port=9099"
    },
    topics = {
        EventTypeConstants.TOPIC_USER_REGISTERED,
        EventTypeConstants.TOPIC_USER_REGISTERED + "-retry-0",
        EventTypeConstants.TOPIC_USER_REGISTERED + "-retry-1",
        EventTypeConstants.TOPIC_USER_REGISTERED + "-retry-2",
        EventTypeConstants.TOPIC_USER_REGISTERED + "-dlt"
    }
)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("UserRegisteredEventConsumer 통합 테스트")
class UserRegisteredEventConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private AuthUserRepository authUserRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private FailedMessageRepository failedMessageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // 테스트 전 DB 초기화
        authUserRepository.deleteAll();
        processedEventRepository.deleteAll();
        failedMessageRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        // 테스트 후 정리
        authUserRepository.deleteAll();
        processedEventRepository.deleteAll();
        failedMessageRepository.deleteAll();
    }

    @Test
    @DisplayName("정상 메시지 처리 - AuthUser와 ProcessedEvent 생성")
    void testNormalMessageProcessing() throws Exception {
        // given
        String email = "test@example.com";
        Long userId = 1L;

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userId", userId);
        eventData.put("email", email);
        eventData.put("name", "홍길동");
        eventData.put("phone", "010-1234-5678");
        eventData.put("hashedPassword", "$2a$10$hashedPassword");
        eventData.put("registeredAt", LocalDateTime.now().toString());

        String message = objectMapper.writeValueAsString(eventData);

        // when
        kafkaTemplate.send(EventTypeConstants.TOPIC_USER_REGISTERED, message);

        // then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            // AuthUser 생성 확인
            AuthUser authUser = authUserRepository.findByEmail(email).orElse(null);
            assertThat(authUser).isNotNull();
            assertThat(authUser.getEmail()).isEqualTo(email);
            assertThat(authUser.getStatus()).isEqualTo(AuthUser.UserStatus.ACTIVE);

            // ProcessedEvent 생성 확인
            ProcessedEvent processedEvent = processedEventRepository
                .findByEventTypeAndEventKey("USER_REGISTERED", userId.toString())
                .orElse(null);
            assertThat(processedEvent).isNotNull();
            assertThat(processedEvent.getStatus()).isEqualTo(ProcessedEvent.ProcessStatus.SUCCESS);
        });
    }

    @Test
    @DisplayName("중복 이벤트 처리 - Idempotency 검증")
    void testIdempotency() throws Exception {
        // given
        String email = "duplicate@example.com";
        Long userId = 2L;

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userId", userId);
        eventData.put("email", email);
        eventData.put("name", "김철수");
        eventData.put("phone", "010-9876-5432");
        eventData.put("hashedPassword", "$2a$10$anotherHash");
        eventData.put("registeredAt", LocalDateTime.now().toString());

        String message = objectMapper.writeValueAsString(eventData);

        // when - 동일 메시지 2번 전송
        kafkaTemplate.send(EventTypeConstants.TOPIC_USER_REGISTERED, message);
        kafkaTemplate.send(EventTypeConstants.TOPIC_USER_REGISTERED, message);

        // then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            // AuthUser는 1개만 생성되어야 함
            long authUserCount = authUserRepository.count();
            assertThat(authUserCount).isEqualTo(1);

            // ProcessedEvent 확인 (중복 표시)
            ProcessedEvent processedEvent = processedEventRepository
                .findByEventTypeAndEventKey("USER_REGISTERED", userId.toString())
                .orElse(null);
            assertThat(processedEvent).isNotNull();
            // 첫 번째는 SUCCESS, 두 번째는 DUPLICATE로 업데이트됨
            assertThat(processedEvent.getStatus()).isEqualTo(ProcessedEvent.ProcessStatus.DUPLICATE);
        });
    }

    @Test
    @DisplayName("잘못된 JSON 메시지 처리 - DLQ 저장")
    void testInvalidJsonMessage() throws Exception {
        // given
        String invalidMessage = "{invalid json format}";

        // when
        kafkaTemplate.send(EventTypeConstants.TOPIC_USER_REGISTERED, invalidMessage);

        // then
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            // FailedMessage에 저장되어야 함
            long failedMessageCount = failedMessageRepository.count();
            assertThat(failedMessageCount).isGreaterThan(0);

            FailedMessage failedMessage = failedMessageRepository.findAll().get(0);
            assertThat(failedMessage.getPayload()).isEqualTo(invalidMessage);
            assertThat(failedMessage.getEventType()).contains("PARSE_FAILED");
            assertThat(failedMessage.getStatus()).isEqualTo(FailedMessage.FailedMessageStatus.PENDING);
        });
    }

    @Test
    @DisplayName("이메일 중복 처리 - ProcessedEvent DUPLICATE 상태")
    void testDuplicateEmail() throws Exception {
        // given
        String email = "same@example.com";

        // 먼저 AuthUser를 직접 생성 (같은 이메일)
        AuthUser existingUser = AuthUser.builder()
            .email(email)
            .password("$2a$10$existingPassword")
            .status(AuthUser.UserStatus.ACTIVE)
            .build();
        authUserRepository.save(existingUser);

        // 다른 userId로 이벤트 전송
        Long userId = 3L;
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userId", userId);
        eventData.put("email", email);
        eventData.put("name", "중복이메일");
        eventData.put("phone", "010-1111-2222");
        eventData.put("hashedPassword", "$2a$10$newHash");
        eventData.put("registeredAt", LocalDateTime.now().toString());

        String message = objectMapper.writeValueAsString(eventData);

        // when
        kafkaTemplate.send(EventTypeConstants.TOPIC_USER_REGISTERED, message);

        // then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            // AuthUser는 여전히 1개여야 함
            long authUserCount = authUserRepository.count();
            assertThat(authUserCount).isEqualTo(1);

            // ProcessedEvent는 DUPLICATE 상태로 저장
            ProcessedEvent processedEvent = processedEventRepository
                .findByEventTypeAndEventKey("USER_REGISTERED", userId.toString())
                .orElse(null);
            assertThat(processedEvent).isNotNull();
            assertThat(processedEvent.getStatus()).isEqualTo(ProcessedEvent.ProcessStatus.DUPLICATE);
            assertThat(processedEvent.getResultMessage()).contains("이메일 중복");
        });
    }

    @Test
    @DisplayName("여러 메시지 동시 처리")
    void testMultipleMessages() throws Exception {
        // given
        int messageCount = 5;

        // when - 5개 메시지 전송
        for (int i = 0; i < messageCount; i++) {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("userId", 100L + i);
            eventData.put("email", "user" + i + "@example.com");
            eventData.put("name", "사용자" + i);
            eventData.put("phone", "010-0000-000" + i);
            eventData.put("hashedPassword", "$2a$10$hash" + i);
            eventData.put("registeredAt", LocalDateTime.now().toString());

            String message = objectMapper.writeValueAsString(eventData);
            kafkaTemplate.send(EventTypeConstants.TOPIC_USER_REGISTERED, message);
        }

        // then
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            // 모든 AuthUser가 생성되어야 함
            long authUserCount = authUserRepository.count();
            assertThat(authUserCount).isEqualTo(messageCount);

            // 모든 ProcessedEvent가 SUCCESS 상태여야 함
            long processedEventCount = processedEventRepository.count();
            assertThat(processedEventCount).isEqualTo(messageCount);
        });
    }

    @Test
    @DisplayName("필수 필드 누락 메시지 - DLQ 저장")
    void testMissingRequiredFields() throws Exception {
        // given - email 필드 누락
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userId", 999L);
        eventData.put("name", "필드누락");
        // email 누락
        eventData.put("phone", "010-9999-9999");
        eventData.put("hashedPassword", "$2a$10$hash");
        eventData.put("registeredAt", LocalDateTime.now().toString());

        String message = objectMapper.writeValueAsString(eventData);

        // when
        kafkaTemplate.send(EventTypeConstants.TOPIC_USER_REGISTERED, message);

        // then
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            // FailedMessage에 저장되어야 함
            long failedMessageCount = failedMessageRepository.count();
            assertThat(failedMessageCount).isGreaterThan(0);

            // AuthUser는 생성되지 않아야 함 (필드 누락으로 실패)
            long authUserCount = authUserRepository.count();
            assertThat(authUserCount).isEqualTo(0);
        });
    }
}

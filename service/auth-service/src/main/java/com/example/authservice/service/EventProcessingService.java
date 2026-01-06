package com.example.authservice.service;

import com.example.authservice.domain.entity.AuthUser;
import com.example.authservice.domain.entity.ProcessedEvent;
import com.example.authservice.domain.event.UserRegisteredEvent;
import com.example.authservice.repository.AuthUserRepository;
import com.example.authservice.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이벤트 처리 서비스
 * Idempotency를 보장하며 이벤트를 처리하고 이력을 관리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventProcessingService {

    private final AuthUserRepository authUserRepository;
    private final ProcessedEventRepository processedEventRepository;

    /**
     * UserRegisteredEvent로부터 AuthUser 생성 (Idempotency 보장)
     *
     * @param event 회원 등록 이벤트
     * @param eventPayload 원본 이벤트 페이로드 (추적용)
     * @throws IllegalStateException 이벤트 처리 실패 시
     */
    @Transactional
    public void processUserRegisteredEvent(UserRegisteredEvent event, String eventPayload) {
        String eventType = "USER_REGISTERED";
        String eventKey = String.valueOf(event.getUserId()); // userId를 이벤트 키로 사용

        // 1. 이벤트 중복 처리 체크 (Idempotency)
        if (processedEventRepository.existsByEventTypeAndEventKey(eventType, eventKey)) {
            log.info("이미 처리된 이벤트입니다. eventType: {}, eventKey: {}", eventType, eventKey);

            // 중복 처리 이력 업데이트
            processedEventRepository.findByEventTypeAndEventKey(eventType, eventKey)
                    .ifPresent(processed -> {
                        processed.updateStatus(
                            ProcessedEvent.ProcessStatus.DUPLICATE,
                            "중복 이벤트 감지 - 처리 스킵"
                        );
                        processedEventRepository.save(processed);
                    });
            return;
        }

        try {
            // 2. 이메일 중복 체크 (비즈니스 검증)
            if (authUserRepository.existsByEmail(event.getEmail())) {
                log.warn("동일한 이메일로 이미 등록된 사용자가 있습니다. email: {}, userId: {}",
                    event.getEmail(), event.getUserId());

                // 처리 이력 기록 (중복)
                ProcessedEvent processedEvent = ProcessedEvent.builder()
                        .eventType(eventType)
                        .eventKey(eventKey)
                        .payload(eventPayload)
                        .status(ProcessedEvent.ProcessStatus.DUPLICATE)
                        .resultMessage("이메일 중복: " + event.getEmail())
                        .build();
                processedEventRepository.save(processedEvent);
                return;
            }

            // 3. AuthUser 생성 (비밀번호는 이미 해시된 상태)
            AuthUser authUser = AuthUser.builder()
                    .email(event.getEmail())
                    .password(event.getHashedPassword())
                    .status(AuthUser.UserStatus.ACTIVE)
                    .build();

            authUserRepository.save(authUser);

            // 4. 처리 성공 이력 기록
            ProcessedEvent processedEvent = ProcessedEvent.builder()
                    .eventType(eventType)
                    .eventKey(eventKey)
                    .payload(eventPayload)
                    .status(ProcessedEvent.ProcessStatus.SUCCESS)
                    .resultMessage("AuthUser 생성 완료: " + authUser.getUserId())
                    .build();
            processedEventRepository.save(processedEvent);

            log.info("UserRegisteredEvent 처리 완료. email: {}, userId: {}, authUserId: {}",
                    event.getEmail(), event.getUserId(), authUser.getUserId());

        } catch (Exception e) {
            log.error("UserRegisteredEvent 처리 중 예외 발생. userId: {}, email: {}",
                event.getUserId(), event.getEmail(), e);

            // 처리 실패 이력 기록
            ProcessedEvent processedEvent = ProcessedEvent.builder()
                    .eventType(eventType)
                    .eventKey(eventKey)
                    .payload(eventPayload)
                    .status(ProcessedEvent.ProcessStatus.FAILED)
                    .resultMessage("처리 실패: " + e.getMessage())
                    .build();
            processedEventRepository.save(processedEvent);

            // 예외를 다시 던져서 재시도/DLQ 처리가 가능하도록 함
            throw new IllegalStateException("이벤트 처리 실패", e);
        }
    }
}

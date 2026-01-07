# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

전자상거래 MSA 시스템의 사용자 관리 마이크로서비스입니다. 사용자 등록을 처리하고 Transactional Outbox 패턴을 사용하여 Kafka를 통해 도메인 이벤트를 발행합니다.

**기술 스택:** Spring Boot 3.5.9 (JDK 17), MySQL, Kafka, Spring Data JPA, Springdoc OpenAPI, Springwolf AsyncAPI, Config Client

## 명령어

### 빌드 & 테스트
```bash
./gradlew build                                                    # 프로젝트 빌드
./gradlew test                                                     # 전체 테스트 실행
./gradlew test --tests "ClassName"                                # 특정 테스트 클래스 실행
./gradlew test --tests "*ClassName.methodName*"                   # 특정 테스트 메서드 실행
./gradlew bootRun                                                  # 애플리케이션 실행
```

## 아키텍처

### Transactional Outbox 패턴

이 서비스는 신뢰성 있는 이벤트 발행을 위해 Transactional Outbox 패턴을 사용합니다:

1. **쓰기 단계:** 도메인 변경(예: 사용자 등록)시 `outbox` 테이블에 이벤트를 같은 DB 트랜잭션으로 저장 (`UserService.signUp()` 참조 - src/main/java/com/example/userservice/service/UserService.java:32)

2. **폴링 발행자:** `OutboxEventScheduler`가 1초마다 실행되며 MySQL 분산 락(`GET_LOCK()`)을 사용하여 다중 서버 환경에서 한 인스턴스만 이벤트를 처리하도록 보장 (src/main/java/com/example/userservice/service/outbox/OutboxEventScheduler.java:24)

3. **이벤트 발행:** `OutboxEventPublisher`가 PENDING 이벤트를 조회하여 Kafka에 발행하고 상태를 PUBLISHED/FAILED로 업데이트. DB-Kafka 간 분산 트랜잭션을 피하기 위해 Kafka 트랜잭션은 사용하지 않음 (src/main/java/com/example/userservice/service/outbox/OutboxEventPublisher.java:33)

**주요 구현 사항:**
- 각 이벤트 타입은 AsyncAPI 문서화를 위해 `@AsyncPublisher`로 장식된 전용 발행 메서드를 가짐 (예: `publishUserRegisteredEvent()`)
- Kafka 메시지 키: `{aggregateType}-{aggregateId}` 형식 (예: "User-123")
- `outbox(event_type, status)` 복합 인덱스로 PENDING 이벤트 조회 최적화
- Outbox 엔티티 추적 항목: aggregateType, aggregateId, eventType, payload (JSON), status

### 패키지 구조

```
com.example.userservice/
├── domain/
│   ├── entity/          # User, Outbox (JPA 엔티티)
│   └── event/           # UserRegisteredEvent (도메인 이벤트)
├── service/
│   └── outbox/          # OutboxEventScheduler, OutboxEventPublisher
├── repository/          # Spring Data JPA 인터페이스
├── controller/          # REST 엔드포인트
├── dto/                 # Request/Response 객체
├── config/              # SecurityConfig (BCryptPasswordEncoder), CORS, OpenAPI
├── exception/           # 커스텀 예외, 전역 핸들러
└── common/              # EventTypeConstants
```

### 이벤트 플로우

현재 발행되는 이벤트:
- `user.registered` - `EventTypeConstants.TOPIC_USER_REGISTERED`에 정의된 토픽

### 설정

**환경 변수:**
- `KAFKA_BOOTSTRAP_SERVERS` (기본값: localhost:9091,localhost:9092,localhost:9094)

**데이터베이스:** localhost:3306/user_service의 MySQL (스키마는 src/main/resources/schema.sql 참조)

**API 문서:**
- REST: `/swagger-ui.html` (Springdoc OpenAPI)
- Events: `asyncapi.json` 또는 Springwolf UI (이벤트 스키마)

### 테스트

테스트는 H2 인메모리 데이터베이스를 사용합니다. 참고: MySQL 전용 `GET_LOCK()` 함수를 사용하므로 H2에서는 분산 락 통합 테스트가 불가능합니다.

## 주요 패턴

- **트랜잭션 범위:** 도메인 상태 변경 + outbox 이벤트 생성을 단일 `@Transactional` 메서드에서 처리
- **Kafka 트랜잭션 미사용:** Kafka 프로듀서는 트랜잭션을 사용하지 않음 (분산 트랜잭션 복잡도 회피)
- **멱등성 프로듀서:** Kafka는 `enable.idempotence=true`와 `acks=all`로 설정
- **이벤트 직렬화:** Jackson ObjectMapper가 이벤트를 JSON 문자열로 직렬화하여 outbox.payload에 저장

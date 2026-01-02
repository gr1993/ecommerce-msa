# CLAUDE.md

이 파일은 Claude Code(claude.ai/code)가 이 리포지토리의 코드 작업 시 참고하는 가이드입니다.

## 빌드 및 실행 명령어

```bash
# 프로젝트 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun

# 모든 테스트 실행
./gradlew test

# 단일 테스트 클래스 실행
./gradlew test --tests "com.example.authservice.SomeTestClass"

# 단일 테스트 메서드 실행
./gradlew test --tests "com.example.authservice.SomeTestClass.testMethodName"

# 클린 빌드
./gradlew clean build
```

## 아키텍처 개요

Spring Cloud 기반 이커머스 MSA 시스템의 인증 마이크로서비스입니다.

**서비스 책임:**
- 로그인 및 JWT 토큰 발급/갱신
- JWT 서명 키 관리
- 토큰 검증 (또는 검증을 위한 공개 키 제공)

**기술 스택:**
- Spring Boot 3.5.9 with JDK 17
- Gradle 8.14.3 (wrapper)
- MySQL 8 (영속성 저장소, 공유 인스턴스, 논리적 분리)
- Kafka (서비스 간 메시징, 3-node 클러스터)
- Springdoc OpenAPI (REST API 문서화, `/swagger-ui.html`)
- Springwolf (AsyncAPI/Kafka 이벤트 문서화)

**패키지 구조 (DDD 기반):**
```
com.example.authservice
├── client/         # 외부 서비스 통신 (FeignClient, RestTemplate)
├── common/         # 유틸리티 및 공통 상수
├── config/         # 설정 클래스 (Security, Swagger, Bean 설정)
├── controller/     # REST API 엔드포인트
├── domain/
│   ├── entity/     # JPA 엔티티
│   ├── event/      # 도메인 이벤트 클래스
│   ├── service/    # 도메인 서비스
│   └── value/      # Value Objects
├── dto/
│   ├── request/
│   └── response/
├── exception/      # 커스텀 예외 및 글로벌 핸들러
├── repository/     # Spring Data JPA 인터페이스
└── service/        # 비즈니스 로직 인터페이스 및 구현체
```

## MSA 컨텍스트

gateway, user, product, catalog, order, payment, promotion, delivery, settlement, return 서비스들과 함께 구성된 이커머스 시스템의 일부입니다. 서비스 간 통신 방식:
- REST API (동기 통신)
- Kafka 이벤트 (비동기 통신, outbox 패턴 사용)

## 인프라 설정

Docker Compose로 MySQL과 Kafka 클러스터를 관리합니다. `infra/` 디렉토리에서:
```bash
docker-compose -f docker-compose.infra.yml up -d
```
- MySQL: port 3306 (root 비밀번호: 123123)
- Kafka brokers: ports 9091, 9092, 9094
- Kafka-UI: port 8090

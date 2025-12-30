# user-service
회원 정보를 관리하고 관련 API를 제공하는 MSA 서비스


### 프로젝트 패키지 구조
```
com.example.userservice
├── client              # 외부 서비스 통신 (FeignClient, RestTemplate)
├── common              # 유틸리티, 공통 상수
├── config              # 설정 클래스 (Security, Swagger, Bean 설정 등)
├── controller          # API 엔드포인트 (REST Controller)
├── domain              # Entity 및 관련 값 객체 (VO)
│   ├── entity/         # DB와 매핑되는 JPA 엔티티
│   ├── event/          # 도메인 이벤트 클래스
│   ├── service/        # 도메인 서비스 (비즈니스 로직)
│   └── value/          # VO(Value Object) 등
├── dto                 # Request/Response 데이터 전송 객체
│   ├── request
│   └── response
├── exception           # 커스텀 예외 처리 및 Global Handler
├── repository          # DB 접근 (Spring Data JPA Interface 등)
│── service             # 비즈니스 로직 인터페이스 및 구현체
└   └── outbox/         # 이벤트 발행 Polling
```


### 백엔드 기술
* Spring Boot 3.5.9 (JDK 17)
* spring-boot-starter-data-jpa
* spring-security-crypto : BCryptPasswordEncoder를 위해 추가
* spring-kafka
* springdoc-openapi-starter-webmvc-ui:2.8.9 : swagger


### REST API

REST API 명세는 다음 방법으로 확인할 수 있다.

1. 브라우저에서 Swagger UI 열기: `/swagger-ui.html`
2. 정적 문서 확인: [`openapi.json`](./openapi.json)


### Events

이벤트 상세 명세는 [`asyncapi.yaml`](./asyncapi.yaml) 파일을 참고하면 된다.

| 구분 | 설명 |
|-----|------|
| 발행(Published) | user.registered |
| 구독(Subscribed) | - |
# user-service
회원 정보를 관리하고 관련 API를 제공하는 MSA 서비스


### 프로젝트 패키지 구조
```
com.example.userservice
├── config              # 설정 클래스 (Security, Swagger, Bean 설정 등)
├── controller          # API 엔드포인트 (REST Controller)
├── service             # 비즈니스 로직 인터페이스 및 구현체
├── repository          # DB 접근 (Spring Data JPA Interface 등)
├── domain              # Entity 및 관련 값 객체 (VO)
    ├─ entity/          # DB와 매핑되는 JPA 엔티티
    ├─ event/           # 도메인 이벤트 클래스
    ├─ service/         # 도메인 서비스 (비즈니스 로직)
    └─ value/           # VO(Value Object) 등
├── dto                 # Request/Response 데이터 전송 객체
│   ├── request
│   └── response
├── client              # 외부 서비스 통신 (FeignClient, RestTemplate)
├── exception           # 커스텀 예외 처리 및 Global Handler
└── common              # 유틸리티, 공통 상수
```


### 백엔드 기술
* Spring Boot 3.5.9 (JDK 17)
* spring-boot-starter-data-jpa
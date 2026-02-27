# order-service
주문 데이터를 생성하고 관리를 담당하는 MSA 서비스


### 주문 상태 머신 다이어그램
```mermaid
stateDiagram-v2
    %% 초기 상태
    [*] --> CREATED : 주문 생성

    %% 결제 단계
    CREATED --> PAID : 결제 완료
    CREATED --> FAILED : 결제/재고차감 실패
    CREATED --> CANCELED : 결제 전 취소

    %% 배송 단계
    PAID --> SHIPPING : 상품 발송
    PAID --> CANCELED : 배송 전 취소 (환불)
    
    SHIPPING --> DELIVERED : 배송 완료

    %% 반품(역방향 물류) 단계
    DELIVERED --> RETURN_REQUESTED : 반품 신청
    
    RETURN_REQUESTED --> RETURN_APPROVED : 관리자 승인
    RETURN_REQUESTED --> DELIVERED : 관리자 거절 (원상태 복구)

    RETURN_APPROVED --> RETURN_IN_TRANSIT : 반품 수거 중 (택배사 연동)
    RETURN_IN_TRANSIT --> RETURNED : 창고 도착 및 검수 완료 (환불)

    %% 교환 프로세스 단계 (투트랙 운송장 처리)
    DELIVERED --> EXCHANGE_REQUESTED : 교환 신청
    
    EXCHANGE_REQUESTED --> EXCHANGE_APPROVED : 관리자 승인 (회수/발송 운송장 발급)
    EXCHANGE_REQUESTED --> DELIVERED : 관리자 거절 (원상태 복구)

    EXCHANGE_APPROVED --> EXCHANGE_COLLECTING : 기존 물품 회수 중
    EXCHANGE_COLLECTING --> EXCHANGE_RETURN_COMPLETED : 회수 완료 및 검수 대기
    
    EXCHANGE_RETURN_COMPLETED --> EXCHANGE_SHIPPING : 관리자 출고 지시 및 새 물품 발송 중
    EXCHANGE_SHIPPING --> EXCHANGED : 새 물품 배송 완료 (교환 최종 완료)

    %% 종료 상태
    FAILED --> [*]
    CANCELED --> [*]
    RETURNED --> [*]
    EXCHANGED --> [*]
```


### 주문 취소 프로세스
```mermaid
sequenceDiagram
    autonumber
    participant User as 사용자
    participant Order as Order-Service
    participant Kafka
    participant Payment as Payment-Service
    participant Product as Product-Service

    User->>Order: 주문 취소 요청
    
    rect rgb(240, 240, 240)
        Order->>Order: 1. 주문 상태 변경 (CANCELLED)
        Order-->>Kafka: 2. order.cancelled 발행 (Outbox)
    end

    Note right of Kafka: 보상 트랜잭션 시작

    par 병렬 처리
        Kafka->>Payment: 3a. order.cancelled 구독
        Payment->>Payment: 4a. 실제 환불/결제 취소 로직
    and
        Kafka->>Product: 3b. order.cancelled 구독
        Product->>Product: 4b. 재고 복구 (멱등성 체크)
    end

    Payment-->>User: 결제 취소/환불 완료 알림
```
* 사용자/CS에서 주문 취소 요청을 하면 order.cancelled 이벤트를 발행한다.
* 주문 생성 후 10분 이내에 결제되지 않으면 order.cancelled 이벤트를 발행한다.


### 프로젝트 패키지 구조
```
com.example.orderservice
├── common              # 유틸리티, 공통 상수
├── config              # 설정 클래스 (Security, JWT, CORS, Swagger 등)
├── consumer            # Kafka 이벤트 컨슈머
├── controller          # API 엔드포인트 (REST Controller)
├── domain
│   ├── entity/         # DB와 매핑되는 JPA 엔티티
│   └── event/          # 도메인 이벤트 클래스
├── dto                 # Request/Response 데이터 전송 객체
│   ├── request
│   └── response
├── exception           # 커스텀 예외 처리 및 Global Handler
├── repository          # DB 접근 (Spring Data JPA Interface)
└── service             # 비즈니스 로직 (JWT 토큰 생성/검증, 인증 처리)
```


### 백엔드 기술
* Spring Boot 3.5.10 (JDK 17)
* spring-boot-starter-web
* spring-boot-starter-data-jpa
* MySQL : 영속성 저장소
* Spring Kafka : 이벤트 메시징
* openfeign : Http Client
* Springwolf 1.20.0 : AsyncAPI 문서 자동 생성
* springdoc-openapi-starter-webmvc-ui:2.8.9 : Swagger
* spring-cloud-starter-config : Config Client
* eureka-client


### REST API

REST API 명세는 다음 방법으로 확인할 수 있다.

1. 브라우저에서 Swagger UI 열기: `/swagger-ui.html`
2. 정적 문서 확인: [`openapi.json`](./openapi.json)


### Events

Producer에서 Transaction Outbox 패턴을 적용하였다.  
Consumer에서 실패 처리 전략을 적용하여 메시지를 DLQ로 전송하도록 설계하고, 처리된 메시지는  
processed_events 테이블에서 관리하여 중복 전송 시에도 멱등성을 보장하도록 구성하였다.  
이벤트 상세 명세는 다음 방법으로 확인할 수 있다.

1. 브라우저에서 Swagger UI 열기: `/springwolf/asyncapi-ui.html`
2. 정적 문서 확인: [`asyncapi.yaml`](./asyncapi.yaml)

| 구분 | 설명 |
|-----|-----|
| 발행(Published) | order.created, order.cancelled, coupon.used, coupon.restored, inventory.increase, inventory.decrease |
| 구독(Subscribed) | payment.confirmed, payment.cancelled, shipping.started, shipping.delivered, return.approved, return.in-transit, return.completed, exchange.approved, exchange.collecting, exchange.return-completed, exchange.shipping, exchange.completed |
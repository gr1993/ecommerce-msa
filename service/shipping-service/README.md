# shipping-service
외부 택배사 API와 연동하여 배송 처리를 담당하고, 반품·교환과 같은 업무까지 제공하는 MSA 기반 서비스이다.  
이 도메인 엔티티에서 가장 중요한 개념은 우리 서비스의 배송 상태(shipping_status)와 외부 택배사 API 연동  
상태(delivery_service_status)를 분리하여 관리하는 것이다.  
우리 서비스의 배송 상태가 SHIPPING이라는 것은 외부 배송사에 배송 요청이 완료되었음을 의미한다.  
그리고 배송사 연동 상태를 통해 실제 택배가 현재 어떤 단계까지 진행되었는지를 보다 상세하게 파악할 수 있다.  


### Full Sync API
Full Sync API는 shipping-service 구축 시, order-service에 존재하는 배송 대상 주문 데이터를 이관하기  
위해 사용된다. 상품 Full Sync API를 구현했던 방식과 동일하게 멱등성을 보장하여, 동일한 데이터를 여러 번  
수신하더라도 문제가 발생하지 않도록 설계한다.  


### 배송 프로세스
```mermaid
sequenceDiagram
    participant Order as Order Service (Publisher)
    participant Broker as Message Broker (Kafka/RabbitMQ)
    participant Ship as Shipping Service (Subscriber)
    participant Mock as Mock Delivery Server (CJ/SmartTracker)

    Note over Order, Ship: [Phase 1: 주문 데이터 동기화]
    Order->>Broker: Publish (order.created)
    Broker-->>Ship: Consume (order.created)
    
    Ship->>Ship: 로컬 DB 저장 (order_shipping)<br/>status: READY<br/>service_status: NOT_SENT

    Note over Ship, Mock: [Phase 2: 송장 발급 처리]
    Ship->>Mock: POST /api/v1/courier/orders/bulk-upload (송장 발급 요청)
    Mock-->>Ship: Response (invoice_no: 12345...)
    
    Ship->>Ship: 로컬 DB 업데이트<br/>status: READY<br/>service_status: SENT<br/>tracking_number: 12345

    Note over Ship, Mock: [Phase 3: 배송 추적 및 상태 변경]
    loop 주기적 폴링 (Scheduler)
        Ship->>Mock: POST /api/v1/trackingInfo (배송 조회 요청)
        Mock-->>Ship: Response (status: IN_TRANSIT)
        
        Ship->>Ship: 로컬 DB 업데이트<br/>status: SHIPPING<br/>service_status: IN_TRANSIT
    end

    loop 주기적 폴링 (배송 완료 시점)
        Ship->>Mock: POST /api/v1/trackingInfo (배송 조회 요청)
        Mock-->>Ship: Response (status: DELIVERED)
        
        Ship->>Ship: 로컬 DB 업데이트<br/>status: DELIVERED<br/>service_status: DELIVERED
    end
```


### 배송 취소 프로세스
취소 가능 여부 확인은 Feign 동기 호출로 즉각적인 피드백을 제공하고,
실제 배송 취소 처리는 `order.cancelled` 이벤트를 소비하여 비동기로 수행한다.

```mermaid
sequenceDiagram
    participant User as 관리자/사용자
    participant Order as Order Service
    participant Broker as Message Broker (Kafka)
    participant Ship as Shipping Service
    participant Mock as Mock Delivery Server

    User->>Order: 주문 취소 요청

    Note over Order, Ship: [Phase 1: 배송 취소 가능 여부 확인 - Feign 동기 호출]
    Order->>Ship: GET /internal/shipping/orders/{orderId}/cancellable

    Ship->>Ship: deliveryServiceStatus 확인

    alt IN_TRANSIT 이상 (취소 불가)
        Ship-->>Order: { cancellable: false, reason: "이미 배송이 진행 중입니다." }
        Order-->>User: 409 Conflict (이미 배송이 시작되었습니다)
    else NOT_SENT 또는 SENT (취소 가능)
        Ship-->>Order: { cancellable: true }

        Note over Order, Broker: [Phase 2: 주문 취소 처리 및 이벤트 발행]
        Order->>Order: 주문 상태 변경 (CANCELLED)
        Order->>Broker: Publish (order.cancelled)
        Order-->>User: 200 OK (취소 완료)

        Note over Broker, Mock: [Phase 3: 배송 취소 처리 - 이벤트 소비]
        Broker-->>Ship: Consume (order.cancelled)

        alt deliveryServiceStatus = NOT_SENT (미발송)
            Ship->>Ship: 배송 상태 변경 (CANCELLED)
        else deliveryServiceStatus = SENT (발송 완료)
            Ship->>Mock: POST /api/v1/courier/orders/bulk-cancel
            Mock-->>Ship: 200 OK
            Ship->>Ship: 배송 상태 변경 (CANCELLED)
        end
    end
```


### 프로젝트 패키지 구조
```
com.example.shippingservice
├── common              # 유틸리티, 공통 상수
├── config              # 설정 클래스 (Security, JWT, CORS, Swagger 등)
├── consumer            # Kafka 이벤트 컨슈머
├── exception           # 커스텀 예외 처리 및 Global Handler
├── exchange            # 교환 도메인 패키지
│   ├── controller      # API 엔드포인트 (REST Controller)
│   ├── repository      # DB 접근 (Spring Data JPA Interface)
│   ├── service         # 비즈니스 로직 (JWT 토큰 생성/검증, 인증 처리)
│   ├── entity
│   └── dto
├── exchange            # 환불 도메인 패키지
│   ├── ...
├── shipping            # 배송 도메인 패키지
│   └── ...
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
| 발행(Published) |  |
| 구독(Subscribed) | order.created, order.cancelled |
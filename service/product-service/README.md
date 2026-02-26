# product-service

product-service는 e-commerce MSA 시스템에서 **상품 도메인의 쓰기 모델**을 담당하는  
마이크로서비스이다. 상품의 생성, 수정, 삭제 및 가격, 옵션 등 핵심 비즈니스 규칙을 관리한다.  

사용자에게 제공되는 상품 조회 및 검색 기능은 CQRS 패턴에 따라 읽기 모델을 분리한 `catalog-service`에서 처리한다.  

상품 재고의 변경은 product-service에서 단일 책임으로 관리하며,  
조회 성능을 위해 catalog-service는 재고 정보를 비동기적으로 복제한다.


### 재고 처리 동시성 문제
현재 사용 중인 MySQL의 트랜잭션 격리 수준은 기본값인 **Repeatable Read**이다.  
MySQL은 MVCC 기반으로 동작하며, 트랜잭션 시작 시점의 스냅샷을 기준으로 읽기를 수행한다.  
이로 인해 동시에 실행되는 트랜잭션 간의 읽기 충돌은 방지된다.  
하지만 동일한 객체(예: 동일 SKU의 재고)에 대해 여러 트랜잭션이 동시에 수정 작업을 수행하는 경우,  
이를 자동으로 감지하거나 방지하지는 못한다. 그 결과 **Lost Update(갱신 손실) 문제**가 발생할 수 있다.  

따라서 재고와 같이 강한 일관성이 요구되는 데이터에 대해서는 격리 수준만으로는 충분하지 않으며,  
추가적인 동시성 제어가 필요하다. 이를 해결하기 위한 방법으로는 비관적 락, 낙관적 락, 분산 락 등이 있다.  
이번 구현에서는 재고 데이터의 정합성을 우선적으로 보장하기 위해, 보다 보수적인 접근 방식인 **비관적 락** 기반의  
동시성 제어 방식을 적용하였다.  


### 재고 부족 시 보상 트랜잭션 트리거
```mermaid
sequenceDiagram
    autonumber
    participant Product as Product Service
    participant Kafka
    participant Order as Order Service
    participant Payment as Payment Service
    participant User as 사용자

    Note over Product, Payment: [실패 상황: 재고 부족 발생]
    
    Product->>Product: 재고 부족 확인
    Product-->>Kafka: StockRejected 이벤트 발행
    
    Note right of Kafka: 모든 관련 서비스에 취소 전파
    
    par 보상 트랜잭션 병렬 처리
        Kafka->>Order: StockRejected 구독
        Order->>Order: 주문 상태 변경 (PENDING -> CANCELLED)
    and
        Kafka->>Payment: StockRejected 구독
        Payment->>Payment: 결제 대기 정보 삭제 또는 상태 변경
    end
    
    Order-->>User: 알림: 재고 부족으로 주문 취소
```
Product-Service는 order.created 이벤트를 구독하여 실시간 재고 차감을 수행한다. 만약 재고가 
부족할 경우 stock.rejected 이벤트를 발행하여 **보상 트랜잭션(Compensation Transaction)**을 
유도하며, 이를 통해 Order-Service가 주문을 즉시 취소 상태로 변경하도록 설계하였다. 또한, 분산 
환경에서의 중복 처리를 방지하기 위해 주문 ID 기반의 멱등성 로직을 적용하여 데이터 정합성을 보장한다.

### 결제 실패 및 주문 취소에 따른 재고 복구(Rollback)
``` mermaid
sequenceDiagram
    autonumber
    participant Payment as Payment Service
    participant Order as Order Service
    participant Kafka
    participant Product as Product Service

    Note over Payment, Order: [다양한 사유에 의한 취소 이벤트 발행]
    
    Payment-->>Kafka: 1a. 결제 시간 초과 시 발송 (payment.cancelled)
    Order-->>Kafka: 1b. 사용자 취소 요청 시 발송 (order.cancelled)
    
    Note right of Kafka: 이벤트를 수신하여 보상 트랜잭션 통합 처리

    Kafka->>Product: 2. 취소 이벤트 구독 (Subscribe)
    
    rect rgb(240, 248, 255)
        Note over Product: 보상 트랜잭션 (Compensation)
        Product->>Product: 3. 멱등성 체크 (주문 ID 기준 중복 확인)
        Product->>Product: 4. 재고 가산 처리 (Inventory Rollback)
        Product->>Product: 5. 처리 결과 로깅
    end
    
    Note over Product: 시스템 전체 재고 정합성 복구 완료
```


### 프로젝트 패키지 구조
```
com.example.productservice/
├── ProductServiceApplication.java
├── global/                          # 전역 설정, 공통 모듈
│   ├── common/
│   │    └── dto/
│   ├── config/
│   ├── domain/
│   ├── exception/
│   ├── repository/
│   └── service/
│        └── outbox/                 # 이벤트 발행 Polling
├── product/                         # 상품 도메인
│   ├── controller/
│   ├── service/
│   ├── domain/
│   ├── repository/
│   └── dto/
├── file/                            # 파일 도메인
│   ├── ...
│   └── scheduler/
├── category/                        # 카테고리 도메인
│   ├── ...
├── display/                         # 화면/노출 도메인
│   ├── ...
└── search/                          # 검색 도메인
    ├── ...
```


### 백엔드 기술

* Java 17
* Spring Boot 3.5.9
* Spring Data JPA
* Spring Cloud Config : 외부 설정 서버 연동
* Spring Cloud Netflix Eureka Client : 서비스 디스커버리 등록
* Spring Kafka : 이벤트 메시징
* Springwolf 1.20.0 : AsyncAPI 문서 자동 생성
* springdoc-openapi 2.8.9 : OpenAPI 문서 자동 생성
* MySQL : 운영 데이터베이스
* H2 : 테스트 데이터베이스
* Lombok


### REST API

REST API 명세는 다음 방법으로 확인할 수 있다.

1. 브라우저에서 Swagger UI 열기: `/swagger-ui.html`


### Events

Producer에서 Transaction Outbox 패턴을 적용하였다.  
Consumer에서 실패 처리 전략을 적용하여 메시지를 DLQ로 전송하도록 설계하고, 처리된 메시지는  
processed_events 테이블에서 관리하여 중복 전송 시에도 멱등성을 보장하도록 구성하였다.  
이벤트 상세 명세는 다음 방법으로 확인할 수 있다.

1. 브라우저에서 Swagger UI 열기: `/springwolf/asyncapi-ui.html`
2. 정적 문서 확인: [`asyncapi.yaml`](./asyncapi.yaml)

| 구분 | 설명 |
|-----|------|
| 발행(Published) | product.created, product.updated, category.created, category.updated, category.deleted, keyword.created, keyword.deleted, stock.rejected |
| 구독(Subscribed) | order.created, payment.cancelled, order.cancelled, inventory.decrease |
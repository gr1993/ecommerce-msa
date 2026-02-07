# catalog-service

catalog-service는 사용자에게 상품 정보를 제공하는 검색 전용 서비스이며, **읽기 전용 모델**로 동작한다.  
주 저장소는 Elasticsearch를 사용하며, 이 서비스는 다음과 같은 기능을 제공할 예정이다.
* Elasticsearch
  * 자동완성 기능: Edge N-gram 사용 (부분 매칭용 n-gram은 사용하지 않을 예정)
  * 효율적인 한글 검색: nori 분석기 적용
  * 정렬, 집계, 페이지네이션 등 다양한 검색 편의 기능
  * 상품 목록 정렬: 판매량 순과 같은 정보는 실시간성보다 정합성이 중요하므로 ES에 저장하고, 하루 단위 배치로 갱신
* Redis
  * 카테고리 캐싱: 카테고리 관련 정보도 Redis에 캐싱하여, product-service API 호출 없이 빠르게 조회
  * 상품 상세 캐싱: 상품 상세 조회 API 호출시 Cache-Aside 방식을 적용하여, Redis에 데이터가 없으면 Product-Service의 상세 API를 호출하고 결과를 Redis에 캐싱한다. 또한 Kafka 이벤트(product.created, product.updated)의 Consumer는 상품 목록용 Consumer와 상세용 Consumer로 분리하여 등록하였다. 상세용 Consumer는 이벤트 객체가 너무 커지는 문제를 방지하기 위해 Data Enrichment 패턴을 사용한다. 즉, 이벤트 수신 시 이벤트 자체에는 최소한의 정보만 포함하고, 추가 상세 데이터는 Product-Service의 상세 API를 한 번 호출하여 가져온 뒤 Redis에 캐싱한다.


### 프로젝트 패키지 구조
```
com.example.catalogservice
├── client              # 외부 서비스 호출 (Product-Service API 클라이언트)
│   └── dto/            # 외부 API 응답 매핑 DTO
├── config              # 설정 클래스 (OpenAPI, Redis 등)
├── consumer            # Kafka 이벤트 컨슈머
│   └── event/          # 이벤트 페이로드 클래스
├── controller          # API 엔드포인트 (REST Controller)
│   └── dto/            # Request/Response 데이터 전송 객체
├── domain              # 도메인 모델
│   └── document/       # Elasticsearch 문서 매핑 엔티티
├── exception           # 커스텀 예외 처리 및 Global Handler
├── infrastructure      # 인프라 설정 (Elasticsearch 인덱스 초기화)
├── repository          # 데이터 접근 (Spring Data Elasticsearch Repository)
└── service             # 비즈니스 로직 (검색, 동기화, 캐싱)
```


### Full Sync API
Full Sync API는 catalog-service 프로젝트가 처음 생성될 때, 초기 데이터 이관용으로 사용된다.  
또한, catalog-service의 Elasticsearch 데이터가 꼬이거나 이벤트 누락이 발생했을 경우 복구용으로도 활용할 수 있다.  

Full Sync API 구현 시에는 멱등성을 확보하여 같은 데이터를 여러 번 받아도 문제가 없도록 하고, upsert 방식으로 Elasticsearch 색인을 진행해야 한다.  
상품 수가 많을 경우에는 Batch 처리도 고려하는 것이 좋다.  

마지막으로, Full Sync 이후에는 product-service에서 발생한 변경 이벤트도 적용하여 이벤트 기반 증분 동기화가 이루어지도록 구현해야 한다.  
* Product Full Sync
  * Full Sync 시 Zero Downtime을 위해 Elasticsearch Alias(별칭) 기능을 활용하여 인덱스를 교체한다.
  * 검색 대상 전체 상품 정보를 새로운 인덱스에 Full Reindex 후 원자적으로 스위칭한다.
* Category Full Sync
  * Full Sync 시 서비스 중단을 방지하기 위해 Redis의 RENAME 명령어를 활용한다.
  * 전체 카테고리를 Redis에 동기화(캐싱)한다. (일 1회 비트래픽 시간대 스케줄 실행, 테스트용으로 API도 구현)


### 사용자 API 신뢰성 전략
catalog-service는 사용자에게 검색 및 상품 조회 기능을 제공하는 읽기 전용 서비스로, 비즈니스  
핵심 트랜잭션에는 직접 관여하지 않는다. 사용자에게 제공되는 API의 신뢰성을 보장하기 위해,  
catalog-service 자체에 별도의 서킷 브레이커를 두기보다는 앞단 Gateway-Service에서  
timeout 및 서킷 브레이커를 통해 장애를 차단하는 구조를 채택한다.  

Elasticsearch 또는 Redis와의 통신 장애로 인해 응답 지연이나 오류가 발생할 경우, 해당 장애는  
Gateway-Service에서 timeout 또는 fallback 처리되어 장애가 사용자에게 직접 전파되지 않도록 한다.  

또한 catalog-service는 Product-Service의 데이터를 이벤트 기반으로 동기화한 파생(Read)  
모델이므로, Elasticsearch 또는 Redis의 데이터가 비정상 상태가 되더라도 Full Sync API를  
실행하여 전체 데이터를 재색인함으로써 데이터 정합성을 빠르게 복구할 수 있다.  

이와 같은 구조를 통해 catalog-service는 장애 발생 시 빠른 실패(Fail Fast), 장애 반경의 최소화  
, 운영 관점에서의 신속한 복구 를 목표로 사용자 API의 신뢰성을 유지한다.  


### 백엔드 기술

* Java 17
* Spring Boot 3.5.9
* Spring Cloud Config : 외부 설정 서버 연동
* Spring Cloud Netflix Eureka Client : 서비스 디스커버리 등록
* Spring Data Elasticsearch 5.5.7
* Spring Kafka : 이벤트 메시징
* Springwolf 1.20.0 : AsyncAPI 문서 자동 생성
* springdoc-openapi 2.8.9 : OpenAPI 문서 자동 생성
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

| 구분 | 설명                                 |
|-----|------------------------------------|
| 발행(Published) | -                                  |
| 구독(Subscribed) | product.created, product.updated, category.created, category.updated, category.deleted, keyword.created, keyword.deleted |
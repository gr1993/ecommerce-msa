# catalog-service

catalog-service는 사용자에게 상품 정보를 제공하는 검색 전용 서비스이며, **읽기 전용 모델**로 동작한다.  
주 저장소는 Elasticsearch를 사용하며, 이 서비스는 다음과 같은 기능을 제공할 예정이다.
* Elasticsearch
  * 자동완성 기능: Edge N-gram 사용 (부분 매칭용 n-gram은 사용하지 않을 예정)
  * 효율적인 한글 검색: nori 분석기 적용
  * 정렬, 집계, 페이지네이션 등 다양한 검색 편의 기능
  * 상품 목록 정렬: 판매량 순과 같은 정보는 실시간성보다 정합성이 중요하므로 ES에 저장하고, 하루 단위 배치로 갱신
* Redis
  * 재고 관리: 실시간성이 중요한 데이터는 상세 조회에서 주로 사용하며, ES에는 저장하지 않고 Redis를 통해 관리
  * 카테고리 캐싱: 카테고리 관련 정보도 Redis에 캐싱하여, product-service API 호출 없이 빠르게 조회


### Full Sync API
Full Sync API는 catalog-service 프로젝트가 처음 생성될 때, 초기 데이터 이관용으로 사용된다.  
또한, catalog-service의 Elasticsearch 데이터가 꼬이거나 이벤트 누락이 발생했을 경우 복구용으로도 활용할 수 있다.  

Full Sync API 구현 시에는 멱등성을 확보하여 같은 데이터를 여러 번 받아도 문제가 없도록 하고, upsert 방식으로 Elasticsearch 색인을 진행해야 한다.  
상품 수가 많을 경우에는 Batch 처리도 고려하는 것이 좋다.  

마지막으로, Full Sync 이후에는 product-service에서 발생한 변경 이벤트도 적용하여 이벤트 기반 증분 동기화가 이루어지도록 구현해야 한다.  


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
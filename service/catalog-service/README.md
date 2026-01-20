# catalog-service


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
* Spring Kafka : 이벤트 메시징
* Springwolf 1.20.0 : AsyncAPI 문서 자동 생성
* springdoc-openapi 2.8.9 : OpenAPI 문서 자동 생성
* Lombok


### REST API

REST API 명세는 다음 방법으로 확인할 수 있다.

1. 브라우저에서 Swagger UI 열기: `/swagger-ui.html`
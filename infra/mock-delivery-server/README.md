# mock-delivery-server

[외부 배송 서비스 연동 방법을 정리한 블로그](https://little-pecorino-c28.notion.site/30c82094ef0a804d9f33c39ab6584afe#30c82094ef0a808894d2c44d0d35bbec)내용을 참고하여, 현업에서 사용하는 것처럼 동작하는 Mock 서버를 구현하였다.  
이후 Shipping-Service에서는 이 프로젝트의 Mock API를 기반으로, 실제 송장 발급 및 배송 조회 API 연동 기능을 구현할 예정이다.


```프로젝트 패키지 구조
src/main/java/com/example/mockdelivery/
├── config/
│   └── ApiKeyProperties.java          # API 키 설정
├── controller/
│   ├── CourierController.java         # 송장 발급/취소 API
│   └── TrackingController.java        # 배송 조회 API
├── dto/
│   ├── courier/                        # 송장 관련 DTO
│   └── tracking/                       # 배송 조회 DTO
├── entity/
└── store/
```


### 백엔드 기술
* Spring Boot 3.5.10 (JDK 17)
* spring-boot-starter-web
* springdoc-openapi-starter-webmvc-ui:2.8.9 : Swagger


### REST API

REST API 명세는 다음 방법으로 확인할 수 있다.

1. 브라우저에서 Swagger UI 열기: `/swagger-ui.html`
2. 정적 문서 확인: [`openapi.json`](./openapi.json)
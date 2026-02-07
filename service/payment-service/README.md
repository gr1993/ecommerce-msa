# payment-service

payment-service는 토스페이먼츠를 이용한 결제 서비스이며, 결제 승인 및 결제 상태 관리를 담당한다.
주 저장소는 MongoDB를 사용하며, 이 서비스는 다음과 같은 기능을 제공할 예정이다.

* 결제 승인
  * 클라이언트에서 토스페이먼츠 결제창 완료 후 전달받은 파라미터로 결제 승인 처리
  * 결제 금액과 주문 금액 비교를 통한 위변조 방지
  * 토스페이먼츠 결제 승인 API 호출 (OpenFeign)
  * Basic Auth 방식의 인증 (시크릿 키 Base64 인코딩)


### 결제 승인 프로세스

1. **주문 정보 조회**: MongoDB에서 orderId로 주문 정보 조회
2. **금액 검증**: 요청된 amount와 저장된 주문 금액 비교 (위변조 방지)
3. **토스페이먼츠 승인 API 호출**: OpenFeign으로 토스페이먼츠 승인 요청
4. **주문 상태 업데이트**: 결제 승인 완료 상태로 변경


### 백엔드 기술

* Java 17
* Spring Boot 3.5.10
* Spring Data MongoDB : 주문 데이터 저장
* Spring Cloud OpenFeign : 토스페이먼츠 외부 API 호출
* springdoc-openapi 2.8.9 : OpenAPI 문서 자동 생성
* Lombok


### REST API

REST API 명세는 다음 방법으로 확인할 수 있다.

1. 브라우저에서 Swagger UI 열기: `/swagger-ui.html`

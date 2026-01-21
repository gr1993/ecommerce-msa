# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

catalog-service는 e-commerce MSA의 읽기 전용 상품 검색 서비스이다. product-service의 상품 데이터를 Elasticsearch에 동기화하여 사용자에게 검색 기능을 제공한다.

## Sub-agent Delegation Rules

- **테스트 자동화**: Java/Spring Boot 관련 비즈니스 로직(Service, Controller, Repository 등)의 구현이나 수정이 완료되면, **별도의 사용자 지시가 없어도** 즉시 `test-runner` sub-agent를 호출하여 다음을 수행한다.
    - 호출 시 수정된 파일 경로를 인자로 전달한다.
    - `test-runner`가 해당 파일에 대한 JUnit 5 테스트를 작성하거나 업데이트하도록 지시한다.
    - 테스트 실행 결과(성공/실패)를 메인 세션에 요약 보고하게 한다.
- **연속 작업**: 테스트가 실패할 경우, `test-runner`가 스스로 원인을 파악해 수정하거나 메인 에이전트에게 수정을 요청하도록 흐름을 유지한다.

## Tech Stack

- Java 17, Spring Boot 3.5.9
- Spring Data Elasticsearch 5.5.7 (Elasticsearch 9.x)
- Spring Cloud Config, Eureka Client
- Testcontainers (Elasticsearch)

## Build & Test Commands

```bash
# 빌드
./gradlew build

# 테스트 실행 (Testcontainers로 ES 자동 실행)
./gradlew test

# 단일 테스트 실행
./gradlew test --tests "ProductSearchRepositoryIntegrationTest"

# 애플리케이션 실행
./gradlew bootRun
```

## Architecture

### Data Flow
```
product-service → ProductServiceClient → ProductSyncService → Elasticsearch
```

- **ProductServiceClient**: product-service의 `/api/internal/products/sync` API 호출
- **ProductSyncService**: 페이지네이션 기반 Full Sync 구현 (기본 100건씩)
- **ProductSearchRepository**: Spring Data Elasticsearch Repository

### Elasticsearch 설정

`ProductDocument`에 정의된 인덱스 매핑:
- `productName`: nori 분석기(한글) + edge_ngram(자동완성)
- `description`: nori 분석기
- `categoryIds`: Leaf 카테고리 ID 배열

분석기 설정은 `src/main/resources/elasticsearch/settings.json`에 정의됨.

## Testing

통합 테스트는 Testcontainers로 Elasticsearch를 실행한다. `ElasticsearchTestContainerConfig`에서 nori 플러그인 설치 포함.

테스트 시 비활성화되는 설정:
```java
@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "springwolf.enabled=false"
})
```

## External Dependencies

- Config Server: `http://${CONFIG_HOST:localhost}:8888`
- Product Service: `${PRODUCT_SERVICE_URL:http://product-service}`

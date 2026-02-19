# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Mock Delivery Server는 택배 API를 시뮬레이션하는 Spring Boot 애플리케이션이다. Shipping-Service에서 송장 발급 및 배송 조회 기능을 테스트하기 위한 Mock 서버로 사용된다.

## Build & Run Commands

```bash
# 빌드 (테스트 제외)
./gradlew build -x test

# 애플리케이션 실행
./gradlew bootRun

# 테스트 실행
./gradlew test
```

## Architecture

### API 구조
- **송장 발급/취소**: `POST /api/v1/courier/orders/bulk-upload`, `POST /api/v1/courier/orders/bulk-cancel`
  - 인증: `courierAccountKey` 필드 (application.yml의 `mock-delivery.courier.account-key`)
- **배송 조회**: `POST /api/v1/trackingInfo`
  - 인증: `t_key` 필드 (application.yml의 `mock-delivery.smart-delivery.api-key`)

### 배송 상태 자동 진행
`DeliveryStatusScheduler`가 1초마다 실행되어, 마지막 상태 변경 후 10초가 경과한 송장을 다음 상태로 진행시킨다.

```
ACCEPTED → PICKED_UP → IN_TRANSIT → AT_DESTINATION → OUT_FOR_DELIVERY → DELIVERED
```

### 데이터 저장
`DeliveryOrderStore`가 `ConcurrentHashMap`을 사용하여 메모리에 송장 정보를 저장한다. 서버 재시작 시 데이터가 초기화된다.

## API Documentation

Swagger UI: `http://localhost:8080/swagger-ui.html`

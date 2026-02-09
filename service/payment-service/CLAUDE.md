# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.paymentservice.repository.OrderRepositoryTest"

# Run a single test method
./gradlew test --tests "com.example.paymentservice.repository.OrderRepositoryTest.findByOrderId"

# Run the application
./gradlew bootRun
```

## Architecture Overview

This is a **payment-service** microservice that handles payment confirmation via TossPayments API. It's part of an e-commerce MSA system.

### Core Flow
1. Receives payment confirmation request with `orderId`, `paymentKey`, and `amount`
2. Validates amount against stored order (anti-tampering)
3. If validation fails: marks order as FAILED and saves `PaymentCancelledEvent` to Outbox
4. Calls TossPayments confirm API via OpenFeign
5. Updates order status to APPROVED

### Key Patterns

**Transaction Outbox Pattern**: Events are saved to the `Outbox` collection (MongoDB) with PENDING status. `OutboxEventScheduler` polls every 1 second and publishes pending events to Kafka via `OutboxEventPublisher`.

**OpenFeign with Basic Auth**: `TossPaymentsClient` uses `TossPaymentsConfig` for Basic Auth header (Base64-encoded secret key).

### Data Storage
- **MongoDB**: Primary storage for `orders` and `outbox` collections
- **Kafka**: Event publishing (topic: `payment.cancelled`)

### Key Files
- `PaymentService.java`: Core payment confirmation logic
- `TossPaymentsClient.java`: Feign client for TossPayments API
- `OutboxEventPublisher.java`: Handles event type routing and publishing
- `OutboxEventScheduler.java`: Scheduled task for outbox polling

## Testing

Uses embedded MongoDB (`de.flapdoodle.embed.mongo`) for repository tests. Test configuration is in `src/test/resources/application.yml`.

Repository tests use `@DataMongoTest` annotation for slice testing.

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build (skip tests)
./gradlew build -x test

# Run all tests
./gradlew test

# Run single test class
./gradlew test --tests "com.example.orderservice.service.OrderServiceTest"

# Run single test method
./gradlew test --tests "com.example.orderservice.service.OrderServiceTest.createOrder_Success"

# Run application (requires config-server and MySQL)
./gradlew bootRun
```

## Architecture Overview

This is an **order-service** microservice within an e-commerce MSA system. It manages order creation and lifecycle.

### Key Patterns

**Transaction Outbox Pattern**: Orders are created with events stored in the `outbox` table within the same transaction. A scheduler (`OutboxEventScheduler`) polls pending events and publishes them to Kafka, ensuring eventual consistency without distributed transactions.

**Distributed Lock for Scheduler**: Uses MySQL `GET_LOCK()` to prevent duplicate event publishing when multiple instances run. The lock name is `order_outbox_event_publisher_lock`.

**Inter-Service Communication**: Uses OpenFeign (`ProductServiceClient`) to fetch product details from `product-service` during order creation.

### Domain Model

- `Order` → `OrderItem` (1:N) - contains product snapshot at order time
- `Order` → `OrderDelivery` (1:1) - shipping information
- `Order` → `OrderPayment` (1:N) - payment records
- `Outbox` - event outbox for reliable messaging

### Event Flow

1. `OrderServiceImpl.createOrder()` saves Order and Outbox entry atomically
2. `OutboxEventScheduler` runs every 1 second, acquires distributed lock
3. `OutboxEventPublisher` finds PENDING events and publishes to Kafka
4. Published events are marked as PUBLISHED; failed events marked as FAILED

### Package Structure

- `client/` - Feign clients for inter-service calls
- `domain/entity/` - JPA entities
- `domain/event/` - Domain event classes (e.g., `OrderCreatedEvent`)
- `global/service/outbox/` - Outbox pattern implementation
- `global/common/` - Constants including Kafka topic names

## Testing Notes

- Unit tests use Mockito with `@ExtendWith(MockitoExtension.class)`
- Repository tests use H2 in-memory database (configured via `testRuntimeOnly 'com.h2database:h2'`)
- Distributed lock cannot be tested with H2 (MySQL-specific `GET_LOCK`)

## Configuration

Application uses Spring Cloud Config. Local config server expected at `http://localhost:8888`. Override with `CONFIG_HOST` environment variable.

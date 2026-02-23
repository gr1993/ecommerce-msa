# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Test Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.shippingservice.SomeTestClass"

# Run a single test method
./gradlew test --tests "com.example.shippingservice.SomeTestClass.someTestMethod"

# Run the application locally
./gradlew bootRun

# Clean build
./gradlew clean build
```

## Architecture Overview

This is a **shipping-service** microservice in an e-commerce MSA architecture. It integrates with external courier APIs to handle shipping, returns, and exchanges.

### Key Architectural Concept

The service separates **internal shipping status** (`ShippingStatus`) from **external courier API status** (`DeliveryServiceStatus`):
- `ShippingStatus`: Our service's shipping state (READY, SHIPPING, DELIVERED, CANCELLED, RETURNED)
- `DeliveryServiceStatus`: External courier's tracking state (NOT_SENT, SENT, IN_TRANSIT, DELIVERED)

### Domain Packages

The codebase follows a domain-driven package structure:

- **shipping**: Core shipping domain - order shipping records, tracking, status management
- **returns**: Return processing - user-initiated returns, admin approval workflow
- **exchange**: Exchange processing - similar to returns but with re-shipment of new items
- **consumer**: Kafka event consumers for `order.created`, `order.cancelled`
- **client**: Feign clients for Order Service and Mock Delivery Server

### API Layer Structure

Each domain has three controller types:
- `MarketController`: User-facing APIs (`/api/shipping/...`)
- `AdminController`: Admin APIs (`/api/admin/shipping/...`)
- `InternalController`: Internal APIs for inter-service calls via Feign (`/internal/shipping/...`)

### Event-Driven Architecture

- **Transaction Outbox Pattern**: Used for publishing events
- **Idempotent Consumers**: `processed_events` table prevents duplicate processing
- **DLQ Strategy**: Failed messages are sent to Dead Letter Queue

| Direction | Events |
|-----------|--------|
| Published | `return.completed` |
| Subscribed | `order.created`, `order.cancelled` |

### External Integrations

- **Order Service**: Feign client for order data sync
- **Mock Delivery Server**: Simulates courier API (bulk upload, tracking, cancel)
- **Config Server**: Centralized configuration
- **Eureka**: Service discovery

### Scheduler

`ShippingTrackingScheduler` periodically polls the Mock Delivery Server to update shipment tracking status.

## API Documentation

- Swagger UI: `/swagger-ui.html`
- AsyncAPI (Kafka events): `/springwolf/asyncapi-ui.html`

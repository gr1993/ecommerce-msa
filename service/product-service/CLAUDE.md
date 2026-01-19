# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests "com.example.productservice.product.service.ProductServiceImplTest"

# Run a specific test method
./gradlew test --tests "com.example.productservice.product.integration.ProductIntegrationTest.createProductWithFileUpload_success"

# Clean build
./gradlew clean build
```

## Architecture Overview

This is a **Product Service** microservice within an e-commerce MSA system. It uses Spring Boot 3.5 with Java 17.

### Infrastructure Dependencies
- **Spring Cloud Config**: Configuration is fetched from an external config server (`CONFIG_HOST:8888`)
- **Eureka Client**: Service discovery registration
- **Kafka**: Event messaging (Springwolf for AsyncAPI documentation)
- **MySQL**: Production database (H2 for tests)

### Domain Structure

The codebase follows a domain-driven package structure under `com.example.productservice`:

- **product**: Core product domain with Option/SKU variant management
  - Product → ProductOptionGroup → ProductOptionValue (hierarchical options)
  - Product → ProductSku → ProductSkuOption (variant pricing/inventory)
  - Product → ProductImage (media assets)

- **category**: Hierarchical category management (self-referential parent-child)

- **file**: File upload handling with temp→confirmed lifecycle
  - FileCleanupScheduler runs daily at 3AM to delete orphan files

- **global**: Cross-cutting concerns (OpenApiConfig, PageResponse DTO)

### API Patterns

All admin APIs are prefixed with `/api/admin/`:
- `/api/admin/products` - Product CRUD with file upload
- `/api/admin/categories` - Category tree CRUD

OpenAPI documentation is available via springdoc-openapi (`/swagger-ui.html`).

## Testing

Tests use H2 in-memory database with `@ActiveProfiles("test")`. The test configuration:
- Disables Spring Cloud Config
- Disables Springwolf async documentation
- Uses `create-drop` DDL strategy

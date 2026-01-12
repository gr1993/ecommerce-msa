# Swagger Documentation Endpoints

## API Gateway

**Production Gateway URL**: `http://localhost:8080`

All API requests from the frontend MUST go through the API Gateway. Never call microservices directly.

## Microservices Swagger Documentation URLs

| Service | Swagger Docs URL | Port | Description |
|---------|------------------|------|-------------|
| auth-service | http://localhost:8081/api-docs | 8081 | Authentication, JWT token issuance/validation |
| user-service | http://localhost:8082/api-docs | 8082 | User profile, member management, points, coupons |
| product-service | http://localhost:8083/api-docs | 8083 | Product management (admin), SKU, inventory |
| catalog-service | http://localhost:8084/api-docs | 8084 | Product catalog (customer-facing), search, display |
| order-service | http://localhost:8085/api-docs | 8085 | Order creation, order status management |
| payment-service | http://localhost:8086/api-docs | 8086 | Payment processing, PG integration |
| promotion-service | http://localhost:8087/api-docs | 8087 | Discounts, coupons, promotion policies |
| delivery-service | http://localhost:8088/api-docs | 8088 | Shipping, delivery tracking |
| settlement-service | http://localhost:8089/api-docs | 8089 | Settlement, revenue statistics |
| return-service | http://localhost:8090/api-docs | 8090 | Returns, exchanges, refunds |

## Service Discovery Workflow

1. User requests API integration for a specific feature or endpoint
2. If user specifies service name and endpoint → fetch that service's Swagger docs directly
3. If user describes functionality → determine which service(s) likely handle it:
   - Authentication, login → **auth-service**
   - User info, profile, member data → **user-service**
   - Admin product CRUD → **product-service**
   - Customer product browsing, search → **catalog-service**
   - Cart, orders → **order-service**
   - Payment processing → **payment-service**
   - Discounts, coupons → **promotion-service**
   - Shipping status → **delivery-service**
   - Settlement, statistics → **settlement-service**
   - Returns, exchanges → **return-service**
4. Fetch the Swagger documentation from the appropriate service
5. Generate TypeScript API functions that call through the Gateway (localhost:8080)

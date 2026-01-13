---
name: api-integrator
description: Generate TypeScript API integration code by fetching Swagger documentation from microservices and creating fully-typed fetch-based API functions that call through the API Gateway. Use when the user requests API integration, wants to connect to backend endpoints, asks to "integrate [service] API", "create API function for [feature]", "connect to [endpoint]", or needs TypeScript API client code for the MSA ecommerce backend services.
---

# API Integrator

Generates TypeScript API integration code for the MSA ecommerce backend by fetching Swagger documentation and creating type-safe API functions.

## Workflow

### 1. Identify the Target Service

**If user specifies service and endpoint:**
- User says: "Integrate user-service GET /users/{id}"
- Go directly to step 2 with known service

**If user describes functionality:**
- User says: "Create API to fetch user profile" or "I need to add product to cart"
- Determine which service handles the feature using [swagger-endpoints.md](references/swagger-endpoints.md)
- Common mappings:
  - Login/auth → auth-service
  - User profile → user-service
  - Product CRUD (admin) → product-service
  - Product browsing → catalog-service
  - Orders/cart → order-service
  - Payment → payment-service
  - Coupons/discounts → promotion-service

### 2. Fetch and Cache Swagger Documentation

**IMPORTANT**: Since Swagger docs are served from localhost and cannot be accessed directly by LLM, we must fetch and cache them locally first.

**Step 2a: Check for existing Swagger documentation**
- Check if `swagger-docs/{service-name}-swagger.json` exists
- If it exists and is recent, use it directly (skip to step 3)

**Step 2b: Fetch Swagger documentation using bash script**
- Run: `bash fetch-swagger.sh {service-name}`
- Example: `bash fetch-swagger.sh user-service`
- This script will:
  1. Fetch the service-specific Swagger spec from `http://localhost:{port}/api-docs/{service-name}`
  2. Save it to `swagger-docs/{service-name}-swagger.json`

**Step 2c: Read the Swagger documentation**
- Use the Read tool to read `swagger-docs/{service-name}-swagger.json`
- Parse the Swagger spec to extract:
  - HTTP method (GET, POST, PUT, DELETE)
  - Endpoint path
  - Request parameters (path, query, body)
  - Request body schema
  - Response schema
  - Security requirements (authentication needed?)

### 3. Generate TypeScript API Function

Create the API function following patterns in [api-patterns.md](references/api-patterns.md):

**Key elements:**
1. **TypeScript Interfaces**: Generate from Swagger schemas
   - Request interface (if body/params exist)
   - Response interface

2. **Function Signature**: Type parameters and return value
   ```typescript
   export const functionName = async (params: RequestType): Promise<ResponseType>
   ```

3. **Gateway URL**: Always use `http://localhost:8080` + endpoint path

4. **Authentication**: Determine token type
   - Admin endpoints (`/admin/*`) → `localStorage.getItem('adminToken')`
   - User endpoints (authenticated) → `localStorage.getItem('userToken')`
   - Public endpoints → no token

5. **Error Handling**: Wrap in try-catch, check response.ok, parse errors

6. **HTTP Method**: Match Swagger spec (GET, POST, PUT, DELETE, PATCH)

### 4. Save to File

**Default location:** `src/api/[serviceName]Api.ts`
- Example: `src/api/userApi.ts` for user-service endpoints
- If user specifies a different path, use that instead
- If the file already exists, add the new function to it (don't overwrite)

## Examples

### Example 1: Feature-Based Request

**User request:** "I need an API function to get user profile information"

**Process:**
1. Identify service: user-service (handles user profiles)
2. Check if `swagger-docs/user-service-swagger.json` exists
3. If not, run: `bash fetch-swagger.sh user-service`
4. Read `swagger-docs/user-service-swagger.json`
5. Find relevant endpoint (e.g., GET /users/{userId})
6. Generate TypeScript function with:
   - Interface for User response
   - Function that calls `http://localhost:8080/users/{userId}`
   - Uses `userToken` from localStorage
7. Save to `src/api/userApi.ts`

### Example 2: Specific Endpoint Request

**User request:** "Integrate the product-service POST /products endpoint"

**Process:**
1. Service is known: product-service
2. Check if `swagger-docs/product-service-swagger.json` exists
3. If not, run: `bash fetch-swagger.sh product-service`
4. Read `swagger-docs/product-service-swagger.json`
5. Extract POST /products schema
6. Generate function with:
   - CreateProductRequest interface
   - CreateProductResponse interface
   - Calls `http://localhost:8080/products`
   - Uses `adminToken` (admin operation)
7. Save to `src/api/productApi.ts`

## Reference Files

- **[swagger-endpoints.md](references/swagger-endpoints.md)** - Complete list of microservice Swagger documentation URLs and service-to-feature mappings
- **[api-patterns.md](references/api-patterns.md)** - Detailed TypeScript patterns, examples for GET/POST/PUT/DELETE, token handling, error handling

## Important Notes

- **Always use API Gateway**: All API calls must go through `http://localhost:8080`, never call services directly
- **Two token types**: Market users (`userToken`) and admin users (`adminToken`) have separate authentication
- **Type safety**: Generate interfaces from Swagger schemas - don't guess types
- **File organization**: Group functions by service (userApi.ts, productApi.ts, etc.)
- **Error messages**: Provide user-friendly error messages, especially for auth failures

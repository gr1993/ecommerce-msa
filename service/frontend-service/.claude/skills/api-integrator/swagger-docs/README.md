# Swagger Documentation Cache

This directory contains cached Swagger/OpenAPI documentation from the MSA microservices.

## Why Cache Swagger Docs?

The Swagger documentation endpoints (`/api-docs`) are served from localhost, which LLMs cannot access directly. To work around this limitation, we fetch and cache the Swagger specs locally as JSON files.

## Directory Structure

```
swagger-docs/
├── README.md                          # This file
├── {service-name}-swagger.json        # Full Swagger spec from service
└── {service-name}-filtered.json       # Filtered spec (only relevant endpoints)
```

## Fetching Swagger Documentation

Use the `fetch-swagger.sh` script (or directly call `fetch-swagger.cjs`) to fetch and cache Swagger docs:

```bash
# From the api-integrator skill directory
bash fetch-swagger.sh <service-name>

# Or call Node.js script directly:
node fetch-swagger.cjs <service-name>

# Examples:
bash fetch-swagger.sh user-service
node fetch-swagger.cjs product-service
node fetch-swagger.cjs order-service
```

### Available Services

- `auth-service` (port 8081)
- `user-service` (port 8082)
- `product-service` (port 8083)
- `catalog-service` (port 8084)
- `order-service` (port 8085)
- `payment-service` (port 8086)
- `promotion-service` (port 8087)
- `delivery-service` (port 8088)
- `settlement-service` (port 8089)
- `return-service` (port 8090)

## File Types

### Full Swagger File (`{service-name}-swagger.json`)

Contains the complete OpenAPI specification from the service, including:
- All endpoints (including internal/admin endpoints)
- All schemas and components
- Complete request/response definitions

### Filtered Swagger File (`{service-name}-filtered.json`)

Contains only endpoints relevant to the service's primary domain:
- Endpoints with paths containing the service tag (e.g., `/user` for user-service)
- Endpoints with operation tags matching the service
- Same schema/component definitions as the full file

**This is the file you should use for API integration** to avoid cluttering with irrelevant endpoints.

## Workflow Integration

When the api-integrator skill is invoked:

1. **Check for cached file**: Look for `{service-name}-filtered.json`
2. **If missing**: Run `fetch-swagger.sh {service-name}` to fetch and cache
3. **Read the filtered file**: Use it to generate TypeScript API functions
4. **Generate code**: Create type-safe API integration code

## Updating Cached Docs

Swagger documentation should be refreshed when:
- Backend API endpoints change
- New fields are added to request/response schemas
- Service contract is updated

To refresh, simply run the fetch script again:

```bash
bash fetch-swagger.sh user-service
```

This will overwrite the existing cached files with fresh data.

## Filtering Logic

The filtering process (in `fetch-swagger.cjs`) uses **exact tag matching** to identify relevant endpoints:

- **user-service** → only endpoints with `tags: ["User"]`
- **auth-service** → only endpoints with `tags: ["Auth"]`
- **product-service** → only endpoints with `tags: ["Product"]`
- **order-service** → only endpoints with `tags: ["Order"]`
- etc.

**How it works:**
1. Iterates through all paths in the Swagger spec
2. For each HTTP operation (GET, POST, PUT, DELETE, etc.), checks the `tags` array
3. Only includes operations where the tag **exactly matches** the service tag (case-sensitive)
4. Excludes internal endpoints like Springwolf, Actuator, etc.

This ensures that when generating API code for a specific service, we only see endpoints actually provided by that service's domain logic.

## Prerequisites

The fetch script requires:
- **Node.js**: For running the fetch script (already available in frontend projects)

No additional dependencies needed! The script uses Node.js built-in modules only.

## Troubleshooting

### "Failed to fetch Swagger documentation"

**Cause**: The microservice is not running or not accessible on the expected port.

**Solution**:
1. Check if the service is running: `curl http://localhost:{port}/api-docs`
2. Start the service if it's not running
3. Verify the port mapping in `fetch-swagger.cjs` matches your service configuration

### Empty filtered file (0 endpoints)

**Cause**: The filtering logic didn't find any endpoints with the expected tag.

**Solution**:
- Check the service tag mapping in `fetch-swagger.cjs` (SERVICE_TAGS constant)
- Verify the backend service is using the correct tag in its Swagger annotations
- Use the full swagger file instead: `{service-name}-swagger.json`
- Example: If user-service endpoints have `@Tag(name = "User")`, they will be included

### Node.js version issues

**Cause**: Using an older Node.js version that doesn't support the required features.

**Solution**:
- The script requires Node.js 12+ (uses built-in http module)
- Check your version: `node --version`
- Update Node.js if needed

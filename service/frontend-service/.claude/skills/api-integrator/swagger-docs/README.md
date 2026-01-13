# Swagger Documentation Cache

This directory contains cached Swagger/OpenAPI documentation from the MSA microservices.

## Why Cache Swagger Docs?

The Swagger documentation endpoints (`/api-docs`) are served from localhost, which LLMs cannot access directly. To work around this limitation, we fetch and cache the Swagger specs locally as JSON files.

## Directory Structure

```
swagger-docs/
├── README.md                          # This file
└── {service-name}-swagger.json        # Service-specific Swagger spec
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

## File Format

### Service Swagger File (`{service-name}-swagger.json`)

Contains the service-specific OpenAPI specification fetched from `http://localhost:{port}/api-docs/{service-name}`:
- Only endpoints relevant to the service's primary domain
- All schemas and components
- Complete request/response definitions

The backend API Gateway provides filtered documentation per service, so no client-side filtering is needed.

## Workflow Integration

When the api-integrator skill is invoked:

1. **Check for cached file**: Look for `{service-name}-swagger.json`
2. **If missing**: Run `fetch-swagger.sh {service-name}` to fetch and cache
3. **Read the swagger file**: Use it to generate TypeScript API functions
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

## Prerequisites

The fetch script requires:
- **Node.js**: For running the fetch script (already available in frontend projects)

No additional dependencies needed! The script uses Node.js built-in modules only.

## Troubleshooting

### "Failed to fetch Swagger documentation"

**Cause**: The microservice is not running or not accessible on the expected port.

**Solution**:
1. Check if the service is running: `curl http://localhost:{port}/api-docs/{service-name}`
2. Start the service if it's not running
3. Verify the port mapping in `fetch-swagger.cjs` matches your service configuration

### Node.js version issues

**Cause**: Using an older Node.js version that doesn't support the required features.

**Solution**:
- The script requires Node.js 12+ (uses built-in http module)
- Check your version: `node --version`
- Update Node.js if needed

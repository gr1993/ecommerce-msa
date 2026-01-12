#!/usr/bin/env node

/**
 * Fetch and filter Swagger documentation for MSA services
 *
 * Usage: node fetch-swagger.js <service-name>
 * Example: node fetch-swagger.js user-service
 */

const fs = require('fs');
const path = require('path');
const https = require('http');

// Service port mapping
const SERVICE_PORTS = {
  'auth-service': 8081,
  'user-service': 8082,
  'product-service': 8083,
  'catalog-service': 8084,
  'order-service': 8085,
  'payment-service': 8086,
  'promotion-service': 8087,
  'delivery-service': 8088,
  'settlement-service': 8089,
  'return-service': 8090,
};

// Service tag mapping for filtering (exact match in tags array)
const SERVICE_TAGS = {
  'auth-service': 'Auth',
  'user-service': 'User',
  'product-service': 'Product',
  'catalog-service': 'Catalog',
  'order-service': 'Order',
  'payment-service': 'Payment',
  'promotion-service': 'Promotion',
  'delivery-service': 'Delivery',
  'settlement-service': 'Settlement',
  'return-service': 'Return',
};

// Parse command line arguments
const serviceName = process.argv[2];

if (!serviceName) {
  console.error('Usage: node fetch-swagger.js <service-name>');
  console.error('Available services:', Object.keys(SERVICE_PORTS).join(', '));
  process.exit(1);
}

if (!SERVICE_PORTS[serviceName]) {
  console.error(`Error: Unknown service '${serviceName}'`);
  console.error('Available services:', Object.keys(SERVICE_PORTS).join(', '));
  process.exit(1);
}

const port = SERVICE_PORTS[serviceName];
const tag = SERVICE_TAGS[serviceName];
const swaggerUrl = `http://localhost:${port}/api-docs`;
const swaggerDocsDir = path.join(__dirname, 'swagger-docs');
const outputFile = path.join(swaggerDocsDir, `${serviceName}-swagger.json`);
const filteredFile = path.join(swaggerDocsDir, `${serviceName}-filtered.json`);

// Ensure swagger-docs directory exists
if (!fs.existsSync(swaggerDocsDir)) {
  fs.mkdirSync(swaggerDocsDir, { recursive: true });
}

console.log(`Fetching Swagger documentation for ${serviceName} from ${swaggerUrl}...`);

// Fetch Swagger documentation
https.get(swaggerUrl, (res) => {
  let data = '';

  res.on('data', (chunk) => {
    data += chunk;
  });

  res.on('end', () => {
    if (res.statusCode !== 200) {
      console.error(`Error: Failed to fetch Swagger documentation (HTTP ${res.statusCode})`);
      console.error(`Make sure the ${serviceName} is running on port ${port}`);
      process.exit(1);
    }

    try {
      const swagger = JSON.parse(data);

      // Save full Swagger documentation
      fs.writeFileSync(outputFile, JSON.stringify(swagger, null, 2));
      console.log(`✓ Successfully saved full Swagger docs to: ${outputFile}`);

      // Filter endpoints by tag
      console.log(`\nFiltering endpoints for tag: "${tag}"`);
      const filtered = filterSwaggerByTag(swagger, tag);

      // Save filtered Swagger documentation
      fs.writeFileSync(filteredFile, JSON.stringify(filtered, null, 2));
      console.log(`✓ Successfully created filtered Swagger docs: ${filteredFile}`);

      // Print summary
      const totalPaths = Object.keys(swagger.paths || {}).length;
      const filteredPaths = Object.keys(filtered.paths || {}).length;
      console.log('\n=== Summary ===');
      console.log(`Service: ${serviceName}`);
      console.log(`Port: ${port}`);
      console.log(`Filter tag: ${tag}`);
      console.log(`Total endpoints: ${totalPaths}`);
      console.log(`Filtered endpoints: ${filteredPaths}`);
      console.log(`Full docs: ${outputFile}`);
      console.log(`Filtered docs: ${filteredFile}`);
      console.log('\nYou can now reference these files in the API integration workflow.');
    } catch (error) {
      console.error('Error: Failed to parse Swagger JSON:', error.message);
      process.exit(1);
    }
  });
}).on('error', (error) => {
  console.error(`Error: Failed to fetch Swagger documentation from ${swaggerUrl}`);
  console.error(error.message);
  console.error(`\nMake sure the ${serviceName} is running on port ${port}`);
  process.exit(1);
});

/**
 * Filter Swagger spec to only include endpoints with specific tag
 *
 * @param {Object} swagger - Full Swagger/OpenAPI spec
 * @param {string} targetTag - Tag to filter by (exact match)
 * @returns {Object} Filtered Swagger spec
 */
function filterSwaggerByTag(swagger, targetTag) {
  const filtered = {
    openapi: swagger.openapi,
    info: swagger.info,
    servers: swagger.servers,
    tags: swagger.tags,
    paths: {},
    components: swagger.components,
  };

  // Filter paths based on operation tags
  for (const [pathKey, pathItem] of Object.entries(swagger.paths || {})) {
    const filteredPathItem = {};
    let hasMatchingOperation = false;

    // Check each HTTP method (get, post, put, delete, patch, etc.)
    for (const [method, operation] of Object.entries(pathItem)) {
      // Skip non-operation fields like 'parameters', 'summary', etc.
      if (typeof operation !== 'object' || !operation.tags) {
        continue;
      }

      // Check if operation has the target tag (exact match, case-sensitive)
      if (Array.isArray(operation.tags) && operation.tags.includes(targetTag)) {
        filteredPathItem[method] = operation;
        hasMatchingOperation = true;
      }
    }

    // Only include path if it has at least one matching operation
    if (hasMatchingOperation) {
      // Copy non-operation fields (like parameters)
      for (const [key, value] of Object.entries(pathItem)) {
        if (!filteredPathItem[key] && typeof value === 'object') {
          filteredPathItem[key] = value;
        }
      }
      filtered.paths[pathKey] = filteredPathItem;
    }
  }

  return filtered;
}

#!/usr/bin/env node

/**
 * Fetch Swagger documentation for MSA services
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
const swaggerUrl = `http://localhost:${port}/api-docs/${serviceName}`;
const swaggerDocsDir = path.join(__dirname, 'swagger-docs');
const outputFile = path.join(swaggerDocsDir, `${serviceName}-swagger.json`);

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

      // Save Swagger documentation
      fs.writeFileSync(outputFile, JSON.stringify(swagger, null, 2));
      console.log(`✓ Successfully saved Swagger docs to: ${outputFile}`);

      // Print summary
      const totalPaths = Object.keys(swagger.paths || {}).length;
      console.log('\n=== Summary ===');
      console.log(`Service: ${serviceName}`);
      console.log(`Port: ${port}`);
      console.log(`Total endpoints: ${totalPaths}`);
      console.log(`Swagger docs: ${outputFile}`);
      console.log('\nYou can now reference this file in the API integration workflow.');
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

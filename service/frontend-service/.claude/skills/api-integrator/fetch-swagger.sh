#!/bin/bash

# Fetch and filter Swagger documentation for MSA services
# Usage: ./fetch-swagger.sh <service-name>
# Example: ./fetch-swagger.sh user-service

# This is a wrapper script that calls the Node.js fetcher
# Node.js provides better JSON processing than bash+jq

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Call the Node.js script
node "$SCRIPT_DIR/fetch-swagger.cjs" "$@"

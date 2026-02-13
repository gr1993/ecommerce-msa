/**
 * Environment Configuration
 *
 * Centralized environment variables using Vite
 */

/**
 * API Gateway Base URL
 * @default http://localhost:8080
 */
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

/**
 * Product File URL (static resources)
 * @default http://localhost:8080/product
 */
export const PRODUCT_FILE_URL = import.meta.env.VITE_PRODUCT_FILE_URL ?? 'http://localhost:8080/product'

/**
 * 인증 비활성화 여부 (local 모드에서 사용)
 * npm run local 실행 시 true
 * @default false
 */
export const AUTH_DISABLED = import.meta.env.VITE_AUTH_DISABLED === 'true'

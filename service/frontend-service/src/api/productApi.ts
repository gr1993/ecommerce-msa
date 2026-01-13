/**
 * Product Service API Integration
 *
 * Type-safe API functions for product-service endpoints
 * All requests go through API Gateway (http://localhost:8080)
 */

import { useAuthStore } from '../stores/authStore'

// ==================== Interfaces ====================

/**
 * 상품 응답 DTO
 */
export interface ProductResponse {
  /** 상품 ID */
  productId: number
  /** 상품명 */
  productName: string
  /** 상품 코드 */
  productCode?: string
  /** 상품 설명 */
  description?: string
  /** 기본 가격 */
  basePrice: number
  /** 할인 가격 */
  salePrice?: number
  /** 상품 상태 */
  status: 'ACTIVE' | 'INACTIVE' | 'SOLD_OUT'
  /** 진열 여부 */
  isDisplayed: boolean
  /** 대표 이미지 URL */
  primaryImageUrl?: string
  /** 총 재고 수량 */
  totalStockQty: number
  /** 생성일시 */
  createdAt: string
  /** 수정일시 */
  updatedAt: string
}

/**
 * 페이지네이션 응답 DTO
 */
export interface PageResponse<T> {
  /** 데이터 목록 */
  content: T[]
  /** 현재 페이지 번호 (0부터 시작) */
  page: number
  /** 페이지 크기 */
  size: number
  /** 전체 데이터 개수 */
  totalElements: number
  /** 전체 페이지 수 */
  totalPages: number
  /** 첫 페이지 여부 */
  first: boolean
  /** 마지막 페이지 여부 */
  last: boolean
}

/**
 * 상품 목록 조회 요청 파라미터
 */
export interface SearchProductsParams {
  /** 상품명 (부분 검색) */
  productName?: string
  /** 상품 코드 */
  productCode?: string
  /** 상품 상태 (ACTIVE, INACTIVE, SOLD_OUT) */
  status?: 'ACTIVE' | 'INACTIVE' | 'SOLD_OUT'
  /** 진열 여부 */
  isDisplayed?: boolean
  /** 최소 가격 */
  minPrice?: number
  /** 최대 가격 */
  maxPrice?: number
  /** 페이지 번호 (0부터 시작) */
  page?: number
  /** 페이지 크기 */
  size?: number
  /** 정렬 기준 (예: createdAt,desc) */
  sort?: string
}

// ==================== API Functions ====================

/**
 * 상품 목록 조회 (페이지네이션 + 검색)
 *
 * 페이지네이션과 검색 필터를 적용하여 상품 목록을 조회합니다. (관리자 전용)
 *
 * @param params - 검색 및 페이지네이션 파라미터
 * @returns 페이지네이션된 상품 목록
 * @throws Error - 조회 실패 시 (인증 실패, 권한 없음 등)
 *
 * @example
 * ```typescript
 * const result = await searchProducts({
 *   productName: '나이키',
 *   status: 'ACTIVE',
 *   page: 0,
 *   size: 10
 * })
 * console.log('Products:', result.content)
 * console.log('Total:', result.totalElements)
 * ```
 */
export const searchProducts = async (params?: SearchProductsParams): Promise<PageResponse<ProductResponse>> => {
  try {
    // Build query parameters
    const queryParams = new URLSearchParams()

    if (params?.productName) {
      queryParams.append('productName', params.productName)
    }
    if (params?.productCode) {
      queryParams.append('productCode', params.productCode)
    }
    if (params?.status) {
      queryParams.append('status', params.status)
    }
    if (params?.isDisplayed !== undefined) {
      queryParams.append('isDisplayed', params.isDisplayed.toString())
    }
    if (params?.minPrice !== undefined) {
      queryParams.append('minPrice', params.minPrice.toString())
    }
    if (params?.maxPrice !== undefined) {
      queryParams.append('maxPrice', params.maxPrice.toString())
    }
    if (params?.page !== undefined) {
      queryParams.append('page', params.page.toString())
    }
    if (params?.size !== undefined) {
      queryParams.append('size', params.size.toString())
    }
    if (params?.sort) {
      queryParams.append('sort', params.sort)
    }

    const queryString = queryParams.toString()
    const url = `http://localhost:8080/api/admin/products${queryString ? `?${queryString}` : ''}`

    // Get admin token from Zustand store
    const adminToken = useAuthStore.getState().adminToken
    if (!adminToken) {
      throw new Error('관리자 인증이 필요합니다. 다시 로그인해주세요.')
    }

    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${adminToken}`,
      },
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Unknown error' }))

      // Handle specific error cases
      if (response.status === 401) {
        throw new Error(error.message || '인증이 만료되었습니다. 다시 로그인해주세요.')
      }
      if (response.status === 403) {
        throw new Error(error.message || '접근 권한이 없습니다.')
      }

      throw new Error(error.message || `상품 목록 조회 실패 (HTTP ${response.status})`)
    }

    const data: PageResponse<ProductResponse> = await response.json()
    return data
  } catch (error) {
    console.error('Search products error:', error)

    // Re-throw if it's already our custom error
    if (error instanceof Error) {
      throw error
    }

    // Network or unexpected errors
    throw new Error('상품 목록 조회 중 오류가 발생했습니다. 네트워크 연결을 확인해주세요.')
  }
}

/**
 * Catalog Service API Integration
 *
 * Type-safe API functions for catalog-service endpoints (사용자용)
 * All requests go through API Gateway
 */

import { API_BASE_URL } from '../config/env'

// ==================== Interfaces ====================

/**
 * 사용자용 상품 응답 DTO (Catalog API)
 */
export interface CatalogProductResponse {
  /** 상품 ID */
  productId: string
  /** 상품명 */
  productName: string
  /** 상품 설명 */
  description?: string
  /** 기본 가격 */
  basePrice: number
  /** 할인 가격 */
  salePrice?: number
  /** 상품 상태 */
  status: string
  /** 대표 이미지 URL */
  primaryImageUrl?: string
  /** 카테고리 ID 목록 */
  categoryIds?: number[]
  /** 생성일시 */
  createdAt: string
  /** 수정일시 */
  updatedAt: string
}

/**
 * 사용자용 상품 목록 조회 요청 파라미터 (Catalog API)
 */
export interface CatalogSearchProductsParams {
  /** 상품명 (부분 검색) */
  productName?: string
  /** 카테고리 ID */
  categoryId?: number
  /** 상품 상태 (ACTIVE, INACTIVE, SOLD_OUT) */
  status?: string
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

// ---- Product Detail Types ----

export interface OptionValueResponse {
  id: number
  optionValueName: string
  displayOrder: number
}

export interface OptionGroupResponse {
  id: number
  optionGroupName: string
  displayOrder: number
  optionValues: OptionValueResponse[]
}

export interface SkuResponse {
  id: number
  skuCode: string
  price: number
  stockQty: number
  status: string
  optionValueIds: number[]
}

export interface ImageResponse {
  id: number
  fileId: number
  imageUrl: string
  isPrimary: boolean
  displayOrder: number
}

export interface CategoryResponse {
  categoryId: number
  categoryName: string
  displayOrder: number
}

export interface ProductDetailResponse {
  productId: number
  productName: string
  productCode: string
  description: string
  basePrice: number
  salePrice: number | null
  status: string
  isDisplayed: boolean
  optionGroups: OptionGroupResponse[]
  skus: SkuResponse[]
  images: ImageResponse[]
  categories: CategoryResponse[]
  createdAt: string
  updatedAt: string
}

// ==================== API Functions ====================

/**
 * 사용자용 상품 목록 조회
 *
 * 검색 필터와 페이지네이션을 적용하여 상품 목록을 조회합니다. (인증 불필요)
 *
 * @param params - 검색 및 페이지네이션 파라미터
 * @returns 페이지네이션된 상품 목록
 * @throws Error - 조회 실패 시
 */
export const getCatalogProducts = async (params?: CatalogSearchProductsParams): Promise<PageResponse<CatalogProductResponse>> => {
  try {
    const queryParams = new URLSearchParams()

    if (params?.productName) {
      queryParams.append('productName', params.productName)
    }
    if (params?.categoryId !== undefined) {
      queryParams.append('categoryId', params.categoryId.toString())
    }
    if (params?.status) {
      queryParams.append('status', params.status)
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
    const url = `${API_BASE_URL}/api/catalog/products${queryString ? `?${queryString}` : ''}`

    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Unknown error' }))
      throw new Error(error.message || `상품 목록 조회 실패 (HTTP ${response.status})`)
    }

    const data: PageResponse<CatalogProductResponse> = await response.json()
    return data
  } catch (error) {
    console.error('Get catalog products error:', error)

    if (error instanceof Error) {
      throw error
    }

    throw new Error('상품 목록 조회 중 오류가 발생했습니다. 네트워크 연결을 확인해주세요.')
  }
}

/**
 * 상품명 자동완성
 *
 * 입력된 키워드로 시작하는 상품명을 최대 5개까지 반환합니다. (인증 불필요)
 *
 * @param keyword - 검색 키워드
 * @returns 자동완성 상품명 목록 (최대 5개)
 * @throws Error - 조회 실패 시
 */
export const autocompleteProductName = async (keyword: string): Promise<string[]> => {
  try {
    if (!keyword || keyword.trim().length === 0) {
      return []
    }

    const queryParams = new URLSearchParams()
    queryParams.append('keyword', keyword)

    const url = `${API_BASE_URL}/api/catalog/products/autocomplete?${queryParams.toString()}`

    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Unknown error' }))
      throw new Error(error.message || `자동완성 조회 실패 (HTTP ${response.status})`)
    }

    const data: string[] = await response.json()
    return data
  } catch (error) {
    console.error('Autocomplete product name error:', error)

    if (error instanceof Error) {
      throw error
    }

    throw new Error('자동완성 조회 중 오류가 발생했습니다.')
  }
}

/**
 * 상품 상세 조회
 *
 * 상품 ID로 상품의 상세 정보를 조회합니다. 옵션 그룹, SKU, 이미지 정보를 포함합니다. (인증 불필요)
 *
 * @param productId - 상품 ID
 * @returns 상품 상세 정보
 * @throws Error - 조회 실패 시
 */
export const getProductDetail = async (productId: number): Promise<ProductDetailResponse> => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/catalog/products/${productId}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    })

    if (!response.ok) {
      throw new Error(`Failed to fetch product detail: ${response.status}`)
    }

    const data: ProductDetailResponse = await response.json()
    return data
  } catch (error) {
    console.error('Get product detail error:', error)
    throw error
  }
}

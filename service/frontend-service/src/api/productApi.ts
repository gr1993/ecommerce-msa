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

/**
 * 옵션 값 요청 DTO
 */
export interface OptionValueRequest {
  /** 프론트 임시 ID (매핑용) */
  id: string
  /** 옵션 값명 */
  optionValueName: string
  /** 정렬 순서 */
  displayOrder?: number
}

/**
 * 옵션 그룹 요청 DTO
 */
export interface OptionGroupRequest {
  /** 프론트 임시 ID (매핑용) */
  id: string
  /** 옵션 그룹명 */
  optionGroupName: string
  /** 정렬 순서 */
  displayOrder?: number
  /** 옵션 값 목록 */
  optionValues: OptionValueRequest[]
}

/**
 * SKU 요청 DTO
 */
export interface SkuRequest {
  /** 프론트 임시 ID (매핑용) */
  id: string
  /** SKU 코드 */
  skuCode: string
  /** 가격 */
  price: number
  /** 재고 수량 */
  stockQty: number
  /** 상태 (ACTIVE, SOLD_OUT, INACTIVE) */
  status: string
  /** 옵션 값 ID 목록 (프론트 임시 ID) */
  optionValueIds?: string[]
}

/**
 * 상품 이미지 요청 DTO
 */
export interface ProductImageRequest {
  /** 프론트 임시 ID (매핑용) */
  id: string
  /** 파일 업로드 ID */
  fileId?: number
  /** 이미지 URL (임시 URL) */
  imageUrl: string
  /** 대표 이미지 여부 */
  isPrimary?: boolean
  /** 정렬 순서 */
  displayOrder?: number
}

/**
 * 상품 생성 요청 DTO
 */
export interface ProductCreateRequest {
  /** 상품명 */
  productName: string
  /** 상품 코드 */
  productCode?: string
  /** 상품 상세 설명 */
  description?: string
  /** 기본 가격 */
  basePrice: number
  /** 할인 가격 */
  salePrice?: number
  /** 상품 상태 (ACTIVE, INACTIVE, SOLD_OUT) */
  status: string
  /** 진열 여부 */
  isDisplayed?: boolean
  /** 옵션 그룹 목록 */
  optionGroups?: OptionGroupRequest[]
  /** SKU 목록 */
  skus?: SkuRequest[]
  /** 이미지 목록 */
  images?: ProductImageRequest[]
}

/**
 * 파일 업로드 응답 DTO
 */
export interface FileUploadResponse {
  /** 파일 ID */
  fileId: number
  /** 원본 파일명 */
  originalFilename: string
  /** 파일 URL */
  url: string
  /** 파일 크기 (bytes) */
  fileSize: number
  /** 파일 타입 */
  contentType: string
  /** 상태 */
  status: string
  /** 업로드 일시 */
  uploadedAt: string
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

/**
 * 파일 업로드
 *
 * 상품 이미지를 임시 저장합니다. 상품 등록 시 확정됩니다. (관리자 전용)
 *
 * @param file - 업로드할 파일
 * @returns 업로드된 파일 정보
 * @throws Error - 업로드 실패 시 (인증 실패, 파일 형식 오류 등)
 *
 * @example
 * ```typescript
 * const result = await uploadProductImage(file)
 * console.log('File ID:', result.fileId)
 * console.log('URL:', result.url)
 * ```
 */
export const uploadProductImage = async (file: File): Promise<FileUploadResponse> => {
  try {
    // Get admin token from Zustand store
    const adminToken = useAuthStore.getState().adminToken
    if (!adminToken) {
      throw new Error('관리자 인증이 필요합니다. 다시 로그인해주세요.')
    }

    const formData = new FormData()
    formData.append('file', file)

    const response = await fetch('http://localhost:8080/api/admin/products/files/upload', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${adminToken}`,
      },
      body: formData,
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
      if (response.status === 400) {
        throw new Error(error.message || '잘못된 파일 형식입니다.')
      }

      throw new Error(error.message || `파일 업로드 실패 (HTTP ${response.status})`)
    }

    const data: FileUploadResponse = await response.json()
    return data
  } catch (error) {
    console.error('Upload file error:', error)

    // Re-throw if it's already our custom error
    if (error instanceof Error) {
      throw error
    }

    // Network or unexpected errors
    throw new Error('파일 업로드 중 오류가 발생했습니다. 네트워크 연결을 확인해주세요.')
  }
}

/**
 * 상품 등록
 *
 * 옵션, SKU, 이미지를 포함한 상품을 등록합니다. (관리자 전용)
 *
 * @param productData - 상품 등록 요청 데이터
 * @returns 생성된 상품 정보
 * @throws Error - 등록 실패 시 (인증 실패, 유효성 검증 실패 등)
 *
 * @example
 * ```typescript
 * const result = await createProduct({
 *   productName: '나이키 에어맥스',
 *   productCode: 'NIKE-001',
 *   basePrice: 150000,
 *   status: 'ACTIVE',
 *   isDisplayed: true,
 *   optionGroups: [...],
 *   skus: [...],
 *   images: [...]
 * })
 * console.log('Created product ID:', result.productId)
 * ```
 */
export const createProduct = async (productData: ProductCreateRequest): Promise<ProductResponse> => {
  try {
    // Get admin token from Zustand store
    const adminToken = useAuthStore.getState().adminToken
    if (!adminToken) {
      throw new Error('관리자 인증이 필요합니다. 다시 로그인해주세요.')
    }

    const response = await fetch('http://localhost:8080/api/admin/products', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${adminToken}`,
      },
      body: JSON.stringify(productData),
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
      if (response.status === 400) {
        throw new Error(error.message || '입력 정보를 확인해주세요.')
      }

      throw new Error(error.message || `상품 등록 실패 (HTTP ${response.status})`)
    }

    const data: ProductResponse = await response.json()
    return data
  } catch (error) {
    console.error('Create product error:', error)

    // Re-throw if it's already our custom error
    if (error instanceof Error) {
      throw error
    }

    // Network or unexpected errors
    throw new Error('상품 등록 중 오류가 발생했습니다. 네트워크 연결을 확인해주세요.')
  }
}

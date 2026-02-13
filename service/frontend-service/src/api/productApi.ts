/**
 * Product Service API Integration
 *
 * Type-safe API functions for product-service endpoints
 * All requests go through API Gateway
 */

import { API_BASE_URL } from '../config/env'
import { getAdminHeaders, getAdminUploadHeaders } from '../utils/apiHelper'

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
  /** 카테고리 ID (해당 카테고리 및 하위 카테고리 상품 조회) */
  categoryId?: number
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
  /** 프론트 임시 ID (SKU 옵션값 매핑용) */
  id?: string
  /** 옵션 값명 */
  optionValueName: string
  /** 정렬 순서 */
  displayOrder?: number
}

/**
 * 옵션 그룹 요청 DTO
 */
export interface OptionGroupRequest {
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
  /** 파일 업로드 ID */
  fileId?: number
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
  /** 카테고리 ID 목록 (3단계 카테고리만 등록 가능) */
  categoryIds?: number[]
  /** 옵션 그룹 목록 */
  optionGroups?: OptionGroupRequest[]
  /** SKU 목록 */
  skus?: SkuRequest[]
  /** 이미지 목록 */
  images?: ProductImageRequest[]
}

/**
 * 옵션 값 응답 DTO
 */
export interface OptionValueResponse {
  /** 옵션 값 ID */
  id: number
  /** 옵션 값명 */
  optionValueName: string
  /** 표시 순서 */
  displayOrder: number
}

/**
 * 옵션 그룹 응답 DTO
 */
export interface OptionGroupResponse {
  /** 옵션 그룹 ID */
  id: number
  /** 옵션 그룹명 */
  optionGroupName: string
  /** 표시 순서 */
  displayOrder: number
  /** 옵션 값 목록 */
  optionValues: OptionValueResponse[]
}

/**
 * SKU 응답 DTO
 */
export interface SkuResponse {
  /** SKU ID */
  id: number
  /** SKU 코드 */
  skuCode: string
  /** 가격 */
  price: number
  /** 재고 수량 */
  stockQty: number
  /** 상태 */
  status: string
  /** 옵션 값 ID 목록 */
  optionValueIds: number[]
}

/**
 * 이미지 응답 DTO
 */
export interface ImageResponse {
  /** 이미지 ID */
  id: number
  /** 파일 ID */
  fileId: number
  /** 이미지 URL */
  imageUrl: string
  /** 대표 이미지 여부 */
  isPrimary: boolean
  /** 표시 순서 */
  displayOrder: number
}

/**
 * 카테고리 응답 DTO (상품 상세 조회용)
 */
export interface ProductCategoryResponse {
  /** 카테고리 ID */
  categoryId: number
  /** 카테고리명 */
  categoryName: string
}

/**
 * 상품 상세 응답 DTO
 */
export interface ProductDetailResponse {
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
  status: string
  /** 진열 여부 */
  isDisplayed: boolean
  /** 카테고리 목록 */
  categories: ProductCategoryResponse[]
  /** 옵션 그룹 목록 */
  optionGroups: OptionGroupResponse[]
  /** SKU 목록 */
  skus: SkuResponse[]
  /** 이미지 목록 */
  images: ImageResponse[]
  /** 생성일시 */
  createdAt: string
  /** 수정일시 */
  updatedAt: string
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
    if (params?.categoryId !== undefined) {
      queryParams.append('categoryId', params.categoryId.toString())
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
    const url = `${API_BASE_URL}/api/admin/products${queryString ? `?${queryString}` : ''}`

    const response = await fetch(url, {
      method: 'GET',
      headers: getAdminHeaders(),
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
    const formData = new FormData()
    formData.append('file', file)

    const response = await fetch(`${API_BASE_URL}/api/admin/products/files/upload`, {
      method: 'POST',
      headers: getAdminUploadHeaders(),
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
    const response = await fetch(`${API_BASE_URL}/api/admin/products`, {
      method: 'POST',
      headers: getAdminHeaders(),
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

/**
 * 상품 상세 조회
 *
 * 상품 ID로 상품의 상세 정보를 조회합니다. 옵션 그룹, SKU, 이미지 정보를 포함합니다. (관리자 전용)
 *
 * @param productId - 상품 ID
 * @returns 상품 상세 정보
 * @throws Error - 조회 실패 시 (인증 실패, 상품 없음 등)
 *
 * @example
 * ```typescript
 * const product = await getProductDetail(1)
 * console.log('Product:', product.productName)
 * console.log('Options:', product.optionGroups)
 * console.log('SKUs:', product.skus)
 * ```
 */
export const getProductDetail = async (productId: number): Promise<ProductDetailResponse> => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/admin/products/${productId}`, {
      method: 'GET',
      headers: getAdminHeaders(),
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Unknown error' }))

      if (response.status === 401) {
        throw new Error(error.message || '인증이 만료되었습니다. 다시 로그인해주세요.')
      }
      if (response.status === 403) {
        throw new Error(error.message || '접근 권한이 없습니다.')
      }
      if (response.status === 404) {
        throw new Error(error.message || '상품을 찾을 수 없습니다.')
      }

      throw new Error(error.message || `상품 조회 실패 (HTTP ${response.status})`)
    }

    const data: ProductDetailResponse = await response.json()
    return data
  } catch (error) {
    console.error('Get product detail error:', error)

    if (error instanceof Error) {
      throw error
    }

    throw new Error('상품 조회 중 오류가 발생했습니다. 네트워크 연결을 확인해주세요.')
  }
}

/**
 * 상품 수정
 *
 * 상품 ID로 상품 정보를 수정합니다. 옵션 그룹, SKU, 이미지 정보를 포함하여 전체 교체됩니다. (관리자 전용)
 *
 * @param productId - 상품 ID
 * @param productData - 상품 수정 요청 데이터
 * @returns 수정된 상품 정보
 * @throws Error - 수정 실패 시 (인증 실패, 유효성 검증 실패, 상품 없음 등)
 *
 * @example
 * ```typescript
 * const result = await updateProduct(1, {
 *   productName: '수정된 상품명',
 *   basePrice: 200000,
 *   status: 'ACTIVE',
 *   optionGroups: [...],
 *   skus: [...],
 *   images: [...]
 * })
 * console.log('Updated product ID:', result.productId)
 * ```
 */
export const updateProduct = async (productId: number, productData: ProductCreateRequest): Promise<ProductResponse> => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/admin/products/${productId}`, {
      method: 'PUT',
      headers: getAdminHeaders(),
      body: JSON.stringify(productData),
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Unknown error' }))

      if (response.status === 401) {
        throw new Error(error.message || '인증이 만료되었습니다. 다시 로그인해주세요.')
      }
      if (response.status === 403) {
        throw new Error(error.message || '접근 권한이 없습니다.')
      }
      if (response.status === 404) {
        throw new Error(error.message || '상품을 찾을 수 없습니다.')
      }
      if (response.status === 400) {
        throw new Error(error.message || '입력 정보를 확인해주세요.')
      }

      throw new Error(error.message || `상품 수정 실패 (HTTP ${response.status})`)
    }

    const data: ProductResponse = await response.json()
    return data
  } catch (error) {
    console.error('Update product error:', error)

    if (error instanceof Error) {
      throw error
    }

    throw new Error('상품 수정 중 오류가 발생했습니다. 네트워크 연결을 확인해주세요.')
  }
}

// ==================== 검색 키워드 관리 API ====================

/**
 * 검색 키워드 응답 DTO
 */
export interface SearchKeywordResponse {
  /** 키워드 ID */
  keywordId: number
  /** 상품 ID */
  productId: number
  /** 검색 키워드 */
  keyword: string
  /** 생성일시 */
  createdAt: string
}

/**
 * 검색 키워드 등록 요청 DTO
 */
export interface SearchKeywordRequest {
  /** 검색 키워드 (최대 100자) */
  keyword: string
}

/**
 * 상품별 검색 키워드 목록 조회
 *
 * 특정 상품에 등록된 검색 키워드 목록을 조회합니다. (관리자 전용)
 *
 * @param productId - 상품 ID
 * @returns 검색 키워드 목록
 * @throws Error - 조회 실패 시 (인증 실패, 상품 없음 등)
 */
export const getProductSearchKeywords = async (productId: number): Promise<SearchKeywordResponse[]> => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/admin/products/${productId}/keywords`, {
      method: 'GET',
      headers: getAdminHeaders(),
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Unknown error' }))

      if (response.status === 401) {
        throw new Error(error.message || '인증이 만료되었습니다. 다시 로그인해주세요.')
      }
      if (response.status === 403) {
        throw new Error(error.message || '접근 권한이 없습니다.')
      }
      if (response.status === 400) {
        throw new Error(error.message || '상품을 찾을 수 없습니다.')
      }

      throw new Error(error.message || `키워드 조회 실패 (HTTP ${response.status})`)
    }

    const data: SearchKeywordResponse[] = await response.json()
    return data
  } catch (error) {
    console.error('Get product search keywords error:', error)

    if (error instanceof Error) {
      throw error
    }

    throw new Error('키워드 조회 중 오류가 발생했습니다. 네트워크 연결을 확인해주세요.')
  }
}

/**
 * 검색 키워드 등록
 *
 * 특정 상품에 검색 키워드를 등록합니다. 동일 상품에 중복 키워드는 등록할 수 없습니다. (관리자 전용)
 *
 * @param productId - 상품 ID
 * @param request - 키워드 등록 요청
 * @returns 등록된 키워드 정보
 * @throws Error - 등록 실패 시 (인증 실패, 중복 키워드, 상품 없음 등)
 */
export const addProductSearchKeyword = async (productId: number, request: SearchKeywordRequest): Promise<SearchKeywordResponse> => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/admin/products/${productId}/keywords`, {
      method: 'POST',
      headers: getAdminHeaders(),
      body: JSON.stringify(request),
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Unknown error' }))

      if (response.status === 401) {
        throw new Error(error.message || '인증이 만료되었습니다. 다시 로그인해주세요.')
      }
      if (response.status === 403) {
        throw new Error(error.message || '접근 권한이 없습니다.')
      }
      if (response.status === 400) {
        throw new Error(error.message || '키워드 등록에 실패했습니다.')
      }

      throw new Error(error.message || `키워드 등록 실패 (HTTP ${response.status})`)
    }

    const data: SearchKeywordResponse = await response.json()
    return data
  } catch (error) {
    console.error('Add product search keyword error:', error)

    if (error instanceof Error) {
      throw error
    }

    throw new Error('키워드 등록 중 오류가 발생했습니다. 네트워크 연결을 확인해주세요.')
  }
}

/**
 * 검색 키워드 삭제
 *
 * 특정 상품의 검색 키워드를 삭제합니다. (관리자 전용)
 *
 * @param productId - 상품 ID
 * @param keywordId - 키워드 ID
 * @throws Error - 삭제 실패 시 (인증 실패, 키워드 없음, 상품 불일치 등)
 */
export const deleteProductSearchKeyword = async (productId: number, keywordId: number): Promise<void> => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/admin/products/${productId}/keywords/${keywordId}`, {
      method: 'DELETE',
      headers: getAdminHeaders(),
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Unknown error' }))

      if (response.status === 401) {
        throw new Error(error.message || '인증이 만료되었습니다. 다시 로그인해주세요.')
      }
      if (response.status === 403) {
        throw new Error(error.message || '접근 권한이 없습니다.')
      }
      if (response.status === 400) {
        throw new Error(error.message || '키워드 삭제에 실패했습니다.')
      }

      throw new Error(error.message || `키워드 삭제 실패 (HTTP ${response.status})`)
    }
  } catch (error) {
    console.error('Delete product search keyword error:', error)

    if (error instanceof Error) {
      throw error
    }

    throw new Error('키워드 삭제 중 오류가 발생했습니다. 네트워크 연결을 확인해주세요.')
  }
}

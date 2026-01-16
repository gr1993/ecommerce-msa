/**
 * Category Service API Integration
 *
 * Type-safe API functions for category management endpoints
 * All requests go through API Gateway (http://localhost:8080)
 */

import { useAuthStore } from '../stores/authStore'

// ==================== Interfaces ====================

/**
 * 카테고리 트리 응답 DTO
 */
export interface CategoryTreeResponse {
  /** 카테고리 ID */
  categoryId: number
  /** 상위 카테고리 ID */
  parentId: number | null
  /** 카테고리명 */
  categoryName: string
  /** 전시 순서 */
  displayOrder: number
  /** 전시 여부 */
  isDisplayed: boolean
  /** 카테고리 깊이 (1단계부터 시작) */
  depth: number
  /** 하위 카테고리 목록 */
  children: CategoryTreeResponse[] | null
}

/**
 * 카테고리 상세 응답 DTO
 */
export interface CategoryResponse {
  /** 카테고리 ID */
  categoryId: number
  /** 상위 카테고리 ID */
  parentId: number | null
  /** 카테고리명 */
  categoryName: string
  /** 전시 순서 */
  displayOrder: number
  /** 전시 여부 */
  isDisplayed: boolean
  /** 생성일시 */
  createdAt: string
  /** 수정일시 */
  updatedAt: string
}

/**
 * 카테고리 등록 요청 DTO
 */
export interface CategoryCreateRequest {
  /** 상위 카테고리 ID (최상위 카테고리인 경우 null) */
  parentId?: number | null
  /** 카테고리명 */
  categoryName: string
  /** 전시 순서 */
  displayOrder?: number
  /** 전시 여부 */
  isDisplayed?: boolean
}

// ==================== API Functions ====================

/**
 * 카테고리 트리 조회
 *
 * 모든 카테고리를 계층 구조(트리)로 조회합니다. (관리자 전용)
 *
 * @returns 카테고리 트리 목록
 * @throws Error - 조회 실패 시 (인증 실패, 권한 없음 등)
 */
export const getCategoryTree = async (): Promise<CategoryTreeResponse[]> => {
  try {
    const adminToken = useAuthStore.getState().adminToken
    if (!adminToken) {
      throw new Error('관리자 인증이 필요합니다. 다시 로그인해주세요.')
    }

    const response = await fetch('http://localhost:8080/api/admin/categories', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${adminToken}`,
      },
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Unknown error' }))

      if (response.status === 401) {
        throw new Error(error.message || '인증이 만료되었습니다. 다시 로그인해주세요.')
      }
      if (response.status === 403) {
        throw new Error(error.message || '접근 권한이 없습니다.')
      }

      throw new Error(error.message || `카테고리 트리 조회 실패 (HTTP ${response.status})`)
    }

    const data: CategoryTreeResponse[] = await response.json()
    return data
  } catch (error) {
    console.error('Get category tree error:', error)

    if (error instanceof Error) {
      throw error
    }

    throw new Error('카테고리 트리 조회 중 오류가 발생했습니다. 네트워크 연결을 확인해주세요.')
  }
}

/**
 * 카테고리 상세 조회
 *
 * 특정 카테고리의 상세 정보를 조회합니다. (관리자 전용)
 *
 * @param categoryId - 카테고리 ID
 * @returns 카테고리 상세 정보
 * @throws Error - 조회 실패 시 (인증 실패, 카테고리 없음 등)
 */
export const getCategory = async (categoryId: number): Promise<CategoryResponse> => {
  try {
    const adminToken = useAuthStore.getState().adminToken
    if (!adminToken) {
      throw new Error('관리자 인증이 필요합니다. 다시 로그인해주세요.')
    }

    const response = await fetch(`http://localhost:8080/api/admin/categories/${categoryId}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${adminToken}`,
      },
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
        throw new Error(error.message || '카테고리를 찾을 수 없습니다.')
      }

      throw new Error(error.message || `카테고리 조회 실패 (HTTP ${response.status})`)
    }

    const data: CategoryResponse = await response.json()
    return data
  } catch (error) {
    console.error('Get category error:', error)

    if (error instanceof Error) {
      throw error
    }

    throw new Error('카테고리 조회 중 오류가 발생했습니다. 네트워크 연결을 확인해주세요.')
  }
}

/**
 * 카테고리 등록
 *
 * 새로운 카테고리를 등록합니다. (관리자 전용)
 *
 * @param request - 카테고리 등록 요청 데이터
 * @returns 생성된 카테고리 정보
 * @throws Error - 등록 실패 시 (인증 실패, 유효성 검증 실패, 상위 카테고리 없음 등)
 */
export const createCategory = async (request: CategoryCreateRequest): Promise<CategoryResponse> => {
  try {
    const adminToken = useAuthStore.getState().adminToken
    if (!adminToken) {
      throw new Error('관리자 인증이 필요합니다. 다시 로그인해주세요.')
    }

    const response = await fetch('http://localhost:8080/api/admin/categories', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${adminToken}`,
      },
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
        throw new Error(error.message || '입력 정보를 확인해주세요.')
      }
      if (response.status === 404) {
        throw new Error(error.message || '상위 카테고리를 찾을 수 없습니다.')
      }

      throw new Error(error.message || `카테고리 등록 실패 (HTTP ${response.status})`)
    }

    const data: CategoryResponse = await response.json()
    return data
  } catch (error) {
    console.error('Create category error:', error)

    if (error instanceof Error) {
      throw error
    }

    throw new Error('카테고리 등록 중 오류가 발생했습니다. 네트워크 연결을 확인해주세요.')
  }
}

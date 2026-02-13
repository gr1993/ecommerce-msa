/**
 * User Service API Integration
 *
 * Type-safe API functions for user-service endpoints
 * All requests go through API Gateway
 */

import { API_BASE_URL } from '../config/env'
import { getAdminHeaders } from '../utils/apiHelper'

// ==================== Interfaces ====================

/**
 * 회원가입 요청 DTO
 */
export interface SignUpRequest {
  /** 이메일 주소 */
  email: string
  /** 비밀번호 (최소 8자, 영문/숫자/특수문자 포함) */
  password: string
  /** 비밀번호 확인 */
  passwordConfirm: string
  /** 사용자 이름 */
  name: string
  /** 연락처 (예: 010-1234-5678) */
  phone?: string
}

/**
 * 회원가입 응답 DTO
 */
export interface SignUpResponse {
  /** 사용자 ID */
  userId: number
  /** 이메일 주소 */
  email: string
  /** 사용자 이름 */
  name: string
  /** 연락처 */
  phone?: string
  /** 생성 일시 */
  createdAt: string
}

/**
 * 회원 정보 응답 DTO
 */
export interface UserResponse {
  /** 사용자 ID */
  userId: number
  /** 이메일 주소 */
  email: string
  /** 사용자 이름 */
  name: string
  /** 연락처 */
  phone?: string
  /** 사용자 상태 */
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED'
  /** 회원 등급 */
  grade: 'NORMAL' | 'VIP' | 'GOLD' | 'SILVER'
  /** 포인트 */
  points: number
  /** 생성 일시 */
  createdAt: string
  /** 수정 일시 */
  updatedAt: string
}

/**
 * 페이지네이션 응답 DTO
 */
export interface PageResponse<T> {
  /** 컨텐츠 목록 */
  content: T[]
  /** 현재 페이지 번호 (0부터 시작) */
  page: number
  /** 페이지 크기 */
  size: number
  /** 전체 요소 개수 */
  totalElements: number
  /** 전체 페이지 개수 */
  totalPages: number
  /** 마지막 페이지 여부 */
  last: boolean
}

/**
 * 회원 목록 조회 요청 파라미터
 */
export interface SearchUsersParams {
  /** 검색 텍스트 (이메일 또는 이름) */
  searchText?: string
  /** 상태 필터 */
  status?: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED'
  /** 등급 필터 */
  grade?: 'NORMAL' | 'VIP' | 'GOLD' | 'SILVER'
  /** 페이지 번호 (0부터 시작) */
  page?: number
  /** 페이지 크기 */
  size?: number
  /** 정렬 기준 */
  sortBy?: string
  /** 정렬 방향 */
  sortDirection?: 'ASC' | 'DESC'
}

// ==================== API Functions ====================

/**
 * 회원가입
 *
 * 새로운 사용자를 등록합니다. 이메일, 비밀번호, 이름, 연락처 정보가 필요합니다.
 *
 * @param signUpData - 회원가입 요청 데이터
 * @returns 생성된 사용자 정보
 * @throws Error - 회원가입 실패 시 (유효성 검증 실패, 이메일 중복 등)
 *
 * @example
 * ```typescript
 * const result = await signUp({
 *   email: 'user@example.com',
 *   password: 'Password123!',
 *   passwordConfirm: 'Password123!',
 *   name: '홍길동',
 *   phone: '010-1234-5678'
 * })
 * console.log('Created user ID:', result.userId)
 * ```
 */
export const signUp = async (signUpData: SignUpRequest): Promise<SignUpResponse> => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/users/signup`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(signUpData),
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Unknown error' }))

      // Handle specific error cases
      if (response.status === 400) {
        throw new Error(error.message || '입력 정보를 확인해주세요.')
      }
      if (response.status === 409) {
        throw new Error(error.message || '이미 사용 중인 이메일입니다.')
      }

      throw new Error(error.message || `회원가입 실패 (HTTP ${response.status})`)
    }

    const data: SignUpResponse = await response.json()
    return data
  } catch (error) {
    console.error('Sign up error:', error)

    // Re-throw if it's already our custom error
    if (error instanceof Error) {
      throw error
    }

    // Network or unexpected errors
    throw new Error('회원가입 중 오류가 발생했습니다. 네트워크 연결을 확인해주세요.')
  }
}

/**
 * 회원 목록 조회 (페이지네이션 + 검색)
 *
 * 페이지네이션과 검색 필터를 적용하여 사용자 목록을 조회합니다. (관리자 전용)
 *
 * @param params - 검색 및 페이지네이션 파라미터
 * @returns 페이지네이션된 사용자 목록
 * @throws Error - 조회 실패 시 (인증 실패, 권한 없음 등)
 *
 * @example
 * ```typescript
 * const result = await searchUsers({
 *   searchText: 'user',
 *   status: 'ACTIVE',
 *   page: 0,
 *   size: 10
 * })
 * console.log('Users:', result.content)
 * console.log('Total:', result.totalElements)
 * ```
 */
export const searchUsers = async (params?: SearchUsersParams): Promise<PageResponse<UserResponse>> => {
  try {
    // Build query parameters
    const queryParams = new URLSearchParams()

    if (params?.searchText) {
      queryParams.append('searchText', params.searchText)
    }
    if (params?.status) {
      queryParams.append('status', params.status)
    }
    if (params?.grade) {
      queryParams.append('grade', params.grade)
    }
    if (params?.page !== undefined) {
      queryParams.append('page', params.page.toString())
    }
    if (params?.size !== undefined) {
      queryParams.append('size', params.size.toString())
    }
    if (params?.sortBy) {
      queryParams.append('sortBy', params.sortBy)
    }
    if (params?.sortDirection) {
      queryParams.append('sortDirection', params.sortDirection)
    }

    const queryString = queryParams.toString()
    const url = `${API_BASE_URL}/api/admin/users${queryString ? `?${queryString}` : ''}`

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

      throw new Error(error.message || `회원 목록 조회 실패 (HTTP ${response.status})`)
    }

    const data: PageResponse<UserResponse> = await response.json()
    return data
  } catch (error) {
    console.error('Search users error:', error)

    // Re-throw if it's already our custom error
    if (error instanceof Error) {
      throw error
    }

    // Network or unexpected errors
    throw new Error('회원 목록 조회 중 오류가 발생했습니다. 네트워크 연결을 확인해주세요.')
  }
}

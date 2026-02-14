/**
 * Discount Policy (Promotion) Service API Integration
 *
 * Type-safe API functions for promotion-service discount policy endpoints
 * All requests go through API Gateway
 */

import { API_BASE_URL } from '../config/env'
import { getAdminHeaders } from '../utils/apiHelper'

// ==================== API Response Interfaces ====================

interface AdminDiscountPolicyApiResponse {
  discountId: number
  discountName: string
  discountType: string
  discountValue: number
  targetType: string
  targetId: number | null
  minOrderAmount: number
  maxDiscountAmount: number | null
  validFrom: string
  validTo: string
  status: string
  createdAt: string
  updatedAt: string
}

interface AdminDiscountPolicyPageApiResponse {
  content: AdminDiscountPolicyApiResponse[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

// ==================== Frontend Interfaces ====================

export interface DiscountPolicy {
  discount_id: string
  discount_name: string
  discount_type: string
  discount_value: number
  target_type: string
  target_id: number | null
  min_order_amount: number
  max_discount_amount: number | null
  valid_from: string
  valid_to: string
  status: string
  created_at: string
  updated_at: string
}

export interface AdminDiscountPolicyPageResponse {
  content: DiscountPolicy[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

// ==================== Mappers ====================

function mapDiscountPolicy(data: AdminDiscountPolicyApiResponse): DiscountPolicy {
  return {
    discount_id: String(data.discountId),
    discount_name: data.discountName,
    discount_type: data.discountType,
    discount_value: data.discountValue,
    target_type: data.targetType,
    target_id: data.targetId,
    min_order_amount: data.minOrderAmount,
    max_discount_amount: data.maxDiscountAmount,
    valid_from: data.validFrom,
    valid_to: data.validTo,
    status: data.status,
    created_at: data.createdAt,
    updated_at: data.updatedAt,
  }
}

// ==================== Admin API Functions ====================

/**
 * 관리자 할인 정책 목록 조회 (페이지네이션)
 *
 * @param keyword - 할인 정책명 (부분 검색)
 * @param status - 할인 상태 필터 (ACTIVE, INACTIVE, EXPIRED)
 * @param page - 페이지 번호 (0부터 시작)
 * @param size - 페이지 크기
 * @returns 페이지네이션된 할인 정책 목록
 */
export const getAdminDiscountPolicies = async (
  keyword?: string,
  status?: string,
  page: number = 0,
  size: number = 20,
): Promise<AdminDiscountPolicyPageResponse> => {
  try {
    const queryParams = new URLSearchParams()
    if (keyword) queryParams.append('keyword', keyword)
    if (status) queryParams.append('status', status)
    queryParams.append('page', String(page))
    queryParams.append('size', String(size))

    const url = `${API_BASE_URL}/api/admin/discount-policies?${queryParams.toString()}`

    const response = await fetch(url, {
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
      throw new Error(error.message || `할인 정책 목록 조회 실패 (HTTP ${response.status})`)
    }

    const data: AdminDiscountPolicyPageApiResponse = await response.json()
    return {
      content: data.content.map(mapDiscountPolicy),
      page: data.page,
      size: data.size,
      totalElements: data.totalElements,
      totalPages: data.totalPages,
      first: data.first,
      last: data.last,
    }
  } catch (error) {
    console.error('Get admin discount policies error:', error)
    if (error instanceof Error) throw error
    throw new Error('할인 정책 목록 조회 중 오류가 발생했습니다.')
  }
}

/**
 * 관리자 할인 정책 상세 조회
 *
 * @param discountId - 할인 정책 ID
 * @returns 할인 정책 상세
 */
export const getAdminDiscountPolicyDetail = async (discountId: string): Promise<DiscountPolicy> => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/admin/discount-policies/${discountId}`, {
      method: 'GET',
      headers: getAdminHeaders(),
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Unknown error' }))
      if (response.status === 401) {
        throw new Error(error.message || '인증이 만료되었습니다. 다시 로그인해주세요.')
      }
      if (response.status === 404) {
        throw new Error(error.message || '할인 정책을 찾을 수 없습니다.')
      }
      throw new Error(error.message || `할인 정책 상세 조회 실패 (HTTP ${response.status})`)
    }

    const data: AdminDiscountPolicyApiResponse = await response.json()
    return mapDiscountPolicy(data)
  } catch (error) {
    console.error('Get admin discount policy detail error:', error)
    if (error instanceof Error) throw error
    throw new Error('할인 정책 상세 조회 중 오류가 발생했습니다.')
  }
}

/**
 * 관리자 할인 정책 등록
 *
 * @param request - 할인 정책 등록 요청 데이터
 * @returns 등록된 할인 정책 상세
 */
export const createAdminDiscountPolicy = async (request: {
  discountName: string
  discountType: string
  discountValue: number
  targetType: string
  targetId?: number
  minOrderAmount?: number
  maxDiscountAmount?: number
  validFrom: string
  validTo: string
  status: string
}): Promise<DiscountPolicy> => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/admin/discount-policies`, {
      method: 'POST',
      headers: getAdminHeaders(),
      body: JSON.stringify(request),
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Unknown error' }))
      if (response.status === 401) {
        throw new Error(error.message || '인증이 만료되었습니다. 다시 로그인해주세요.')
      }
      if (response.status === 400) {
        throw new Error(error.message || '입력 정보를 확인해주세요.')
      }
      throw new Error(error.message || `할인 정책 등록 실패 (HTTP ${response.status})`)
    }

    const data: AdminDiscountPolicyApiResponse = await response.json()
    return mapDiscountPolicy(data)
  } catch (error) {
    console.error('Create admin discount policy error:', error)
    if (error instanceof Error) throw error
    throw new Error('할인 정책 등록 중 오류가 발생했습니다.')
  }
}

/**
 * 관리자 할인 정책 수정
 *
 * @param discountId - 할인 정책 ID
 * @param request - 할인 정책 수정 요청 데이터
 * @returns 수정된 할인 정책 상세
 */
export const updateAdminDiscountPolicy = async (discountId: string, request: {
  discountName: string
  discountType: string
  discountValue: number
  targetType: string
  targetId?: number
  minOrderAmount?: number
  maxDiscountAmount?: number
  validFrom: string
  validTo: string
  status: string
}): Promise<DiscountPolicy> => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/admin/discount-policies/${discountId}`, {
      method: 'PUT',
      headers: getAdminHeaders(),
      body: JSON.stringify(request),
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Unknown error' }))
      if (response.status === 401) {
        throw new Error(error.message || '인증이 만료되었습니다. 다시 로그인해주세요.')
      }
      if (response.status === 400) {
        throw new Error(error.message || '입력 정보를 확인해주세요.')
      }
      if (response.status === 404) {
        throw new Error(error.message || '할인 정책을 찾을 수 없습니다.')
      }
      throw new Error(error.message || `할인 정책 수정 실패 (HTTP ${response.status})`)
    }

    const data: AdminDiscountPolicyApiResponse = await response.json()
    return mapDiscountPolicy(data)
  } catch (error) {
    console.error('Update admin discount policy error:', error)
    if (error instanceof Error) throw error
    throw new Error('할인 정책 수정 중 오류가 발생했습니다.')
  }
}

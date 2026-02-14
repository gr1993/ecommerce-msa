/**
 * Coupon (Promotion) Service API Integration
 *
 * Type-safe API functions for promotion-service coupon endpoints
 * All requests go through API Gateway
 */

import { API_BASE_URL } from '../config/env'
import { getAdminHeaders } from '../utils/apiHelper'

// ==================== API Response Interfaces ====================

interface AdminCouponApiResponse {
  couponId: number
  couponCode: string
  couponName: string
  discountType: string
  discountValue: number
  minOrderAmount: number
  maxDiscountAmount: number | null
  validFrom: string
  validTo: string
  status: string
  issuedCount: number
  createdAt: string
  updatedAt: string
}

interface AdminCouponUsageApiResponse {
  userCouponId: number
  userId: number
  couponStatus: string
  usedAt: string | null
  issuedAt: string
}

interface AdminCouponDetailApiResponse extends AdminCouponApiResponse {
  userCoupons: AdminCouponUsageApiResponse[]
}

interface AdminCouponPageApiResponse {
  content: AdminCouponApiResponse[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

// ==================== Frontend Interfaces ====================

export interface Coupon {
  coupon_id: string
  coupon_code: string
  coupon_name: string
  discount_type: string
  discount_value: number
  min_order_amount: number
  max_discount_amount: number | null
  valid_from: string
  valid_to: string
  status: string
  issued_count: number
  created_at: string
  updated_at: string
}

export interface CouponUsage {
  user_coupon_id: string
  user_id: string
  coupon_status: string
  used_at: string | null
  issued_at: string
}

export interface CouponDetail extends Coupon {
  user_coupons: CouponUsage[]
}

export interface AdminCouponPageResponse {
  content: Coupon[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

// ==================== Mappers ====================

function mapCoupon(data: AdminCouponApiResponse): Coupon {
  return {
    coupon_id: String(data.couponId),
    coupon_code: data.couponCode,
    coupon_name: data.couponName,
    discount_type: data.discountType,
    discount_value: data.discountValue,
    min_order_amount: data.minOrderAmount,
    max_discount_amount: data.maxDiscountAmount,
    valid_from: data.validFrom,
    valid_to: data.validTo,
    status: data.status,
    issued_count: data.issuedCount,
    created_at: data.createdAt,
    updated_at: data.updatedAt,
  }
}

function mapCouponUsage(data: AdminCouponUsageApiResponse): CouponUsage {
  return {
    user_coupon_id: String(data.userCouponId),
    user_id: String(data.userId),
    coupon_status: data.couponStatus,
    used_at: data.usedAt,
    issued_at: data.issuedAt,
  }
}

function mapCouponDetail(data: AdminCouponDetailApiResponse): CouponDetail {
  return {
    ...mapCoupon(data),
    user_coupons: data.userCoupons.map(mapCouponUsage),
  }
}

// ==================== Admin API Functions ====================

/**
 * 관리자 쿠폰 목록 조회 (페이지네이션)
 *
 * @param keyword - 쿠폰 코드 또는 쿠폰명 (부분 검색)
 * @param status - 쿠폰 상태 필터 (ACTIVE, INACTIVE, EXPIRED)
 * @param page - 페이지 번호 (0부터 시작)
 * @param size - 페이지 크기
 * @returns 페이지네이션된 쿠폰 목록
 */
export const getAdminCoupons = async (
  keyword?: string,
  status?: string,
  page: number = 0,
  size: number = 20,
): Promise<AdminCouponPageResponse> => {
  try {
    const queryParams = new URLSearchParams()
    if (keyword) queryParams.append('keyword', keyword)
    if (status) queryParams.append('status', status)
    queryParams.append('page', String(page))
    queryParams.append('size', String(size))

    const url = `${API_BASE_URL}/api/admin/coupons?${queryParams.toString()}`

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
      throw new Error(error.message || `쿠폰 목록 조회 실패 (HTTP ${response.status})`)
    }

    const data: AdminCouponPageApiResponse = await response.json()
    return {
      content: data.content.map(mapCoupon),
      page: data.page,
      size: data.size,
      totalElements: data.totalElements,
      totalPages: data.totalPages,
      first: data.first,
      last: data.last,
    }
  } catch (error) {
    console.error('Get admin coupons error:', error)
    if (error instanceof Error) throw error
    throw new Error('쿠폰 목록 조회 중 오류가 발생했습니다.')
  }
}

/**
 * 관리자 쿠폰 상세 조회
 *
 * @param couponId - 쿠폰 ID
 * @returns 쿠폰 상세 (쿠폰 정보 + 발급 내역)
 */
export const getAdminCouponDetail = async (couponId: string): Promise<CouponDetail> => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/admin/coupons/${couponId}`, {
      method: 'GET',
      headers: getAdminHeaders(),
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Unknown error' }))
      if (response.status === 401) {
        throw new Error(error.message || '인증이 만료되었습니다. 다시 로그인해주세요.')
      }
      if (response.status === 404) {
        throw new Error(error.message || '쿠폰을 찾을 수 없습니다.')
      }
      throw new Error(error.message || `쿠폰 상세 조회 실패 (HTTP ${response.status})`)
    }

    const data: AdminCouponDetailApiResponse = await response.json()
    return mapCouponDetail(data)
  } catch (error) {
    console.error('Get admin coupon detail error:', error)
    if (error instanceof Error) throw error
    throw new Error('쿠폰 상세 조회 중 오류가 발생했습니다.')
  }
}

/**
 * 관리자 쿠폰 등록
 *
 * @param request - 쿠폰 등록 요청 데이터
 * @returns 등록된 쿠폰 상세
 */
export const createAdminCoupon = async (request: {
  couponCode: string
  couponName: string
  discountType: string
  discountValue: number
  minOrderAmount?: number
  maxDiscountAmount?: number
  validFrom: string
  validTo: string
  status: string
}): Promise<CouponDetail> => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/admin/coupons`, {
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
      throw new Error(error.message || `쿠폰 등록 실패 (HTTP ${response.status})`)
    }

    const data: AdminCouponDetailApiResponse = await response.json()
    return mapCouponDetail(data)
  } catch (error) {
    console.error('Create admin coupon error:', error)
    if (error instanceof Error) throw error
    throw new Error('쿠폰 등록 중 오류가 발생했습니다.')
  }
}

/**
 * 관리자 쿠폰 수정
 *
 * @param couponId - 쿠폰 ID
 * @param request - 쿠폰 수정 요청 데이터
 * @returns 수정된 쿠폰 상세
 */
export const updateAdminCoupon = async (couponId: string, request: {
  couponCode: string
  couponName: string
  discountType: string
  discountValue: number
  minOrderAmount?: number
  maxDiscountAmount?: number
  validFrom: string
  validTo: string
  status: string
}): Promise<CouponDetail> => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/admin/coupons/${couponId}`, {
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
        throw new Error(error.message || '쿠폰을 찾을 수 없습니다.')
      }
      throw new Error(error.message || `쿠폰 수정 실패 (HTTP ${response.status})`)
    }

    const data: AdminCouponDetailApiResponse = await response.json()
    return mapCouponDetail(data)
  } catch (error) {
    console.error('Update admin coupon error:', error)
    if (error instanceof Error) throw error
    throw new Error('쿠폰 수정 중 오류가 발생했습니다.')
  }
}

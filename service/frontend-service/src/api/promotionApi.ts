/**
 * Promotion Service API (User-facing)
 *
 * Type-safe API functions for user promotion endpoints
 * (coupons + discount policies)
 */

import { API_BASE_URL } from '../config/env'
import { userFetch } from '../utils/apiHelper'

// ==================== API Response Interfaces ====================

interface UserCouponApiResponse {
  userCouponId: number
  couponId: number
  couponCode: string
  couponName: string
  discountType: string
  discountValue: number
  minOrderAmount: number
  maxDiscountAmount: number | null
  validFrom: string
  validTo: string
  couponStatus: string
  usedAt: string | null
  issuedAt: string
}

interface ApplicableDiscountPolicyApiResponse {
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
}

// ==================== Frontend Interfaces ====================

export interface UserCoupon {
  user_coupon_id: string
  coupon_id: string
  coupon_code: string
  coupon_name: string
  discount_type: string
  discount_value: number
  min_order_amount: number
  max_discount_amount: number | null
  valid_from: string
  valid_to: string
  coupon_status: string
  used_at: string | null
  issued_at: string
}

export interface ApplicableDiscountPolicy {
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
}

// ==================== Mappers ====================

function mapUserCoupon(data: UserCouponApiResponse): UserCoupon {
  return {
    user_coupon_id: String(data.userCouponId),
    coupon_id: String(data.couponId),
    coupon_code: data.couponCode,
    coupon_name: data.couponName,
    discount_type: data.discountType,
    discount_value: data.discountValue,
    min_order_amount: data.minOrderAmount,
    max_discount_amount: data.maxDiscountAmount,
    valid_from: data.validFrom,
    valid_to: data.validTo,
    coupon_status: data.couponStatus,
    used_at: data.usedAt,
    issued_at: data.issuedAt,
  }
}

function mapDiscountPolicy(data: ApplicableDiscountPolicyApiResponse): ApplicableDiscountPolicy {
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
  }
}

// ==================== API Functions ====================

/**
 * 사용자 보유 쿠폰 목록 조회
 */
export const getUserCoupons = async (): Promise<UserCoupon[]> => {
  try {
    const response = await userFetch(`${API_BASE_URL}/api/promotion/coupons`, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Unknown error' }))
      throw new Error(error.message || `쿠폰 목록 조회 실패 (HTTP ${response.status})`)
    }

    const data: UserCouponApiResponse[] = await response.json()
    return data.map(mapUserCoupon)
  } catch (error) {
    console.error('Get user coupons error:', error)
    if (error instanceof Error) throw error
    throw new Error('쿠폰 목록 조회 중 오류가 발생했습니다.')
  }
}

/**
 * 적용 가능 할인 정책 조회
 *
 * @param productIds - 상품 ID 목록
 */
export const getApplicableDiscountPolicies = async (productIds: number[]): Promise<ApplicableDiscountPolicy[]> => {
  try {
    const queryParams = new URLSearchParams()
    productIds.forEach(id => queryParams.append('productIds', String(id)))

    const response = await userFetch(
      `${API_BASE_URL}/api/promotion/discount-policies?${queryParams.toString()}`,
      {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
      },
    )

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Unknown error' }))
      throw new Error(error.message || `할인 정책 조회 실패 (HTTP ${response.status})`)
    }

    const data: ApplicableDiscountPolicyApiResponse[] = await response.json()
    return data.map(mapDiscountPolicy)
  } catch (error) {
    console.error('Get applicable discount policies error:', error)
    if (error instanceof Error) throw error
    throw new Error('할인 정책 조회 중 오류가 발생했습니다.')
  }
}

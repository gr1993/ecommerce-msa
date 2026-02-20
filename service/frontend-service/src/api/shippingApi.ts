/**
 * Shipping Service API Integration
 *
 * Type-safe API functions for shipping-service endpoints
 * All requests go through API Gateway
 */

import { API_BASE_URL } from '../config/env'
import { getAdminHeaders } from '../utils/apiHelper'

// ==================== Interfaces ====================

/**
 * 배송 상태
 */
export type ShippingStatus = 'READY' | 'SHIPPING' | 'DELIVERED' | 'RETURNED'

/**
 * 배송사 연동 상태
 */
export type DeliveryServiceStatus = 'NOT_SENT' | 'SENT' | 'IN_TRANSIT' | 'DELIVERED'

/**
 * 관리자 배송 응답 DTO
 */
export interface AdminShippingResponse {
  /** 배송 ID */
  shippingId: number
  /** 주문 ID */
  orderId: number
  /** 주문 번호 */
  orderNumber?: string
  /** 수령인 이름 */
  receiverName: string
  /** 수령인 연락처 */
  receiverPhone: string
  /** 배송 주소 */
  address: string
  /** 우편번호 */
  postalCode?: string
  /** 배송 상태 */
  shippingStatus: ShippingStatus
  /** 배송사 */
  shippingCompany?: string
  /** 운송장 번호 */
  trackingNumber?: string
  /** 배송사 연동 상태 */
  deliveryServiceStatus?: DeliveryServiceStatus
  /** 생성 일시 */
  createdAt: string
  /** 수정 일시 */
  updatedAt: string
}

/**
 * 페이지네이션 응답
 */
export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

/**
 * 관리자 배송 목록 페이지 응답
 */
export type AdminShippingPageResponse = PageResponse<AdminShippingResponse>

// ==================== API Functions ====================

/**
 * 관리자 배송 목록 조회 (페이지네이션)
 *
 * @param shippingStatus - 배송 상태 필터
 * @param trackingNumber - 운송장 번호 (부분 검색)
 * @param page - 페이지 번호 (0부터 시작)
 * @param size - 페이지 크기
 * @returns 페이지네이션된 배송 목록
 */
export const getAdminShippings = async (
  shippingStatus?: string,
  trackingNumber?: string,
  page: number = 0,
  size: number = 20,
): Promise<AdminShippingPageResponse> => {
  try {
    const queryParams = new URLSearchParams()
    queryParams.append('page', String(page))
    queryParams.append('size', String(size))
    queryParams.append('sort', 'updatedAt,desc')
    if (shippingStatus) queryParams.append('shippingStatus', shippingStatus)
    if (trackingNumber) queryParams.append('trackingNumber', trackingNumber)

    const url = `${API_BASE_URL}/api/admin/shipping?${queryParams.toString()}`

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
      throw new Error(error.message || `배송 목록 조회 실패 (HTTP ${response.status})`)
    }

    const data: AdminShippingPageResponse = await response.json()
    return data
  } catch (error) {
    console.error('Get admin shippings error:', error)
    if (error instanceof Error) throw error
    throw new Error('배송 목록 조회 중 오류가 발생했습니다.')
  }
}

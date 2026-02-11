/**
 * Order Service API Integration
 *
 * Type-safe API functions for order-service endpoints
 * All requests go through API Gateway
 *
 * 토큰 정책:
 * - API 요청 전 Access Token 만료 시간 확인
 * - 만료되었거나 5분 이내 만료 예정 시 자동 갱신
 * - 갱신 실패 시 로그아웃 유도
 */

import { API_BASE_URL } from '../config/env'
import { authenticatedFetch, TokenRefreshError, AuthRequiredError } from '../utils/authFetch'

// ==================== Interfaces ====================

/**
 * 주문 상품 요청 DTO
 */
export interface OrderItemRequest {
  /** 상품 ID */
  productId: number
  /** SKU ID */
  skuId: number
  /** 수량 */
  quantity: number
}

/**
 * 배송 정보 요청 DTO
 */
export interface DeliveryInfoRequest {
  /** 수령인 이름 */
  receiverName: string
  /** 수령인 연락처 */
  receiverPhone: string
  /** 우편번호 */
  zipcode: string
  /** 주소 */
  address: string
  /** 상세 주소 */
  addressDetail?: string
  /** 배송 메모 */
  deliveryMemo?: string
}

/**
 * 주문 생성 요청 DTO
 */
export interface OrderCreateRequest {
  /** 주문 상품 목록 */
  orderItems: OrderItemRequest[]
  /** 배송 정보 */
  deliveryInfo: DeliveryInfoRequest
}

/**
 * 주문 상태
 */
export type OrderStatus = 'CREATED' | 'PAID' | 'PREPARING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED'

/**
 * 주문 생성 응답 DTO
 */
export interface OrderResponse {
  /** 주문 ID */
  orderId: number
  /** 주문 번호 */
  orderNumber: string
  /** 주문 상태 */
  orderStatus: OrderStatus
  /** 주문 일시 */
  orderedAt: string
}

// ==================== API Functions ====================

/**
 * 주문 생성
 *
 * 새로운 주문을 생성합니다. (인증 필요)
 * Access Token 만료 시 자동으로 갱신 후 요청을 수행합니다.
 *
 * @param request - 주문 생성 요청 데이터
 * @returns 주문 생성 응답
 * @throws Error - 주문 생성 실패 시
 * @throws TokenRefreshError - 토큰 갱신 실패 시 (재로그인 필요)
 * @throws AuthRequiredError - 로그인되어 있지 않은 경우
 */
export const createOrder = async (request: OrderCreateRequest): Promise<OrderResponse> => {
  try {
    // authenticatedFetch가 토큰 만료 확인 및 자동 갱신 처리
    const response = await authenticatedFetch(`${API_BASE_URL}/api/orders`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    })

    if (!response.ok) {
      if (response.status === 403) {
        throw new Error('주문 권한이 없습니다.')
      }
      const error = await response.json().catch(() => ({ message: '주문 생성에 실패했습니다.' }))
      throw new Error(error.message || `주문 생성 실패 (HTTP ${response.status})`)
    }

    const data: OrderResponse = await response.json()
    return data
  } catch (error) {
    console.error('Create order error:', error)

    // 토큰 갱신 실패 또는 인증 필요 에러는 그대로 전파
    if (error instanceof TokenRefreshError || error instanceof AuthRequiredError) {
      throw error
    }

    if (error instanceof Error) {
      throw error
    }

    throw new Error('주문 생성 중 오류가 발생했습니다. 네트워크 연결을 확인해주세요.')
  }
}

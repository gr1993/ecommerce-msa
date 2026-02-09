/**
 * Order Service API Integration
 *
 * Type-safe API functions for order-service endpoints
 * All requests go through API Gateway
 */

import { API_BASE_URL } from '../config/env'

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
 *
 * @param request - 주문 생성 요청 데이터
 * @returns 주문 생성 응답
 * @throws Error - 주문 생성 실패 시
 */
export const createOrder = async (request: OrderCreateRequest): Promise<OrderResponse> => {
  const token = localStorage.getItem('userToken')

  if (!token) {
    throw new Error('로그인이 필요합니다.')
  }

  try {
    const response = await fetch(`${API_BASE_URL}/api/orders`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
      body: JSON.stringify(request),
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: '주문 생성에 실패했습니다.' }))
      throw new Error(error.message || `주문 생성 실패 (HTTP ${response.status})`)
    }

    const data: OrderResponse = await response.json()
    return data
  } catch (error) {
    console.error('Create order error:', error)

    if (error instanceof Error) {
      throw error
    }

    throw new Error('주문 생성 중 오류가 발생했습니다. 네트워크 연결을 확인해주세요.')
  }
}

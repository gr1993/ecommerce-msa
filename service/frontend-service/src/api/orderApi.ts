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
import { TokenRefreshError, AuthRequiredError } from '../utils/authFetch'
import { getAdminHeaders, userFetch } from '../utils/apiHelper'
import { getProductDetail, type ProductDetailResponse } from './catalogApi'
import type { Order, OrderItem, OrderShipping } from '../pages/admin/order/OrderDetailModal'

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
 * 할인 적용 요청 DTO
 */
export interface DiscountRequest {
  /** 할인 유형 (COUPON, POLICY) */
  discountType: string
  /** 참조 ID (userCouponId 또는 discountPolicyId) */
  referenceId: number
  /** 할인명 */
  discountName: string
  /** 할인 금액 */
  discountAmount: number
  /** 할인율 (%) */
  discountRate?: number
  /** 할인 설명 */
  description?: string
}

/**
 * 주문 생성 요청 DTO
 */
export interface OrderCreateRequest {
  /** 주문 상품 목록 */
  orderItems: OrderItemRequest[]
  /** 배송 정보 */
  deliveryInfo: DeliveryInfoRequest
  /** 할인 정보 목록 */
  discounts?: DiscountRequest[]
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
    // userFetch: AUTH_DISABLED 시 일반 fetch, 아닐 때 authenticatedFetch (토큰 자동 갱신)
    const response = await userFetch(`${API_BASE_URL}/api/orders`, {
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

// ==================== My Order API Interfaces ====================

/**
 * 내 주문 상품 응답 DTO
 */
export interface MyOrderItemResponse {
  orderItemId: number
  productId: number
  skuId: number
  productName: string
  productCode: string
  quantity: number
  unitPrice: number
  totalPrice: number
}

/**
 * 내 주문 응답 DTO
 */
export interface MyOrderResponse {
  orderId: number
  orderNumber: string
  orderStatus: OrderStatus
  totalPaymentAmount: number
  orderedAt: string
  items: MyOrderItemResponse[]
}

/**
 * 내 주문 목록 페이지네이션 응답
 */
export interface MyOrderPageResponse {
  content: MyOrderResponse[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

// ==================== My Order API Functions ====================

/**
 * 내 주문 목록 조회 (페이지네이션)
 *
 * @param page - 페이지 번호 (0부터 시작)
 * @param size - 페이지 크기
 * @returns 페이지네이션된 주문 목록
 */
export const getMyOrders = async (
  page: number = 0,
  size: number = 10,
): Promise<MyOrderPageResponse> => {
  try {
    const queryParams = new URLSearchParams()
    queryParams.append('page', String(page))
    queryParams.append('size', String(size))
    queryParams.append('sort', 'orderedAt,desc')

    const url = `${API_BASE_URL}/api/orders?${queryParams.toString()}`

    const response = await userFetch(url, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    })

    if (!response.ok) {
      if (response.status === 403) {
        throw new Error('주문 조회 권한이 없습니다.')
      }
      const error = await response.json().catch(() => ({ message: '주문 목록 조회에 실패했습니다.' }))
      throw new Error(error.message || `주문 목록 조회 실패 (HTTP ${response.status})`)
    }

    const data: MyOrderPageResponse = await response.json()
    return data
  } catch (error) {
    console.error('Get my orders error:', error)

    if (error instanceof TokenRefreshError || error instanceof AuthRequiredError) {
      throw error
    }

    if (error instanceof Error) {
      throw error
    }

    throw new Error('주문 목록 조회 중 오류가 발생했습니다. 네트워크 연결을 확인해주세요.')
  }
}

/**
 * 주문 취소 응답 DTO
 */
export interface CancelOrderResponse {
  orderId: number
  orderNumber: string
  orderStatus: OrderStatus
  cancellationReason: string
  cancelledAt: string
}

/**
 * 반품 신청 요청 DTO
 */
export interface ReturnOrderRequest {
  /** 반품 사유 */
  reason?: string
}

/**
 * 반품 신청 응답 DTO
 */
export interface ReturnOrderResponse {
  /** 반품 ID */
  returnId: number
  /** 주문 ID */
  orderId: number
  /** 주문 번호 */
  orderNumber: string
  /** 반품 상태 */
  returnStatus: string
  /** 반품 사유 */
  reason?: string
  /** 신청 일시 */
  requestedAt: string
}

/**
 * 교환 상품 요청 DTO
 */
export interface ExchangeItemRequest {
  /** 주문 상품 ID */
  orderItemId: number
  /** 새 SKU ID (교환받을 상품 옵션) */
  newSkuId: number
  /** 교환 수량 */
  quantity: number
}

/**
 * 교환 신청 요청 DTO
 */
export interface ExchangeOrderRequest {
  /** 교환 상품 목록 */
  exchangeItems: ExchangeItemRequest[]
  /** 교환 사유 */
  reason?: string
}

/**
 * 교환 신청 응답 DTO
 */
export interface ExchangeOrderResponse {
  /** 교환 ID */
  exchangeId: number
  /** 주문 ID */
  orderId: number
  /** 주문 번호 */
  orderNumber: string
  /** 교환 상태 */
  exchangeStatus: string
  /** 교환 사유 */
  reason?: string
  /** 신청 일시 */
  requestedAt: string
}

/**
 * 주문 취소 (사용자)
 *
 * @param orderId - 주문 ID
 * @param cancellationReason - 취소 사유 (생략 시 기본값 사용)
 * @returns 취소된 주문 정보
 */
export const cancelOrder = async (
  orderId: number,
  cancellationReason?: string,
): Promise<CancelOrderResponse> => {
  try {
    const response = await userFetch(`${API_BASE_URL}/api/orders/${orderId}/cancel`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: cancellationReason ? JSON.stringify({ cancellationReason }) : '{}',
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: '주문 취소에 실패했습니다.' }))
      if (response.status === 409) {
        throw new Error(error.message || '취소할 수 없는 주문 상태입니다.')
      }
      if (response.status === 404) {
        throw new Error(error.message || '주문을 찾을 수 없습니다.')
      }
      throw new Error(error.message || `주문 취소 실패 (HTTP ${response.status})`)
    }

    return await response.json() as CancelOrderResponse
  } catch (error) {
    console.error('Cancel order error:', error)
    if (error instanceof TokenRefreshError || error instanceof AuthRequiredError) throw error
    if (error instanceof Error) throw error
    throw new Error('주문 취소 중 오류가 발생했습니다.')
  }
}

/**
 * 반품 신청
 *
 * 배송 완료(DELIVERED) 상태의 주문에 대해 반품을 신청합니다.
 *
 * @param orderId - 주문 ID
 * @param reason - 반품 사유
 * @returns 반품 신청 결과
 */
export const requestReturn = async (
  orderId: number,
  reason?: string,
): Promise<ReturnOrderResponse> => {
  try {
    const response = await userFetch(`${API_BASE_URL}/api/orders/${orderId}/returns`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: reason ? JSON.stringify({ reason }) : '{}',
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: '반품 신청에 실패했습니다.' }))
      if (response.status === 409) {
        throw new Error(error.message || '반품 신청이 불가능한 상태입니다.')
      }
      if (response.status === 404) {
        throw new Error(error.message || '주문을 찾을 수 없습니다.')
      }
      throw new Error(error.message || `반품 신청 실패 (HTTP ${response.status})`)
    }

    return await response.json() as ReturnOrderResponse
  } catch (error) {
    console.error('Request return error:', error)
    if (error instanceof TokenRefreshError || error instanceof AuthRequiredError) throw error
    if (error instanceof Error) throw error
    throw new Error('반품 신청 중 오류가 발생했습니다.')
  }
}

/**
 * 교환 신청
 *
 * 배송 완료(DELIVERED) 상태의 주문에 대해 교환을 신청합니다.
 *
 * @param orderId - 주문 ID
 * @param request - 교환 신청 요청 (교환 상품 목록, 교환 사유)
 * @returns 교환 신청 결과
 */
export const requestExchange = async (
  orderId: number,
  request: ExchangeOrderRequest,
): Promise<ExchangeOrderResponse> => {
  try {
    const response = await userFetch(`${API_BASE_URL}/api/orders/${orderId}/exchanges`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: '교환 신청에 실패했습니다.' }))
      if (response.status === 409) {
        throw new Error(error.message || '교환 신청이 불가능한 상태입니다.')
      }
      if (response.status === 404) {
        throw new Error(error.message || '주문을 찾을 수 없습니다.')
      }
      if (response.status === 400) {
        throw new Error(error.message || '교환 신청 정보를 확인해주세요.')
      }
      throw new Error(error.message || `교환 신청 실패 (HTTP ${response.status})`)
    }

    return await response.json() as ExchangeOrderResponse
  } catch (error) {
    console.error('Request exchange error:', error)
    if (error instanceof TokenRefreshError || error instanceof AuthRequiredError) throw error
    if (error instanceof Error) throw error
    throw new Error('교환 신청 중 오류가 발생했습니다.')
  }
}

// ==================== Admin API Interfaces ====================

interface AdminOrderApiResponse {
  orderId: number
  orderNumber: string
  userId: number
  orderStatus: string
  totalProductAmount: number
  totalDiscountAmount: number
  totalPaymentAmount: number
  orderMemo?: string
  orderedAt: string
  updatedAt: string
}

interface AdminOrderItemApiResponse {
  orderItemId: number
  orderId: number
  productId: number
  productName: string
  productCode: string
  quantity: number
  unitPrice: number
  totalPrice: number
  createdAt: string
}

interface AdminOrderShippingApiResponse {
  shippingId: number
  orderId: number
  receiverName: string
  receiverPhone: string
  address: string
  postalCode?: string
  shippingStatus: string
  createdAt: string
}

interface AdminOrderDetailApiResponse extends AdminOrderApiResponse {
  orderItems: AdminOrderItemApiResponse[]
  orderShipping: AdminOrderShippingApiResponse | null
}

// ==================== Admin Mappers ====================

function mapOrder(data: AdminOrderApiResponse): Order {
  return {
    order_id: String(data.orderId),
    order_number: data.orderNumber,
    user_id: String(data.userId),
    order_status: data.orderStatus as Order['order_status'],
    total_product_amount: data.totalProductAmount,
    total_discount_amount: data.totalDiscountAmount,
    total_payment_amount: data.totalPaymentAmount,
    order_memo: data.orderMemo,
    ordered_at: data.orderedAt,
    updated_at: data.updatedAt,
  }
}

function mapOrderItem(data: AdminOrderItemApiResponse): OrderItem {
  return {
    order_item_id: String(data.orderItemId),
    order_id: String(data.orderId),
    product_id: String(data.productId),
    product_name: data.productName,
    product_code: data.productCode,
    quantity: data.quantity,
    unit_price: data.unitPrice,
    total_price: data.totalPrice,
    created_at: data.createdAt,
  }
}

function mapOrderShipping(data: AdminOrderShippingApiResponse): OrderShipping {
  return {
    shipping_id: String(data.shippingId),
    order_id: String(data.orderId),
    receiver_name: data.receiverName,
    receiver_phone: data.receiverPhone,
    address: data.address,
    postal_code: data.postalCode,
    shipping_status: data.shippingStatus as OrderShipping['shipping_status'],
    created_at: data.createdAt,
  }
}

// ==================== Admin API Functions ====================

/**
 * 관리자 주문 목록 페이지네이션 응답
 */
export interface AdminOrderPageResponse {
  content: Order[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

interface AdminOrderPageApiResponse {
  content: AdminOrderApiResponse[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

/**
 * 관리자 주문 목록 조회 (페이지네이션)
 *
 * @param orderNumber - 주문 번호 (부분 검색)
 * @param orderStatus - 주문 상태 필터
 * @param page - 페이지 번호 (0부터 시작)
 * @param size - 페이지 크기
 * @returns 페이지네이션된 주문 목록
 */
export const getAdminOrders = async (
  orderNumber?: string,
  orderStatus?: string,
  page: number = 0,
  size: number = 20,
): Promise<AdminOrderPageResponse> => {
  try {
    const queryParams = new URLSearchParams()
    if (orderNumber) queryParams.append('orderNumber', orderNumber)
    if (orderStatus) queryParams.append('orderStatus', orderStatus)
    queryParams.append('page', String(page))
    queryParams.append('size', String(size))

    const url = `${API_BASE_URL}/api/admin/orders?${queryParams.toString()}`

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
      throw new Error(error.message || `주문 목록 조회 실패 (HTTP ${response.status})`)
    }

    const data: AdminOrderPageApiResponse = await response.json()
    return {
      content: data.content.map(mapOrder),
      page: data.page,
      size: data.size,
      totalElements: data.totalElements,
      totalPages: data.totalPages,
      first: data.first,
      last: data.last,
    }
  } catch (error) {
    console.error('Get admin orders error:', error)
    if (error instanceof Error) throw error
    throw new Error('주문 목록 조회 중 오류가 발생했습니다.')
  }
}

/**
 * 관리자 주문 상세 조회
 *
 * @param orderId - 주문 ID
 * @returns 주문 상세 (주문 정보 + 상품 목록 + 배송 정보)
 */
export const getAdminOrderDetail = async (orderId: string): Promise<{
  order: Order
  orderItems: OrderItem[]
  orderShipping: OrderShipping | null
}> => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/admin/orders/${orderId}`, {
      method: 'GET',
      headers: getAdminHeaders(),
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Unknown error' }))
      if (response.status === 401) {
        throw new Error(error.message || '인증이 만료되었습니다. 다시 로그인해주세요.')
      }
      if (response.status === 404) {
        throw new Error(error.message || '주문을 찾을 수 없습니다.')
      }
      throw new Error(error.message || `주문 상세 조회 실패 (HTTP ${response.status})`)
    }

    const data: AdminOrderDetailApiResponse = await response.json()
    return {
      order: mapOrder(data),
      orderItems: data.orderItems.map(mapOrderItem),
      orderShipping: data.orderShipping ? mapOrderShipping(data.orderShipping) : null,
    }
  } catch (error) {
    console.error('Get admin order detail error:', error)
    if (error instanceof Error) throw error
    throw new Error('주문 상세 조회 중 오류가 발생했습니다.')
  }
}

/**
 * 관리자 주문 수정 (상태 및 메모)
 *
 * @param orderId - 주문 ID
 * @param orderStatus - 변경할 주문 상태
 * @param orderMemo - 주문 메모
 * @returns 수정된 주문 상세
 */
/**
 * 관리자 주문 취소
 *
 * @param orderId - 주문 ID
 * @param cancellationReason - 취소 사유 (생략 시 기본값 사용)
 * @returns 취소된 주문 정보
 */
export const cancelAdminOrder = async (
  orderId: string,
  cancellationReason?: string,
): Promise<CancelOrderResponse> => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/admin/orders/${orderId}/cancel`, {
      method: 'POST',
      headers: getAdminHeaders(),
      body: cancellationReason ? JSON.stringify({ cancellationReason }) : '{}',
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: '주문 취소에 실패했습니다.' }))
      if (response.status === 401) throw new Error(error.message || '인증이 만료되었습니다. 다시 로그인해주세요.')
      if (response.status === 409) throw new Error(error.message || '취소할 수 없는 주문 상태입니다.')
      if (response.status === 404) throw new Error(error.message || '주문을 찾을 수 없습니다.')
      throw new Error(error.message || `주문 취소 실패 (HTTP ${response.status})`)
    }

    return await response.json() as CancelOrderResponse
  } catch (error) {
    console.error('Cancel admin order error:', error)
    if (error instanceof Error) throw error
    throw new Error('주문 취소 중 오류가 발생했습니다.')
  }
}

export const updateAdminOrder = async (orderId: string, orderStatus: string, orderMemo: string): Promise<{
  order: Order
  orderItems: OrderItem[]
  orderShipping: OrderShipping | null
}> => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/admin/orders/${orderId}`, {
      method: 'PUT',
      headers: getAdminHeaders(),
      body: JSON.stringify({ orderStatus, orderMemo }),
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
        throw new Error(error.message || '주문을 찾을 수 없습니다.')
      }
      throw new Error(error.message || `주문 수정 실패 (HTTP ${response.status})`)
    }

    const data: AdminOrderDetailApiResponse = await response.json()
    return {
      order: mapOrder(data),
      orderItems: data.orderItems.map(mapOrderItem),
      orderShipping: data.orderShipping ? mapOrderShipping(data.orderShipping) : null,
    }
  } catch (error) {
    console.error('Update admin order error:', error)
    if (error instanceof Error) throw error
    throw new Error('주문 수정 중 오류가 발생했습니다.')
  }
}

// ==================== Exchange Helper Functions ====================

/**
 * 주문 상품과 상품 상세 정보를 함께 조회하는 인터페이스
 */
export interface OrderItemWithProductDetail {
  /** 주문 상품 정보 */
  orderItem: MyOrderItemResponse
  /** 상품 상세 정보 (SKU 옵션 포함) */
  productDetail: ProductDetailResponse | null
}

/**
 * 교환 신청을 위한 주문 상품 및 상품 상세 정보 조회
 *
 * 주문 ID로 주문 상품 목록을 조회하고, 각 상품의 상세 정보(SKU 옵션)를 함께 가져옵니다.
 *
 * @param orderId - 주문 ID
 * @returns 주문 상품과 상품 상세 정보 배열
 * @throws Error - 조회 실패 시
 */
export const getOrderItemsWithProductDetails = async (
  orderId: number
): Promise<OrderItemWithProductDetail[]> => {
  try {
    // 1. 주문 상세 정보 조회
    const ordersData = await getMyOrders(0, 100)
    const orderDetail = ordersData.content.find(o => o.orderId === orderId)

    if (!orderDetail || !orderDetail.items || orderDetail.items.length === 0) {
      throw new Error('주문 상품 정보를 찾을 수 없습니다.')
    }

    // 2. 각 상품의 상세 정보 조회 (병렬 처리)
    const itemsWithDetails = await Promise.all(
      orderDetail.items.map(async (orderItem) => {
        try {
          const productDetail = await getProductDetail(orderItem.productId)
          return {
            orderItem,
            productDetail
          }
        } catch (error) {
          console.error(`상품 ${orderItem.productId} 상세 정보 로드 실패:`, error)
          return {
            orderItem,
            productDetail: null
          }
        }
      })
    )

    return itemsWithDetails
  } catch (error) {
    console.error('Get order items with product details error:', error)
    if (error instanceof Error) throw error
    throw new Error('주문 상품 정보를 불러오는데 실패했습니다.')
  }
}

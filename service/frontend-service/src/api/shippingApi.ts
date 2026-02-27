/**
 * Shipping Service API Integration
 *
 * Type-safe API functions for shipping-service endpoints
 * All requests go through API Gateway
 */

import { API_BASE_URL } from '../config/env'
import { getAdminHeaders, userFetch } from '../utils/apiHelper'

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

/**
 * 사용자 배송 조회 응답 DTO
 */
export interface MarketShippingResponse {
  /** 배송 ID */
  shippingId: number
  /** 주문 ID */
  orderId: number
  /** 주문 번호 */
  orderNumber: string
  /** 배송 상태 */
  shippingStatus: ShippingStatus
  /** 배송사 */
  shippingCompany?: string
  /** 운송장 번호 */
  trackingNumber?: string
  /** 수령인 이름 */
  receiverName: string
  /** 수령인 연락처 */
  receiverPhone: string
  /** 배송 주소 */
  address: string
  /** 우편번호 */
  postalCode?: string
  /** 배송사 연동 상태 */
  deliveryServiceStatus?: DeliveryServiceStatus
  /** 생성 일시 */
  createdAt: string
  /** 수정 일시 */
  updatedAt: string
}

/**
 * 운송장 등록 요청 DTO
 */
export interface RegisterTrackingRequest {
  /** 택배사 코드 (01: 우체국, 04: CJ대한통운, 05: 한진, 06: 로젠, 08: 롯데) */
  carrierCode: string
}

/**
 * 반품 상태
 */
export type ReturnStatus = 'RETURN_REQUESTED' | 'RETURN_APPROVED' | 'RETURN_IN_TRANSIT' | 'RETURN_REJECTED' | 'RETURNED'

/**
 * 관리자 반품 응답 DTO
 */
export interface AdminReturnResponse {
  /** 반품 ID */
  returnId: number
  /** 주문 ID */
  orderId: number
  /** 사용자 ID */
  userId: number
  /** 반품 상태 */
  returnStatus: ReturnStatus
  /** 반품 사유 */
  reason?: string
  /** 거절 사유 */
  rejectReason?: string
  /** 택배사 */
  courier?: string
  /** 운송장 번호 */
  trackingNumber?: string
  /** 수거지 수령인 */
  receiverName?: string
  /** 수거지 연락처 */
  receiverPhone?: string
  /** 수거지 주소 */
  returnAddress?: string
  /** 우편번호 */
  postalCode?: string
  /** 신청 일시 */
  requestedAt: string
  /** 수정 일시 */
  updatedAt: string
}

/**
 * 사용자 반품 조회 응답 DTO
 */
export interface MarketReturnResponse {
  /** 반품 ID */
  returnId: number
  /** 주문 ID */
  orderId: number
  /** 반품 상태 */
  returnStatus: ReturnStatus
  /** 반품 사유 */
  reason?: string
  /** 거절 사유 */
  rejectReason?: string
  /** 택배사 */
  courier?: string
  /** 운송장 번호 */
  trackingNumber?: string
  /** 수거지 주소 */
  returnAddress?: string
  /** 신청 일시 */
  requestedAt: string
  /** 수정 일시 */
  updatedAt: string
}

/**
 * 반품 승인 요청 DTO
 */
export interface AdminReturnApproveRequest {
  /** 수거지 수령인 */
  receiverName: string
  /** 수거지 연락처 */
  receiverPhone: string
  /** 수거지 주소 */
  returnAddress: string
  /** 우편번호 */
  postalCode?: string
}

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

/**
 * 운송장 등록
 * 택배사 API를 통해 운송장을 발급하고 배송 정보를 업데이트합니다.
 *
 * @param shippingId - 배송 ID
 * @param request    - 운송장 등록 요청 (택배사 코드)
 * @returns 업데이트된 배송 정보
 */
export const registerTracking = async (
  shippingId: number,
  request: RegisterTrackingRequest,
): Promise<AdminShippingResponse> => {
  try {
    const url = `${API_BASE_URL}/api/admin/shipping/${shippingId}/tracking`

    const response = await fetch(url, {
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
      if (response.status === 404) {
        throw new Error(error.message || '배송 정보를 찾을 수 없습니다.')
      }
      if (response.status === 409) {
        throw new Error(error.message || '이미 택배사에 전송된 배송 건입니다.')
      }
      throw new Error(error.message || `운송장 등록 실패 (HTTP ${response.status})`)
    }

    const data: AdminShippingResponse = await response.json()
    return data
  } catch (error) {
    console.error('Register tracking error:', error)
    if (error instanceof Error) throw error
    throw new Error('운송장 등록 중 오류가 발생했습니다.')
  }
}

/**
 * 내 배송 목록 조회
 *
 * @param page - 페이지 번호 (0부터 시작)
 * @param size - 페이지 크기
 * @returns 페이지네이션된 배송 목록
 */
export const getMyShippings = async (
  page: number = 0,
  size: number = 10,
): Promise<PageResponse<MarketShippingResponse>> => {
  try {
    const queryParams = new URLSearchParams()
    queryParams.append('page', String(page))
    queryParams.append('size', String(size))
    queryParams.append('sort', 'updatedAt,desc')

    const url = `${API_BASE_URL}/api/shipping?${queryParams.toString()}`

    const response = await userFetch(url, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Unknown error' }))
      if (response.status === 401) {
        throw new Error(error.message || '인증이 만료되었습니다. 다시 로그인해주세요.')
      }
      throw new Error(error.message || `배송 조회 실패 (HTTP ${response.status})`)
    }

    const data: PageResponse<MarketShippingResponse> = await response.json()
    return data
  } catch (error) {
    console.error('Get my shippings error:', error)
    if (error instanceof Error) throw error
    throw new Error('배송 조회 중 오류가 발생했습니다.')
  }
}

/**
 * 반품/교환 신청 가능한 배송 목록 조회
 *
 * 배송 완료(DELIVERED) 상태이며 진행 중인 반품/교환이 없는 주문 목록을 반환합니다.
 *
 * @returns 반품/교환 신청 가능한 배송 목록
 */
export const getReturnableShippings = async (): Promise<MarketShippingResponse[]> => {
  try {
    const url = `${API_BASE_URL}/api/shipping/returnable`

    const response = await userFetch(url, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Unknown error' }))
      if (response.status === 401) {
        throw new Error(error.message || '인증이 만료되었습니다. 다시 로그인해주세요.')
      }
      throw new Error(error.message || `반품 가능 목록 조회 실패 (HTTP ${response.status})`)
    }

    const data: MarketShippingResponse[] = await response.json()
    return data
  } catch (error) {
    console.error('Get returnable shippings error:', error)
    if (error instanceof Error) throw error
    throw new Error('반품 가능 목록 조회 중 오류가 발생했습니다.')
  }
}

// ==================== Return Management APIs ====================

/**
 * 관리자 반품 목록 조회 (페이지네이션)
 *
 * @param returnStatus - 반품 상태 필터
 * @param orderNumber - 주문 번호
 * @param page - 페이지 번호 (0부터 시작)
 * @param size - 페이지 크기
 * @returns 페이지네이션된 반품 목록
 */
export const getAdminReturns = async (
  returnStatus?: string,
  orderNumber?: string,
  page: number = 0,
  size: number = 20,
): Promise<PageResponse<AdminReturnResponse>> => {
  try {
    const queryParams = new URLSearchParams()
    queryParams.append('page', String(page))
    queryParams.append('size', String(size))
    queryParams.append('sort', 'updatedAt,desc')
    if (returnStatus) queryParams.append('returnStatus', returnStatus)
    if (orderNumber) queryParams.append('orderNumber', orderNumber)

    const url = `${API_BASE_URL}/api/admin/shipping/returns?${queryParams.toString()}`

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
      throw new Error(error.message || `반품 목록 조회 실패 (HTTP ${response.status})`)
    }

    const data: PageResponse<AdminReturnResponse> = await response.json()
    return data
  } catch (error) {
    console.error('Get admin returns error:', error)
    if (error instanceof Error) throw error
    throw new Error('반품 목록 조회 중 오류가 발생했습니다.')
  }
}

/**
 * 반품 승인
 *
 * 반품을 승인하고 수거지 정보를 설정합니다.
 * RETURN_REQUESTED 상태에서만 가능합니다.
 *
 * @param returnId - 반품 ID
 * @param request - 반품 승인 요청
 * @returns 업데이트된 반품 정보
 */
export const approveReturn = async (
  returnId: number,
  request: AdminReturnApproveRequest,
): Promise<AdminReturnResponse> => {
  try {
    const url = `${API_BASE_URL}/api/admin/shipping/returns/${returnId}/approve`

    const response = await fetch(url, {
      method: 'PATCH',
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
      if (response.status === 404) {
        throw new Error(error.message || '반품 정보를 찾을 수 없습니다.')
      }
      if (response.status === 409) {
        throw new Error(error.message || '승인 불가 상태입니다.')
      }
      throw new Error(error.message || `반품 승인 실패 (HTTP ${response.status})`)
    }

    const data: AdminReturnResponse = await response.json()
    return data
  } catch (error) {
    console.error('Approve return error:', error)
    if (error instanceof Error) throw error
    throw new Error('반품 승인 중 오류가 발생했습니다.')
  }
}

/**
 * 반품 완료 처리
 *
 * 반품을 완료 처리합니다. order_shipping 상태도 RETURNED로 변경됩니다.
 * RETURN_APPROVED 상태에서만 가능합니다.
 *
 * @param returnId - 반품 ID
 * @returns 업데이트된 반품 정보
 */
export const completeReturn = async (
  returnId: number,
): Promise<AdminReturnResponse> => {
  try {
    const url = `${API_BASE_URL}/api/admin/shipping/returns/${returnId}/complete`

    const response = await fetch(url, {
      method: 'PATCH',
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
        throw new Error(error.message || '반품 정보를 찾을 수 없습니다.')
      }
      if (response.status === 409) {
        throw new Error(error.message || '완료 불가 상태입니다.')
      }
      throw new Error(error.message || `반품 완료 처리 실패 (HTTP ${response.status})`)
    }

    const data: AdminReturnResponse = await response.json()
    return data
  } catch (error) {
    console.error('Complete return error:', error)
    if (error instanceof Error) throw error
    throw new Error('반품 완료 처리 중 오류가 발생했습니다.')
  }
}

/**
 * 내 반품 목록 조회
 *
 * 로그인한 사용자의 반품 목록을 최신순으로 조회합니다.
 *
 * @param page - 페이지 번호 (0부터 시작)
 * @param size - 페이지 크기
 * @returns 페이지네이션된 반품 목록
 */
export const getMyReturns = async (
  page: number = 0,
  size: number = 10,
): Promise<PageResponse<MarketReturnResponse>> => {
  try {
    const queryParams = new URLSearchParams()
    queryParams.append('page', String(page))
    queryParams.append('size', String(size))
    queryParams.append('sort', 'updatedAt,desc')

    const url = `${API_BASE_URL}/api/shipping/returns?${queryParams.toString()}`

    const response = await userFetch(url, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Unknown error' }))
      if (response.status === 401) {
        throw new Error(error.message || '인증이 만료되었습니다. 다시 로그인해주세요.')
      }
      throw new Error(error.message || `반품 목록 조회 실패 (HTTP ${response.status})`)
    }

    const data: PageResponse<MarketReturnResponse> = await response.json()
    return data
  } catch (error) {
    console.error('Get my returns error:', error)
    if (error instanceof Error) throw error
    throw new Error('반품 목록 조회 중 오류가 발생했습니다.')
  }
}

// ==================== Exchange Management APIs ====================

/**
 * 교환 상태
 */
export type ExchangeStatus =
  | 'EXCHANGE_REQUESTED'        // 교환 신청
  | 'EXCHANGE_APPROVED'         // 교환 승인 (회수 운송장 발급)
  | 'EXCHANGE_COLLECTING'       // 회수 중
  | 'EXCHANGE_RETURN_COMPLETED' // 회수 완료
  | 'EXCHANGE_SHIPPING'         // 교환품 발송 중
  | 'EXCHANGED'                 // 교환 완료
  | 'EXCHANGE_REJECTED'         // 교환 거절

/**
 * 관리자 교환 응답 DTO
 */
export interface AdminExchangeResponse {
  /** 교환 ID */
  exchangeId: number
  /** 주문 ID */
  orderId: number
  /** 사용자 ID */
  userId: number
  /** 교환 상품 목록 */
  exchangeItems: ExchangeItemDto[]
  /** 교환 상태 */
  exchangeStatus: ExchangeStatus
  /** 교환 사유 */
  reason?: string
  /** 거절 사유 */
  rejectReason?: string
  /** 회수 택배사 */
  collectCourier?: string
  /** 회수 운송장 번호 */
  collectTrackingNumber?: string
  /** 회수 수령인 */
  collectReceiverName?: string
  /** 회수 수령인 연락처 */
  collectReceiverPhone?: string
  /** 회수 주소 */
  collectAddress?: string
  /** 회수 우편번호 */
  collectPostalCode?: string
  /** 교환 배송 택배사 */
  courier?: string
  /** 교환 배송 운송장 번호 */
  trackingNumber?: string
  /** 교환품 수령인 */
  receiverName?: string
  /** 교환품 수령 연락처 */
  receiverPhone?: string
  /** 교환품 배송 주소 */
  exchangeAddress?: string
  /** 교환품 배송 우편번호 */
  postalCode?: string
  /** 신청 일시 */
  requestedAt: string
  /** 수정 일시 */
  updatedAt: string
}

/**
 * 교환 품목 DTO
 */
export interface ExchangeItemDto {
  /** 주문 상품 ID */
  orderItemId: number
  /** 원래 옵션 ID */
  originalOptionId: number
  /** 새 옵션 ID */
  newOptionId: number
  /** 교환 수량 */
  quantity: number
}

/**
 * 교환 승인 요청 DTO (회수 수거지 정보)
 */
export interface AdminExchangeApproveRequest {
  /** 회수 수령인 */
  collectReceiverName: string
  /** 회수 수령인 연락처 */
  collectReceiverPhone: string
  /** 회수 주소 */
  collectAddress: string
  /** 회수 우편번호 */
  collectPostalCode?: string
}

/**
 * 교환 배송 시작 요청 DTO
 */
export interface AdminExchangeStartShippingRequest {
  /** 택배사 코드 (01: 우체국, 04: CJ대한통운, 05: 한진택배, 06: 로젠택배, 08: 롯데택배) */
  carrierCode: string
}

/**
 * 관리자 교환 목록 조회 (페이지네이션)
 *
 * @param exchangeStatus - 교환 상태 필터
 * @param orderNumber - 주문 번호
 * @param page - 페이지 번호 (0부터 시작)
 * @param size - 페이지 크기
 * @returns 페이지네이션된 교환 목록
 */
export const getAdminExchanges = async (
  exchangeStatus?: string,
  orderNumber?: string,
  page: number = 0,
  size: number = 20,
): Promise<PageResponse<AdminExchangeResponse>> => {
  try {
    const queryParams = new URLSearchParams()
    queryParams.append('page', String(page))
    queryParams.append('size', String(size))
    queryParams.append('sort', 'updatedAt,desc')
    if (exchangeStatus) queryParams.append('exchangeStatus', exchangeStatus)
    if (orderNumber) queryParams.append('orderNumber', orderNumber)

    const url = `${API_BASE_URL}/api/admin/shipping/exchanges?${queryParams.toString()}`

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
      throw new Error(error.message || `교환 목록 조회 실패 (HTTP ${response.status})`)
    }

    const data: PageResponse<AdminExchangeResponse> = await response.json()
    return data
  } catch (error) {
    console.error('Get admin exchanges error:', error)
    if (error instanceof Error) throw error
    throw new Error('교환 목록 조회 중 오류가 발생했습니다.')
  }
}


/**
 * 교환 승인
 *
 * 교환을 승인하고 교환품 배송지를 설정합니다.
 * Mock 택배사 API를 통해 교환품 송장이 자동 발급됩니다.
 * EXCHANGE_REQUESTED 상태에서만 가능합니다.
 *
 * @param exchangeId - 교환 ID
 * @param request - 교환 승인 요청 (교환품 배송지 정보)
 * @returns 업데이트된 교환 정보
 */
export const approveExchange = async (
  exchangeId: number,
  request: AdminExchangeApproveRequest,
): Promise<AdminExchangeResponse> => {
  try {
    const url = `${API_BASE_URL}/api/admin/shipping/exchanges/${exchangeId}/approve`

    const response = await fetch(url, {
      method: 'PATCH',
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
      if (response.status === 404) {
        throw new Error(error.message || '교환 정보를 찾을 수 없습니다.')
      }
      if (response.status === 409) {
        throw new Error(error.message || '승인 불가 상태입니다.')
      }
      throw new Error(error.message || `교환 승인 실패 (HTTP ${response.status})`)
    }

    const data: AdminExchangeResponse = await response.json()
    return data
  } catch (error) {
    console.error('Approve exchange error:', error)
    if (error instanceof Error) throw error
    throw new Error('교환 승인 중 오류가 발생했습니다.')
  }
}

/**
 * 교환 배송 시작
 *
 * 원주문 배송지 기반으로 교환품 운송장을 자동 발급합니다.
 * EXCHANGE_RETURN_COMPLETED 상태에서만 가능합니다.
 *
 * @param exchangeId - 교환 ID
 * @param request - 택배사 코드
 * @returns 업데이트된 교환 정보
 */
export const startExchangeShipping = async (
  exchangeId: number,
  request: AdminExchangeStartShippingRequest,
): Promise<AdminExchangeResponse> => {
  try {
    const url = `${API_BASE_URL}/api/admin/shipping/exchanges/${exchangeId}/shipping`

    const response = await fetch(url, {
      method: 'PATCH',
      headers: getAdminHeaders(),
      body: JSON.stringify(request),
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Unknown error' }))
      if (response.status === 401) throw new Error(error.message || '인증이 만료되었습니다. 다시 로그인해주세요.')
      if (response.status === 403) throw new Error(error.message || '접근 권한이 없습니다.')
      if (response.status === 404) throw new Error(error.message || '교환 정보를 찾을 수 없습니다.')
      if (response.status === 409) throw new Error(error.message || '배송 시작 불가 상태입니다.')
      throw new Error(error.message || `교환 배송 시작 실패 (HTTP ${response.status})`)
    }

    return response.json()
  } catch (error) {
    console.error('Start exchange shipping error:', error)
    if (error instanceof Error) throw error
    throw new Error('교환 배송 시작 중 오류가 발생했습니다.')
  }
}

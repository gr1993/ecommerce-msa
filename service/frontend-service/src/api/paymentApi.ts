/**
 * Payment Service API Integration
 *
 * 토스페이먼츠 결제 승인 및 관련 API
 *
 * 토큰 정책:
 * - API 요청 전 Access Token 만료 시간 확인
 * - 만료되었거나 5분 이내 만료 예정 시 자동 갱신
 * - 갱신 실패 시 로그아웃 유도
 */

import { API_BASE_URL } from '../config/env'
import { authenticatedFetch, TokenRefreshError, AuthRequiredError } from '../utils/authFetch'

/**
 * 결제 승인 요청 DTO
 */
export interface PaymentConfirmRequest {
  /** 토스페이먼츠 paymentKey */
  paymentKey: string
  /** 주문번호 */
  orderId: string
  /** 결제 금액 */
  amount: number
}

/**
 * 결제 승인 응답 DTO
 */
export interface PaymentConfirmResponse {
  /** 결제 ID */
  paymentId: number
  /** 주문 ID */
  orderId: number
  /** 주문번호 */
  orderNumber: string
  /** 결제 금액 */
  amount: number
  /** 결제 상태 */
  paymentStatus: string
  /** 결제 방법 */
  paymentMethod: string
  /** 결제 승인 일시 */
  approvedAt: string
}

/**
 * 결제 승인 API
 *
 * 토스페이먼츠에서 전달받은 paymentKey로 결제를 최종 승인합니다.
 * Access Token 만료 시 자동으로 갱신 후 요청을 수행합니다.
 *
 * @param request - 결제 승인 요청 데이터
 * @returns 결제 승인 응답
 * @throws Error - 결제 승인 실패 시
 * @throws TokenRefreshError - 토큰 갱신 실패 시 (재로그인 필요)
 * @throws AuthRequiredError - 로그인되어 있지 않은 경우
 */
export const confirmPayment = async (request: PaymentConfirmRequest): Promise<PaymentConfirmResponse> => {
  try {
    // authenticatedFetch가 토큰 만료 확인 및 자동 갱신 처리
    const response = await authenticatedFetch(`${API_BASE_URL}/api/payments/confirm`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    })

    if (!response.ok) {
      if (response.status === 403) {
        throw new Error('결제 권한이 없습니다.')
      }
      const error = await response.json().catch(() => ({ message: '결제 승인에 실패했습니다.' }))
      throw new Error(error.message || `결제 승인 실패 (HTTP ${response.status})`)
    }

    const data: PaymentConfirmResponse = await response.json()
    return data
  } catch (error) {
    console.error('Confirm payment error:', error)

    // 토큰 갱신 실패 또는 인증 필요 에러는 그대로 전파
    if (error instanceof TokenRefreshError || error instanceof AuthRequiredError) {
      throw error
    }

    if (error instanceof Error) {
      throw error
    }

    throw new Error('결제 승인 중 오류가 발생했습니다. 네트워크 연결을 확인해주세요.')
  }
}

/**
 * Payment Service API Integration
 *
 * 토스페이먼츠 결제 승인 및 관련 API
 */

import { API_BASE_URL } from '../config/env'
import { useAuthStore } from '../stores/authStore'

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
 *
 * @param request - 결제 승인 요청 데이터
 * @returns 결제 승인 응답
 * @throws Error - 결제 승인 실패 시
 */
export const confirmPayment = async (request: PaymentConfirmRequest): Promise<PaymentConfirmResponse> => {
  const token = useAuthStore.getState().userToken

  try {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    }

    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }

    const response = await fetch(`${API_BASE_URL}/api/payments/confirm`, {
      method: 'POST',
      headers,
      body: JSON.stringify(request),
    })

    if (!response.ok) {
      if (response.status === 401) {
        throw new Error('로그인이 필요합니다.')
      }
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

    if (error instanceof Error) {
      throw error
    }

    throw new Error('결제 승인 중 오류가 발생했습니다. 네트워크 연결을 확인해주세요.')
  }
}

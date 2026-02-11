/**
 * 결제 유틸리티 함수
 * 토스페이먼츠 SDK 연동
 */

import type { TossPaymentSuccessParams, TossPaymentFailParams } from '../types/tossPayments'

const TOSS_CLIENT_KEY = import.meta.env.VITE_TOSS_CLIENT_KEY

export interface PaymentRequest {
  orderId: string
  orderName: string
  totalAmount: number
  customerEmail?: string
  customerName?: string
  customerMobilePhone?: string
}

export interface PaymentResponse {
  success: boolean
  orderId?: string
  paymentId?: string
  receiptId?: string
  error?: string
}

/**
 * 토스페이먼츠 결제 요청
 * 결제 완료 후 successUrl/failUrl로 리다이렉트됨
 */
export const requestTossPayment = async (
  paymentRequest: PaymentRequest,
  customerKey: string
): Promise<void> => {
  if (!window.TossPayments) {
    throw new Error('토스페이먼츠 SDK가 로드되지 않았습니다.')
  }

  if (!TOSS_CLIENT_KEY) {
    throw new Error('토스페이먼츠 클라이언트 키가 설정되지 않았습니다.')
  }

  const tossPayments = window.TossPayments(TOSS_CLIENT_KEY)
  const payment = tossPayments.payment({ customerKey })

  const baseUrl = window.location.origin

  await payment.requestPayment({
    method: 'CARD',
    amount: {
      currency: 'KRW',
      value: paymentRequest.totalAmount,
    },
    orderId: paymentRequest.orderId,
    orderName: paymentRequest.orderName,
    successUrl: `${baseUrl}/market/payment/success`,
    failUrl: `${baseUrl}/market/payment/fail`,
    customerEmail: paymentRequest.customerEmail,
    customerName: paymentRequest.customerName,
    customerMobilePhone: paymentRequest.customerMobilePhone?.replace(/-/g, ''),
    card: {
      useEscrow: false,
      flowMode: 'DEFAULT',
      useCardPoint: false,
      useAppCardOnly: false,
    },
  })
}

/**
 * 비회원 결제 요청
 */
export const requestTossPaymentAnonymous = async (
  paymentRequest: PaymentRequest
): Promise<void> => {
  if (!window.TossPayments) {
    throw new Error('토스페이먼츠 SDK가 로드되지 않았습니다.')
  }

  if (!TOSS_CLIENT_KEY) {
    throw new Error('토스페이먼츠 클라이언트 키가 설정되지 않았습니다.')
  }

  const tossPayments = window.TossPayments(TOSS_CLIENT_KEY)
  const payment = tossPayments.payment({ customerKey: window.TossPayments.ANONYMOUS })

  const baseUrl = window.location.origin

  await payment.requestPayment({
    method: 'CARD',
    amount: {
      currency: 'KRW',
      value: paymentRequest.totalAmount,
    },
    orderId: paymentRequest.orderId,
    orderName: paymentRequest.orderName,
    successUrl: `${baseUrl}/market/payment/success`,
    failUrl: `${baseUrl}/market/payment/fail`,
    customerEmail: paymentRequest.customerEmail,
    customerName: paymentRequest.customerName,
    customerMobilePhone: paymentRequest.customerMobilePhone?.replace(/-/g, ''),
    card: {
      useEscrow: false,
      flowMode: 'DEFAULT',
      useCardPoint: false,
      useAppCardOnly: false,
    },
  })
}

/**
 * URL 쿼리 파라미터에서 결제 성공 정보 추출
 */
export const parsePaymentSuccessParams = (searchParams: URLSearchParams): TossPaymentSuccessParams | null => {
  const paymentKey = searchParams.get('paymentKey')
  const orderId = searchParams.get('orderId')
  const amount = searchParams.get('amount')

  if (!paymentKey || !orderId || !amount) {
    return null
  }

  return { paymentKey, orderId, amount }
}

/**
 * URL 쿼리 파라미터에서 결제 실패 정보 추출
 */
export const parsePaymentFailParams = (searchParams: URLSearchParams): TossPaymentFailParams | null => {
  const code = searchParams.get('code')
  const message = searchParams.get('message')
  const orderId = searchParams.get('orderId')

  if (!code || !message) {
    return null
  }

  return { code, message, orderId: orderId || undefined }
}

/**
 * 결제 방법 한글명
 */
export const getPaymentMethodName = (paymentMethod: string): string => {
  const nameMap: Record<string, string> = {
    CARD: '신용카드',
    TRANSFER: '계좌이체',
    VIRTUAL_ACCOUNT: '가상계좌',
    MOBILE_PHONE: '휴대폰 결제',
  }
  return nameMap[paymentMethod] || '신용카드'
}

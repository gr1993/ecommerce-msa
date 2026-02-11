/**
 * 토스페이먼츠 SDK v2 타입 정의
 * https://docs.tosspayments.com/reference/widget-sdk
 */

declare global {
  interface Window {
    TossPayments: TossPaymentsConstructor
  }
}

interface TossPaymentsConstructor {
  (clientKey: string): TossPaymentsInstance
  ANONYMOUS: symbol
}

interface TossPaymentsInstance {
  payment: (options: PaymentOptions) => PaymentWidget
}

interface PaymentOptions {
  customerKey: string | symbol
}

interface PaymentWidget {
  requestPayment: (options: PaymentRequestOptions) => Promise<void>
}

interface PaymentRequestOptions {
  method: 'CARD' | 'TRANSFER' | 'VIRTUAL_ACCOUNT' | 'MOBILE_PHONE' | 'CULTURE_GIFT_CERTIFICATE' | 'FOREIGN_EASY_PAY'
  amount: {
    currency: 'KRW'
    value: number
  }
  orderId: string
  orderName: string
  successUrl: string
  failUrl: string
  customerEmail?: string
  customerName?: string
  customerMobilePhone?: string
  card?: {
    useEscrow?: boolean
    flowMode?: 'DEFAULT' | 'DIRECT'
    useCardPoint?: boolean
    useAppCardOnly?: boolean
  }
}

export interface TossPaymentSuccessParams {
  paymentKey: string
  orderId: string
  amount: string
}

export interface TossPaymentFailParams {
  code: string
  message: string
  orderId?: string
}

export {}

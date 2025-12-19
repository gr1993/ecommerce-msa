/**
 * 결제 유틸리티 함수
 * 부트페이(Bootpay) 등 외부 결제 서비스 연동을 위한 유틸리티
 */

export interface PaymentRequest {
  orderId: string
  orderName: string
  totalAmount: number
  paymentMethod: string
  items: Array<{
    id: string
    name: string
    qty: number
    price: number
  }>
  userInfo?: {
    username?: string
    email?: string
    phone?: string
  }
  shippingInfo?: {
    receiver_name: string
    receiver_phone: string
    postal_code: string
    address: string
    address_detail: string
  }
}

export interface PaymentResponse {
  success: boolean
  orderId?: string
  paymentId?: string
  receiptId?: string
  error?: string
}

/**
 * 부트페이 결제 요청
 * TODO: 실제 부트페이 SDK 연동
 * 
 * @example
 * // 부트페이 SDK 설치 필요: npm install @bootpay/client-js
 * // import Bootpay from '@bootpay/client-js'
 * 
 * const bootpay = Bootpay.setApplicationId('YOUR_APPLICATION_ID', 'YOUR_PRIVATE_KEY')
 * 
 * const response = await bootpay.request({
 *   price: paymentRequest.totalAmount,
 *   application_id: 'YOUR_APPLICATION_ID',
 *   name: paymentRequest.orderName,
 *   order_id: paymentRequest.orderId,
 *   pg: getPaymentPg(paymentRequest.paymentMethod),
 *   method: getPaymentMethod(paymentRequest.paymentMethod),
 *   items: paymentRequest.items,
 *   user_info: paymentRequest.userInfo,
 *   extra: {
 *     shipping_info: paymentRequest.shippingInfo
 *   }
 * })
 */
export const requestPayment = async (paymentRequest: PaymentRequest): Promise<PaymentResponse> => {
  try {
    // TODO: 부트페이 SDK 연동
    // 1. 부트페이 SDK 초기화
    // 2. 결제 요청 API 호출
    // 3. 결제 결과 반환
    
    console.log('Payment Request:', paymentRequest)
    
    // 임시: 실제 결제 API 호출 시뮬레이션
    await new Promise(resolve => setTimeout(resolve, 2000))
    
    // TODO: 실제 부트페이 API 호출
    // const bootpay = Bootpay.setApplicationId('YOUR_APPLICATION_ID', 'YOUR_PRIVATE_KEY')
    // const response = await bootpay.request({ ... })
    
    return {
      success: true,
      orderId: paymentRequest.orderId,
      paymentId: `PAY-${Date.now()}`,
      receiptId: `RECEIPT-${Date.now()}`
    }
  } catch (error) {
    console.error('Payment error:', error)
    return {
      success: false,
      error: error instanceof Error ? error.message : '결제 처리 중 오류가 발생했습니다.'
    }
  }
}

/**
 * 결제 방법에 따른 부트페이 PG 설정
 */
export const getPaymentPg = (paymentMethod: string): string => {
  const pgMap: Record<string, string> = {
    CARD: '나이스페이', // 또는 '다날', 'KG이니시스' 등
    BANK_TRANSFER: '나이스페이',
    VIRTUAL_ACCOUNT: '나이스페이',
    MOBILE: '나이스페이',
    EASYPAY: '나이스페이'
  }
  return pgMap[paymentMethod] || '나이스페이'
}

/**
 * 결제 방법에 따른 부트페이 method 설정
 */
export const getPaymentMethod = (paymentMethod: string): string[] => {
  const methodMap: Record<string, string[]> = {
    CARD: ['card'],
    BANK_TRANSFER: ['bank'],
    VIRTUAL_ACCOUNT: ['vbank'],
    MOBILE: ['phone'],
    EASYPAY: ['easy']
  }
  return methodMap[paymentMethod] || ['card']
}

/**
 * 결제 방법 한글명
 */
export const getPaymentMethodName = (paymentMethod: string): string => {
  const nameMap: Record<string, string> = {
    CARD: '신용카드',
    BANK_TRANSFER: '계좌이체',
    VIRTUAL_ACCOUNT: '가상계좌',
    MOBILE: '휴대폰 결제',
    EASYPAY: '간편결제'
  }
  return nameMap[paymentMethod] || '신용카드'
}

/**
 * 결제 완료 후 검증
 * TODO: 부트페이 receipt_id로 실제 결제 검증
 */
export const verifyPayment = async (receiptId: string): Promise<boolean> => {
  try {
    // TODO: 부트페이 결제 검증 API 호출
    // const bootpay = Bootpay.setApplicationId('YOUR_APPLICATION_ID', 'YOUR_PRIVATE_KEY')
    // const response = await bootpay.verify(receiptId)
    // return response.status === 1
    
    console.log('Verify payment:', receiptId)
    return true
  } catch (error) {
    console.error('Payment verification error:', error)
    return false
  }
}


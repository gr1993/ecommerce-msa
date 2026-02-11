import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Card, Result, Button } from 'antd'
import MarketHeader from '../../components/market/MarketHeader'
import MarketFooter from '../../components/market/MarketFooter'
import { parsePaymentFailParams } from '../../utils/paymentUtils'
import { usePendingOrderStore } from '../../stores/pendingOrderStore'
import './MarketPaymentFail.css'

function MarketPaymentFail() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [errorCode, setErrorCode] = useState<string>('')
  const [errorMessage, setErrorMessage] = useState<string>('')
  const [orderId, setOrderId] = useState<string>('')
  const clearPendingOrder = usePendingOrderStore((state) => state.clearPendingOrder)

  useEffect(() => {
    const failParams = parsePaymentFailParams(searchParams)

    if (failParams) {
      setErrorCode(failParams.code)
      setErrorMessage(failParams.message)
      setOrderId(failParams.orderId || '')
    }

    // store에서 임시 주문 정보 삭제 (실패했으므로)
    clearPendingOrder()
  }, [searchParams, clearPendingOrder])

  const getErrorDescription = (code: string): string => {
    const errorMap: Record<string, string> = {
      PAY_PROCESS_CANCELED: '사용자가 결제를 취소했습니다.',
      PAY_PROCESS_ABORTED: '결제가 중단되었습니다.',
      REJECT_CARD_COMPANY: '카드사에서 결제를 거부했습니다.',
      INVALID_CARD_EXPIRATION: '카드 유효기간이 만료되었습니다.',
      EXCEED_MAX_DAILY_PAYMENT_COUNT: '일일 결제 한도를 초과했습니다.',
      NOT_SUPPORTED_INSTALLMENT_PLAN: '지원하지 않는 할부 기간입니다.',
      INVALID_STOPPED_CARD: '정지된 카드입니다.',
      EXCEED_MAX_PAYMENT_AMOUNT: '결제 한도를 초과했습니다.',
      NOT_ALLOWED_POINT_USE: '포인트 사용이 불가능합니다.',
    }
    return errorMap[code] || '결제 처리 중 오류가 발생했습니다.'
  }

  return (
    <div className="market-payment-fail">
      <MarketHeader />

      <div className="payment-fail-container">
        <Card className="payment-fail-card">
          <Result
            status="error"
            title="결제에 실패했습니다"
            subTitle={getErrorDescription(errorCode)}
            extra={[
              <Button
                type="primary"
                key="retry"
                onClick={() => navigate('/market/order')}
              >
                다시 주문하기
              </Button>,
              <Button key="cart" onClick={() => navigate('/market/cart')}>
                장바구니로 돌아가기
              </Button>,
            ]}
          >
            <div className="error-details">
              {orderId && (
                <p>
                  <strong>주문번호:</strong> {orderId}
                </p>
              )}
              <p>
                <strong>오류 코드:</strong> {errorCode}
              </p>
              <p>
                <strong>오류 내용:</strong> {errorMessage}
              </p>
            </div>
          </Result>
        </Card>
      </div>

      <MarketFooter />
    </div>
  )
}

export default MarketPaymentFail

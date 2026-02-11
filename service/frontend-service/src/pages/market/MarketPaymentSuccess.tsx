import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Card, Spin, Result, Button } from 'antd'
import { LoadingOutlined } from '@ant-design/icons'
import MarketHeader from '../../components/market/MarketHeader'
import MarketFooter from '../../components/market/MarketFooter'
import { parsePaymentSuccessParams } from '../../utils/paymentUtils'
import { confirmPayment } from '../../api/paymentApi'
import { useCartStore } from '../../stores/cartStore'
import { usePendingOrderStore } from '../../stores/pendingOrderStore'
import './MarketPaymentSuccess.css'

function MarketPaymentSuccess() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const removeFromCart = useCartStore((state) => state.removeFromCart)
  const pendingOrder = usePendingOrderStore((state) => state.pendingOrder)
  const clearPendingOrder = usePendingOrderStore((state) => state.clearPendingOrder)

  useEffect(() => {
    const processPayment = async () => {
      // 1. URL 쿼리 파라미터에서 결제 정보 추출
      const paymentParams = parsePaymentSuccessParams(searchParams)

      if (!paymentParams) {
        setError('결제 정보가 올바르지 않습니다.')
        setLoading(false)
        return
      }

      // 2. store에서 주문 정보 확인
      if (!pendingOrder) {
        setError('주문 정보를 찾을 수 없습니다.')
        setLoading(false)
        return
      }

      // 주문번호 검증
      if (pendingOrder.orderNumber !== paymentParams.orderId) {
        setError('주문 정보가 일치하지 않습니다.')
        setLoading(false)
        return
      }

      // 금액 검증
      if (pendingOrder.totalAmount !== Number(paymentParams.amount)) {
        setError('결제 금액이 일치하지 않습니다.')
        setLoading(false)
        return
      }

      try {
        // 3. 백엔드에 결제 승인 API 호출
        const confirmResponse = await confirmPayment({
          paymentKey: paymentParams.paymentKey,
          orderId: paymentParams.orderId,
          amount: Number(paymentParams.amount),
        })

        // 4. 장바구니에서 해당 상품 제거
        if (pendingOrder.fromCart && pendingOrder.cartItemIds) {
          pendingOrder.cartItemIds.forEach(item => {
            removeFromCart(item.productId, item.skuId)
          })
        }

        // 5. store에서 임시 주문 정보 삭제
        clearPendingOrder()

        // 6. 주문 완료 페이지로 이동
        navigate('/market/order/complete', {
          replace: true,
          state: {
            orderId: pendingOrder.orderId,
            orderNumber: confirmResponse.orderNumber,
            paymentId: confirmResponse.paymentId,
            totalAmount: confirmResponse.amount,
          },
        })
      } catch (err) {
        console.error('Payment confirmation error:', err)
        setError(err instanceof Error ? err.message : '결제 승인에 실패했습니다.')
        setLoading(false)
      }
    }

    processPayment()
  }, [searchParams, navigate, removeFromCart, pendingOrder, clearPendingOrder])

  return (
    <div className="market-payment-success">
      <MarketHeader />

      <div className="payment-success-container">
        <Card className="payment-success-card">
          {loading ? (
            <div className="payment-processing">
              <Spin indicator={<LoadingOutlined style={{ fontSize: 48 }} spin />} />
              <h2>결제를 확인하고 있습니다...</h2>
              <p>잠시만 기다려주세요.</p>
            </div>
          ) : error ? (
            <Result
              status="error"
              title="결제 승인 실패"
              subTitle={error}
              extra={[
                <Button
                  type="primary"
                  key="retry"
                  onClick={() => navigate('/market/order')}
                >
                  다시 주문하기
                </Button>,
                <Button key="home" onClick={() => navigate('/market')}>
                  홈으로
                </Button>,
              ]}
            />
          ) : null}
        </Card>
      </div>

      <MarketFooter />
    </div>
  )
}

export default MarketPaymentSuccess

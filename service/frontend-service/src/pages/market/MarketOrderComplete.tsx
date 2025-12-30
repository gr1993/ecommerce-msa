import { useEffect, useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { Card, Button, Result, Space, Divider } from 'antd'
import { CheckCircleOutlined, ShoppingOutlined, HomeOutlined } from '@ant-design/icons'
import MarketHeader from '../../components/market/MarketHeader'
import MarketFooter from '../../components/market/MarketFooter'
import './MarketOrderComplete.css'

function MarketOrderComplete() {
  const navigate = useNavigate()
  const location = useLocation()
  const [orderId, setOrderId] = useState<string>('')
  const [paymentId, setPaymentId] = useState<string>('')
  const [receiptId, setReceiptId] = useState<string>('')
  const [totalAmount, setTotalAmount] = useState<number>(0)

  useEffect(() => {
    // location.state에서 주문 정보 가져오기
    if (location.state?.orderId) {
      setOrderId(location.state.orderId)
      setPaymentId(location.state.paymentId || '')
      setReceiptId(location.state.receiptId || '')
      setTotalAmount(location.state.totalAmount || 0)
    } else {
      // 주문 정보가 없으면 장바구니로 리다이렉트
      navigate('/market/cart')
    }
  }, [location, navigate])

  return (
    <div className="market-order-complete">
      <MarketHeader />

      <div className="order-complete-container">
        <Card className="order-complete-card">
          <Result
            icon={<CheckCircleOutlined style={{ color: '#52c41a' }} />}
            title="주문이 완료되었습니다!"
            subTitle={`주문번호: ${orderId}`}
            extra={[
              <Space key="actions" size="middle">
                <Button
                  type="primary"
                  icon={<ShoppingOutlined />}
                  size="large"
                  onClick={() => navigate('/market/products')}
                >
                  쇼핑 계속하기
                </Button>
                <Button
                  icon={<HomeOutlined />}
                  size="large"
                  onClick={() => navigate('/market')}
                >
                  홈으로
                </Button>
              </Space>
            ]}
          >
            <div className="order-summary">
              <Divider />
              <div className="summary-content">
                <div className="summary-row">
                  <span>주문번호</span>
                  <span className="order-id">{orderId}</span>
                </div>
                {paymentId && (
                  <div className="summary-row">
                    <span>결제번호</span>
                    <span className="payment-id">{paymentId}</span>
                  </div>
                )}
                {receiptId && (
                  <div className="summary-row">
                    <span>영수증번호</span>
                    <span className="receipt-id">{receiptId}</span>
                  </div>
                )}
                <div className="summary-row">
                  <span>결제금액</span>
                  <span className="total-amount">{totalAmount.toLocaleString()}원</span>
                </div>
              </div>
              <Divider />
              <div className="order-notice">
                <p>주문이 정상적으로 접수되었습니다.</p>
                <p>주문 내역은 마이페이지에서 확인하실 수 있습니다.</p>
                <p>배송 정보는 입력하신 연락처로 안내드리겠습니다.</p>
              </div>
            </div>
          </Result>
        </Card>
      </div>

      <MarketFooter />
    </div>
  )
}

export default MarketOrderComplete


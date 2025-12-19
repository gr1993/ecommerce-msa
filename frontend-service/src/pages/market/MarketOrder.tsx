import { useState, useEffect } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import {
  Card,
  Form,
  Input,
  Button,
  Space,
  Divider,
  Radio,
  message,
  Image,
  Steps
} from 'antd'
import {
  ShoppingCartOutlined,
  HomeOutlined,
  CreditCardOutlined,
  CheckCircleOutlined,
  ArrowLeftOutlined
} from '@ant-design/icons'
import MarketHeader from '../../components/market/MarketHeader'
import MarketFooter from '../../components/market/MarketFooter'
import type { CartItem } from '../../utils/cartUtils'
import { requestPayment, type PaymentRequest } from '../../utils/paymentUtils'
import './MarketOrder.css'

const { Step } = Steps
const { TextArea } = Input

interface OrderFormData {
  receiver_name: string
  receiver_phone: string
  postal_code: string
  address: string
  address_detail: string
  delivery_memo: string
}

function MarketOrder() {
  const navigate = useNavigate()
  const location = useLocation()
  const [form] = Form.useForm()
  const [orderItems, setOrderItems] = useState<CartItem[]>([])
  const [currentStep, setCurrentStep] = useState(0)
  const [loading, setLoading] = useState(false)

  // URL 파라미터나 location state에서 주문할 상품 가져오기
  useEffect(() => {
    // 장바구니에서 선택한 상품들 (location.state로 전달)
    if (location.state?.items) {
      setOrderItems(location.state.items)
    } else if (location.state?.item) {
      // 바로구매 (단일 상품)
      setOrderItems([location.state.item])
    } else {
      // URL 파라미터로 전달된 경우 (선택사항)
      const params = new URLSearchParams(location.search)
      const itemIds = params.get('items')?.split(',')
      if (itemIds) {
        // localStorage에서 장바구니 아이템 가져오기
        const cartData = localStorage.getItem('cart')
        if (cartData) {
          try {
            const allItems: CartItem[] = JSON.parse(cartData)
            const selectedItems = allItems.filter(item => itemIds.includes(item.product_id))
            setOrderItems(selectedItems)
          } catch (error) {
            console.error('주문 상품 로드 실패:', error)
            message.error('주문 상품을 불러오는데 실패했습니다.')
            navigate('/market/cart')
          }
        }
      } else {
        message.warning('주문할 상품이 없습니다.')
        navigate('/market/cart')
      }
    }
  }, [location, navigate])

  // 총 주문 금액 계산
  const calculateTotal = () => {
    return orderItems.reduce((sum, item) => sum + (item.base_price * item.quantity), 0)
  }

  // 배송비 계산 (5만원 이상 무료배송)
  const calculateShippingFee = () => {
    const total = calculateTotal()
    return total >= 50000 ? 0 : 3000
  }

  // 최종 결제 금액
  const calculateFinalTotal = () => {
    return calculateTotal() + calculateShippingFee()
  }

  // 주소 검색 (다음 주소 API 연동 예시)
  const handleAddressSearch = () => {
    // TODO: 다음 주소 API 연동
    message.info('주소 검색 기능은 준비 중입니다.')
  }

  // 결제하기 (부트페이 SDK 연동)
  const handleOrderSubmit = async (values: OrderFormData) => {
    if (orderItems.length === 0) {
      message.warning('주문할 상품이 없습니다.')
      return
    }

    setLoading(true)
    try {
      // 주문 ID 생성
      const orderId = `ORD-${Date.now()}`
      const orderName = orderItems.length === 1 
        ? orderItems[0].product_name 
        : `${orderItems[0].product_name} 외 ${orderItems.length - 1}개`

      // 결제 요청 데이터 구성
      const paymentRequest: PaymentRequest = {
        orderId,
        orderName,
        totalAmount: calculateFinalTotal(),
        paymentMethod: '', // 부트페이 SDK에서 결제 방법 선택
        items: orderItems.map(item => ({
          id: item.product_id,
          name: item.product_name,
          qty: item.quantity,
          price: item.base_price
        })),
        shippingInfo: {
          receiver_name: values.receiver_name,
          receiver_phone: values.receiver_phone,
          postal_code: values.postal_code,
          address: values.address,
          address_detail: values.address_detail
        }
      }

      // 부트페이 SDK로 결제 요청
      // TODO: 부트페이 SDK 설치 후 실제 결제 처리
      // import Bootpay from '@bootpay/client-js'
      // const bootpay = Bootpay.setApplicationId('YOUR_APPLICATION_ID', 'YOUR_PRIVATE_KEY')
      // const response = await bootpay.request({ ... })
      
      const paymentResponse = await requestPayment(paymentRequest)

      if (!paymentResponse.success) {
        message.error(paymentResponse.error || '결제 처리 중 오류가 발생했습니다.')
        return
      }

      // TODO: 백엔드 API로 주문 정보 저장
      // await saveOrder({
      //   orderId: paymentResponse.orderId,
      //   paymentId: paymentResponse.paymentId,
      //   receiptId: paymentResponse.receiptId,
      //   items: orderItems,
      //   shipping: paymentRequest.shippingInfo,
      //   payment: {
      //     amount: calculateFinalTotal()
      //   }
      // })

      // 주문 성공 시 장바구니에서 선택한 상품 제거
      if (location.state?.fromCart) {
        const cartData = localStorage.getItem('cart')
        if (cartData) {
          try {
            const allItems: CartItem[] = JSON.parse(cartData)
            const itemIds = new Set(orderItems.map(item => item.product_id))
            const remainingItems = allItems.filter(item => !itemIds.has(item.product_id))
            localStorage.setItem('cart', JSON.stringify(remainingItems))
          } catch (error) {
            console.error('장바구니 업데이트 실패:', error)
          }
        }
      }

      message.success('결제가 완료되었습니다!')
      navigate('/market/order/complete', {
        state: {
          orderId: paymentResponse.orderId || orderId,
          paymentId: paymentResponse.paymentId,
          receiptId: paymentResponse.receiptId,
          totalAmount: calculateFinalTotal()
        }
      })
    } catch (error) {
      console.error('Payment error:', error)
      message.error('결제 처리 중 오류가 발생했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const totalPrice = calculateTotal()
  const shippingFee = calculateShippingFee()
  const finalTotal = calculateFinalTotal()

  if (orderItems.length === 0) {
    return (
      <div className="market-order">
        <MarketHeader />
        <div className="order-container">
          <Card>
            <p>주문할 상품이 없습니다.</p>
            <Button onClick={() => navigate('/market/cart')}>장바구니로 이동</Button>
          </Card>
        </div>
        <MarketFooter />
      </div>
    )
  }

  return (
    <div className="market-order">
      <MarketHeader />

      <div className="order-container">
        <div className="order-header">
          <Button
            icon={<ArrowLeftOutlined />}
            onClick={() => navigate('/market/cart')}
            className="back-button"
          >
            돌아가기
          </Button>
          <h1 className="order-title">
            <ShoppingCartOutlined /> 주문하기
          </h1>
        </div>

        <Steps current={currentStep} className="order-steps">
          <Step title="배송지 정보" icon={<HomeOutlined />} />
          <Step title="주문 확인" icon={<CheckCircleOutlined />} />
        </Steps>

        <div className="order-content">
          <div className="order-main">
            <Form
              form={form}
              layout="vertical"
              onFinish={handleOrderSubmit}
              className="order-form"
            >
              {/* 배송지 정보 */}
              {currentStep === 0 && (
                <Card title="배송지 정보" className="order-section-card">
                  <Form.Item
                    name="receiver_name"
                    label="받는 분 이름"
                    rules={[{ required: true, message: '받는 분 이름을 입력해주세요.' }]}
                  >
                    <Input placeholder="이름을 입력해주세요" size="large" />
                  </Form.Item>

                  <Form.Item
                    name="receiver_phone"
                    label="받는 분 연락처"
                    rules={[
                      { required: true, message: '연락처를 입력해주세요.' },
                      { pattern: /^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$/, message: '올바른 전화번호 형식이 아닙니다.' }
                    ]}
                  >
                    <Input placeholder="010-1234-5678" size="large" />
                  </Form.Item>

                  <Form.Item
                    name="postal_code"
                    label="우편번호"
                    rules={[{ required: true, message: '우편번호를 입력해주세요.' }]}
                  >
                    <Space.Compact style={{ width: '100%' }}>
                      <Input placeholder="우편번호" size="large" />
                      <Button onClick={handleAddressSearch} size="large">
                        주소 검색
                      </Button>
                    </Space.Compact>
                  </Form.Item>

                  <Form.Item
                    name="address"
                    label="주소"
                    rules={[{ required: true, message: '주소를 입력해주세요.' }]}
                  >
                    <Input placeholder="기본 주소" size="large" />
                  </Form.Item>

                  <Form.Item
                    name="address_detail"
                    label="상세 주소"
                    rules={[{ required: true, message: '상세 주소를 입력해주세요.' }]}
                  >
                    <Input placeholder="상세 주소를 입력해주세요" size="large" />
                  </Form.Item>

                  <Form.Item
                    name="delivery_memo"
                    label="배송 메모 (선택사항)"
                  >
                    <TextArea
                      placeholder="배송 시 요청사항을 입력해주세요"
                      rows={3}
                    />
                  </Form.Item>

                  <Button
                    type="primary"
                    size="large"
                    block
                    onClick={() => setCurrentStep(1)}
                    className="step-button"
                  >
                    주문 확인
                  </Button>
                </Card>
              )}

              {/* 주문 확인 */}
              {currentStep === 1 && (
                <Card title="주문 확인" className="order-section-card">
                  <div className="order-items-review">
                    <h3>주문 상품</h3>
                    {orderItems.map((item) => (
                      <div key={item.product_id} className="order-item-review">
                        <Image
                          src={item.image_url || 'https://via.placeholder.com/100x100'}
                          alt={item.product_name}
                          width={80}
                          height={80}
                          preview={false}
                          style={{ borderRadius: '8px', objectFit: 'cover' }}
                        />
                        <div className="order-item-info">
                          <h4>{item.product_name}</h4>
                          <p>{item.product_code}</p>
                          <div className="order-item-quantity">
                            수량: {item.quantity}개
                          </div>
                        </div>
                        <div className="order-item-price">
                          {(item.base_price * item.quantity).toLocaleString()}원
                        </div>
                      </div>
                    ))}
                  </div>

                  <Divider />

                  <div className="order-summary-review">
                    <div className="summary-row">
                      <span>상품 금액</span>
                      <span>{totalPrice.toLocaleString()}원</span>
                    </div>
                    <div className="summary-row">
                      <span>배송비</span>
                      <span>
                        {shippingFee === 0 ? (
                          <span style={{ color: '#52c41a' }}>무료</span>
                        ) : (
                          `${shippingFee.toLocaleString()}원`
                        )}
                      </span>
                    </div>
                    {totalPrice < 50000 && (
                      <div className="shipping-notice">
                        <small>5만원 이상 구매 시 무료배송</small>
                      </div>
                    )}
                    <Divider />
                    <div className="summary-row total-row">
                      <span>최종 결제금액</span>
                      <span className="final-total">{finalTotal.toLocaleString()}원</span>
                    </div>
                  </div>

                  <Space style={{ width: '100%', justifyContent: 'space-between', marginTop: '1.5rem' }}>
                    <Button size="large" onClick={() => setCurrentStep(0)}>
                      이전
                    </Button>
                    <Button
                      type="primary"
                      size="large"
                      htmlType="submit"
                      loading={loading}
                      className="order-submit-button"
                      icon={<CreditCardOutlined />}
                    >
                      결제하기
                    </Button>
                  </Space>
                </Card>
              )}
            </Form>
          </div>

          {/* 주문 요약 (사이드바) */}
          <div className="order-sidebar">
            <Card title="주문 요약" className="order-summary-card">
              <div className="order-items-summary">
                {orderItems.map((item) => (
                  <div key={item.product_id} className="summary-item">
                    <Image
                      src={item.image_url || 'https://via.placeholder.com/60x60'}
                      alt={item.product_name}
                      width={60}
                      height={60}
                      preview={false}
                      style={{ borderRadius: '4px', objectFit: 'cover' }}
                    />
                    <div className="summary-item-info">
                      <p className="summary-item-name">{item.product_name}</p>
                      <p className="summary-item-quantity">{item.quantity}개</p>
                    </div>
                    <p className="summary-item-price">
                      {(item.base_price * item.quantity).toLocaleString()}원
                    </p>
                  </div>
                ))}
              </div>

              <Divider />

              <div className="order-summary">
                <div className="summary-row">
                  <span>상품 금액</span>
                  <span>{totalPrice.toLocaleString()}원</span>
                </div>
                <div className="summary-row">
                  <span>배송비</span>
                  <span>
                    {shippingFee === 0 ? (
                      <span style={{ color: '#52c41a' }}>무료</span>
                    ) : (
                      `${shippingFee.toLocaleString()}원`
                    )}
                  </span>
                </div>
                {totalPrice < 50000 && (
                  <div className="shipping-notice">
                    <small>5만원 이상 구매 시 무료배송</small>
                  </div>
                )}
                <Divider />
                <div className="summary-row total-row">
                  <span>총 결제금액</span>
                  <span className="final-total">{finalTotal.toLocaleString()}원</span>
                </div>
              </div>
            </Card>
          </div>
        </div>
      </div>

      <MarketFooter />
    </div>
  )
}

export default MarketOrder


import { useState, useEffect } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import {
  Card,
  Form,
  Input,
  Button,
  Space,
  Divider,
  message,
  Image,
  Steps,
  Select,
  Tag,
  Spin
} from 'antd'
import {
  ShoppingCartOutlined,
  HomeOutlined,
  CreditCardOutlined,
  CheckCircleOutlined,
  ArrowLeftOutlined,
  GiftOutlined,
  TagOutlined
} from '@ant-design/icons'
import MarketHeader from '../../components/market/MarketHeader'
import MarketFooter from '../../components/market/MarketFooter'
import { useCartStore, type CartItem } from '../../stores/cartStore'
import { useAuthStore } from '../../stores/authStore'
import { usePendingOrderStore } from '../../stores/pendingOrderStore'
import { requestTossPayment, type PaymentRequest } from '../../utils/paymentUtils'
import { createOrder, type OrderCreateRequest, type OrderResponse } from '../../api/orderApi'
import {
  getUserCoupons,
  getApplicableDiscountPolicies,
  type UserCoupon,
  type ApplicableDiscountPolicy
} from '../../api/promotionApi'
import './MarketOrder.css'

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
  const [orderResponse, setOrderResponse] = useState<OrderResponse | null>(null)
  const [savedFormData, setSavedFormData] = useState<OrderFormData | null>(null)
  const cartItems = useCartStore((state) => state.items)
  const isLoggedIn = useAuthStore((state) => state.isLoggedIn)
  const setPendingOrder = usePendingOrderStore((state) => state.setPendingOrder)
  const clearPendingOrder = usePendingOrderStore((state) => state.clearPendingOrder)

  // 프로모션 state
  const [userCoupons, setUserCoupons] = useState<UserCoupon[]>([])
  const [selectedCouponId, setSelectedCouponId] = useState<string | undefined>(undefined)
  const [discountPolicies, setDiscountPolicies] = useState<ApplicableDiscountPolicy[]>([])
  const [promotionLoading, setPromotionLoading] = useState(false)

  // 비로그인 상태면 로그인 페이지로 이동
  useEffect(() => {
    if (!isLoggedIn()) {
      message.warning('주문하려면 로그인이 필요합니다.')
      navigate('/market/login', {
        state: {
          from: location.pathname,
          orderItems: location.state
        }
      })
    }
  }, [isLoggedIn, navigate, location])

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
        // Zustand cartStore에서 장바구니 아이템 가져오기
        const selectedItems = cartItems.filter(item => itemIds.includes(item.product_id))
        if (selectedItems.length > 0) {
          setOrderItems(selectedItems)
        } else {
          message.error('주문 상품을 불러오는데 실패했습니다.')
          navigate('/market/cart')
        }
      } else {
        message.warning('주문할 상품이 없습니다.')
        navigate('/market/cart')
      }
    }
  }, [location, navigate, cartItems])

  // orderItems가 설정되면 쿠폰 + 할인정책 병렬 조회
  useEffect(() => {
    if (orderItems.length === 0) return

    const fetchPromotions = async () => {
      setPromotionLoading(true)
      const productIds = orderItems.map(item => Number(item.product_id))

      const results = await Promise.allSettled([
        getUserCoupons(),
        getApplicableDiscountPolicies(productIds),
      ])

      if (results[0].status === 'fulfilled') {
        // ISSUED(사용 가능) 상태인 쿠폰만 필터링
        setUserCoupons(results[0].value.filter(c => c.coupon_status === 'ISSUED'))
      }
      if (results[1].status === 'fulfilled') {
        setDiscountPolicies(results[1].value)
      }

      setPromotionLoading(false)
    }

    fetchPromotions()
  }, [orderItems])

  // 총 주문 금액 계산
  const calculateTotal = () => {
    return orderItems.reduce((sum, item) => sum + (item.base_price * item.quantity), 0)
  }

  // 쿠폰 할인 금액 계산
  const calculateCouponDiscount = () => {
    if (!selectedCouponId) return 0
    const coupon = userCoupons.find(c => c.user_coupon_id === selectedCouponId)
    if (!coupon) return 0

    const totalPrice = calculateTotal()
    if (totalPrice < coupon.min_order_amount) return 0

    let discount = 0
    if (coupon.discount_type === 'RATE') {
      discount = Math.floor(totalPrice * coupon.discount_value / 100)
    } else {
      discount = coupon.discount_value
    }

    if (coupon.max_discount_amount && discount > coupon.max_discount_amount) {
      discount = coupon.max_discount_amount
    }

    return discount
  }

  // 자동 할인 정책 할인 금액 계산
  const calculatePolicyDiscount = () => {
    const totalPrice = calculateTotal()
    let totalDiscount = 0

    for (const policy of discountPolicies) {
      if (totalPrice < policy.min_order_amount) continue

      let discount = 0
      if (policy.target_type === 'ORDER') {
        if (policy.discount_type === 'RATE') {
          discount = Math.floor(totalPrice * policy.discount_value / 100)
        } else {
          discount = policy.discount_value
        }
      } else if (policy.target_type === 'PRODUCT') {
        const targetItem = orderItems.find(item => Number(item.product_id) === policy.target_id)
        if (!targetItem) continue
        const itemTotal = targetItem.base_price * targetItem.quantity
        if (policy.discount_type === 'RATE') {
          discount = Math.floor(itemTotal * policy.discount_value / 100)
        } else {
          discount = policy.discount_value
        }
      }

      if (policy.max_discount_amount && discount > policy.max_discount_amount) {
        discount = policy.max_discount_amount
      }

      totalDiscount += discount
    }

    return totalDiscount
  }

  // 최종 결제 금액 (배송비 무료)
  const calculateFinalTotal = () => {
    const total = calculateTotal()
    const couponDiscount = calculateCouponDiscount()
    const policyDiscount = calculatePolicyDiscount()
    return Math.max(0, total - couponDiscount - policyDiscount)
  }

  // 주소 검색 (다음 주소 API 연동 예시)
  const handleAddressSearch = () => {
    // TODO: 다음 주소 API 연동
    message.info('주소 검색 기능은 준비 중입니다.')
  }

  // Step 0 → Step 1 이동 (배송지 정보 저장)
  const handleNextStep = async () => {
    try {
      const values = await form.validateFields()
      setSavedFormData(values)
      setCurrentStep(1)
    } catch (error) {
      console.error('Validation failed:', error)
    }
  }

  // 주문하기 (주문 생성 API 호출)
  const handleOrderCreate = async () => {
    if (orderItems.length === 0) {
      message.warning('주문할 상품이 없습니다.')
      return
    }

    if (!savedFormData) {
      message.error('배송지 정보가 없습니다.')
      setCurrentStep(0)
      return
    }

    setLoading(true)
    try {
      // 백엔드 API로 주문 생성
      const orderRequest: OrderCreateRequest = {
        orderItems: orderItems.map(item => ({
          productId: Number(item.product_id),
          skuId: Number(item.sku_id),
          quantity: item.quantity,
        })),
        deliveryInfo: {
          receiverName: savedFormData.receiver_name,
          receiverPhone: savedFormData.receiver_phone,
          zipcode: savedFormData.postal_code,
          address: savedFormData.address,
          addressDetail: savedFormData.address_detail,
          deliveryMemo: savedFormData.delivery_memo,
        },
      }

      const response = await createOrder(orderRequest)
      setOrderResponse(response)

      message.success('주문이 생성되었습니다. 결제를 진행해주세요.')
      setCurrentStep(2) // 결제 화면으로 이동
    } catch (error) {
      console.error('Order create error:', error)
      if (error instanceof Error) {
        message.error(error.message)
      } else {
        message.error('주문 생성 중 오류가 발생했습니다.')
      }
    } finally {
      setLoading(false)
    }
  }

  // 결제하기 (토스페이먼츠 SDK 연동)
  const handlePayment = async () => {
    if (!orderResponse || !savedFormData) {
      message.error('주문 정보가 없습니다.')
      return
    }

    setLoading(true)
    try {
      const orderName = orderItems.length === 1
        ? orderItems[0].product_name
        : `${orderItems[0].product_name} 외 ${orderItems.length - 1}개`

      const totalAmount = calculateFinalTotal()

      // 토스 리다이렉트 후 복원을 위해 store에 주문 정보 저장
      setPendingOrder({
        orderId: orderResponse.orderId,
        orderNumber: orderResponse.orderNumber,
        cartItemIds: orderItems.map(item => ({
          productId: item.product_id,
          skuId: item.sku_id
        })),
        totalAmount,
        fromCart: location.state?.fromCart || false
      })

      // 결제 요청 데이터 구성
      const paymentRequest: PaymentRequest = {
        orderId: orderResponse.orderNumber,
        orderName,
        totalAmount,
        customerName: savedFormData.receiver_name,
        customerMobilePhone: savedFormData.receiver_phone
      }

      // 토스페이먼츠 SDK로 결제 요청 (사용자 ID를 customerKey로 사용)
      const customerKey = `USER_${orderResponse.orderId}`
      await requestTossPayment(paymentRequest, customerKey)

      // 토스페이먼츠는 결제 완료 후 successUrl로 리다이렉트됨
      // 실제 결제 승인 및 후처리는 MarketPaymentSuccess 페이지에서 처리
    } catch (error) {
      console.error('Payment error:', error)
      // 결제 실패 시 store에서 임시 주문 정보 삭제
      clearPendingOrder()
      message.error(error instanceof Error ? error.message : '결제 처리 중 오류가 발생했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const totalPrice = calculateTotal()
  const couponDiscount = calculateCouponDiscount()
  const policyDiscount = calculatePolicyDiscount()
  const finalTotal = calculateFinalTotal()

  // 할인 요약 렌더링 (Step 1, Step 2, Sidebar 공용)
  const renderDiscountSummary = () => (
    <>
      <div className="summary-row">
        <span>상품 금액</span>
        <span>{totalPrice.toLocaleString()}원</span>
      </div>
      {couponDiscount > 0 && (
        <div className="summary-row discount-row">
          <span>쿠폰 할인</span>
          <span className="discount-amount">-{couponDiscount.toLocaleString()}원</span>
        </div>
      )}
      {policyDiscount > 0 && (
        <div className="summary-row discount-row">
          <span>자동 할인</span>
          <span className="discount-amount">-{policyDiscount.toLocaleString()}원</span>
        </div>
      )}
      <div className="summary-row">
        <span>배송비</span>
        <span style={{ color: '#52c41a' }}>무료</span>
      </div>
      <Divider />
      <div className="summary-row total-row">
        <span>최종 결제금액</span>
        <span className="final-total">{finalTotal.toLocaleString()}원</span>
      </div>
    </>
  )

  // 비로그인 상태면 렌더링하지 않음 (로그인 페이지로 리다이렉트 중)
  if (!isLoggedIn()) {
    return null
  }

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

        <Steps
          current={currentStep}
          className="order-steps"
          items={[
            { title: '배송지 정보', icon: <HomeOutlined /> },
            { title: '주문 확인', icon: <CheckCircleOutlined /> },
            { title: '결제', icon: <CreditCardOutlined /> },
          ]}
        />

        <div className="order-content">
          <div className="order-main">
            <Form
              form={form}
              layout="vertical"
              onFinish={handleOrderCreate}
              className="order-form"
              initialValues={{
                receiver_name: '박강림',
                receiver_phone: '010-1234-5678',
                postal_code: '04524',
                address: '서울특별시 강남구 테스트로 123',
                address_detail: '테스트빌딩 10층',
                delivery_memo: '빠른 배송 부탁합니다.',
              }}
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

                  <Form.Item label="우편번호" required>
                    <Space.Compact style={{ width: '100%' }}>
                      <Form.Item
                        name="postal_code"
                        noStyle
                        rules={[{ required: true, message: '우편번호를 입력해주세요.' }]}
                      >
                        <Input placeholder="우편번호" size="large" />
                      </Form.Item>
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
                    onClick={handleNextStep}
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

                  {/* 쿠폰 선택 */}
                  <div className="promotion-section">
                    <h3><GiftOutlined /> 쿠폰 적용</h3>
                    {promotionLoading ? (
                      <Spin size="small" />
                    ) : (
                      <Select
                        placeholder="쿠폰을 선택해주세요"
                        value={selectedCouponId}
                        onChange={(value) => setSelectedCouponId(value)}
                        allowClear
                        style={{ width: '100%' }}
                        size="large"
                        options={userCoupons.map(coupon => {
                          const meetsMinOrder = totalPrice >= coupon.min_order_amount
                          const discountLabel = coupon.discount_type === 'RATE'
                            ? `${coupon.discount_value}% 할인`
                            : `${coupon.discount_value.toLocaleString()}원 할인`
                          const minOrderLabel = coupon.min_order_amount > 0
                            ? ` (${coupon.min_order_amount.toLocaleString()}원 이상)`
                            : ''
                          return {
                            value: coupon.user_coupon_id,
                            label: `${coupon.coupon_name} - ${discountLabel}${minOrderLabel}`,
                            disabled: !meetsMinOrder,
                          }
                        })}
                        notFoundContent="사용 가능한 쿠폰이 없습니다"
                      />
                    )}
                  </div>

                  {/* 자동 할인 정책 */}
                  {discountPolicies.length > 0 && (
                    <div className="promotion-section">
                      <h3><TagOutlined /> 자동 할인</h3>
                      <div className="discount-policy-list">
                        {discountPolicies.map(policy => {
                          const discountLabel = policy.discount_type === 'RATE'
                            ? `${policy.discount_value}%`
                            : `${policy.discount_value.toLocaleString()}원`
                          return (
                            <div key={policy.discount_id} className="discount-policy-item">
                              <Tag color="blue">
                                {policy.target_type === 'ORDER' ? '주문' : '상품'}
                              </Tag>
                              <span className="discount-policy-name">{policy.discount_name}</span>
                              <span className="discount-policy-value">{discountLabel} 할인</span>
                            </div>
                          )
                        })}
                      </div>
                    </div>
                  )}

                  <Divider />

                  <div className="order-summary-review">
                    {renderDiscountSummary()}
                  </div>

                  <Space style={{ width: '100%', justifyContent: 'space-between', marginTop: '1.5rem' }}>
                    <Button size="large" onClick={() => setCurrentStep(0)}>
                      이전
                    </Button>
                    <Button
                      type="primary"
                      size="large"
                      onClick={handleOrderCreate}
                      loading={loading}
                      className="order-submit-button"
                      icon={<ShoppingCartOutlined />}
                    >
                      주문하기
                    </Button>
                  </Space>
                </Card>
              )}

              {/* 결제 */}
              {currentStep === 2 && orderResponse && (
                <Card title="결제" className="order-section-card">
                  <div className="payment-info">
                    <h3>주문 정보</h3>
                    <div className="payment-order-info">
                      <div className="info-row">
                        <span>주문 번호</span>
                        <span>{orderResponse.orderNumber}</span>
                      </div>
                      <div className="info-row">
                        <span>주문 상태</span>
                        <span>{orderResponse.orderStatus === 'CREATED' ? '주문 생성됨' : orderResponse.orderStatus}</span>
                      </div>
                    </div>
                  </div>

                  <Divider />

                  <div className="payment-items">
                    <h3>결제 상품</h3>
                    {orderItems.map((item) => (
                      <div key={item.product_id} className="order-item-review">
                        <Image
                          src={item.image_url || 'https://via.placeholder.com/100x100'}
                          alt={item.product_name}
                          width={60}
                          height={60}
                          preview={false}
                          style={{ borderRadius: '8px', objectFit: 'cover' }}
                        />
                        <div className="order-item-info">
                          <h4>{item.product_name}</h4>
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
                    {renderDiscountSummary()}
                  </div>

                  <Space style={{ width: '100%', justifyContent: 'space-between', marginTop: '1.5rem' }}>
                    <Button size="large" onClick={() => setCurrentStep(1)}>
                      이전
                    </Button>
                    <Button
                      type="primary"
                      size="large"
                      onClick={handlePayment}
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
                {renderDiscountSummary()}
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

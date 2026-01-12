import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { 
  Card, 
  Button, 
  InputNumber, 
  Space, 
  Divider, 
  Empty, 
  Checkbox, 
  message,
  Image,
  Popconfirm
} from 'antd'
import { 
  ShoppingCartOutlined, 
  DeleteOutlined, 
  MinusOutlined, 
  PlusOutlined,
  ArrowLeftOutlined
} from '@ant-design/icons'
import MarketHeader from '../../components/market/MarketHeader'
import MarketFooter from '../../components/market/MarketFooter'
import { useCartStore } from '../../stores/cartStore'
import type { CartItem } from '../../stores/cartStore'
import './MarketCart.css'

function MarketCart() {
  const navigate = useNavigate()
  const cartItems = useCartStore((state) => state.items)
  const updateQuantity = useCartStore((state) => state.updateQuantity)
  const removeFromCart = useCartStore((state) => state.removeFromCart)
  const [selectedItems, setSelectedItems] = useState<Set<string>>(new Set())
  const [isAllSelected, setIsAllSelected] = useState(false)

  // 장바구니 데이터 로드 시 모든 아이템 선택
  useEffect(() => {
    if (cartItems.length > 0) {
      setSelectedItems(new Set(cartItems.map((item: CartItem) => item.product_id)))
      setIsAllSelected(true)
    }
  }, [])

  // 수량 변경
  const handleQuantityChange = (productId: string, newQuantity: number | null) => {
    if (!newQuantity || newQuantity < 1) return
    updateQuantity(productId, newQuantity)
  }

  // 아이템 삭제
  const handleRemoveItem = (productId: string) => {
    removeFromCart(productId)
    setSelectedItems(prev => {
      const newSet = new Set(prev)
      newSet.delete(productId)
      return newSet
    })
  }

  // 선택된 아이템 삭제
  const handleRemoveSelected = () => {
    if (selectedItems.size === 0) {
      message.warning('삭제할 상품을 선택해주세요.')
      return
    }

    selectedItems.forEach(productId => removeFromCart(productId))
    setSelectedItems(new Set())
    setIsAllSelected(false)
    message.success('선택한 상품이 삭제되었습니다.')
  }

  // 개별 선택/해제
  const handleItemSelect = (productId: string, checked: boolean) => {
    const newSelected = new Set(selectedItems)
    if (checked) {
      newSelected.add(productId)
    } else {
      newSelected.delete(productId)
    }
    setSelectedItems(newSelected)
    setIsAllSelected(newSelected.size === cartItems.length && cartItems.length > 0)
  }

  // 전체 선택/해제
  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      setSelectedItems(new Set(cartItems.map(item => item.product_id)))
      setIsAllSelected(true)
    } else {
      setSelectedItems(new Set())
      setIsAllSelected(false)
    }
  }

  // 선택된 아이템들의 총 금액 계산
  const calculateTotal = () => {
    return cartItems
      .filter(item => selectedItems.has(item.product_id))
      .reduce((sum, item) => sum + (item.base_price * item.quantity), 0)
  }

  // 주문하기
  const handleOrder = () => {
    if (selectedItems.size === 0) {
      message.warning('주문할 상품을 선택해주세요.')
      return
    }
    // 선택된 아이템들만 주문 페이지로 전달
    const selectedCartItems = cartItems.filter(item => selectedItems.has(item.product_id))
    navigate('/market/order', {
      state: {
        items: selectedCartItems,
        fromCart: true
      }
    })
  }

  const totalPrice = calculateTotal()
  const selectedCount = selectedItems.size

  return (
    <div className="market-cart">
      <MarketHeader />
      
      <div className="cart-container">
        <div className="cart-header">
          <Button 
            icon={<ArrowLeftOutlined />} 
            onClick={() => navigate('/market')}
            className="back-button"
          >
            쇼핑 계속하기
          </Button>
          <h1 className="cart-title">
            <ShoppingCartOutlined /> 장바구니
          </h1>
        </div>

        {cartItems.length === 0 ? (
          <Card className="empty-cart-card">
            <Empty
              description="장바구니가 비어있습니다"
              image={Empty.PRESENTED_IMAGE_SIMPLE}
            >
              <Button type="primary" onClick={() => navigate('/market/products')}>
                상품 둘러보기
              </Button>
            </Empty>
          </Card>
        ) : (
          <>
            <Card className="cart-items-card">
              <div className="cart-actions">
                <Checkbox
                  checked={isAllSelected}
                  onChange={(e) => handleSelectAll(e.target.checked)}
                >
                  전체 선택 ({selectedCount}/{cartItems.length})
                </Checkbox>
                <Popconfirm
                  title="선택한 상품을 삭제하시겠습니까?"
                  onConfirm={handleRemoveSelected}
                  okText="삭제"
                  cancelText="취소"
                >
                  <Button 
                    danger 
                    icon={<DeleteOutlined />}
                    disabled={selectedCount === 0}
                  >
                    선택 삭제
                  </Button>
                </Popconfirm>
              </div>

              <Divider />

              <div className="cart-items-list">
                {cartItems.map((item) => (
                  <div key={item.product_id} className="cart-item">
                    <Checkbox
                      checked={selectedItems.has(item.product_id)}
                      onChange={(e) => handleItemSelect(item.product_id, e.target.checked)}
                      className="item-checkbox"
                    />
                    <div 
                      className="item-image"
                      onClick={() => navigate(`/market/product/${item.product_id}`)}
                    >
                      <Image
                        src={item.image_url || 'https://via.placeholder.com/150x150'}
                        alt={item.product_name}
                        preview={false}
                        width={120}
                        height={120}
                        style={{ objectFit: 'cover', borderRadius: '8px' }}
                      />
                    </div>
                    <div className="item-info">
                      <h3 
                        className="item-name"
                        onClick={() => navigate(`/market/product/${item.product_id}`)}
                      >
                        {item.product_name}
                      </h3>
                      <p className="item-code">{item.product_code}</p>
                      <div className="item-price">
                        {item.base_price.toLocaleString()}원
                      </div>
                    </div>
                    <div className="item-quantity">
                      <Space>
                        <Button
                          icon={<MinusOutlined />}
                          onClick={() => handleQuantityChange(item.product_id, item.quantity - 1)}
                          disabled={item.quantity <= 1}
                        />
                        <InputNumber
                          min={1}
                          max={item.stock}
                          value={item.quantity}
                          onChange={(value) => handleQuantityChange(item.product_id, value)}
                          controls={false}
                          style={{ width: 80, textAlign: 'center' }}
                        />
                        <Button
                          icon={<PlusOutlined />}
                          onClick={() => handleQuantityChange(item.product_id, item.quantity + 1)}
                          disabled={item.quantity >= item.stock}
                        />
                      </Space>
                      <div className="item-total-price">
                        {(item.base_price * item.quantity).toLocaleString()}원
                      </div>
                    </div>
                    <Popconfirm
                      title="이 상품을 삭제하시겠습니까?"
                      onConfirm={() => handleRemoveItem(item.product_id)}
                      okText="삭제"
                      cancelText="취소"
                    >
                      <Button
                        danger
                        icon={<DeleteOutlined />}
                        className="item-delete-btn"
                      />
                    </Popconfirm>
                  </div>
                ))}
              </div>
            </Card>

            <Card className="cart-summary-card">
              <div className="summary-content">
                <div className="summary-row">
                  <span>선택 상품 수</span>
                  <span>{selectedCount}개</span>
                </div>
                <Divider style={{ margin: '1rem 0' }} />
                <div className="summary-row total-row">
                  <span>총 주문금액</span>
                  <span className="total-price">{totalPrice.toLocaleString()}원</span>
                </div>
                <Button
                  type="primary"
                  size="large"
                  block
                  className="order-button"
                  onClick={handleOrder}
                  disabled={selectedCount === 0}
                >
                  주문하기 ({selectedCount}개)
                </Button>
              </div>
            </Card>
          </>
        )}
      </div>

      <MarketFooter />
    </div>
  )
}

export default MarketCart


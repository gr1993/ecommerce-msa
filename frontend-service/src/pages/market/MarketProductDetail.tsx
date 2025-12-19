import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Button, Card, Row, Col, InputNumber, Space, Divider, Tag, Image, message } from 'antd'
import { ShoppingCartOutlined, ArrowLeftOutlined, MinusOutlined, PlusOutlined } from '@ant-design/icons'
import MarketHeader from '../../components/market/MarketHeader'
import MarketFooter from '../../components/market/MarketFooter'
import type { CartItem } from '../../utils/cartUtils'
import './MarketProductDetail.css'

interface Product {
  product_id: string
  product_name: string
  product_code: string
  base_price: number
  category_id: string
  category_name: string
  sales_count: number
  stock: number
  description: string
  created_at: string
  image_url?: string
  images?: string[]
}

function MarketProductDetail() {
  const { productId } = useParams<{ productId: string }>()
  const navigate = useNavigate()
  
  const [product, setProduct] = useState<Product | null>(null)
  const [quantity, setQuantity] = useState<number>(1)
  const [selectedImageIndex, setSelectedImageIndex] = useState<number>(0)
  const [loading, setLoading] = useState<boolean>(true)

  // 상품 데이터 로드
  useEffect(() => {
    // TODO: API 호출로 상품 상세 데이터 로드
    const loadProduct = async () => {
      setLoading(true)
      try {
        // 샘플 데이터
        const sampleProducts: Product[] = [
          {
            product_id: '1',
            product_name: '프리미엄 노트북',
            product_code: 'PRD-001',
            base_price: 1200000,
            category_id: '1-1',
            category_name: '노트북',
            sales_count: 150,
            stock: 50,
            description: '최신 기술이 적용된 프리미엄 노트북입니다. 고성능 프로세서와 대용량 메모리로 업무와 게임 모두 완벽하게 지원합니다. 슬림한 디자인과 긴 배터리 수명으로 어디서나 편리하게 사용할 수 있습니다.',
            created_at: '2024-01-01 10:00:00',
            image_url: 'https://via.placeholder.com/600x600?text=노트북',
            images: [
              'https://via.placeholder.com/600x600?text=노트북1',
              'https://via.placeholder.com/600x600?text=노트북2',
              'https://via.placeholder.com/600x600?text=노트북3',
              'https://via.placeholder.com/600x600?text=노트북4'
            ]
          },
          {
            product_id: '2',
            product_name: '최신 스마트폰',
            product_code: 'PRD-002',
            base_price: 800000,
            category_id: '1-2',
            category_name: '스마트폰',
            sales_count: 230,
            stock: 100,
            description: '혁신적인 카메라 시스템과 강력한 성능을 갖춘 최신 스마트폰입니다. 5G 네트워크를 지원하며, 빠른 충전과 긴 배터리 수명을 자랑합니다.',
            created_at: '2024-01-05 14:30:00',
            image_url: 'https://via.placeholder.com/600x600?text=스마트폰',
            images: [
              'https://via.placeholder.com/600x600?text=스마트폰1',
              'https://via.placeholder.com/600x600?text=스마트폰2',
              'https://via.placeholder.com/600x600?text=스마트폰3'
            ]
          }
        ]

        const foundProduct = sampleProducts.find(p => p.product_id === productId)
        
        if (!foundProduct) {
          // 실제로는 API에서 가져오기
          // 샘플 데이터로 대체
          const defaultProduct: Product = {
            product_id: productId || '1',
            product_name: '상품명',
            product_code: `PRD-${productId?.padStart(3, '0') || '001'}`,
            base_price: 100000,
            category_id: '1',
            category_name: '카테고리',
            sales_count: 0,
            stock: 10,
            description: '상품 설명이 여기에 표시됩니다.',
            created_at: '2024-01-01 10:00:00',
            image_url: 'https://via.placeholder.com/600x600?text=상품',
            images: [
              'https://via.placeholder.com/600x600?text=상품1',
              'https://via.placeholder.com/600x600?text=상품2'
            ]
          }
          setProduct(foundProduct || defaultProduct)
        } else {
          setProduct(foundProduct)
        }
      } catch (error) {
        message.error('상품 정보를 불러오는데 실패했습니다.')
      } finally {
        setLoading(false)
      }
    }

    if (productId) {
      loadProduct()
    }
  }, [productId])

  const handleQuantityChange = (value: number | null) => {
    if (value && value > 0 && product && value <= product.stock) {
      setQuantity(value)
    }
  }

  const handleAddToCart = () => {
    // TODO: 장바구니 추가 API 호출
    message.success(`${product?.product_name}을(를) 장바구니에 추가했습니다.`)
  }

  const handleBuyNow = () => {
    if (!product) return
    
    if (product.stock === 0) {
      message.warning('재고가 없습니다.')
      return
    }

    if (quantity > product.stock) {
      message.warning(`재고가 부족합니다. (최대 ${product.stock}개)`)
      return
    }

    // 바로구매: 주문 페이지로 이동
    const orderItem: CartItem = {
      product_id: product.product_id,
      product_name: product.product_name,
      product_code: product.product_code,
      base_price: product.base_price,
      image_url: product.image_url,
      quantity: quantity,
      stock: product.stock
    }

    navigate('/market/order', {
      state: {
        item: orderItem,
        fromCart: false
      }
    })
  }


  if (loading) {
    return (
      <div className="market-product-detail">
        <MarketHeader />
        <div className="loading-container">
          <p>로딩 중...</p>
        </div>
        <MarketFooter />
      </div>
    )
  }

  if (!product) {
    return (
      <div className="market-product-detail">
        <MarketHeader />
        <div className="error-container">
          <p>상품을 찾을 수 없습니다.</p>
          <Button onClick={() => navigate('/market/products')}>상품 목록으로 돌아가기</Button>
        </div>
        <MarketFooter />
      </div>
    )
  }

  const totalPrice = product.base_price * quantity

  return (
    <div className="market-product-detail">
      <MarketHeader />
      
      <div className="product-detail-container">
        <Button
          type="text"
          icon={<ArrowLeftOutlined />}
          onClick={() => navigate('/market/products')}
          className="back-button"
        >
          상품 목록으로 돌아가기
        </Button>

        <div className="product-detail-content">
          {/* 상품 이미지 영역 */}
          <div className="product-images-section">
            <div className="main-image">
              <Image
                src={product.images?.[selectedImageIndex] || product.image_url || 'https://via.placeholder.com/600x600'}
                alt={product.product_name}
                preview={false}
                className="main-product-image"
              />
            </div>
            {product.images && product.images.length > 1 && (
              <div className="thumbnail-images">
                {product.images.map((image, index) => (
                  <div
                    key={index}
                    className={`thumbnail-item ${selectedImageIndex === index ? 'active' : ''}`}
                    onClick={() => setSelectedImageIndex(index)}
                  >
                    <img src={image} alt={`${product.product_name} ${index + 1}`} />
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* 상품 정보 영역 */}
          <div className="product-info-section">
            <div className="product-header">
              <Tag color="blue">{product.category_name}</Tag>
              <h1 className="product-title">{product.product_name}</h1>
              <p className="product-code">상품 코드: {product.product_code}</p>
            </div>

            <Divider />

            <div className="product-price-section">
              <div className="price-info">
                <span className="price-label">판매가격</span>
                <span className="product-price">{product.base_price.toLocaleString()}원</span>
              </div>
              {quantity > 1 && (
                <div className="total-price-info">
                  <span className="total-label">총 상품금액</span>
                  <span className="total-price">{totalPrice.toLocaleString()}원</span>
                </div>
              )}
            </div>

            <Divider />

            <div className="product-options">
              <div className="option-row">
                <span className="option-label">수량</span>
                <div className="quantity-control">
                  <Button
                    icon={<MinusOutlined />}
                    onClick={() => handleQuantityChange(quantity - 1)}
                    disabled={quantity <= 1}
                  />
                  <InputNumber
                    min={1}
                    max={product.stock}
                    value={quantity}
                    onChange={handleQuantityChange}
                    className="quantity-input"
                  />
                  <Button
                    icon={<PlusOutlined />}
                    onClick={() => handleQuantityChange(quantity + 1)}
                    disabled={quantity >= product.stock}
                  />
                  <span className="stock-info">(재고: {product.stock}개)</span>
                </div>
              </div>
            </div>

            <Divider />

            <div className="product-meta">
              <div className="meta-item">
                <span className="meta-label">판매량</span>
                <span className="meta-value">{product.sales_count}개</span>
              </div>
              <div className="meta-item">
                <span className="meta-label">재고</span>
                <span className="meta-value">{product.stock > 0 ? `${product.stock}개` : '품절'}</span>
              </div>
            </div>

            <Divider />

            <div className="product-actions">
              <Space size="middle" className="action-buttons">
                <Button
                  type="primary"
                  icon={<ShoppingCartOutlined />}
                  size="large"
                  onClick={handleAddToCart}
                  className="cart-btn"
                  disabled={product.stock === 0}
                >
                  장바구니
                </Button>
                <Button
                  type="primary"
                  size="large"
                  onClick={handleBuyNow}
                  className="buy-btn"
                  disabled={product.stock === 0}
                >
                  바로구매
                </Button>
              </Space>
            </div>
          </div>
        </div>

        {/* 상품 상세 설명 */}
        <div className="product-description-section">
          <Card title="상품 상세 정보" className="description-card">
            <div className="product-description">
              <p>{product.description}</p>
              <div className="description-details">
                <h3>상품 특징</h3>
                <ul>
                  <li>고품질 소재 사용</li>
                  <li>엄격한 품질 검사</li>
                  <li>안전한 배송</li>
                  <li>만족도 보장</li>
                </ul>
              </div>
            </div>
          </Card>
        </div>
      </div>

      <MarketFooter />
    </div>
  )
}

export default MarketProductDetail


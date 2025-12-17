import { useState, useEffect, useRef } from 'react'
import { Card, Carousel, Button, Badge } from 'antd'
import { ShoppingCartOutlined, FireOutlined, StarOutlined, GiftOutlined, LeftOutlined, RightOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import MarketHeader from '../../components/market/MarketHeader'
import MarketFooter from '../../components/market/MarketFooter'
import './MarketMain.css'

interface DisplayProduct {
  id: string
  product_id: string
  product_name: string
  product_code: string
  base_price: number
  display_order: number
  image_url?: string
}

function MarketMain() {
  const navigate = useNavigate()
  const [mainProducts, setMainProducts] = useState<DisplayProduct[]>([])
  const [recommendProducts, setRecommendProducts] = useState<DisplayProduct[]>([])
  const [eventProducts, setEventProducts] = useState<DisplayProduct[]>([])
  const mainScrollRef = useRef<HTMLDivElement>(null)
  const recommendScrollRef = useRef<HTMLDivElement>(null)
  const eventScrollRef = useRef<HTMLDivElement>(null)
  
  // 드래그 상태 관리
  const [isDragging, setIsDragging] = useState(false)
  const [startX, setStartX] = useState(0)
  const [scrollLeft, setScrollLeft] = useState(0)
  const [currentScrollRef, setCurrentScrollRef] = useState<React.RefObject<HTMLDivElement | null> | null>(null)

  // 전시 상품 데이터 로드 (샘플 데이터)
  useEffect(() => {
    // TODO: API 호출로 전시 상품 데이터 로드
    setMainProducts([
      {
        id: '1',
        product_id: '1',
        product_name: '프리미엄 노트북',
        product_code: 'PRD-001',
        base_price: 1200000,
        display_order: 1,
        image_url: 'https://via.placeholder.com/300x300?text=노트북'
      },
      {
        id: '2',
        product_id: '2',
        product_name: '최신 스마트폰',
        product_code: 'PRD-002',
        base_price: 800000,
        display_order: 2,
        image_url: 'https://via.placeholder.com/300x300?text=스마트폰'
      },
      {
        id: '3',
        product_id: '3',
        product_name: '고성능 태블릿',
        product_code: 'PRD-003',
        base_price: 600000,
        display_order: 3,
        image_url: 'https://via.placeholder.com/300x300?text=태블릿'
      },
      {
        id: '4',
        product_id: '4',
        product_name: '무선 이어폰',
        product_code: 'PRD-004',
        base_price: 150000,
        display_order: 4,
        image_url: 'https://via.placeholder.com/300x300?text=이어폰'
      },
      {
        id: '13',
        product_id: '13',
        product_name: '게이밍 마우스',
        product_code: 'PRD-013',
        base_price: 120000,
        display_order: 5,
        image_url: 'https://via.placeholder.com/300x300?text=마우스'
      },
      {
        id: '14',
        product_id: '14',
        product_name: '기계식 키보드',
        product_code: 'PRD-014',
        base_price: 180000,
        display_order: 6,
        image_url: 'https://via.placeholder.com/300x300?text=키보드'
      },
      {
        id: '15',
        product_id: '15',
        product_name: '4K 모니터',
        product_code: 'PRD-015',
        base_price: 500000,
        display_order: 7,
        image_url: 'https://via.placeholder.com/300x300?text=모니터'
      },
      {
        id: '16',
        product_id: '16',
        product_name: '웹캠',
        product_code: 'PRD-016',
        base_price: 200000,
        display_order: 8,
        image_url: 'https://via.placeholder.com/300x300?text=웹캠'
      }
    ])

    setRecommendProducts([
      {
        id: '5',
        product_id: '5',
        product_name: '프리미엄 티셔츠',
        product_code: 'PRD-005',
        base_price: 50000,
        display_order: 1,
        image_url: 'https://via.placeholder.com/300x300?text=티셔츠'
      },
      {
        id: '6',
        product_id: '6',
        product_name: '스타일리시한 바지',
        product_code: 'PRD-006',
        base_price: 80000,
        display_order: 2,
        image_url: 'https://via.placeholder.com/300x300?text=바지'
      },
      {
        id: '7',
        product_id: '7',
        product_name: '트렌디한 신발',
        product_code: 'PRD-007',
        base_price: 120000,
        display_order: 3,
        image_url: 'https://via.placeholder.com/300x300?text=신발'
      },
      {
        id: '8',
        product_id: '8',
        product_name: '모던한 가방',
        product_code: 'PRD-008',
        base_price: 200000,
        display_order: 4,
        image_url: 'https://via.placeholder.com/300x300?text=가방'
      },
      {
        id: '17',
        product_id: '17',
        product_name: '데님 자켓',
        product_code: 'PRD-017',
        base_price: 150000,
        display_order: 5,
        image_url: 'https://via.placeholder.com/300x300?text=자켓'
      },
      {
        id: '18',
        product_id: '18',
        product_name: '캐주얼 후드',
        product_code: 'PRD-018',
        base_price: 90000,
        display_order: 6,
        image_url: 'https://via.placeholder.com/300x300?text=후드'
      },
      {
        id: '19',
        product_id: '19',
        product_name: '스니커즈',
        product_code: 'PRD-019',
        base_price: 180000,
        display_order: 7,
        image_url: 'https://via.placeholder.com/300x300?text=스니커즈'
      },
      {
        id: '20',
        product_id: '20',
        product_name: '크로스백',
        product_code: 'PRD-020',
        base_price: 140000,
        display_order: 8,
        image_url: 'https://via.placeholder.com/300x300?text=크로스백'
      }
    ])

    setEventProducts([
      {
        id: '9',
        product_id: '9',
        product_name: '특가 노트북',
        product_code: 'PRD-009',
        base_price: 900000,
        display_order: 1,
        image_url: 'https://via.placeholder.com/300x300?text=특가노트북'
      },
      {
        id: '10',
        product_id: '10',
        product_name: '할인 스마트폰',
        product_code: 'PRD-010',
        base_price: 600000,
        display_order: 2,
        image_url: 'https://via.placeholder.com/300x300?text=할인폰'
      },
      {
        id: '11',
        product_id: '11',
        product_name: '이벤트 태블릿',
        product_code: 'PRD-011',
        base_price: 450000,
        display_order: 3,
        image_url: 'https://via.placeholder.com/300x300?text=이벤트태블릿'
      },
      {
        id: '12',
        product_id: '12',
        product_name: '프로모션 이어폰',
        product_code: 'PRD-012',
        base_price: 100000,
        display_order: 4,
        image_url: 'https://via.placeholder.com/300x300?text=프로모션이어폰'
      },
      {
        id: '21',
        product_id: '21',
        product_name: '초특가 마우스',
        product_code: 'PRD-021',
        base_price: 80000,
        display_order: 5,
        image_url: 'https://via.placeholder.com/300x300?text=특가마우스'
      },
      {
        id: '22',
        product_id: '22',
        product_name: '할인 키보드',
        product_code: 'PRD-022',
        base_price: 120000,
        display_order: 6,
        image_url: 'https://via.placeholder.com/300x300?text=할인키보드'
      },
      {
        id: '23',
        product_id: '23',
        product_name: '이벤트 모니터',
        product_code: 'PRD-023',
        base_price: 350000,
        display_order: 7,
        image_url: 'https://via.placeholder.com/300x300?text=이벤트모니터'
      },
      {
        id: '24',
        product_id: '24',
        product_name: '프로모션 웹캠',
        product_code: 'PRD-024',
        base_price: 150000,
        display_order: 8,
        image_url: 'https://via.placeholder.com/300x300?text=프로모션웹캠'
      }
    ])
  }, [])

  const handleProductClick = (productId: string) => {
    navigate(`/market/product/${productId}`)
  }

  const scrollProducts = (ref: React.RefObject<HTMLDivElement | null>, direction: 'left' | 'right') => {
    if (ref.current) {
      const scrollAmount = 300
      ref.current.scrollBy({
        left: direction === 'left' ? -scrollAmount : scrollAmount,
        behavior: 'smooth'
      })
    }
  }

  // 드래그 시작
  const handleMouseDown = (e: React.MouseEvent<HTMLDivElement>, ref: React.RefObject<HTMLDivElement | null>) => {
    if (!ref.current) return
    setIsDragging(true)
    setCurrentScrollRef(ref)
    setStartX(e.pageX - ref.current.offsetLeft)
    setScrollLeft(ref.current.scrollLeft)
    ref.current.style.cursor = 'grabbing'
    ref.current.style.userSelect = 'none'
  }

  // 드래그 중
  const handleMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!isDragging || !currentScrollRef?.current) return
    e.preventDefault()
    const x = e.pageX - currentScrollRef.current.offsetLeft
    const walk = (x - startX) * 2 // 스크롤 속도 조절
    currentScrollRef.current.scrollLeft = scrollLeft - walk
  }

  // 드래그 종료
  const handleMouseUp = () => {
    if (currentScrollRef?.current) {
      currentScrollRef.current.style.cursor = 'grab'
      currentScrollRef.current.style.userSelect = 'auto'
    }
    setIsDragging(false)
    setCurrentScrollRef(null)
  }

  // 터치 이벤트 (모바일)
  const handleTouchStart = (e: React.TouchEvent<HTMLDivElement>, ref: React.RefObject<HTMLDivElement | null>) => {
    if (!ref.current) return
    setIsDragging(true)
    setCurrentScrollRef(ref)
    setStartX(e.touches[0].pageX - ref.current.offsetLeft)
    setScrollLeft(ref.current.scrollLeft)
  }

  const handleTouchMove = (e: React.TouchEvent<HTMLDivElement>) => {
    if (!isDragging || !currentScrollRef?.current) return
    const x = e.touches[0].pageX - currentScrollRef.current.offsetLeft
    const walk = (x - startX) * 2
    currentScrollRef.current.scrollLeft = scrollLeft - walk
  }

  const handleTouchEnd = () => {
    setIsDragging(false)
    setCurrentScrollRef(null)
  }

  const ProductCard = ({ product }: { product: DisplayProduct }) => (
    <Card
      hoverable
      className="product-card"
      cover={
        <div className="product-image-container">
          <img
            alt={product.product_name}
            src={product.image_url || 'https://via.placeholder.com/300x300'}
            className="product-image"
          />
          <div className="product-overlay">
            <Button
              type="primary"
              icon={<ShoppingCartOutlined />}
              className="quick-add-btn"
              onClick={(e) => {
                e.stopPropagation()
                // TODO: 장바구니 추가
              }}
            >
              장바구니
            </Button>
          </div>
        </div>
      }
      onClick={() => handleProductClick(product.product_id)}
    >
      <div className="product-info">
        <h3 className="product-name">{product.product_name}</h3>
        <p className="product-code">{product.product_code}</p>
        <div className="product-price">
          <span className="price">{product.base_price.toLocaleString()}원</span>
        </div>
      </div>
    </Card>
  )

  const bannerImages = [
    {
      id: 1,
      url: 'https://via.placeholder.com/1200x400?text=박신사+신상품+출시',
      title: '신상품 출시',
      subtitle: '최신 트렌드를 만나보세요'
    },
    {
      id: 2,
      url: 'https://via.placeholder.com/1200x400?text=특별+할인+이벤트',
      title: '특별 할인 이벤트',
      subtitle: '최대 50% 할인'
    },
    {
      id: 3,
      url: 'https://via.placeholder.com/1200x400?text=프리미엄+컬렉션',
      title: '프리미엄 컬렉션',
      subtitle: '고품질 상품을 만나보세요'
    }
  ]

  return (
    <div className="market-main">
      <MarketHeader />

      {/* 메인 배너 */}
      <section className="main-banner">
        <Carousel 
          autoplay 
          effect="fade" 
          className="banner-carousel"
          draggable={true}
          swipe={true}
        >
          {bannerImages.map((banner) => (
            <div key={banner.id} className="banner-slide">
              <div
                className="banner-image"
                style={{ backgroundImage: `url(${banner.url})` }}
              >
                <div className="banner-content">
                  <h2>{banner.title}</h2>
                  <p>{banner.subtitle}</p>
                  <Button 
                    type="primary" 
                    size="large"
                    onClick={() => navigate('/market/products')}
                  >
                    바로가기
                  </Button>
                </div>
              </div>
            </div>
          ))}
        </Carousel>
      </section>

      {/* 메인 컨텐츠 */}
      <main className="market-content">
        {/* 메인 전시 영역 */}
        <section id="main" className="product-section main-section">
          <div className="section-header">
            <FireOutlined className="section-icon" />
            <h2>메인 상품</h2>
          </div>
          <div className="product-scroll-container">
            <Button
              className="scroll-btn scroll-btn-left"
              icon={<LeftOutlined />}
              onClick={() => scrollProducts(mainScrollRef, 'left')}
            />
            <div
              className="product-scroll-wrapper"
              ref={mainScrollRef}
              onMouseDown={(e) => handleMouseDown(e, mainScrollRef)}
              onMouseMove={handleMouseMove}
              onMouseUp={handleMouseUp}
              onMouseLeave={handleMouseUp}
              onTouchStart={(e) => handleTouchStart(e, mainScrollRef)}
              onTouchMove={handleTouchMove}
              onTouchEnd={handleTouchEnd}
            >
              <div className="product-scroll-content">
                {mainProducts.map((product) => (
                  <div key={product.id} className="product-scroll-item">
                    <ProductCard product={product} />
                  </div>
                ))}
              </div>
            </div>
            <Button
              className="scroll-btn scroll-btn-right"
              icon={<RightOutlined />}
              onClick={() => scrollProducts(mainScrollRef, 'right')}
            />
          </div>
        </section>

        {/* 추천 상품 영역 */}
        <section id="recommend" className="product-section recommend-section">
          <div className="section-header">
            <StarOutlined className="section-icon" />
            <h2>추천 상품</h2>
          </div>
          <div className="product-scroll-container">
            <Button
              className="scroll-btn scroll-btn-left"
              icon={<LeftOutlined />}
              onClick={() => scrollProducts(recommendScrollRef, 'left')}
            />
            <div
              className="product-scroll-wrapper"
              ref={recommendScrollRef}
              onMouseDown={(e) => handleMouseDown(e, recommendScrollRef)}
              onMouseMove={handleMouseMove}
              onMouseUp={handleMouseUp}
              onMouseLeave={handleMouseUp}
              onTouchStart={(e) => handleTouchStart(e, recommendScrollRef)}
              onTouchMove={handleTouchMove}
              onTouchEnd={handleTouchEnd}
            >
              <div className="product-scroll-content">
                {recommendProducts.map((product) => (
                  <div key={product.id} className="product-scroll-item">
                    <ProductCard product={product} />
                  </div>
                ))}
              </div>
            </div>
            <Button
              className="scroll-btn scroll-btn-right"
              icon={<RightOutlined />}
              onClick={() => scrollProducts(recommendScrollRef, 'right')}
            />
          </div>
        </section>

        {/* 이벤트 상품 영역 */}
        <section id="event" className="product-section event-section">
          <div className="section-header">
            <GiftOutlined className="section-icon" />
            <h2>
              이벤트 상품
              <Badge count="HOT" style={{ backgroundColor: '#e74c3c', marginLeft: '10px' }} />
            </h2>
          </div>
          <div className="product-scroll-container">
            <Button
              className="scroll-btn scroll-btn-left"
              icon={<LeftOutlined />}
              onClick={() => scrollProducts(eventScrollRef, 'left')}
            />
            <div
              className="product-scroll-wrapper"
              ref={eventScrollRef}
              onMouseDown={(e) => handleMouseDown(e, eventScrollRef)}
              onMouseMove={handleMouseMove}
              onMouseUp={handleMouseUp}
              onMouseLeave={handleMouseUp}
              onTouchStart={(e) => handleTouchStart(e, eventScrollRef)}
              onTouchMove={handleTouchMove}
              onTouchEnd={handleTouchEnd}
            >
              <div className="product-scroll-content">
                {eventProducts.map((product) => (
                  <div key={product.id} className="product-scroll-item">
                    <ProductCard product={product} />
                  </div>
                ))}
              </div>
            </div>
            <Button
              className="scroll-btn scroll-btn-right"
              icon={<RightOutlined />}
              onClick={() => scrollProducts(eventScrollRef, 'right')}
            />
          </div>
        </section>
      </main>

      <MarketFooter />
    </div>
  )
}

export default MarketMain


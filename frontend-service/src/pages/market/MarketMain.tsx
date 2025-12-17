import { useState, useEffect } from 'react'
import { Card, Row, Col, Carousel, Button, Badge } from 'antd'
import { ShoppingCartOutlined, HeartOutlined, FireOutlined, StarOutlined, GiftOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
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
      }
    ])
  }, [])

  const handleProductClick = (productId: string) => {
    navigate(`/market/product/${productId}`)
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
      {/* 헤더 */}
      <header className="market-header">
        <div className="header-container">
          <div className="logo" onClick={() => navigate('/market')}>
            <h1>박신사</h1>
          </div>
          <nav className="main-nav">
            <a href="#main">메인</a>
            <a href="#recommend">추천</a>
            <a href="#event">이벤트</a>
            <a href="#categories">카테고리</a>
          </nav>
          <div className="header-actions">
            <Button type="text" icon={<HeartOutlined />}>
              찜
            </Button>
            <Button type="text" icon={<ShoppingCartOutlined />}>
              장바구니
            </Button>
            <Button type="primary">로그인</Button>
          </div>
        </div>
      </header>

      {/* 메인 배너 */}
      <section className="main-banner">
        <Carousel autoplay effect="fade" className="banner-carousel">
          {bannerImages.map((banner) => (
            <div key={banner.id} className="banner-slide">
              <div
                className="banner-image"
                style={{ backgroundImage: `url(${banner.url})` }}
              >
                <div className="banner-content">
                  <h2>{banner.title}</h2>
                  <p>{banner.subtitle}</p>
                  <Button type="primary" size="large">
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
          <Row gutter={[24, 24]}>
            {mainProducts.map((product) => (
              <Col xs={12} sm={8} md={6} key={product.id}>
                <ProductCard product={product} />
              </Col>
            ))}
          </Row>
        </section>

        {/* 추천 상품 영역 */}
        <section id="recommend" className="product-section recommend-section">
          <div className="section-header">
            <StarOutlined className="section-icon" />
            <h2>추천 상품</h2>
          </div>
          <Row gutter={[24, 24]}>
            {recommendProducts.map((product) => (
              <Col xs={12} sm={8} md={6} key={product.id}>
                <ProductCard product={product} />
              </Col>
            ))}
          </Row>
        </section>

        {/* 이벤트 상품 영역 */}
        <section id="event" className="product-section event-section">
          <div className="section-header">
            <GiftOutlined className="section-icon" />
            <h2>
              이벤트 상품
              <Badge count="HOT" style={{ backgroundColor: '#ff4d4f', marginLeft: '10px' }} />
            </h2>
          </div>
          <Row gutter={[24, 24]}>
            {eventProducts.map((product) => (
              <Col xs={12} sm={8} md={6} key={product.id}>
                <ProductCard product={product} />
              </Col>
            ))}
          </Row>
        </section>
      </main>

      {/* 푸터 */}
      <footer className="market-footer">
        <div className="footer-container">
          <div className="footer-section">
            <h3>고객센터</h3>
            <p>1588-0000</p>
            <p>평일 09:00 ~ 18:00</p>
          </div>
          <div className="footer-section">
            <h3>회사정보</h3>
            <p>상호: 박신사</p>
            <p>대표: 박신사</p>
            <p>사업자등록번호: 000-00-00000</p>
          </div>
          <div className="footer-section">
            <h3>이용안내</h3>
            <p>배송안내</p>
            <p>교환/반품 안내</p>
            <p>이용약관</p>
          </div>
        </div>
        <div className="footer-bottom">
          <p>© 2024 박신사. All rights reserved.</p>
        </div>
      </footer>
    </div>
  )
}

export default MarketMain


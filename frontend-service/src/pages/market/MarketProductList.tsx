import { useState, useEffect } from 'react'
import { Input, Select, Button, Card, Row, Col, Space, Pagination } from 'antd'
import { SearchOutlined, ShoppingCartOutlined } from '@ant-design/icons'
import { useNavigate, useSearchParams } from 'react-router-dom'
import MarketHeader from '../../components/market/MarketHeader'
import MarketFooter from '../../components/market/MarketFooter'
import './MarketProductList.css'

const { Option } = Select

interface Category {
  category_id: string
  category_name: string
  parent_id: string | null
  children?: Category[]
}

interface Product {
  product_id: string
  product_name: string
  product_code: string
  base_price: number
  discount_price?: number
  category_id: string
  category_name: string
  sales_count: number
  created_at: string
  image_url?: string
}

function MarketProductList() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  
  const [categories, setCategories] = useState<Category[]>([])
  const [selectedCategory, setSelectedCategory] = useState<string>('all')
  const [searchKeyword, setSearchKeyword] = useState('')
  const [sortBy, setSortBy] = useState<string>('latest')
  const [pageSize, setPageSize] = useState<number>(8)
  const [currentPage, setCurrentPage] = useState<number>(1)
  const [products, setProducts] = useState<Product[]>([])
  const [filteredProducts, setFilteredProducts] = useState<Product[]>([])
  const [paginatedProducts, setPaginatedProducts] = useState<Product[]>([])

  // 카테고리 데이터 로드 (샘플 데이터)
  useEffect(() => {
    // TODO: API 호출로 카테고리 데이터 로드
    const sampleCategories: Category[] = [
      {
        category_id: '1',
        category_name: '전자제품',
        parent_id: null,
        children: [
          {
            category_id: '1-1',
            category_name: '노트북',
            parent_id: '1'
          },
          {
            category_id: '1-2',
            category_name: '스마트폰',
            parent_id: '1'
          },
          {
            category_id: '1-3',
            category_name: '태블릿',
            parent_id: '1'
          }
        ]
      },
      {
        category_id: '2',
        category_name: '의류',
        parent_id: null,
        children: [
          {
            category_id: '2-1',
            category_name: '상의',
            parent_id: '2'
          },
          {
            category_id: '2-2',
            category_name: '하의',
            parent_id: '2'
          },
          {
            category_id: '2-3',
            category_name: '신발',
            parent_id: '2'
          }
        ]
      },
      {
        category_id: '3',
        category_name: '도서',
        parent_id: null
      },
      {
        category_id: '4',
        category_name: '식품',
        parent_id: null
      }
    ]
    setCategories(sampleCategories)
  }, [])

  // 상품 데이터 로드 (샘플 데이터)
  useEffect(() => {
    // TODO: API 호출로 상품 데이터 로드
    const sampleProducts: Product[] = [
      {
        product_id: '1',
        product_name: '프리미엄 노트북',
        product_code: 'PRD-001',
        base_price: 1500000,
        discount_price: 1200000,
        category_id: '1-1',
        category_name: '노트북',
        sales_count: 150,
        created_at: '2024-01-01 10:00:00',
        image_url: 'https://via.placeholder.com/300x300?text=노트북'
      },
      {
        product_id: '2',
        product_name: '최신 스마트폰',
        product_code: 'PRD-002',
        base_price: 800000,
        category_id: '1-2',
        category_name: '스마트폰',
        sales_count: 230,
        created_at: '2024-01-05 14:30:00',
        image_url: 'https://via.placeholder.com/300x300?text=스마트폰'
      },
      {
        product_id: '3',
        product_name: '고성능 태블릿',
        product_code: 'PRD-003',
        base_price: 600000,
        category_id: '1-3',
        category_name: '태블릿',
        sales_count: 89,
        created_at: '2024-01-10 09:20:00',
        image_url: 'https://via.placeholder.com/300x300?text=태블릿'
      },
      {
        product_id: '4',
        product_name: '프리미엄 티셔츠',
        product_code: 'PRD-004',
        base_price: 70000,
        discount_price: 50000,
        category_id: '2-1',
        category_name: '상의',
        sales_count: 320,
        created_at: '2024-01-15 11:00:00',
        image_url: 'https://via.placeholder.com/300x300?text=티셔츠'
      },
      {
        product_id: '5',
        product_name: '스타일리시한 바지',
        product_code: 'PRD-005',
        base_price: 80000,
        category_id: '2-2',
        category_name: '하의',
        sales_count: 180,
        created_at: '2024-01-20 15:30:00',
        image_url: 'https://via.placeholder.com/300x300?text=바지'
      },
      {
        product_id: '6',
        product_name: '트렌디한 신발',
        product_code: 'PRD-006',
        base_price: 120000,
        category_id: '2-3',
        category_name: '신발',
        sales_count: 95,
        created_at: '2024-01-25 10:15:00',
        image_url: 'https://via.placeholder.com/300x300?text=신발'
      },
      {
        product_id: '7',
        product_name: '베스트셀러 도서',
        product_code: 'PRD-007',
        base_price: 20000,
        discount_price: 15000,
        category_id: '3',
        category_name: '도서',
        sales_count: 450,
        created_at: '2024-02-01 09:00:00',
        image_url: 'https://via.placeholder.com/300x300?text=도서'
      },
      {
        product_id: '8',
        product_name: '프리미엄 식품',
        product_code: 'PRD-008',
        base_price: 30000,
        category_id: '4',
        category_name: '식품',
        sales_count: 280,
        created_at: '2024-02-05 14:00:00',
        image_url: 'https://via.placeholder.com/300x300?text=식품'
      },
      {
        product_id: '9',
        product_name: '무선 이어폰',
        product_code: 'PRD-009',
        base_price: 150000,
        category_id: '1-2',
        category_name: '스마트폰',
        sales_count: 320,
        created_at: '2024-02-10 11:00:00',
        image_url: 'https://via.placeholder.com/300x300?text=이어폰'
      },
      {
        product_id: '10',
        product_name: '스마트 워치',
        product_code: 'PRD-010',
        base_price: 350000,
        category_id: '1-2',
        category_name: '스마트폰',
        sales_count: 180,
        created_at: '2024-02-12 14:30:00',
        image_url: 'https://via.placeholder.com/300x300?text=워치'
      },
      {
        product_id: '11',
        product_name: '청바지',
        product_code: 'PRD-011',
        base_price: 90000,
        category_id: '2-2',
        category_name: '하의',
        sales_count: 250,
        created_at: '2024-02-15 10:00:00',
        image_url: 'https://via.placeholder.com/300x300?text=청바지'
      },
      {
        product_id: '12',
        product_name: '운동화',
        product_code: 'PRD-012',
        base_price: 130000,
        category_id: '2-3',
        category_name: '신발',
        sales_count: 200,
        created_at: '2024-02-18 16:00:00',
        image_url: 'https://via.placeholder.com/300x300?text=운동화'
      },
      {
        product_id: '13',
        product_name: '소설책',
        product_code: 'PRD-013',
        base_price: 12000,
        category_id: '3',
        category_name: '도서',
        sales_count: 380,
        created_at: '2024-02-20 09:30:00',
        image_url: 'https://via.placeholder.com/300x300?text=소설'
      },
      {
        product_id: '14',
        product_name: '건강식품',
        product_code: 'PRD-014',
        base_price: 45000,
        category_id: '4',
        category_name: '식품',
        sales_count: 190,
        created_at: '2024-02-22 13:00:00',
        image_url: 'https://via.placeholder.com/300x300?text=건강식품'
      }
    ]
    setProducts(sampleProducts)
  }, [])

  // URL 파라미터에서 초기값 설정
  useEffect(() => {
    const category = searchParams.get('category') || 'all'
    const keyword = searchParams.get('keyword') || ''
    const sort = searchParams.get('sort') || 'latest'
    const page = parseInt(searchParams.get('page') || '1', 10)
    const size = parseInt(searchParams.get('pageSize') || '8', 10)
    
    setSelectedCategory(category)
    setSearchKeyword(keyword)
    setSortBy(sort)
    setCurrentPage(page)
    setPageSize(size)
  }, [searchParams])

  // 필터링 및 정렬
  useEffect(() => {
    let filtered = [...products]

    // 카테고리 필터
    if (selectedCategory !== 'all') {
      // 선택된 카테고리와 그 하위 카테고리 모두 포함
      const categoryIds = getCategoryIds(selectedCategory, categories)
      filtered = filtered.filter(product => 
        categoryIds.includes(product.category_id) ||
        product.category_id === selectedCategory
      )
    }

    // 검색어 필터
    if (searchKeyword) {
      filtered = filtered.filter(product =>
        product.product_name.toLowerCase().includes(searchKeyword.toLowerCase()) ||
        product.product_code.toLowerCase().includes(searchKeyword.toLowerCase())
      )
    }

    // 정렬
    filtered.sort((a, b) => {
      switch (sortBy) {
        case 'price-low':
          return a.base_price - b.base_price
        case 'price-high':
          return b.base_price - a.base_price
        case 'sales':
          return b.sales_count - a.sales_count
        case 'latest':
        default:
          return new Date(b.created_at).getTime() - new Date(a.created_at).getTime()
      }
    })

    setFilteredProducts(filtered)
  }, [selectedCategory, searchKeyword, sortBy, products, categories])

  // 페이지네이션 적용
  useEffect(() => {
    const startIndex = (currentPage - 1) * pageSize
    const endIndex = startIndex + pageSize
    const paginated = filteredProducts.slice(startIndex, endIndex)
    setPaginatedProducts(paginated)
  }, [filteredProducts, currentPage, pageSize])

  const getCategoryIds = (categoryId: string, cats: Category[]): string[] => {
    const ids: string[] = [categoryId]
    for (const cat of cats) {
      if (cat.category_id === categoryId && cat.children) {
        cat.children.forEach(child => {
          ids.push(child.category_id)
        })
      }
    }
    return ids
  }

  const handleSearch = () => {
    const params = new URLSearchParams()
    if (selectedCategory !== 'all') {
      params.set('category', selectedCategory)
    }
    if (searchKeyword) {
      params.set('keyword', searchKeyword)
    }
    if (sortBy !== 'latest') {
      params.set('sort', sortBy)
    }
    setSearchParams(params)
  }

  const handleCategoryClick = (categoryId: string) => {
    setSelectedCategory(categoryId)
    const params = new URLSearchParams(searchParams)
    if (categoryId === 'all') {
      params.delete('category')
    } else {
      params.set('category', categoryId)
    }
    setSearchParams(params)
  }

  const handleSortChange = (value: string) => {
    setSortBy(value)
    const params = new URLSearchParams(searchParams)
    if (value === 'latest') {
      params.delete('sort')
    } else {
      params.set('sort', value)
    }
    setCurrentPage(1) // 정렬 변경 시 첫 페이지로 이동
    params.set('page', '1')
    setSearchParams(params)
  }

  const handlePageSizeChange = (value: number) => {
    setPageSize(value)
    setCurrentPage(1) // 페이지 크기 변경 시 첫 페이지로 이동
    const params = new URLSearchParams(searchParams)
    params.set('pageSize', value.toString())
    params.set('page', '1')
    setSearchParams(params)
  }

  const handlePageChange = (page: number) => {
    setCurrentPage(page)
    const params = new URLSearchParams(searchParams)
    params.set('page', page.toString())
    setSearchParams(params)
    // 페이지 변경 시 스크롤을 상단으로 이동
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const handleProductClick = (productId: string) => {
    navigate(`/market/product/${productId}`)
  }

  const ProductCard = ({ product }: { product: Product }) => (
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
        <div className="product-category-badge">{product.category_name}</div>
        <h3 className="product-name">{product.product_name}</h3>
        <div className="product-price">
          {product.discount_price ? (
            <>
              <div className="price-original">
                <span className="original-price">{product.base_price.toLocaleString()}원</span>
              </div>
              <div className="price-discount">
                <span className="discount-rate">
                  {Math.round(((product.base_price - product.discount_price) / product.base_price) * 100)}%
                </span>
                <span className="discount-price">{product.discount_price.toLocaleString()}원</span>
              </div>
            </>
          ) : (
            <span className="price">{product.base_price.toLocaleString()}원</span>
          )}
        </div>
      </div>
    </Card>
  )

  return (
    <div className="market-product-list">
      <MarketHeader />
      
      <div className="product-list-container">
        {/* 검색바 */}
        <div className="search-section">
          <div className="search-box">
            <Select
              value={selectedCategory}
              onChange={setSelectedCategory}
              style={{ width: 150 }}
              className="category-select"
            >
              <Option value="all">전체</Option>
              {categories.map(category => (
                <Option key={category.category_id} value={category.category_id}>
                  {category.category_name}
                </Option>
              ))}
            </Select>
            <Input
              placeholder="상품명 또는 상품 코드로 검색"
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              onPressEnter={handleSearch}
              className="search-input"
            />
            <Button
              type="primary"
              icon={<SearchOutlined />}
              onClick={handleSearch}
              className="search-btn"
            >
              검색
            </Button>
          </div>
        </div>

        <div className="product-list-content">
          {/* 카테고리 사이드바 */}
          <aside className="category-sidebar">
            <div className="sidebar-header">
              <h3>카테고리</h3>
            </div>
            <div className="category-list">
              <div
                className={`category-item ${selectedCategory === 'all' ? 'active' : ''}`}
                onClick={() => handleCategoryClick('all')}
              >
                전체
              </div>
              {categories.map(category => (
                <div key={category.category_id} className="category-group">
                  <div
                    className={`category-item category-parent ${selectedCategory === category.category_id ? 'active' : ''}`}
                    onClick={() => handleCategoryClick(category.category_id)}
                  >
                    {category.category_name}
                  </div>
                  {category.children && category.children.map(child => (
                    <div
                      key={child.category_id}
                      className={`category-item category-child ${selectedCategory === child.category_id ? 'active' : ''}`}
                      onClick={() => handleCategoryClick(child.category_id)}
                    >
                      {child.category_name}
                    </div>
                  ))}
                </div>
              ))}
            </div>
          </aside>

          {/* 상품 목록 영역 */}
          <main className="product-list-main">
            <div className="product-list-header">
              <div className="result-info">
                <span className="result-count">
                  총 {filteredProducts.length}개의 상품
                  {filteredProducts.length > 0 && (
                    <span className="page-info">
                      ({(currentPage - 1) * pageSize + 1} - {Math.min(currentPage * pageSize, filteredProducts.length)}개 표시)
                    </span>
                  )}
                </span>
              </div>
              <div className="sort-controls">
                <Space>
                  <Select
                    value={pageSize}
                    onChange={handlePageSizeChange}
                    style={{ width: 100 }}
                  >
                    <Option value={8}>8개씩</Option>
                    <Option value={12}>12개씩</Option>
                    <Option value={16}>16개씩</Option>
                  </Select>
                  <Select
                    value={sortBy}
                    onChange={handleSortChange}
                    style={{ width: 150 }}
                  >
                    <Option value="latest">최신순</Option>
                    <Option value="price-low">낮은 가격순</Option>
                    <Option value="price-high">높은 가격순</Option>
                    <Option value="sales">판매량순</Option>
                  </Select>
                </Space>
              </div>
            </div>

            {filteredProducts.length === 0 ? (
              <div className="empty-state">
                <p>검색 결과가 없습니다.</p>
              </div>
            ) : (
              <>
                <Row gutter={[24, 24]}>
                  {paginatedProducts.map((product) => (
                    <Col xs={12} sm={8} md={6} key={product.product_id}>
                      <ProductCard product={product} />
                    </Col>
                  ))}
                </Row>
                <div className="pagination-wrapper">
                  <Pagination
                    current={currentPage}
                    total={filteredProducts.length}
                    pageSize={pageSize}
                    onChange={handlePageChange}
                    showSizeChanger={false}
                    showTotal={(total, range) => `${range[0]}-${range[1]} / ${total}개`}
                    pageSizeOptions={[]}
                  />
                </div>
              </>
            )}
          </main>
        </div>
      </div>

      <MarketFooter />
    </div>
  )
}

export default MarketProductList


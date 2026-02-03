import { useState, useEffect, useCallback, useRef } from 'react'
import { Input, Select, Button, Card, Row, Col, Space, Pagination, Spin, message } from 'antd'
import { SearchOutlined, ShoppingCartOutlined } from '@ant-design/icons'
import { useNavigate, useSearchParams } from 'react-router-dom'
import MarketHeader from '../../components/market/MarketHeader'
import MarketFooter from '../../components/market/MarketFooter'
import { getDisplayCategoryTree, type CategoryTreeNode } from '../../api/categoryApi'
import { getCatalogProducts, autocompleteProductName } from '../../api/catalogApi'
import type { CatalogProductResponse } from '../../api/catalogApi'
import { PRODUCT_FILE_URL } from '../../config/env'
import './MarketProductList.css'

const { Option } = Select

function MarketProductList() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()

  const [categories, setCategories] = useState<CategoryTreeNode[]>([])
  const [selectedCategory, setSelectedCategory] = useState<string>('all')
  const [searchKeyword, setSearchKeyword] = useState('')
  const [sortBy, setSortBy] = useState<string>('latest')
  const [pageSize, setPageSize] = useState<number>(8)
  const [currentPage, setCurrentPage] = useState<number>(1)
  const [products, setProducts] = useState<CatalogProductResponse[]>([])
  const [totalElements, setTotalElements] = useState<number>(0)
  const [loading, setLoading] = useState<boolean>(false)

  // 자동완성 관련 state
  const [suggestions, setSuggestions] = useState<string[]>([])
  const [showSuggestions, setShowSuggestions] = useState<boolean>(false)
  const [activeSuggestionIndex, setActiveSuggestionIndex] = useState<number>(-1)
  const searchBoxRef = useRef<HTMLDivElement>(null)
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  // 정렬 옵션을 API 형식으로 변환
  const getSortParam = useCallback((sort: string): string => {
    switch (sort) {
      case 'price-low':
        return 'basePrice,asc'
      case 'price-high':
        return 'basePrice,desc'
      case 'latest':
      default:
        return 'createdAt,desc'
    }
  }, [])

  // 자동완성 API 호출 (debounce 적용)
  const fetchSuggestions = useCallback(async (keyword: string) => {
    if (keyword.trim().length === 0) {
      setSuggestions([])
      setShowSuggestions(false)
      return
    }

    try {
      const data = await autocompleteProductName(keyword)
      setSuggestions(data)
      setShowSuggestions(data.length > 0)
      setActiveSuggestionIndex(-1)
    } catch (error) {
      console.error('자동완성 조회 실패:', error)
      setSuggestions([])
      setShowSuggestions(false)
    }
  }, [])

  // 검색어 입력 핸들러 (자동완성 포함)
  const handleKeywordChange = useCallback((value: string) => {
    setSearchKeyword(value)

    // 기존 debounce 취소
    if (debounceRef.current) {
      clearTimeout(debounceRef.current)
    }

    // 300ms debounce 적용
    debounceRef.current = setTimeout(() => {
      fetchSuggestions(value)
    }, 300)
  }, [fetchSuggestions])

  // 자동완성 항목 선택
  const handleSuggestionSelect = useCallback((suggestion: string) => {
    setSearchKeyword(suggestion)
    setShowSuggestions(false)
    setSuggestions([])
    // 선택 후 바로 검색 실행
    const params = new URLSearchParams()
    if (selectedCategory !== 'all') {
      params.set('category', selectedCategory)
    }
    params.set('keyword', suggestion)
    if (sortBy !== 'latest') {
      params.set('sort', sortBy)
    }
    setSearchParams(params)
  }, [selectedCategory, sortBy, setSearchParams])

  // 키보드 네비게이션 핸들러
  const handleKeyDown = useCallback((e: React.KeyboardEvent<HTMLInputElement>) => {
    if (!showSuggestions || suggestions.length === 0) {
      if (e.key === 'Enter') {
        handleSearch()
      }
      return
    }

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault()
        setActiveSuggestionIndex(prev =>
          prev < suggestions.length - 1 ? prev + 1 : prev
        )
        break
      case 'ArrowUp':
        e.preventDefault()
        setActiveSuggestionIndex(prev => prev > 0 ? prev - 1 : -1)
        break
      case 'Enter':
        e.preventDefault()
        if (activeSuggestionIndex >= 0) {
          handleSuggestionSelect(suggestions[activeSuggestionIndex])
        } else {
          setShowSuggestions(false)
          handleSearch()
        }
        break
      case 'Escape':
        setShowSuggestions(false)
        setActiveSuggestionIndex(-1)
        break
    }
  }, [showSuggestions, suggestions, activeSuggestionIndex, handleSuggestionSelect])

  // 외부 클릭 시 자동완성 닫기
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (searchBoxRef.current && !searchBoxRef.current.contains(event.target as Node)) {
        setShowSuggestions(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [])

  // 컴포넌트 언마운트 시 debounce 정리
  useEffect(() => {
    return () => {
      if (debounceRef.current) {
        clearTimeout(debounceRef.current)
      }
    }
  }, [])

  // 카테고리 데이터 로드
  useEffect(() => {
    const fetchCategories = async () => {
      try {
        const data = await getDisplayCategoryTree()
        setCategories(data)
      } catch (error) {
        console.error('카테고리 조회 실패:', error)
      }
    }
    fetchCategories()
  }, [])

  // 상품 데이터 로드 (API 호출)
  const fetchProducts = useCallback(async () => {
    setLoading(true)
    try {
      const response = await getCatalogProducts({
        productName: searchKeyword || undefined,
        categoryId: selectedCategory !== 'all' ? Number(selectedCategory) : undefined,
        status: 'ACTIVE',
        page: currentPage - 1, // API는 0부터 시작
        size: pageSize,
        sort: getSortParam(sortBy),
      })
      setProducts(response.content)
      setTotalElements(response.totalElements)
    } catch (error) {
      console.error('상품 조회 실패:', error)
      message.error('상품 목록을 불러오는데 실패했습니다.')
      setProducts([])
      setTotalElements(0)
    } finally {
      setLoading(false)
    }
  }, [searchKeyword, selectedCategory, currentPage, pageSize, sortBy, getSortParam])

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

  // 검색 조건이 변경되면 상품 목록 다시 로드
  useEffect(() => {
    fetchProducts()
  }, [fetchProducts])

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

  // 카테고리 ID로 카테고리명 조회 (3단계까지 지원)
  const getCategoryName = (categoryIds?: number[]): string => {
    if (!categoryIds || categoryIds.length === 0) return ''
    const categoryId = categoryIds[0]
    for (const cat of categories) {
      if (cat.categoryId === categoryId) return cat.categoryName
      if (cat.children) {
        for (const child of cat.children) {
          if (child.categoryId === categoryId) return child.categoryName
          if (child.children) {
            for (const grandChild of child.children) {
              if (grandChild.categoryId === categoryId) return grandChild.categoryName
            }
          }
        }
      }
    }
    return ''
  }

  const ProductCard = ({ product }: { product: CatalogProductResponse }) => (
    <Card
      hoverable
      className="product-card"
      cover={
        <div className="product-image-container">
          <img
            alt={product.productName}
            src={product.primaryImageUrl ? `${PRODUCT_FILE_URL}${product.primaryImageUrl}` : 'https://via.placeholder.com/300x300'}
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
      onClick={() => handleProductClick(product.productId)}
    >
      <div className="product-info">
        {getCategoryName(product.categoryIds) && (
          <div className="product-category-badge">{getCategoryName(product.categoryIds)}</div>
        )}
        <h3 className="product-name">{product.productName}</h3>
        <div className="product-price">
          {product.salePrice && product.salePrice < product.basePrice ? (
            <>
              <div className="price-original">
                <span className="original-price">{product.basePrice.toLocaleString()}원</span>
              </div>
              <div className="price-discount">
                <span className="discount-rate">
                  {Math.round(((product.basePrice - product.salePrice) / product.basePrice) * 100)}%
                </span>
                <span className="discount-price">{product.salePrice.toLocaleString()}원</span>
              </div>
            </>
          ) : (
            <span className="price">{product.basePrice.toLocaleString()}원</span>
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
          <div className="search-box" ref={searchBoxRef}>
            <Select
              value={selectedCategory}
              onChange={setSelectedCategory}
              style={{ width: 150 }}
              className="category-select"
            >
              <Option value="all">전체</Option>
              {categories.map(category => (
                <Option key={category.categoryId} value={String(category.categoryId)}>
                  {category.categoryName}
                </Option>
              ))}
            </Select>
            <div className="search-input-wrapper">
              <Input
                placeholder="상품명 또는 상품 코드로 검색"
                value={searchKeyword}
                onChange={(e) => handleKeywordChange(e.target.value)}
                onKeyDown={handleKeyDown}
                onFocus={() => suggestions.length > 0 && setShowSuggestions(true)}
                className="search-input"
              />
              {showSuggestions && suggestions.length > 0 && (
                <div className="autocomplete-dropdown">
                  {suggestions.map((suggestion, index) => (
                    <div
                      key={index}
                      className={`autocomplete-item ${index === activeSuggestionIndex ? 'active' : ''}`}
                      onClick={() => handleSuggestionSelect(suggestion)}
                      onMouseEnter={() => setActiveSuggestionIndex(index)}
                    >
                      <SearchOutlined className="autocomplete-icon" />
                      <span>{suggestion}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
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
                <div key={category.categoryId} className="category-group">
                  <div
                    className={`category-item category-parent ${selectedCategory === String(category.categoryId) ? 'active' : ''}`}
                    onClick={() => handleCategoryClick(String(category.categoryId))}
                  >
                    {category.categoryName}
                  </div>
                  {category.children && category.children.map(child => (
                    <div key={child.categoryId} className="category-subgroup">
                      <div
                        className={`category-item category-child ${selectedCategory === String(child.categoryId) ? 'active' : ''}`}
                        onClick={() => handleCategoryClick(String(child.categoryId))}
                      >
                        {child.categoryName}
                      </div>
                      {child.children && child.children.map(grandChild => (
                        <div
                          key={grandChild.categoryId}
                          className={`category-item category-grandchild ${selectedCategory === String(grandChild.categoryId) ? 'active' : ''}`}
                          onClick={() => handleCategoryClick(String(grandChild.categoryId))}
                        >
                          {grandChild.categoryName}
                        </div>
                      ))}
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
                  총 {totalElements}개의 상품
                  {totalElements > 0 && (
                    <span className="page-info">
                      ({(currentPage - 1) * pageSize + 1} - {Math.min(currentPage * pageSize, totalElements)}개 표시)
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
                  </Select>
                </Space>
              </div>
            </div>

            <Spin spinning={loading}>
              {!loading && products.length === 0 ? (
                <div className="empty-state">
                  <p>검색 결과가 없습니다.</p>
                </div>
              ) : (
                <>
                  <Row gutter={[24, 24]}>
                    {products.map((product) => (
                      <Col xs={12} sm={8} md={6} key={product.productId}>
                        <ProductCard product={product} />
                      </Col>
                    ))}
                  </Row>
                  {totalElements > 0 && (
                    <div className="pagination-wrapper">
                      <Pagination
                        current={currentPage}
                        total={totalElements}
                        pageSize={pageSize}
                        onChange={handlePageChange}
                        showSizeChanger={false}
                        showTotal={(total, range) => `${range[0]}-${range[1]} / ${total}개`}
                        pageSizeOptions={[]}
                      />
                    </div>
                  )}
                </>
              )}
            </Spin>
          </main>
        </div>
      </div>

      <MarketFooter />
    </div>
  )
}

export default MarketProductList


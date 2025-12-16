import { useState, useEffect } from 'react'
import { Table, Card, message, Space, Input, Button, Select, Tag, Popconfirm } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons'
import './AdminSearchKeywordManage.css'

const { Option } = Select

interface Product {
  id: string
  product_name: string
  product_code: string
  category_id: string
  category_name?: string
}

interface SearchKeyword {
  keyword_id: string
  product_id: string
  keyword: string
  created_at: string
}

function AdminSearchKeywordManage() {
  const [products, setProducts] = useState<Product[]>([])
  const [filteredProducts, setFilteredProducts] = useState<Product[]>([])
  const [searchCategory, setSearchCategory] = useState<string | undefined>(undefined)
  const [searchName, setSearchName] = useState('')
  const [keywords, setKeywords] = useState<Record<string, SearchKeyword[]>>({})
  const [newKeyword, setNewKeyword] = useState<Record<string, string>>({})

  const categoryMap: Record<string, string> = {
    electronics: '전자제품',
    clothing: '의류',
    food: '식품',
    books: '도서',
    sports: '스포츠',
    beauty: '뷰티'
  }

  // 상품 데이터 로드 (샘플 데이터)
  useEffect(() => {
    // TODO: API 호출로 상품 데이터 로드
    const sampleProducts: Product[] = [
      {
        id: '1',
        product_name: '노트북',
        product_code: 'PRD-001',
        category_id: 'electronics',
        category_name: '전자제품'
      },
      {
        id: '2',
        product_name: '스마트폰',
        product_code: 'PRD-002',
        category_id: 'electronics',
        category_name: '전자제품'
      },
      {
        id: '3',
        product_name: '티셔츠',
        product_code: 'PRD-003',
        category_id: 'clothing',
        category_name: '의류'
      },
      {
        id: '4',
        product_name: '바지',
        product_code: 'PRD-004',
        category_id: 'clothing',
        category_name: '의류'
      }
    ]
    setProducts(sampleProducts)
  }, [])

  // 키워드 데이터 로드 (샘플 데이터)
  useEffect(() => {
    // TODO: API 호출로 키워드 데이터 로드
    const sampleKeywords: Record<string, SearchKeyword[]> = {
      '1': [
        {
          keyword_id: '1',
          product_id: '1',
          keyword: '랩탑',
          created_at: '2024-01-15 10:00:00'
        },
        {
          keyword_id: '2',
          product_id: '1',
          keyword: '컴퓨터',
          created_at: '2024-01-15 10:01:00'
        }
      ],
      '2': [
        {
          keyword_id: '3',
          product_id: '2',
          keyword: '핸드폰',
          created_at: '2024-01-15 11:00:00'
        }
      ]
    }
    setKeywords(sampleKeywords)
  }, [])

  // 필터링된 데이터
  useEffect(() => {
    const filtered = products.filter((product) => {
      const nameMatch = !searchName || 
        product.product_name.toLowerCase().includes(searchName.toLowerCase()) ||
        product.product_code.toLowerCase().includes(searchName.toLowerCase())
      const categoryMatch = !searchCategory || product.category_id === searchCategory
      return nameMatch && categoryMatch
    })
    setFilteredProducts(filtered)
  }, [searchName, searchCategory, products])

  const handleSearch = () => {
    // 검색 버튼 클릭 시 필터 적용 (현재는 실시간 필터링이므로 별도 로직 불필요)
  }

  const handleReset = () => {
    setSearchName('')
    setSearchCategory(undefined)
  }

  // 키워드 추가
  const handleAddKeyword = (productId: string) => {
    const keyword = newKeyword[productId]?.trim()
    
    if (!keyword) {
      message.warning('키워드를 입력하세요.')
      return
    }

    if (keyword.length > 100) {
      message.error('키워드는 최대 100자까지 입력 가능합니다.')
      return
    }

    // 중복 체크
    const existingKeywords = keywords[productId] || []
    if (existingKeywords.some(k => k.keyword.toLowerCase() === keyword.toLowerCase())) {
      message.warning('이미 등록된 키워드입니다.')
      return
    }

    const newKeywordData: SearchKeyword = {
      keyword_id: `kw_${Date.now()}`,
      product_id: productId,
      keyword: keyword,
      created_at: new Date().toISOString()
    }

    setKeywords(prev => ({
      ...prev,
      [productId]: [...existingKeywords, newKeywordData]
    }))

    setNewKeyword(prev => ({
      ...prev,
      [productId]: ''
    }))

    // TODO: API 호출로 키워드 추가
    message.success('키워드가 추가되었습니다.')
  }

  // 키워드 삭제
  const handleDeleteKeyword = (productId: string, keywordId: string) => {
    setKeywords(prev => ({
      ...prev,
      [productId]: (prev[productId] || []).filter(k => k.keyword_id !== keywordId)
    }))

    // TODO: API 호출로 키워드 삭제
    message.success('키워드가 삭제되었습니다.')
  }

  // 테이블 컬럼 정의
  const columns: ColumnsType<Product> = [
    {
      title: '상품명',
      dataIndex: 'product_name',
      key: 'product_name',
      sorter: (a, b) => a.product_name.localeCompare(b.product_name),
      width: 200,
    },
    {
      title: '상품 코드',
      dataIndex: 'product_code',
      key: 'product_code',
      sorter: (a, b) => a.product_code.localeCompare(b.product_code),
      width: 150,
    },
    {
      title: '카테고리',
      dataIndex: 'category_id',
      key: 'category_id',
      render: (categoryId: string) => categoryMap[categoryId] || categoryId,
      width: 120,
    },
    {
      title: '검색 키워드',
      key: 'keywords',
      render: (_, record: Product) => {
        const productKeywords = keywords[record.id] || []
        const keywordInput = newKeyword[record.id] || ''
        
        return (
          <div className="keyword-cell">
            <div className="keyword-tags">
              {productKeywords.length > 0 ? (
                productKeywords.map(kw => (
                  <Tag
                    key={kw.keyword_id}
                    closable
                    onClose={() => handleDeleteKeyword(record.id, kw.keyword_id)}
                    style={{ marginBottom: '8px' }}
                  >
                    {kw.keyword}
                  </Tag>
                ))
              ) : (
                <span style={{ color: '#999', fontSize: '12px' }}>키워드 없음</span>
              )}
            </div>
            <div className="keyword-input-group">
              <Input
                placeholder="키워드 입력 (최대 100자)"
                maxLength={100}
                value={keywordInput}
                onChange={(e) => setNewKeyword(prev => ({
                  ...prev,
                  [record.id]: e.target.value
                }))}
                onPressEnter={() => handleAddKeyword(record.id)}
                style={{ width: '200px', marginRight: '8px' }}
              />
              <Button
                type="primary"
                size="small"
                icon={<PlusOutlined />}
                onClick={() => handleAddKeyword(record.id)}
              >
                추가
              </Button>
            </div>
          </div>
        )
      },
    },
  ]

  return (
    <div className="admin-search-keyword-manage">
      <div className="search-keyword-container">
        <h2>검색 키워드 관리</h2>

        {/* 필터 박스 */}
        <div className="product-list-filters">
          <div className="filter-inputs">
            <Space size="middle">
              <Input
                placeholder="상품명 또는 상품 코드 검색"
                allowClear
                style={{ width: 250 }}
                value={searchName}
                onChange={(e) => setSearchName(e.target.value)}
                onPressEnter={handleSearch}
              />
              <Select
                placeholder="카테고리 선택"
                allowClear
                style={{ width: 150 }}
                value={searchCategory}
                onChange={(value) => setSearchCategory(value)}
              >
                <Option value="electronics">전자제품</Option>
                <Option value="clothing">의류</Option>
                <Option value="food">식품</Option>
                <Option value="books">도서</Option>
                <Option value="sports">스포츠</Option>
                <Option value="beauty">뷰티</Option>
              </Select>
            </Space>
          </div>
          <div className="filter-actions">
            <Space>
              <Button onClick={handleReset}>초기화</Button>
              <Button type="primary" onClick={handleSearch}>
                검색
              </Button>
            </Space>
          </div>
        </div>

        {/* 상품 및 키워드 테이블 */}
        <Card>
          <Table
            columns={columns}
            dataSource={filteredProducts}
            rowKey="id"
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              showTotal: (total) => `총 ${total}개`,
            }}
          />
        </Card>
      </div>
    </div>
  )
}

export default AdminSearchKeywordManage


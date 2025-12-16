import { useState, useEffect } from 'react'
import { Table, Switch, Card, message, Space, Input, Tabs, Button, Popconfirm, Select } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PlusOutlined, DeleteOutlined, ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons'
import './AdminDisplayProductManage.css'

const { Option } = Select

type DisplayArea = 'MAIN' | 'RECOMMEND' | 'EVENT'

interface Product {
  id: string
  product_name: string
  product_code: string
  base_price: number
  is_displayed: boolean
  category_id: string
  category_name?: string
}

interface DisplayProduct {
  id: string
  product_id: string
  product_name: string
  product_code: string
  base_price: number
  display_order: number
}

function AdminDisplayProductManage() {
  const [selectedDisplayArea, setSelectedDisplayArea] = useState<DisplayArea>('MAIN')
  const [products, setProducts] = useState<Product[]>([])
  const [filteredProducts, setFilteredProducts] = useState<Product[]>([])
  const [displayProducts, setDisplayProducts] = useState<Record<DisplayArea, DisplayProduct[]>>({
    MAIN: [],
    RECOMMEND: [],
    EVENT: []
  })
  const [searchCategory, setSearchCategory] = useState<string | undefined>(undefined)
  const [searchName, setSearchName] = useState('')

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
        base_price: 1200000,
        is_displayed: true,
        category_id: 'electronics',
        category_name: '전자제품'
      },
      {
        id: '2',
        product_name: '스마트폰',
        product_code: 'PRD-002',
        base_price: 800000,
        is_displayed: false,
        category_id: 'electronics',
        category_name: '전자제품'
      },
      {
        id: '3',
        product_name: '티셔츠',
        product_code: 'PRD-003',
        base_price: 30000,
        is_displayed: true,
        category_id: 'clothing',
        category_name: '의류'
      },
      {
        id: '4',
        product_name: '바지',
        product_code: 'PRD-004',
        base_price: 50000,
        is_displayed: true,
        category_id: 'clothing',
        category_name: '의류'
      }
    ]
    setProducts(sampleProducts)
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

  // 전시 여부 토글
  const handleDisplayToggle = (productId: string, checked: boolean) => {
    setProducts(prev => 
      prev.map(p => 
        p.id === productId ? { ...p, is_displayed: checked } : p
      )
    )
    
    // TODO: API 호출로 전시 여부 업데이트
    message.success(`상품 전시 여부가 ${checked ? '활성화' : '비활성화'}되었습니다.`)
  }

  // 전시 영역에 상품 추가
  const handleAddToDisplayArea = (product: Product) => {
    const currentList = displayProducts[selectedDisplayArea]
    
    // 이미 추가된 상품인지 확인
    if (currentList.some(dp => dp.product_id === product.id)) {
      message.warning('이미 해당 전시 영역에 추가된 상품입니다.')
      return
    }

    const newDisplayProduct: DisplayProduct = {
      id: `disp_${Date.now()}`,
      product_id: product.id,
      product_name: product.product_name,
      product_code: product.product_code,
      base_price: product.base_price,
      display_order: currentList.length
    }

    setDisplayProducts(prev => ({
      ...prev,
      [selectedDisplayArea]: [...currentList, newDisplayProduct]
    }))

    message.success('전시 영역에 상품이 추가되었습니다.')
  }

  // 전시 영역에서 상품 제거
  const handleRemoveFromDisplayArea = (displayProductId: string) => {
    setDisplayProducts(prev => ({
      ...prev,
      [selectedDisplayArea]: prev[selectedDisplayArea].filter(dp => dp.id !== displayProductId)
        .map((dp, index) => ({ ...dp, display_order: index }))
    }))
    message.success('전시 영역에서 상품이 제거되었습니다.')
  }

  // 전시 순서 변경
  const handleMoveOrder = (displayProductId: string, direction: 'up' | 'down') => {
    const currentList = [...displayProducts[selectedDisplayArea]]
    const index = currentList.findIndex(dp => dp.id === displayProductId)
    
    if (index === -1) return

    if (direction === 'up' && index > 0) {
      [currentList[index - 1], currentList[index]] = [currentList[index], currentList[index - 1]]
      currentList[index - 1].display_order = index - 1
      currentList[index].display_order = index
    } else if (direction === 'down' && index < currentList.length - 1) {
      [currentList[index], currentList[index + 1]] = [currentList[index + 1], currentList[index]]
      currentList[index].display_order = index
      currentList[index + 1].display_order = index + 1
    }

    setDisplayProducts(prev => ({
      ...prev,
      [selectedDisplayArea]: currentList
    }))
  }

  // 카테고리별 상품 목록 컬럼
  const productColumns: ColumnsType<Product> = [
    {
      title: '상품명',
      dataIndex: 'product_name',
      key: 'product_name',
      sorter: (a, b) => a.product_name.localeCompare(b.product_name),
    },
    {
      title: '상품 코드',
      dataIndex: 'product_code',
      key: 'product_code',
      sorter: (a, b) => a.product_code.localeCompare(b.product_code),
    },
    {
      title: '가격',
      dataIndex: 'base_price',
      key: 'base_price',
      render: (price: number) => `${price.toLocaleString()}원`,
      sorter: (a, b) => a.base_price - b.base_price,
    },
    {
      title: '전시 여부',
      dataIndex: 'is_displayed',
      key: 'is_displayed',
      width: 120,
      render: (isDisplayed: boolean, record: Product) => (
        <Switch
          checked={isDisplayed}
          onChange={(checked) => handleDisplayToggle(record.id, checked)}
          checkedChildren="ON"
          unCheckedChildren="OFF"
        />
      ),
    },
    {
      title: '작업',
      key: 'action',
      width: 100,
      render: (_, record: Product) => (
        <Button
          type="primary"
          size="small"
          icon={<PlusOutlined />}
          onClick={() => handleAddToDisplayArea(record)}
        >
          추가
        </Button>
      ),
    },
  ]

  // 전시 영역 상품 리스트 컬럼
  const displayProductColumns: ColumnsType<DisplayProduct> = [
    {
      title: '순서',
      key: 'order',
      width: 80,
      render: (_, record, index) => index + 1,
    },
    {
      title: '상품명',
      dataIndex: 'product_name',
      key: 'product_name',
    },
    {
      title: '상품 코드',
      dataIndex: 'product_code',
      key: 'product_code',
    },
    {
      title: '가격',
      dataIndex: 'base_price',
      key: 'base_price',
      render: (price: number) => `${price.toLocaleString()}원`,
    },
    {
      title: '작업',
      key: 'action',
      width: 200,
      render: (_, record, index) => {
        const currentList = displayProducts[selectedDisplayArea]
        return (
          <Space>
            <Button
              size="small"
              icon={<ArrowUpOutlined />}
              disabled={index === 0}
              onClick={() => handleMoveOrder(record.id, 'up')}
            >
              위로
            </Button>
            <Button
              size="small"
              icon={<ArrowDownOutlined />}
              disabled={index === currentList.length - 1}
              onClick={() => handleMoveOrder(record.id, 'down')}
            >
              아래로
            </Button>
            <Popconfirm
              title="전시 영역에서 제거하시겠습니까?"
              onConfirm={() => handleRemoveFromDisplayArea(record.id)}
            >
              <Button
                size="small"
                danger
                icon={<DeleteOutlined />}
              >
                제거
              </Button>
            </Popconfirm>
          </Space>
        )
      },
    },
  ]

  const displayAreaLabels: Record<DisplayArea, string> = {
    MAIN: '메인',
    RECOMMEND: '추천',
    EVENT: '이벤트'
  }

  return (
    <div className="admin-display-product-manage">
      <div className="display-product-container">
        <h2>전시 상품 관리</h2>
        
        {/* 전시 영역 탭 */}
        <Card style={{ marginBottom: '1.5rem' }}>
          <Tabs
            activeKey={selectedDisplayArea}
            onChange={(key) => setSelectedDisplayArea(key as DisplayArea)}
            items={[
              { key: 'MAIN', label: '메인 (MAIN)' },
              { key: 'RECOMMEND', label: '추천 (RECOMMEND)' },
              { key: 'EVENT', label: '이벤트 (EVENT)' }
            ]}
          />
        </Card>

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

        {/* 전시 영역 상품 리스트 및 상품 목록 */}
        <div className="display-area-content">
          {/* 전시 영역 상품 리스트 */}
          <div className="display-product-list">
            <Card 
              title={`${displayAreaLabels[selectedDisplayArea]} 전시 상품`}
              className="display-list-card"
            >
              {displayProducts[selectedDisplayArea].length === 0 ? (
                <div className="empty-display-list">
                  <p>전시 상품이 없습니다.</p>
                  <p>오른쪽 목록에서 상품을 추가하세요.</p>
                </div>
              ) : (
                <Table
                  columns={displayProductColumns}
                  dataSource={displayProducts[selectedDisplayArea]}
                  rowKey="id"
                  pagination={false}
                  size="small"
                />
              )}
            </Card>
          </div>

          {/* 상품 목록 */}
          <div className="category-product-list">
            <Card title="상품 목록">
              <Table
                columns={productColumns}
                dataSource={filteredProducts}
                rowKey="id"
                pagination={{
                  pageSize: 10,
                  showSizeChanger: true,
                  showTotal: (total) => `총 ${total}개`,
                }}
                size="small"
              />
            </Card>
          </div>
        </div>
      </div>
    </div>
  )
}

export default AdminDisplayProductManage


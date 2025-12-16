import { useState } from 'react'
import { Table, Input, Select, Button, Space } from 'antd'
import type { ColumnsType, TableProps } from 'antd/es/table'
import { useNavigate } from 'react-router-dom'
import './AdminProductList.css'

const { Option } = Select

interface Product {
  id: string
  product_name: string
  product_code: string
  category: string
  base_price: number
  updated_at: string
}

function AdminProductList() {
  const navigate = useNavigate()
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [searchName, setSearchName] = useState('')
  const [searchCategory, setSearchCategory] = useState<string | undefined>(undefined)

  // 샘플 데이터 (나중에 API로 대체)
  const [products] = useState<Product[]>([
    {
      id: '1',
      product_name: '노트북',
      product_code: 'PRD-001',
      category: 'electronics',
      base_price: 1200000,
      updated_at: '2024-01-15 10:30:00'
    },
    {
      id: '2',
      product_name: '티셔츠',
      product_code: 'PRD-002',
      category: 'clothing',
      base_price: 30000,
      updated_at: '2024-01-14 15:20:00'
    },
    {
      id: '3',
      product_name: '책',
      product_code: 'PRD-003',
      category: 'books',
      base_price: 15000,
      updated_at: '2024-01-13 09:10:00'
    }
  ])

  const categoryMap: Record<string, string> = {
    electronics: '전자제품',
    clothing: '의류',
    food: '식품',
    books: '도서',
    sports: '스포츠',
    beauty: '뷰티'
  }

  const columns: ColumnsType<Product> = [
    {
      title: '카테고리',
      dataIndex: 'category',
      key: 'category',
      render: (category: string) => categoryMap[category] || category,
      filters: [
        { text: '전자제품', value: 'electronics' },
        { text: '의류', value: 'clothing' },
        { text: '식품', value: 'food' },
        { text: '도서', value: 'books' },
        { text: '스포츠', value: 'sports' },
        { text: '뷰티', value: 'beauty' },
      ],
      onFilter: (value, record) => record.category === value,
      sorter: (a, b) => (categoryMap[a.category] || a.category).localeCompare(categoryMap[b.category] || b.category),
    },
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
      title: '수정일',
      dataIndex: 'updated_at',
      key: 'updated_at',
      sorter: (a, b) => new Date(a.updated_at).getTime() - new Date(b.updated_at).getTime(),
      render: (date: string) => {
        const dateObj = new Date(date)
        return dateObj.toLocaleString('ko-KR', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit'
        })
      },
    },
  ]

  const rowSelection: TableProps<Product>['rowSelection'] = {
    selectedRowKeys,
    onChange: (selectedKeys) => {
      setSelectedRowKeys(selectedKeys)
    },
  }

  const handleDelete = () => {
    if (selectedRowKeys.length === 0) {
      return
    }
    // TODO: API 호출로 선택된 상품 삭제
    console.log('삭제할 상품 IDs:', selectedRowKeys)
    setSelectedRowKeys([])
  }

  const handleRegister = () => {
    navigate('/admin/product/register')
  }

  // 필터링된 데이터
  const filteredProducts = products.filter((product) => {
    const nameMatch = !searchName || product.product_name.toLowerCase().includes(searchName.toLowerCase())
    const categoryMatch = !searchCategory || product.category === searchCategory
    return nameMatch && categoryMatch
  })

  const handleSearch = () => {
    // 검색 버튼 클릭 시 필터 적용 (현재는 실시간 필터링이므로 별도 로직 불필요)
    // 필요시 추가 로직 구현 가능
  }

  const handleReset = () => {
    setSearchName('')
    setSearchCategory(undefined)
  }

  return (
    <div className="admin-product-list">
      <div className="admin-product-list-container">
        <div className="product-list-header">
          <h2>상품 목록</h2>
        </div>

        <div className="product-list-filters">
          <div className="filter-inputs">
            <Space size="middle">
              <Input
                placeholder="상품명 검색"
                allowClear
                style={{ width: 200 }}
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

        <div className="product-list-actions">
          <Space>
            <Button 
              type="primary" 
              onClick={handleRegister}
              style={{ backgroundColor: '#FFC107', borderColor: '#FFC107', color: '#343A40', fontWeight: 600 }}
            >
              등록
            </Button>
            <Button 
              danger 
              onClick={handleDelete}
              disabled={selectedRowKeys.length === 0}
            >
              삭제
            </Button>
          </Space>
        </div>

        <Table
          rowSelection={rowSelection}
          columns={columns}
          dataSource={filteredProducts}
          rowKey="id"
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total) => `총 ${total}개`,
          }}
        />
      </div>
    </div>
  )
}

export default AdminProductList


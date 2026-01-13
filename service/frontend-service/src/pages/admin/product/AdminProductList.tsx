import { useState, useEffect } from 'react'
import { Table, Input, Select, Button, Space, message } from 'antd'
import type { ColumnsType, TableProps } from 'antd/es/table'
import { useNavigate } from 'react-router-dom'
import { searchProducts, type ProductResponse } from '../../../api/productApi'
import './AdminProductList.css'

const { Option } = Select

interface Product {
  id: number
  product_name: string
  product_code: string
  category: string
  base_price: number
  updated_at: string
  status: string
}

function AdminProductList() {
  const navigate = useNavigate()
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [searchName, setSearchName] = useState('')
  const [searchCategory, setSearchCategory] = useState<string | undefined>(undefined)
  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading] = useState(false)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })

  // API에서 상품 목록 조회
  const fetchProducts = async (page: number = 0, size: number = 10) => {
    setLoading(true)
    try {
      const response = await searchProducts({
        productName: searchName || undefined,
        page,
        size,
        sort: 'updatedAt,desc',
      })

      // API 응답을 컴포넌트 인터페이스에 맞게 변환
      const mappedProducts: Product[] = response.content.map((item: ProductResponse) => ({
        id: item.productId,
        product_name: item.productName,
        product_code: item.productCode || '',
        category: 'electronics', // TODO: 카테고리 매핑 필요
        base_price: item.basePrice,
        updated_at: item.updatedAt,
        status: item.status,
      }))

      setProducts(mappedProducts)
      setPagination({
        current: page + 1, // API는 0부터 시작, UI는 1부터 시작
        pageSize: size,
        total: response.totalElements,
      })
    } catch (error) {
      console.error('Failed to fetch products:', error)
      message.error(error instanceof Error ? error.message : '상품 목록 조회에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  // 컴포넌트 마운트 시 상품 목록 조회
  useEffect(() => {
    fetchProducts()
  }, [])

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
      render: (text: string, record: Product) => (
        <a
          onClick={() => navigate(`/admin/product/edit/${record.id}`)}
          style={{ cursor: 'pointer', color: '#007BFF' }}
        >
          {text}
        </a>
      ),
    },
    {
      title: '상품 코드',
      dataIndex: 'product_code',
      key: 'product_code',
      sorter: (a, b) => a.product_code.localeCompare(b.product_code),
      render: (text: string, record: Product) => (
        <a
          onClick={() => navigate(`/admin/product/edit/${record.id}`)}
          style={{ cursor: 'pointer', color: '#007BFF' }}
        >
          {text}
        </a>
      ),
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

  const handleSearch = () => {
    // 검색 버튼 클릭 시 첫 페이지부터 다시 조회
    fetchProducts(0, pagination.pageSize)
  }

  const handleReset = () => {
    setSearchName('')
    setSearchCategory(undefined)
    // 초기화 후 다시 조회
    fetchProducts(0, pagination.pageSize)
  }

  const handleTableChange = (newPagination: any) => {
    // 페이지 변경 시 API 재호출
    fetchProducts(newPagination.current - 1, newPagination.pageSize)
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
          dataSource={products}
          rowKey="id"
          loading={loading}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showTotal: (total) => `총 ${total}개`,
          }}
          onChange={handleTableChange}
        />
      </div>
    </div>
  )
}

export default AdminProductList


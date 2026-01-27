import { useState, useEffect } from 'react'
import { Table, Card, message, Space, Input, Button, Select, Tag, Spin } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PlusOutlined, LoadingOutlined } from '@ant-design/icons'
import './AdminSearchKeywordManage.css'
import {
  searchProducts,
  getProductSearchKeywords,
  addProductSearchKeyword,
  deleteProductSearchKeyword,
  type ProductResponse,
  type SearchKeywordResponse,
} from '../../../api/productApi'

const { Option } = Select

interface ProductWithKeywords extends ProductResponse {
  keywords: SearchKeywordResponse[]
  keywordsLoading: boolean
}

function AdminSearchKeywordManage() {
  const [products, setProducts] = useState<ProductWithKeywords[]>([])
  const [loading, setLoading] = useState(false)
  const [searchName, setSearchName] = useState('')
  const [searchStatus, setSearchStatus] = useState<'ACTIVE' | 'INACTIVE' | 'SOLD_OUT' | undefined>(undefined)
  const [newKeyword, setNewKeyword] = useState<Record<number, string>>({})
  const [addingKeyword, setAddingKeyword] = useState<Record<number, boolean>>({})
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 })

  // 상품 데이터 로드
  const loadProducts = async (page = 0, size = 10) => {
    setLoading(true)
    try {
      const response = await searchProducts({
        productName: searchName || undefined,
        status: searchStatus,
        page,
        size,
      })

      // 상품별로 키워드를 별도로 로드할 준비
      const productsWithKeywords: ProductWithKeywords[] = response.content.map((product) => ({
        ...product,
        keywords: [],
        keywordsLoading: true,
      }))

      setProducts(productsWithKeywords)
      setPagination({
        current: response.page + 1,
        pageSize: response.size,
        total: response.totalElements,
      })

      // 각 상품의 키워드를 비동기로 로드
      productsWithKeywords.forEach((product) => {
        loadKeywordsForProduct(product.productId)
      })
    } catch (error) {
      message.error(error instanceof Error ? error.message : '상품 목록 조회에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  // 특정 상품의 키워드 로드
  const loadKeywordsForProduct = async (productId: number) => {
    try {
      const keywords = await getProductSearchKeywords(productId)
      setProducts((prev) =>
        prev.map((p) =>
          p.productId === productId
            ? { ...p, keywords, keywordsLoading: false }
            : p
        )
      )
    } catch {
      setProducts((prev) =>
        prev.map((p) =>
          p.productId === productId
            ? { ...p, keywords: [], keywordsLoading: false }
            : p
        )
      )
    }
  }

  useEffect(() => {
    loadProducts()
  }, [])

  const handleSearch = () => {
    loadProducts(0, pagination.pageSize)
  }

  const handleReset = () => {
    setSearchName('')
    setSearchStatus(undefined)
  }

  // 키워드 추가
  const handleAddKeyword = async (productId: number) => {
    const keyword = newKeyword[productId]?.trim()

    if (!keyword) {
      message.warning('키워드를 입력하세요.')
      return
    }

    if (keyword.length > 100) {
      message.error('키워드는 최대 100자까지 입력 가능합니다.')
      return
    }

    // 프론트엔드 중복 체크
    const product = products.find((p) => p.productId === productId)
    if (product?.keywords.some((k) => k.keyword.toLowerCase() === keyword.toLowerCase())) {
      message.warning('이미 등록된 키워드입니다.')
      return
    }

    setAddingKeyword((prev) => ({ ...prev, [productId]: true }))

    try {
      const newKw = await addProductSearchKeyword(productId, { keyword })

      setProducts((prev) =>
        prev.map((p) =>
          p.productId === productId
            ? { ...p, keywords: [...p.keywords, newKw] }
            : p
        )
      )

      setNewKeyword((prev) => ({ ...prev, [productId]: '' }))
      message.success('키워드가 추가되었습니다.')
    } catch (error) {
      message.error(error instanceof Error ? error.message : '키워드 추가에 실패했습니다.')
    } finally {
      setAddingKeyword((prev) => ({ ...prev, [productId]: false }))
    }
  }

  // 키워드 삭제
  const handleDeleteKeyword = async (productId: number, keywordId: number) => {
    try {
      await deleteProductSearchKeyword(productId, keywordId)

      setProducts((prev) =>
        prev.map((p) =>
          p.productId === productId
            ? { ...p, keywords: p.keywords.filter((k) => k.keywordId !== keywordId) }
            : p
        )
      )

      message.success('키워드가 삭제되었습니다.')
    } catch (error) {
      message.error(error instanceof Error ? error.message : '키워드 삭제에 실패했습니다.')
    }
  }

  // 테이블 페이지 변경
  const handleTableChange = (newPagination: { current?: number; pageSize?: number }) => {
    loadProducts((newPagination.current || 1) - 1, newPagination.pageSize || 10)
  }

  // 테이블 컬럼 정의
  const columns: ColumnsType<ProductWithKeywords> = [
    {
      title: '상품명',
      dataIndex: 'productName',
      key: 'productName',
      width: 200,
    },
    {
      title: '상품 코드',
      dataIndex: 'productCode',
      key: 'productCode',
      width: 150,
      render: (code: string | undefined) => code || '-',
    },
    {
      title: '상태',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => {
        const statusMap: Record<string, { color: string; text: string }> = {
          ACTIVE: { color: 'green', text: '판매중' },
          INACTIVE: { color: 'default', text: '비활성' },
          SOLD_OUT: { color: 'red', text: '품절' },
        }
        const info = statusMap[status] || { color: 'default', text: status }
        return <Tag color={info.color}>{info.text}</Tag>
      },
    },
    {
      title: '검색 키워드',
      key: 'keywords',
      render: (_, record: ProductWithKeywords) => {
        const keywordInput = newKeyword[record.productId] || ''
        const isAdding = addingKeyword[record.productId] || false

        if (record.keywordsLoading) {
          return <Spin indicator={<LoadingOutlined spin />} size="small" />
        }

        return (
          <div className="keyword-cell">
            <div className="keyword-tags">
              {record.keywords.length > 0 ? (
                record.keywords.map((kw) => (
                  <Tag
                    key={kw.keywordId}
                    closable
                    onClose={() => handleDeleteKeyword(record.productId, kw.keywordId)}
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
                onChange={(e) =>
                  setNewKeyword((prev) => ({
                    ...prev,
                    [record.productId]: e.target.value,
                  }))
                }
                onPressEnter={() => handleAddKeyword(record.productId)}
                style={{ width: '200px', marginRight: '8px' }}
                disabled={isAdding}
              />
              <Button
                type="primary"
                size="small"
                icon={<PlusOutlined />}
                onClick={() => handleAddKeyword(record.productId)}
                loading={isAdding}
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
                placeholder="상품명 검색"
                allowClear
                style={{ width: 250 }}
                value={searchName}
                onChange={(e) => setSearchName(e.target.value)}
                onPressEnter={handleSearch}
              />
              <Select
                placeholder="상태 선택"
                allowClear
                style={{ width: 150 }}
                value={searchStatus}
                onChange={(value) => setSearchStatus(value)}
              >
                <Option value="ACTIVE">판매중</Option>
                <Option value="INACTIVE">비활성</Option>
                <Option value="SOLD_OUT">품절</Option>
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
            dataSource={products}
            rowKey="productId"
            loading={loading}
            pagination={{
              current: pagination.current,
              pageSize: pagination.pageSize,
              total: pagination.total,
              showSizeChanger: true,
              showTotal: (total) => `총 ${total}개`,
            }}
            onChange={handleTableChange}
          />
        </Card>
      </div>
    </div>
  )
}

export default AdminSearchKeywordManage


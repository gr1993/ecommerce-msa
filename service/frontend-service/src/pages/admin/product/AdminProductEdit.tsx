import { useState, useEffect } from 'react'
import { message, Spin } from 'antd'
import { useNavigate, useParams } from 'react-router-dom'
import ProductForm from './ProductForm'
import type { OptionGroup, SKU, ProductImage } from './ProductForm'
import './AdminProductRegister.css'

function AdminProductEdit() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const [loading, setLoading] = useState(false)
  const [initialLoading, setInitialLoading] = useState(true)
  const [initialData, setInitialData] = useState<any>(null)

  // 상품 데이터 로드
  useEffect(() => {
    const loadProduct = async () => {
      if (!id) {
        message.error('상품 ID가 없습니다.')
        navigate('/admin/product/list')
        return
      }

      try {
        setInitialLoading(true)
        // TODO: API 호출로 상품 데이터 로드
        // const product = await productApi.getProduct(id)
        
        // 샘플 데이터 (실제로는 API에서 가져옴)
        const sampleData = {
          product_name: '노트북',
          product_code: 'PRD-001',
          description: '고성능 노트북입니다.',
          base_price: 1200000,
          sale_price: 1000000,
          status: 'ACTIVE',
          is_displayed: true,
          optionGroups: [
            {
              id: 'group_1',
              optionGroupName: '색상',
              displayOrder: 0,
              optionValues: [
                { id: 'value_1', optionValueName: 'Black', displayOrder: 0 },
                { id: 'value_2', optionValueName: 'White', displayOrder: 1 }
              ]
            },
            {
              id: 'group_2',
              optionGroupName: '용량',
              displayOrder: 1,
              optionValues: [
                { id: 'value_3', optionValueName: '256GB', displayOrder: 0 },
                { id: 'value_4', optionValueName: '512GB', displayOrder: 1 }
              ]
            }
          ] as OptionGroup[],
          skus: [
            {
              id: 'sku_1',
              skuCode: 'SKU-001',
              price: 1200000,
              stockQty: 10,
              status: 'ACTIVE',
              optionValueIds: ['value_1', 'value_3']
            }
          ] as SKU[],
          images: [] as ProductImage[]
        }

        setInitialData(sampleData)
      } catch (error) {
        message.error('상품 정보를 불러오는데 실패했습니다.')
        console.error(error)
        navigate('/admin/product/list')
      } finally {
        setInitialLoading(false)
      }
    }

    loadProduct()
  }, [id, navigate])

  const handleSubmit = async (formData: any) => {
    if (!id) return

    setLoading(true)
    try {
      console.log('상품 수정:', { id, ...formData })
      // TODO: API 호출로 상품 수정 처리
      // await productApi.updateProduct(id, formData)
      message.success('상품이 수정되었습니다.')
      navigate('/admin/product/list')
    } catch (error) {
      message.error('상품 수정에 실패했습니다.')
      console.error(error)
    } finally {
      setLoading(false)
    }
  }

  const handleCancel = () => {
    navigate('/admin/product/list')
  }

  if (initialLoading) {
    return (
      <div className="admin-product-register">
        <div className="admin-product-register-container">
          <Spin size="large" tip="상품 정보를 불러오는 중..." />
        </div>
      </div>
    )
  }

  return (
    <div className="admin-product-register">
      <div className="admin-product-register-container">
        <h2>상품 수정</h2>
        <ProductForm
          mode="edit"
          initialData={initialData}
          onSubmit={handleSubmit}
          onCancel={handleCancel}
          loading={loading}
        />
      </div>
    </div>
  )
}

export default AdminProductEdit


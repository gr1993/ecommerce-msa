import { useState } from 'react'
import { message } from 'antd'
import { useNavigate } from 'react-router-dom'
import ProductForm from './ProductForm'
import './AdminProductRegister.css'

function AdminProductRegister() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (formData: any) => {
    setLoading(true)
    try {
      console.log('상품 등록:', formData)
      // TODO: API 호출로 상품 등록 처리
      // await productApi.createProduct(formData)
      message.success('상품이 등록되었습니다.')
      navigate('/admin/product/list')
    } catch (error) {
      message.error('상품 등록에 실패했습니다.')
      console.error(error)
    } finally {
      setLoading(false)
    }
  }

  const handleCancel = () => {
    navigate('/admin/product/list')
  }

  return (
    <div className="admin-product-register">
      <div className="admin-product-register-container">
        <h2>상품 등록</h2>
        <ProductForm
          mode="create"
          onSubmit={handleSubmit}
          onCancel={handleCancel}
          loading={loading}
        />
      </div>
    </div>
  )
}

export default AdminProductRegister

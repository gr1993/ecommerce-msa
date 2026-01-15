import { useState } from 'react'
import { message } from 'antd'
import { useNavigate } from 'react-router-dom'
import ProductForm from './ProductForm'
import { createProduct, uploadProductImage, type ProductCreateRequest } from '../../../api/productApi'
import './AdminProductRegister.css'

function AdminProductRegister() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (formData: any) => {
    setLoading(true)
    try {
      console.log('상품 등록:', formData)

      // 1. 이미지 업로드 처리
      const uploadedImages = []
      if (formData.images && formData.images.length > 0) {
        for (const image of formData.images) {
          try {
            const uploadResult = await uploadProductImage(image.file)
            uploadedImages.push({
              id: image.id,
              fileId: uploadResult.fileId,
              imageUrl: uploadResult.url,
              isPrimary: image.isPrimary,
              displayOrder: image.displayOrder,
            })
          } catch (error) {
            console.error('이미지 업로드 실패:', error)
            throw new Error(`이미지 업로드 실패: ${image.file.name}`)
          }
        }
      }

      // 2. API 요청 데이터 포맷 변환
      const productRequest: ProductCreateRequest = {
        productName: formData.product_name,
        productCode: formData.product_code,
        description: formData.description,
        basePrice: formData.base_price,
        salePrice: formData.sale_price,
        status: formData.status,
        isDisplayed: formData.is_displayed,
        optionGroups: formData.optionGroups?.map((group: any) => ({
          optionGroupName: group.optionGroupName,
          displayOrder: group.displayOrder,
          optionValues: group.optionValues.map((value: any) => ({
            id: value.id, // SKU 옵션값 매핑용 프론트 임시 ID
            optionValueName: value.optionValueName,
            displayOrder: value.displayOrder,
          })),
        })),
        skus: formData.skus?.map((sku: any) => ({
          skuCode: sku.skuCode,
          price: sku.price,
          stockQty: sku.stockQty,
          status: sku.status,
          optionValueIds: sku.optionValueIds,
        })),
        images: uploadedImages.map((img: any) => ({
          fileId: img.fileId,
          isPrimary: img.isPrimary,
          displayOrder: img.displayOrder,
        })),
      }

      // 3. 상품 등록 API 호출
      const result = await createProduct(productRequest)
      console.log('상품 등록 성공:', result)

      message.success('상품이 등록되었습니다.')
      navigate('/admin/product/list')
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '상품 등록에 실패했습니다.'
      message.error(errorMessage)
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

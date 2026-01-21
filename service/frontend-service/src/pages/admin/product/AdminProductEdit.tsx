import { useState, useEffect } from 'react'
import { message, Spin } from 'antd'
import { useNavigate, useParams } from 'react-router-dom'
import ProductForm from './ProductForm'
import type { OptionGroup, SKU, ProductImage, SelectedCategory } from './ProductForm'
import { getProductDetail, updateProduct, uploadProductImage, type ProductCreateRequest } from '../../../api/productApi'
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
        const product = await getProductDetail(Number(id))

        // API 응답을 ProductForm initialData 형식으로 변환
        const formData = {
          product_name: product.productName,
          product_code: product.productCode,
          description: product.description,
          base_price: product.basePrice,
          sale_price: product.salePrice,
          status: product.status,
          is_displayed: product.isDisplayed,
          categories: product.categories?.map(cat => ({
            categoryId: cat.categoryId,
            categoryName: cat.categoryName,
            fullPath: cat.categoryName // 서버에서 fullPath를 제공하지 않으면 categoryName만 표시
          })) as SelectedCategory[] || [],
          optionGroups: product.optionGroups.map(group => ({
            id: String(group.id),
            optionGroupName: group.optionGroupName,
            displayOrder: group.displayOrder,
            optionValues: group.optionValues.map(value => ({
              id: String(value.id),
              optionValueName: value.optionValueName,
              displayOrder: value.displayOrder
            }))
          })) as OptionGroup[],
          skus: product.skus.map(sku => ({
            id: String(sku.id),
            skuCode: sku.skuCode,
            price: sku.price,
            stockQty: sku.stockQty,
            status: sku.status,
            optionValueIds: sku.optionValueIds.map(String)
          })) as SKU[],
          images: product.images.map(img => ({
            id: String(img.id),
            fileId: img.fileId, // 기존 이미지의 fileId 보존
            url: `http://localhost:8080/product${img.imageUrl}`, // 정적 리소스 경로
            isPrimary: img.isPrimary,
            displayOrder: img.displayOrder
          })) as ProductImage[]
        }

        setInitialData(formData)
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : '상품 정보를 불러오는데 실패했습니다.'
        message.error(errorMessage)
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
      // 1. 이미지 처리: 새 이미지는 업로드, 기존 이미지는 fileId 유지
      const processedImages = []
      if (formData.images && formData.images.length > 0) {
        for (const image of formData.images as ProductImage[]) {
          if (image.fileId) {
            // 기존 이미지: fileId 유지
            processedImages.push({
              fileId: image.fileId,
              isPrimary: image.isPrimary,
              displayOrder: image.displayOrder,
            })
          } else if (image.file && image.file instanceof File) {
            // 새 이미지: 임시 업로드 후 fileId 획득
            try {
              const uploadResult = await uploadProductImage(image.file)
              processedImages.push({
                fileId: uploadResult.fileId,
                isPrimary: image.isPrimary,
                displayOrder: image.displayOrder,
              })
            } catch (error) {
              console.error('이미지 업로드 실패:', error)
              throw new Error(`이미지 업로드 실패: ${image.file.name}`)
            }
          }
        }
      }

      // 2. ProductForm 데이터를 API 요청 형식으로 변환
      const requestData: ProductCreateRequest = {
        productName: formData.product_name,
        productCode: formData.product_code,
        description: formData.description,
        basePrice: formData.base_price,
        salePrice: formData.sale_price,
        status: formData.status,
        isDisplayed: formData.is_displayed,
        categoryIds: formData.categories?.map((cat: SelectedCategory) => cat.categoryId),
        optionGroups: formData.optionGroups?.map((group: OptionGroup) => ({
          optionGroupName: group.optionGroupName,
          displayOrder: group.displayOrder,
          optionValues: group.optionValues.map(value => ({
            id: value.id, // SKU 옵션값 매핑용 프론트 임시 ID
            optionValueName: value.optionValueName,
            displayOrder: value.displayOrder
          }))
        })),
        skus: formData.skus?.map((sku: SKU) => ({
          skuCode: sku.skuCode,
          price: sku.price,
          stockQty: sku.stockQty,
          status: sku.status,
          optionValueIds: sku.optionValueIds
        })),
        images: processedImages,
      }

      await updateProduct(Number(id), requestData)
      message.success('상품이 수정되었습니다.')
      navigate('/admin/product/list')
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '상품 수정에 실패했습니다.'
      message.error(errorMessage)
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


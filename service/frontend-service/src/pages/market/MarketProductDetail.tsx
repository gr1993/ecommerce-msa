import { useState, useEffect, useMemo } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Button, Card, InputNumber, Space, Divider, Tag, Image, message } from 'antd'
import { ShoppingCartOutlined, ArrowLeftOutlined, MinusOutlined, PlusOutlined } from '@ant-design/icons'
import MarketHeader from '../../components/market/MarketHeader'
import MarketFooter from '../../components/market/MarketFooter'
import { useCartStore } from '../../stores/cartStore'
import type { CartItem } from '../../stores/cartStore'
import { getProductDetail } from '../../api/catalogApi'
import type { ProductDetailResponse, SkuResponse, OptionGroupResponse } from '../../api/catalogApi'
import { PRODUCT_FILE_URL } from '../../config/env'
import './MarketProductDetail.css'

// 이미지 URL 빌드 헬퍼
const buildImageUrl = (imageUrl?: string): string => {
  if (!imageUrl) return 'https://via.placeholder.com/600x600?text=No+Image'
  return `${PRODUCT_FILE_URL}${imageUrl}`
}

function MarketProductDetail() {
  const { productId } = useParams<{ productId: string }>()
  const navigate = useNavigate()
  const addToCart = useCartStore((state) => state.addToCart)

  const [product, setProduct] = useState<ProductDetailResponse | null>(null)
  const [quantity, setQuantity] = useState<number>(1)
  const [selectedImageIndex, setSelectedImageIndex] = useState<number>(0)
  const [loading, setLoading] = useState<boolean>(true)
  // 선택된 옵션값 ID를 옵션그룹별로 관리: { optionGroupId: optionValueId }
  const [selectedOptions, setSelectedOptions] = useState<Record<number, number>>({})

  // 옵션 그룹을 displayOrder 순으로 정렬
  const sortedOptionGroups = useMemo(() => {
    if (!product?.optionGroups) return []
    return [...product.optionGroups].sort((a, b) => a.displayOrder - b.displayOrder)
  }, [product?.optionGroups])

  // 현재 선택된 옵션에 매칭되는 SKU 찾기
  const selectedSku = useMemo((): SkuResponse | null => {
    if (!product?.skus || !product?.optionGroups) return null
    if (product.optionGroups.length === 0) {
      // 옵션이 없으면 첫 번째 SKU 반환
      return product.skus[0] || null
    }
    // 모든 옵션그룹에 대해 선택이 완료되었는지 확인
    const allSelected = sortedOptionGroups.every((g) => selectedOptions[g.id] !== undefined)
    if (!allSelected) return null

    const selectedValueIds = Object.values(selectedOptions)
    return product.skus.find((sku) => {
      if (sku.optionValueIds.length !== selectedValueIds.length) return false
      return selectedValueIds.every((vid) => sku.optionValueIds.includes(vid))
    }) || null
  }, [product?.skus, product?.optionGroups, selectedOptions, sortedOptionGroups])

  // 특정 옵션그룹의 옵션값이 선택 가능한지 확인 (재고 있는 SKU가 존재하는지)
  const isOptionValueAvailable = (groupId: number, valueId: number): boolean => {
    if (!product?.skus) return false
    // 현재 선택된 옵션 + 이 값으로 조합했을 때 유효한 SKU가 있는지 확인
    const testOptions = { ...selectedOptions, [groupId]: valueId }
    const selectedValueIds = Object.values(testOptions)
    return product.skus.some((sku) => {
      return selectedValueIds.every((vid) => sku.optionValueIds.includes(vid)) &&
        sku.status === 'ACTIVE' && sku.stockQty > 0
    })
  }

  // 표시할 가격 (SKU 선택 시 SKU 가격, 아니면 base/sale price)
  const displayPrice = useMemo(() => {
    if (selectedSku) return selectedSku.price
    if (product?.salePrice) return product.salePrice
    return product?.basePrice ?? 0
  }, [selectedSku, product])

  // 표시할 재고
  const displayStock = useMemo(() => {
    if (selectedSku) return selectedSku.stockQty
    if (!product?.skus || product.skus.length === 0) return 0
    // SKU 선택 전이면 전체 재고 합산
    return product.skus.reduce((sum, s) => sum + s.stockQty, 0)
  }, [selectedSku, product?.skus])

  // 이미지 목록 (displayOrder 순)
  const sortedImages = useMemo(() => {
    if (!product?.images) return []
    return [...product.images].sort((a, b) => a.displayOrder - b.displayOrder)
  }, [product?.images])

  const hasOptions = (product?.optionGroups?.length ?? 0) > 0

  // 상품 데이터 로드
  useEffect(() => {
    const loadProduct = async () => {
      setLoading(true)
      try {
        const data = await getProductDetail(Number(productId))
        setProduct(data)
        setSelectedOptions({})
        setQuantity(1)
        setSelectedImageIndex(0)
      } catch (error) {
        console.error('상품 상세 조회 실패:', error)
        message.error('상품 정보를 불러오는데 실패했습니다.')
      } finally {
        setLoading(false)
      }
    }

    if (productId) {
      loadProduct()
    }
  }, [productId])

  const handleOptionSelect = (groupId: number, valueId: number) => {
    setSelectedOptions((prev) => {
      const next = { ...prev }
      if (next[groupId] === valueId) {
        delete next[groupId]
      } else {
        next[groupId] = valueId
      }
      return next
    })
    setQuantity(1)
  }

  const handleQuantityChange = (value: number | null) => {
    const maxStock = selectedSku?.stockQty ?? displayStock
    if (value && value > 0 && value <= maxStock) {
      setQuantity(value)
    }
  }

  const canAddToCart = (): boolean => {
    if (!product) return false
    if (hasOptions && !selectedSku) return false
    if (selectedSku && (selectedSku.stockQty === 0 || selectedSku.status !== 'ACTIVE')) return false
    if (!hasOptions && displayStock === 0) return false
    return true
  }

  const getOptionLabel = (): string => {
    if (!hasOptions || !selectedSku) return ''
    return sortedOptionGroups
      .map((g) => {
        const val = g.optionValues.find((v) => v.id === selectedOptions[g.id])
        return val ? val.optionValueName : ''
      })
      .filter(Boolean)
      .join(' / ')
  }

  const handleAddToCart = () => {
    if (!product || !canAddToCart()) return

    const sku = selectedSku
    const price = sku ? sku.price : displayPrice
    const stock = sku ? sku.stockQty : displayStock
    const optionLabel = getOptionLabel()

    addToCart({
      product_id: String(product.productId),
      sku_id: sku ? String(sku.id) : '',
      product_name: optionLabel ? `${product.productName} (${optionLabel})` : product.productName,
      product_code: sku ? sku.skuCode : product.productCode,
      base_price: price,
      image_url: buildImageUrl(sortedImages[0]?.imageUrl),
      stock: stock
    }, quantity)
  }

  const handleBuyNow = () => {
    if (!product || !canAddToCart()) return

    const sku = selectedSku
    const price = sku ? sku.price : displayPrice
    const stock = sku ? sku.stockQty : displayStock
    const optionLabel = getOptionLabel()

    const orderItem: CartItem = {
      product_id: String(product.productId),
      sku_id: sku ? String(sku.id) : '',
      product_name: optionLabel ? `${product.productName} (${optionLabel})` : product.productName,
      product_code: sku ? sku.skuCode : product.productCode,
      base_price: price,
      image_url: buildImageUrl(sortedImages[0]?.imageUrl),
      quantity: quantity,
      stock: stock
    }

    navigate('/market/order', {
      state: {
        item: orderItem,
        fromCart: false
      }
    })
  }

  if (loading) {
    return (
      <div className="market-product-detail">
        <MarketHeader />
        <div className="loading-container">
          <p>로딩 중...</p>
        </div>
        <MarketFooter />
      </div>
    )
  }

  if (!product) {
    return (
      <div className="market-product-detail">
        <MarketHeader />
        <div className="error-container">
          <p>상품을 찾을 수 없습니다.</p>
          <Button onClick={() => navigate('/market/products')}>상품 목록으로 돌아가기</Button>
        </div>
        <MarketFooter />
      </div>
    )
  }

  const totalPrice = displayPrice * quantity

  return (
    <div className="market-product-detail">
      <MarketHeader />

      <div className="product-detail-container">
        <Button
          type="text"
          icon={<ArrowLeftOutlined />}
          onClick={() => navigate('/market/products')}
          className="back-button"
        >
          상품 목록으로 돌아가기
        </Button>

        <div className="product-detail-content">
          {/* 상품 이미지 영역 */}
          <div className="product-images-section">
            <div className="main-image">
              <Image
                src={buildImageUrl(sortedImages[selectedImageIndex]?.imageUrl)}
                alt={product.productName}
                preview={false}
                className="main-product-image"
              />
            </div>
            {sortedImages.length > 1 && (
              <div className="thumbnail-images">
                {sortedImages.map((img, index) => (
                  <div
                    key={img.id}
                    className={`thumbnail-item ${selectedImageIndex === index ? 'active' : ''}`}
                    onClick={() => setSelectedImageIndex(index)}
                  >
                    <img src={buildImageUrl(img.imageUrl)} alt={`${product.productName} ${index + 1}`} />
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* 상품 정보 영역 */}
          <div className="product-info-section">
            <div className="product-header">
              {product.categories.map((cat) => (
                <Tag color="blue" key={cat.categoryId}>{cat.categoryName}</Tag>
              ))}
              <h1 className="product-title">{product.productName}</h1>
              <p className="product-code">상품 코드: {product.productCode}</p>
            </div>

            <Divider />

            <div className="product-price-section">
              <div className="price-info">
                <span className="price-label">판매가격</span>
                {product.salePrice && product.salePrice < product.basePrice ? (
                  <>
                    <span className="product-price-original">{product.basePrice.toLocaleString()}원</span>
                    <span className="product-price">{displayPrice.toLocaleString()}원</span>
                  </>
                ) : (
                  <span className="product-price">{displayPrice.toLocaleString()}원</span>
                )}
              </div>
            </div>

            <Divider />

            {/* 옵션 선택 영역 */}
            {hasOptions && (
              <>
                <div className="product-options">
                  {sortedOptionGroups.map((group) => (
                    <OptionGroupSelector
                      key={group.id}
                      group={group}
                      selectedValueId={selectedOptions[group.id]}
                      onSelect={(valueId) => handleOptionSelect(group.id, valueId)}
                      isValueAvailable={(valueId) => isOptionValueAvailable(group.id, valueId)}
                    />
                  ))}
                </div>
                <Divider />
              </>
            )}

            {/* 선택된 SKU 정보 */}
            {selectedSku && (
              <>
                <div className="selected-sku-info">
                  <div className="sku-detail">
                    <span className="sku-label">선택 옵션</span>
                    <span className="sku-value">{getOptionLabel()}</span>
                  </div>
                  <div className="sku-detail">
                    <span className="sku-label">SKU</span>
                    <span className="sku-value">{selectedSku.skuCode}</span>
                  </div>
                  <div className="sku-detail">
                    <span className="sku-label">재고</span>
                    <span className={`sku-value ${selectedSku.stockQty <= 5 ? 'low-stock' : ''}`}>
                      {selectedSku.stockQty > 0 ? `${selectedSku.stockQty}개` : '품절'}
                    </span>
                  </div>
                  {selectedSku.price !== product.basePrice && (
                    <div className="sku-detail">
                      <span className="sku-label">옵션 가격</span>
                      <span className="sku-value sku-price">{selectedSku.price.toLocaleString()}원</span>
                    </div>
                  )}
                </div>
                <Divider />
              </>
            )}

            {/* 수량 선택 */}
            <div className="product-options">
              <div className="option-row">
                <span className="option-label">수량</span>
                <div className="quantity-control">
                  <Button
                    icon={<MinusOutlined />}
                    onClick={() => handleQuantityChange(quantity - 1)}
                    disabled={quantity <= 1 || !canAddToCart()}
                  />
                  <InputNumber
                    min={1}
                    max={selectedSku?.stockQty ?? displayStock}
                    value={quantity}
                    onChange={handleQuantityChange}
                    className="quantity-input"
                    disabled={!canAddToCart()}
                  />
                  <Button
                    icon={<PlusOutlined />}
                    onClick={() => handleQuantityChange(quantity + 1)}
                    disabled={quantity >= (selectedSku?.stockQty ?? displayStock) || !canAddToCart()}
                  />
                  <span className="stock-info">(재고: {selectedSku?.stockQty ?? displayStock}개)</span>
                </div>
              </div>
            </div>

            {/* 총 금액 */}
            {canAddToCart() && quantity > 0 && (
              <>
                <Divider />
                <div className="total-price-info">
                  <span className="total-label">총 상품금액</span>
                  <span className="total-price">{totalPrice.toLocaleString()}원</span>
                </div>
              </>
            )}

            {/* 옵션 미선택 안내 */}
            {hasOptions && !selectedSku && (
              <p className="option-guide-msg">옵션을 선택해주세요.</p>
            )}

            <Divider />

            <div className="product-actions">
              <Space size="middle" className="action-buttons">
                <Button
                  type="primary"
                  icon={<ShoppingCartOutlined />}
                  size="large"
                  onClick={handleAddToCart}
                  className="cart-btn"
                  disabled={!canAddToCart()}
                >
                  장바구니
                </Button>
                <Button
                  type="primary"
                  size="large"
                  onClick={handleBuyNow}
                  className="buy-btn"
                  disabled={!canAddToCart()}
                >
                  바로구매
                </Button>
              </Space>
            </div>
          </div>
        </div>

        {/* 상품 상세 설명 */}
        <div className="product-description-section">
          <Card title="상품 상세 정보" className="description-card">
            <div className="product-description">
              <div dangerouslySetInnerHTML={{ __html: product.description || '' }} />
            </div>
          </Card>
        </div>

        {/* SKU 옵션별 재고 현황 */}
        {hasOptions && product.skus.length > 0 && (
          <div className="product-description-section">
            <Card title="옵션별 재고 현황" className="description-card">
              <div className="sku-stock-table">
                <table>
                  <thead>
                    <tr>
                      <th>옵션</th>
                      <th>SKU 코드</th>
                      <th>가격</th>
                      <th>재고</th>
                      <th>상태</th>
                    </tr>
                  </thead>
                  <tbody>
                    {product.skus.map((sku) => (
                      <tr key={sku.id} className={sku.status !== 'ACTIVE' || sku.stockQty === 0 ? 'sku-row-disabled' : ''}>
                        <td>{getSkuOptionLabel(sku, product.optionGroups)}</td>
                        <td>{sku.skuCode}</td>
                        <td>{sku.price.toLocaleString()}원</td>
                        <td className={sku.stockQty <= 5 && sku.stockQty > 0 ? 'low-stock' : ''}>
                          {sku.stockQty}개
                        </td>
                        <td>
                          {sku.status !== 'ACTIVE' ? (
                            <Tag color="default">비활성</Tag>
                          ) : sku.stockQty === 0 ? (
                            <Tag color="red">품절</Tag>
                          ) : sku.stockQty <= 5 ? (
                            <Tag color="orange">품절임박</Tag>
                          ) : (
                            <Tag color="green">판매중</Tag>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          </div>
        )}
      </div>

      <MarketFooter />
    </div>
  )
}

// SKU의 옵션값 이름 조합 반환
function getSkuOptionLabel(sku: SkuResponse, optionGroups: OptionGroupResponse[]): string {
  return sku.optionValueIds
    .map((vid) => {
      for (const g of optionGroups) {
        const val = g.optionValues.find((v) => v.id === vid)
        if (val) return val.optionValueName
      }
      return ''
    })
    .filter(Boolean)
    .join(' / ')
}

// 옵션 그룹 선택 컴포넌트
function OptionGroupSelector({
  group,
  selectedValueId,
  onSelect,
  isValueAvailable
}: {
  group: OptionGroupResponse
  selectedValueId: number | undefined
  onSelect: (valueId: number) => void
  isValueAvailable: (valueId: number) => boolean
}) {
  const sortedValues = [...group.optionValues].sort((a, b) => a.displayOrder - b.displayOrder)

  return (
    <div className="option-group">
      <span className="option-label">{group.optionGroupName}</span>
      <div className="option-values">
        {sortedValues.map((val) => {
          const available = isValueAvailable(val.id)
          const selected = selectedValueId === val.id
          return (
            <button
              key={val.id}
              className={`option-value-btn ${selected ? 'selected' : ''} ${!available ? 'unavailable' : ''}`}
              onClick={() => onSelect(val.id)}
              disabled={!available}
            >
              {val.optionValueName}
              {!available && <span className="sold-out-badge">품절</span>}
            </button>
          )
        })}
      </div>
    </div>
  )
}

export default MarketProductDetail

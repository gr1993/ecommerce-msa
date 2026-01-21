import { useState, useEffect } from 'react'
import {
  Form,
  Input,
  InputNumber,
  Select,
  Button,
  Upload,
  Space,
  message,
  Card,
  Switch,
  Table,
  Popconfirm,
  Tag
} from 'antd'
import { PlusOutlined, DeleteOutlined, UploadOutlined } from '@ant-design/icons'
import type { RcFile, UploadFile } from 'antd/es/upload/interface'
import { getCategoryTree, type CategoryTreeResponse } from '../../../api/categoryApi'
import './AdminProductRegister.css'

const { TextArea } = Input
const { Option } = Select

export interface OptionGroup {
  id: string
  optionGroupName: string
  displayOrder: number
  optionValues: OptionValue[]
}

export interface OptionValue {
  id: string
  optionValueName: string
  displayOrder: number
}

export interface SKU {
  id: string
  skuCode: string
  price: number
  stockQty: number
  status: string
  optionValueIds: string[]
}

export interface ProductImage {
  id: string
  file: File
  fileId?: number // 기존 이미지의 파일 ID (수정 시 사용)
  isPrimary: boolean
  displayOrder: number
  url?: string // 수정 시 기존 이미지 URL
}

export interface SelectedCategory {
  categoryId: number
  categoryName: string
  fullPath: string // 대 > 중 > 소 형태의 경로
}

interface ProductFormProps {
  mode: 'create' | 'edit'
  initialData?: {
    product_name?: string
    product_code?: string
    description?: string
    base_price?: number
    sale_price?: number
    status?: string
    is_displayed?: boolean
    categories?: SelectedCategory[]
    optionGroups?: OptionGroup[]
    skus?: SKU[]
    images?: ProductImage[]
  }
  onSubmit: (data: any) => void
  onCancel: () => void
  loading?: boolean
}

function ProductForm({ mode, initialData, onSubmit, onCancel, loading = false }: ProductFormProps) {
  const [form] = Form.useForm()
  const [optionGroups, setOptionGroups] = useState<OptionGroup[]>([])
  const [skus, setSkus] = useState<SKU[]>([])
  const [images, setImages] = useState<ProductImage[]>([])

  // 카테고리 관련 state
  const [categoryTree, setCategoryTree] = useState<CategoryTreeResponse[]>([])
  const [selectedCategories, setSelectedCategories] = useState<SelectedCategory[]>([])
  const [selectedCategory1, setSelectedCategory1] = useState<number | undefined>(undefined)
  const [selectedCategory2, setSelectedCategory2] = useState<number | undefined>(undefined)
  const [selectedCategory3, setSelectedCategory3] = useState<number | undefined>(undefined)

  // 중/소 카테고리 목록 계산
  const category2List = selectedCategory1
    ? categoryTree.find(c => c.categoryId === selectedCategory1)?.children || []
    : []
  const category3List = selectedCategory2
    ? category2List.find(c => c.categoryId === selectedCategory2)?.children || []
    : []

  // 카테고리 트리 로드
  useEffect(() => {
    const fetchCategoryTree = async () => {
      try {
        const tree = await getCategoryTree()
        setCategoryTree(tree)
      } catch (error) {
        console.error('Failed to fetch category tree:', error)
        message.error('카테고리 목록 조회에 실패했습니다.')
      }
    }
    fetchCategoryTree()
  }, [])

  // 초기 데이터 로드 (수정 모드)
  useEffect(() => {
    if (mode === 'edit' && initialData) {
      form.setFieldsValue({
        product_name: initialData.product_name,
        product_code: initialData.product_code,
        description: initialData.description,
        base_price: initialData.base_price,
        sale_price: initialData.sale_price,
        status: initialData.status || 'ACTIVE',
        is_displayed: initialData.is_displayed !== undefined ? initialData.is_displayed : true,
      })

      if (initialData.categories) {
        setSelectedCategories(initialData.categories)
      }
      if (initialData.optionGroups) {
        setOptionGroups(initialData.optionGroups)
      }
      if (initialData.skus) {
        setSkus(initialData.skus)
      }
      if (initialData.images) {
        setImages(initialData.images)
      }
    }
  }, [mode, initialData, form])

  const handleSubmit = (values: any) => {
    const formData = {
      ...values,
      categories: selectedCategories,
      optionGroups,
      skus,
      images
    }
    onSubmit(formData)
  }

  // 카테고리 추가
  const addCategory = () => {
    if (!selectedCategory3) {
      message.warning('소카테고리(3단계)까지 선택해주세요.')
      return
    }

    // 이미 추가된 카테고리인지 확인
    if (selectedCategories.some(c => c.categoryId === selectedCategory3)) {
      message.warning('이미 추가된 카테고리입니다.')
      return
    }

    // 카테고리 경로 생성
    const cat1 = categoryTree.find(c => c.categoryId === selectedCategory1)
    const cat2 = category2List.find(c => c.categoryId === selectedCategory2)
    const cat3 = category3List.find(c => c.categoryId === selectedCategory3)

    if (cat1 && cat2 && cat3) {
      const newCategory: SelectedCategory = {
        categoryId: selectedCategory3,
        categoryName: cat3.categoryName,
        fullPath: `${cat1.categoryName} > ${cat2.categoryName} > ${cat3.categoryName}`
      }
      setSelectedCategories([...selectedCategories, newCategory])

      // 선택 초기화
      setSelectedCategory1(undefined)
      setSelectedCategory2(undefined)
      setSelectedCategory3(undefined)
    }
  }

  // 카테고리 제거
  const removeCategory = (categoryId: number) => {
    setSelectedCategories(selectedCategories.filter(c => c.categoryId !== categoryId))
  }

  // 대카테고리 변경 시 중/소카테고리 초기화
  const handleCategory1Change = (value: number | undefined) => {
    setSelectedCategory1(value)
    setSelectedCategory2(undefined)
    setSelectedCategory3(undefined)
  }

  // 중카테고리 변경 시 소카테고리 초기화
  const handleCategory2Change = (value: number | undefined) => {
    setSelectedCategory2(value)
    setSelectedCategory3(undefined)
  }

  // 옵션 그룹 관리
  const addOptionGroup = () => {
    const newGroup: OptionGroup = {
      id: `group_${Date.now()}`,
      optionGroupName: '',
      displayOrder: optionGroups.length,
      optionValues: []
    }
    setOptionGroups([...optionGroups, newGroup])
  }

  const removeOptionGroup = (groupId: string) => {
    setOptionGroups(optionGroups.filter(g => g.id !== groupId))
    setSkus(skus.filter(sku => {
      const groupOptionValueIds = optionGroups
        .find(g => g.id === groupId)
        ?.optionValues.map(ov => ov.id) || []
      return !sku.optionValueIds.some(id => groupOptionValueIds.includes(id))
    }))
  }

  const updateOptionGroup = (groupId: string, field: string, value: any) => {
    setOptionGroups(optionGroups.map(g => 
      g.id === groupId ? { ...g, [field]: value } : g
    ))
  }

  const addOptionValue = (groupId: string) => {
    const group = optionGroups.find(g => g.id === groupId)
    if (!group) return

    const newValue: OptionValue = {
      id: `value_${Date.now()}`,
      optionValueName: '',
      displayOrder: group.optionValues.length
    }
    
    setOptionGroups(optionGroups.map(g =>
      g.id === groupId 
        ? { ...g, optionValues: [...g.optionValues, newValue] }
        : g
    ))
  }

  const removeOptionValue = (groupId: string, valueId: string) => {
    setOptionGroups(optionGroups.map(g =>
      g.id === groupId
        ? { ...g, optionValues: g.optionValues.filter(ov => ov.id !== valueId) }
        : g
    ))
    setSkus(skus.filter(sku => !sku.optionValueIds.includes(valueId)))
  }

  const updateOptionValue = (groupId: string, valueId: string, field: string, value: any) => {
    setOptionGroups(optionGroups.map(g =>
      g.id === groupId
        ? {
            ...g,
            optionValues: g.optionValues.map(ov =>
              ov.id === valueId ? { ...ov, [field]: value } : ov
            )
          }
        : g
    ))
  }

  // SKU 자동 생성
  const generateSKUs = () => {
    if (optionGroups.length === 0) {
      message.warning('옵션 그룹을 먼저 추가하세요.')
      return
    }

    const combinations: string[][] = []
    
    function generateCombinations(groupIndex: number, current: string[]) {
      if (groupIndex >= optionGroups.length) {
        combinations.push([...current])
        return
      }

      const group = optionGroups[groupIndex]
      if (group.optionValues.length === 0) {
        generateCombinations(groupIndex + 1, current)
        return
      }

      group.optionValues.forEach(value => {
        generateCombinations(groupIndex + 1, [...current, value.id])
      })
    }

    generateCombinations(0, [])

    const newSkus: SKU[] = combinations.map((optionValueIds, index) => ({
      id: `sku_${Date.now()}_${index}`,
      skuCode: `SKU-${Date.now()}-${index}`,
      price: form.getFieldValue('base_price') || 0,
      stockQty: 0,
      status: 'ACTIVE',
      optionValueIds
    }))

    setSkus(newSkus)
    message.success(`${newSkus.length}개의 SKU가 생성되었습니다.`)
  }

  const updateSKU = (skuId: string, field: string, value: any) => {
    setSkus(skus.map(sku =>
      sku.id === skuId ? { ...sku, [field]: value } : sku
    ))
  }

  const removeSKU = (skuId: string) => {
    setSkus(skus.filter(s => s.id !== skuId))
  }

  // 이미지 관리
  const handleImageChange = (info: any) => {
    const newImages: ProductImage[] = info.fileList.map((file: UploadFile, index: number) => {
      // 기존 이미지인 경우 (수정 모드)
      if (file.url && !file.originFileObj) {
        const existingImage = images.find(img => img.id === file.uid)
        return {
          id: file.uid,
          file: file as any,
          fileId: existingImage?.fileId, // 기존 이미지의 fileId 보존
          isPrimary: existingImage?.isPrimary || false,
          displayOrder: index,
          url: file.url
        }
      }
      // 새로 업로드한 이미지
      return {
        id: file.uid || `img_${Date.now()}_${index}`,
        file: (file.originFileObj as File) || file as any,
        isPrimary: index === 0 && images.length === 0,
        displayOrder: index
      }
    })
    setImages(newImages)
  }

  const setPrimaryImage = (imageId: string) => {
    setImages(images.map(img => ({
      ...img,
      isPrimary: img.id === imageId
    })))
  }

  const getOptionValueName = (valueId: string): string => {
    for (const group of optionGroups) {
      const value = group.optionValues.find(ov => ov.id === valueId)
      if (value) return value.optionValueName
    }
    return ''
  }

  const getSKUDisplayName = (sku: SKU): string => {
    if (sku.optionValueIds.length === 0) return '기본'
    return sku.optionValueIds.map(id => getOptionValueName(id)).join(' / ')
  }

  const getFileList = () => {
    return images.map(img => ({
      uid: img.id,
      name: `image-${img.id}`,
      status: 'done' as const,
      url: img.url || URL.createObjectURL(img.file),
      originFileObj: img.url ? undefined : (img.file as RcFile)
    }))
  }

  return (
    <Form
      form={form}
      layout="vertical"
      onFinish={handleSubmit}
      className="product-form"
    >
      {/* 기본 정보 섹션 */}
      <Card title="기본 정보" className="form-section">
        <Form.Item
          label="상품명"
          name="product_name"
          rules={[{ required: true, message: '상품명을 입력하세요' }]}
        >
          <Input placeholder="상품명을 입력하세요" />
        </Form.Item>

        <Form.Item
          label="상품 코드"
          name="product_code"
        >
          <Input placeholder="상품 코드를 입력하세요 (자동 생성 가능)" />
        </Form.Item>

        <Form.Item
          label="상품 상세 설명"
          name="description"
        >
          <TextArea 
            rows={5} 
            placeholder="상품에 대한 상세 설명을 입력하세요" 
          />
        </Form.Item>

        <div className="form-row">
          <Form.Item
            label="기본 가격"
            name="base_price"
            rules={[
              { required: true, message: '기본 가격을 입력하세요' },
              { type: 'number', min: 0, message: '가격은 0 이상이어야 합니다' }
            ]}
          >
            <InputNumber
              style={{ width: '100%' }}
              placeholder="0"
              min={0}
              step={0.01}
              formatter={(value) => value ? `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',') : ''}
            />
          </Form.Item>

          <Form.Item
            label="할인 가격"
            name="sale_price"
            rules={[
              { type: 'number', min: 0, message: '가격은 0 이상이어야 합니다' }
            ]}
          >
            <InputNumber
              style={{ width: '100%' }}
              placeholder="0"
              min={0}
              step={0.01}
              formatter={(value) => value ? `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',') : ''}
            />
          </Form.Item>
        </div>

        <div className="form-row">
          <Form.Item
            label="상품 상태"
            name="status"
            initialValue="ACTIVE"
            rules={[{ required: true }]}
          >
            <Select>
              <Option value="ACTIVE">활성</Option>
              <Option value="INACTIVE">비활성</Option>
              <Option value="SOLD_OUT">품절</Option>
            </Select>
          </Form.Item>

          <Form.Item
            label="진열 여부"
            name="is_displayed"
            valuePropName="checked"
            initialValue={true}
          >
            <Switch />
          </Form.Item>
        </div>
      </Card>

      {/* 카테고리 섹션 */}
      <Card title="카테고리" className="form-section">
        <div className="category-selector">
          <Space size="middle" wrap style={{ marginBottom: 16 }}>
            <Select
              placeholder="대카테고리"
              allowClear
              style={{ width: 150 }}
              value={selectedCategory1}
              onChange={handleCategory1Change}
            >
              {categoryTree.map((cat) => (
                <Option key={cat.categoryId} value={cat.categoryId}>
                  {cat.categoryName}
                </Option>
              ))}
            </Select>
            <Select
              placeholder="중카테고리"
              allowClear
              style={{ width: 150 }}
              value={selectedCategory2}
              onChange={handleCategory2Change}
              disabled={!selectedCategory1 || category2List.length === 0}
            >
              {category2List.map((cat) => (
                <Option key={cat.categoryId} value={cat.categoryId}>
                  {cat.categoryName}
                </Option>
              ))}
            </Select>
            <Select
              placeholder="소카테고리"
              allowClear
              style={{ width: 150 }}
              value={selectedCategory3}
              onChange={setSelectedCategory3}
              disabled={!selectedCategory2 || category3List.length === 0}
            >
              {category3List.map((cat) => (
                <Option key={cat.categoryId} value={cat.categoryId}>
                  {cat.categoryName}
                </Option>
              ))}
            </Select>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={addCategory}
              disabled={!selectedCategory3}
            >
              추가
            </Button>
          </Space>
        </div>
        <div className="selected-categories">
          {selectedCategories.length === 0 ? (
            <div className="empty-state">카테고리를 선택하세요 (3단계 카테고리만 등록 가능)</div>
          ) : (
            <Space wrap>
              {selectedCategories.map((cat) => (
                <Tag
                  key={cat.categoryId}
                  closable
                  onClose={() => removeCategory(cat.categoryId)}
                  style={{ padding: '4px 8px', fontSize: '13px' }}
                >
                  {cat.fullPath}
                </Tag>
              ))}
            </Space>
          )}
        </div>
      </Card>

      {/* 옵션 그룹 섹션 */}
      <Card
        title="옵션 그룹"
        className="form-section"
        extra={
          <Button type="dashed" icon={<PlusOutlined />} onClick={addOptionGroup}>
            옵션 그룹 추가
          </Button>
        }
      >
        {optionGroups.length === 0 ? (
          <div className="empty-state">옵션 그룹을 추가하세요 (예: 색상, 사이즈)</div>
        ) : (
          optionGroups.map((group) => (
            <Card
              key={group.id}
              type="inner"
              title={
                <Input
                  placeholder="옵션 그룹명 (예: 색상, 사이즈)"
                  value={group.optionGroupName}
                  onChange={(e) => updateOptionGroup(group.id, 'optionGroupName', e.target.value)}
                  style={{ width: 200 }}
                />
              }
              extra={
                <Popconfirm
                  title="이 옵션 그룹을 삭제하시겠습니까?"
                  onConfirm={() => removeOptionGroup(group.id)}
                >
                  <Button type="text" danger icon={<DeleteOutlined />} />
                </Popconfirm>
              }
              className="option-group-card"
            >
              <Space direction="vertical" style={{ width: '100%' }}>
                {group.optionValues.map((value) => (
                  <Space key={value.id} style={{ width: '100%' }}>
                    <Input
                      placeholder="옵션 값 (예: Red, M, L)"
                      value={value.optionValueName}
                      onChange={(e) => updateOptionValue(group.id, value.id, 'optionValueName', e.target.value)}
                      style={{ flex: 1 }}
                    />
                    <Popconfirm
                      title="이 옵션 값을 삭제하시겠습니까?"
                      onConfirm={() => removeOptionValue(group.id, value.id)}
                    >
                      <Button type="text" danger icon={<DeleteOutlined />} />
                    </Popconfirm>
                  </Space>
                ))}
                <Button
                  type="dashed"
                  icon={<PlusOutlined />}
                  onClick={() => addOptionValue(group.id)}
                  block
                >
                  옵션 값 추가
                </Button>
              </Space>
            </Card>
          ))
        )}
      </Card>

      {/* SKU 섹션 */}
      <Card 
        title="SKU 정의" 
        className="form-section"
        extra={
          <Button 
            type="primary" 
            onClick={generateSKUs}
            disabled={optionGroups.length === 0}
          >
            SKU 자동 생성
          </Button>
        }
      >
        {skus.length === 0 ? (
          <div className="empty-state">
            옵션 그룹을 설정한 후 'SKU 자동 생성' 버튼을 클릭하세요
          </div>
        ) : (
          <Table
            dataSource={skus}
            rowKey="id"
            pagination={false}
            size="small"
            columns={[
              {
                title: '옵션 조합',
                key: 'options',
                render: (_, sku) => getSKUDisplayName(sku)
              },
              {
                title: 'SKU 코드',
                dataIndex: 'skuCode',
                render: (text, sku) => (
                  <Input
                    value={text}
                    onChange={(e) => updateSKU(sku.id, 'skuCode', e.target.value)}
                    placeholder="SKU 코드"
                  />
                )
              },
              {
                title: '가격',
                dataIndex: 'price',
                render: (text, sku) => (
                  <InputNumber
                    value={text}
                    onChange={(value) => updateSKU(sku.id, 'price', value)}
                    min={0}
                    step={0.01}
                    formatter={(value) => value ? `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',') : ''}
                    style={{ width: '100%' }}
                  />
                )
              },
              {
                title: '재고 수량',
                dataIndex: 'stockQty',
                render: (text, sku) => (
                  <InputNumber
                    value={text}
                    onChange={(value) => updateSKU(sku.id, 'stockQty', value)}
                    min={0}
                    style={{ width: '100%' }}
                  />
                )
              },
              {
                title: '상태',
                dataIndex: 'status',
                render: (text, sku) => (
                  <Select
                    value={text}
                    onChange={(value) => updateSKU(sku.id, 'status', value)}
                    style={{ width: 100 }}
                  >
                    <Option value="ACTIVE">활성</Option>
                    <Option value="SOLD_OUT">품절</Option>
                    <Option value="INACTIVE">비활성</Option>
                  </Select>
                )
              },
              {
                title: '작업',
                key: 'action',
                render: (_, sku) => (
                  <Popconfirm
                    title="이 SKU를 삭제하시겠습니까?"
                    onConfirm={() => removeSKU(sku.id)}
                  >
                    <Button type="text" danger icon={<DeleteOutlined />} />
                  </Popconfirm>
                )
              }
            ]}
          />
        )}
      </Card>

      {/* 이미지 섹션 */}
      <Card title="상품 이미지" className="form-section">
        <Form.Item name="images">
          <Upload
            listType="picture-card"
            fileList={getFileList()}
            onChange={handleImageChange}
            beforeUpload={() => false}
            accept="image/*"
            multiple
          >
            {images.length < 10 && (
              <div>
                <UploadOutlined />
                <div style={{ marginTop: 8 }}>업로드</div>
              </div>
            )}
          </Upload>
        </Form.Item>
        {images.length > 0 && (
          <div className="image-actions">
            <Space>
              {images.map((img, index) => (
                <Button
                  key={img.id}
                  type={img.isPrimary ? 'primary' : 'default'}
                  size="small"
                  onClick={() => setPrimaryImage(img.id)}
                >
                  {img.isPrimary ? '대표' : `${index + 1}`}
                </Button>
              ))}
            </Space>
          </div>
        )}
      </Card>

      <Form.Item className="form-actions">
        <Space>
          <Button onClick={onCancel} disabled={loading}>
            취소
          </Button>
          <Button 
            type="primary" 
            htmlType="submit"
            loading={loading}
            style={{ backgroundColor: '#FFC107', borderColor: '#FFC107', color: '#343A40', fontWeight: 600 }}
          >
            {mode === 'create' ? '등록하기' : '수정하기'}
          </Button>
        </Space>
      </Form.Item>
    </Form>
  )
}

export default ProductForm


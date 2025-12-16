import { useState } from 'react'
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
  Popconfirm
} from 'antd'
import { PlusOutlined, DeleteOutlined, UploadOutlined } from '@ant-design/icons'
import type { RcFile, UploadFile } from 'antd/es/upload/interface'
import { useNavigate } from 'react-router-dom'
import './AdminProductRegister.css'

const { TextArea } = Input
const { Option } = Select

interface OptionGroup {
  id: string
  optionGroupName: string
  displayOrder: number
  optionValues: OptionValue[]
}

interface OptionValue {
  id: string
  optionValueName: string
  displayOrder: number
}

interface SKU {
  id: string
  skuCode: string
  price: number
  stockQty: number
  status: string
  optionValueIds: string[]
}

interface ProductImage {
  id: string
  file: File
  isPrimary: boolean
  displayOrder: number
}

function AdminProductRegister() {
  const [form] = Form.useForm()
  const navigate = useNavigate()
  const [optionGroups, setOptionGroups] = useState<OptionGroup[]>([])
  const [skus, setSkus] = useState<SKU[]>([])
  const [images, setImages] = useState<ProductImage[]>([])

  const handleSubmit = (values: any) => {
    const formData = {
      ...values,
      optionGroups,
      skus,
      images
    }
    console.log('상품 등록:', formData)
    // TODO: API 호출로 상품 등록 처리
    message.success('상품이 등록되었습니다.')
    navigate('/admin/product/list')
  }

  const handleCancel = () => {
    navigate('/admin/product/list')
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
    // 해당 그룹의 옵션을 사용하는 SKU도 제거
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
    // 해당 옵션 값을 사용하는 SKU도 제거
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

    // 모든 옵션 그룹의 옵션 값 조합 생성
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
    const newImages: ProductImage[] = info.fileList.map((file: UploadFile, index: number) => ({
      id: file.uid || `img_${Date.now()}_${index}`,
      file: (file.originFileObj as File) || file as any,
      isPrimary: index === 0 && images.length === 0,
      displayOrder: index
    }))
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

  return (
    <div className="admin-product-register">
      <div className="admin-product-register-container">
        <h2>상품 등록</h2>
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
                fileList={images.map(img => ({
                  uid: img.id,
                  name: `image-${img.id}`,
                  status: 'done' as const,
                  url: URL.createObjectURL(img.file),
                  originFileObj: img.file as RcFile
                }))}
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
              <Button onClick={handleCancel}>
                취소
              </Button>
              <Button 
                type="primary" 
                htmlType="submit"
                style={{ backgroundColor: '#FFC107', borderColor: '#FFC107', color: '#343A40', fontWeight: 600 }}
              >
                등록하기
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </div>
    </div>
  )
}

export default AdminProductRegister

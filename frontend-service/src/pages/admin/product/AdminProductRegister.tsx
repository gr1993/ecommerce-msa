import { useState } from 'react'
import { Form, Input, InputNumber, Select, Button, Upload, Space, message } from 'antd'
import type { UploadFile } from 'antd/es/upload/interface'
import { useNavigate } from 'react-router-dom'
import './AdminProductRegister.css'

const { TextArea } = Input
const { Option } = Select

function AdminProductRegister() {
  const [form] = Form.useForm()
  const navigate = useNavigate()
  const [fileList, setFileList] = useState<UploadFile[]>([])

  const handleSubmit = (values: any) => {
    const formData = {
      ...values,
      image: fileList.length > 0 ? fileList[0] : null
    }
    console.log('상품 등록:', formData)
    // TODO: API 호출로 상품 등록 처리
    message.success('상품이 등록되었습니다.')
    navigate('/admin/product/list')
  }

  const handleCancel = () => {
    navigate('/admin/product/list')
  }

  const handleImageChange = (info: any) => {
    setFileList(info.fileList)
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
          <Form.Item
            label="상품명"
            name="name"
            rules={[{ required: true, message: '상품명을 입력하세요' }]}
          >
            <Input placeholder="상품명을 입력하세요" />
          </Form.Item>

          <Form.Item
            label="상품 설명"
            name="description"
            rules={[{ required: true, message: '상품 설명을 입력하세요' }]}
          >
            <TextArea 
              rows={5} 
              placeholder="상품에 대한 상세 설명을 입력하세요" 
            />
          </Form.Item>

          <div className="form-row">
            <Form.Item
              label="가격"
              name="price"
              rules={[
                { required: true, message: '가격을 입력하세요' },
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
              label="재고 수량"
              name="stock"
              rules={[
                { required: true, message: '재고 수량을 입력하세요' },
                { type: 'number', min: 0, message: '재고 수량은 0 이상이어야 합니다' }
              ]}
            >
              <InputNumber
                style={{ width: '100%' }}
                placeholder="0"
                min={0}
              />
            </Form.Item>
          </div>

          <Form.Item
            label="카테고리"
            name="category"
            rules={[{ required: true, message: '카테고리를 선택하세요' }]}
          >
            <Select placeholder="카테고리를 선택하세요">
              <Option value="electronics">전자제품</Option>
              <Option value="clothing">의류</Option>
              <Option value="food">식품</Option>
              <Option value="books">도서</Option>
              <Option value="sports">스포츠</Option>
              <Option value="beauty">뷰티</Option>
            </Select>
          </Form.Item>

          <Form.Item
            label="상품 이미지"
            name="image"
          >
            <Upload
              listType="text"
              fileList={fileList}
              onChange={handleImageChange}
              beforeUpload={() => false}
              accept="image/*"
            >
              <Button>이미지 선택</Button>
            </Upload>
          </Form.Item>

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


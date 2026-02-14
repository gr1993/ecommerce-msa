import { useState, useEffect } from 'react'
import { Table, Space, Input, Button, Select, Tag, Modal, Form, InputNumber, DatePicker, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PlusOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import {
  getAdminDiscountPolicies,
  getAdminDiscountPolicyDetail,
  createAdminDiscountPolicy,
  updateAdminDiscountPolicy,
} from '../../../api/discountPolicyApi'
import type { DiscountPolicy } from '../../../api/discountPolicyApi'
import './AdminDiscountPolicyManage.css'

const { Option } = Select
const { RangePicker } = DatePicker

function AdminDiscountPolicyManage() {
  const [policies, setPolicies] = useState<DiscountPolicy[]>([])
  const [loading, setLoading] = useState(false)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [searchStatus, setSearchStatus] = useState<string | undefined>(undefined)
  const [currentPage, setCurrentPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [totalElements, setTotalElements] = useState(0)
  const [isRegisterModalVisible, setIsRegisterModalVisible] = useState(false)
  const [isDetailModalVisible, setIsDetailModalVisible] = useState(false)
  const [selectedPolicy, setSelectedPolicy] = useState<DiscountPolicy | null>(null)
  const [policyForm] = Form.useForm()
  const [detailForm] = Form.useForm()

  const discountTypeMap: Record<string, string> = {
    FIXED: '정액 할인',
    RATE: '정률 할인'
  }

  const targetTypeMap: Record<string, string> = {
    PRODUCT: '상품',
    CATEGORY: '카테고리',
    ORDER: '주문'
  }

  const statusMap: Record<string, { label: string; color: string }> = {
    ACTIVE: { label: '활성', color: 'green' },
    INACTIVE: { label: '비활성', color: 'red' },
    EXPIRED: { label: '만료', color: 'default' }
  }

  // 할인 정책 목록 조회
  const fetchPolicies = async (keyword?: string, status?: string, page: number = 1, size: number = pageSize) => {
    setLoading(true)
    try {
      const data = await getAdminDiscountPolicies(keyword || undefined, status || undefined, page - 1, size)
      setPolicies(data.content)
      setTotalElements(data.totalElements)
      setCurrentPage(page)
      setPageSize(size)
    } catch (error) {
      message.error(error instanceof Error ? error.message : '할인 정책 목록을 불러오는데 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchPolicies()
  }, [])

  const handleSearch = () => {
    fetchPolicies(searchKeyword, searchStatus, 1)
  }

  const handleReset = () => {
    setSearchKeyword('')
    setSearchStatus(undefined)
    fetchPolicies(undefined, undefined, 1)
  }

  const handleRegister = () => {
    policyForm.resetFields()
    policyForm.setFieldsValue({
      discount_type: 'RATE',
      target_type: 'ORDER',
      min_order_amount: 0,
      status: 'ACTIVE'
    })
    setIsRegisterModalVisible(true)
  }

  const handleRegisterSave = async () => {
    try {
      const values = await policyForm.validateFields()

      await createAdminDiscountPolicy({
        discountName: values.discount_name,
        discountType: values.discount_type,
        discountValue: values.discount_value,
        targetType: values.target_type,
        targetId: values.target_type === 'ORDER' ? undefined : values.target_id,
        minOrderAmount: values.min_order_amount || undefined,
        maxDiscountAmount: values.discount_type === 'RATE' ? values.max_discount_amount : undefined,
        validFrom: values.valid_period[0].format('YYYY-MM-DDTHH:mm:ss'),
        validTo: values.valid_period[1].format('YYYY-MM-DDTHH:mm:ss'),
        status: values.status,
      })

      message.success('할인 정책이 등록되었습니다.')
      setIsRegisterModalVisible(false)
      policyForm.resetFields()
      fetchPolicies(searchKeyword, searchStatus, 1)
    } catch (error) {
      if (error instanceof Error && error.message !== 'Validation failed') {
        message.error(error.message)
      }
    }
  }

  const handleRegisterModalClose = () => {
    setIsRegisterModalVisible(false)
    policyForm.resetFields()
  }

  // 할인 정책 상세 조회
  const handlePolicyClick = async (policy: DiscountPolicy) => {
    try {
      const detail = await getAdminDiscountPolicyDetail(policy.discount_id)
      setSelectedPolicy(detail)

      detailForm.setFieldsValue({
        discount_name: detail.discount_name,
        discount_type: detail.discount_type,
        discount_value: detail.discount_value,
        target_type: detail.target_type,
        target_id: detail.target_id || undefined,
        min_order_amount: detail.min_order_amount || undefined,
        max_discount_amount: detail.max_discount_amount || undefined,
        valid_period: [dayjs(detail.valid_from), dayjs(detail.valid_to)],
        status: detail.status,
      })

      setIsDetailModalVisible(true)
    } catch (error) {
      message.error(error instanceof Error ? error.message : '할인 정책 상세 정보를 불러오는데 실패했습니다.')
    }
  }

  const handleDetailModalClose = () => {
    setIsDetailModalVisible(false)
    setSelectedPolicy(null)
    detailForm.resetFields()
  }

  const handleDetailSave = async () => {
    if (!selectedPolicy) return

    try {
      const values = await detailForm.validateFields()

      await updateAdminDiscountPolicy(selectedPolicy.discount_id, {
        discountName: values.discount_name,
        discountType: values.discount_type,
        discountValue: values.discount_value,
        targetType: values.target_type,
        targetId: values.target_type === 'ORDER' ? undefined : values.target_id,
        minOrderAmount: values.min_order_amount || undefined,
        maxDiscountAmount: values.discount_type === 'RATE' ? values.max_discount_amount : undefined,
        validFrom: values.valid_period[0].format('YYYY-MM-DDTHH:mm:ss'),
        validTo: values.valid_period[1].format('YYYY-MM-DDTHH:mm:ss'),
        status: values.status,
      })

      message.success('할인 정책 정보가 수정되었습니다.')
      setIsDetailModalVisible(false)
      setSelectedPolicy(null)
      detailForm.resetFields()
      fetchPolicies(searchKeyword, searchStatus, currentPage, pageSize)
    } catch (error) {
      if (error instanceof Error && error.message !== 'Validation failed') {
        message.error(error.message)
      }
    }
  }

  const columns: ColumnsType<DiscountPolicy> = [
    {
      title: '할인 정책 ID',
      dataIndex: 'discount_id',
      key: 'discount_id',
      width: 120,
    },
    {
      title: '할인 정책명',
      dataIndex: 'discount_name',
      key: 'discount_name',
      sorter: (a, b) => a.discount_name.localeCompare(b.discount_name),
      render: (text: string, record: DiscountPolicy) => (
        <a
          onClick={() => handlePolicyClick(record)}
          style={{ color: '#007BFF', cursor: 'pointer' }}
        >
          {text}
        </a>
      ),
      width: 200,
    },
    {
      title: '할인 유형',
      dataIndex: 'discount_type',
      key: 'discount_type',
      filters: [
        { text: '정액 할인', value: 'FIXED' },
        { text: '정률 할인', value: 'RATE' },
      ],
      onFilter: (value, record) => record.discount_type === value,
      render: (type: string) => discountTypeMap[type] || type,
      width: 120,
    },
    {
      title: '할인 금액/율',
      key: 'discount',
      render: (_, record: DiscountPolicy) => {
        if (record.discount_type === 'RATE') {
          return `${record.discount_value}%${record.max_discount_amount ? ` (최대 ${record.max_discount_amount.toLocaleString()}원)` : ''}`
        } else {
          return `${record.discount_value.toLocaleString()}원`
        }
      },
      width: 180,
    },
    {
      title: '적용 대상',
      key: 'target',
      render: (_, record: DiscountPolicy) => {
        const targetLabel = targetTypeMap[record.target_type] || record.target_type
        return record.target_id ? `${targetLabel} (ID: ${record.target_id})` : targetLabel
      },
      width: 150,
    },
    {
      title: '최소 구매 금액',
      dataIndex: 'min_order_amount',
      key: 'min_order_amount',
      render: (amount: number) => amount > 0 ? `${amount.toLocaleString()}원` : '-',
      align: 'right',
      width: 130,
    },
    {
      title: '유효 기간',
      key: 'valid_period',
      render: (_, record: DiscountPolicy) => {
        const from = new Date(record.valid_from).toLocaleDateString('ko-KR')
        const to = new Date(record.valid_to).toLocaleDateString('ko-KR')
        return `${from} ~ ${to}`
      },
      width: 200,
    },
    {
      title: '상태',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        const statusInfo = statusMap[status]
        return (
          <Tag color={statusInfo?.color || 'default'}>
            {statusInfo?.label || status}
          </Tag>
        )
      },
      width: 100,
    },
    {
      title: '생성 일시',
      dataIndex: 'created_at',
      key: 'created_at',
      sorter: (a, b) => new Date(a.created_at).getTime() - new Date(b.created_at).getTime(),
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
      width: 160,
    },
  ]

  // 폼 렌더링 함수 (등록/수정 공통)
  const renderPolicyForm = (form: ReturnType<typeof Form.useForm>[0]) => (
    <Form
      form={form}
      layout="vertical"
      initialValues={{
        discount_type: 'RATE',
        target_type: 'ORDER',
        min_order_amount: 0,
        status: 'ACTIVE'
      }}
    >
      <Form.Item
        label="할인 정책명"
        name="discount_name"
        rules={[
          { required: true, message: '할인 정책명을 입력하세요' },
          { max: 100, message: '할인 정책명은 최대 100자까지 입력 가능합니다.' }
        ]}
      >
        <Input placeholder="할인 정책명을 입력하세요" maxLength={100} />
      </Form.Item>

      <Form.Item
        label="할인 유형"
        name="discount_type"
        rules={[{ required: true, message: '할인 유형을 선택하세요' }]}
      >
        <Select>
          <Option value="FIXED">정액 할인</Option>
          <Option value="RATE">정률 할인</Option>
        </Select>
      </Form.Item>

      <Form.Item
        noStyle
        shouldUpdate={(prevValues, currentValues) => prevValues.discount_type !== currentValues.discount_type}
      >
        {({ getFieldValue }) => {
          const discountType = getFieldValue('discount_type')
          return (
            <Form.Item
              label={discountType === 'RATE' ? '할인율 (%)' : '할인 금액 (원)'}
              name="discount_value"
              rules={[
                { required: true, message: discountType === 'RATE' ? '할인율을 입력하세요' : '할인 금액을 입력하세요' },
                { type: 'number', min: 1, max: discountType === 'RATE' ? 100 : undefined, message: discountType === 'RATE' ? '할인율은 1~100% 사이여야 합니다' : '할인 금액은 1원 이상이어야 합니다' }
              ]}
            >
              <InputNumber
                style={{ width: '100%' }}
                placeholder={discountType === 'RATE' ? '할인율을 입력하세요 (1~100)' : '할인 금액을 입력하세요'}
                min={1}
                max={discountType === 'RATE' ? 100 : undefined}
                formatter={discountType === 'RATE' ? (value) => `${value}%` : (value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                parser={discountType === 'RATE' ? (value) => Number(value!.replace('%', '')) as never : (value) => Number(value!.replace(/\$\s?|(,*)/g, '')) as never}
              />
            </Form.Item>
          )
        }}
      </Form.Item>

      <Form.Item
        noStyle
        shouldUpdate={(prevValues, currentValues) => prevValues.discount_type !== currentValues.discount_type}
      >
        {({ getFieldValue }) => {
          const discountType = getFieldValue('discount_type')
          return discountType === 'RATE' ? (
            <Form.Item
              label="최대 할인 금액 (원)"
              name="max_discount_amount"
              rules={[
                { type: 'number', min: 1, message: '최대 할인 금액은 1원 이상이어야 합니다' }
              ]}
            >
              <InputNumber
                style={{ width: '100%' }}
                placeholder="최대 할인 금액을 입력하세요 (선택사항)"
                min={1}
                formatter={(value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                parser={(value) => Number(value!.replace(/\$\s?|(,*)/g, '')) as never}
              />
            </Form.Item>
          ) : null
        }}
      </Form.Item>

      <Form.Item
        label="적용 대상"
        name="target_type"
        rules={[{ required: true, message: '적용 대상을 선택하세요' }]}
      >
        <Select>
          <Option value="PRODUCT">상품</Option>
          <Option value="CATEGORY">카테고리</Option>
          <Option value="ORDER">주문</Option>
        </Select>
      </Form.Item>

      <Form.Item
        noStyle
        shouldUpdate={(prevValues, currentValues) => prevValues.target_type !== currentValues.target_type}
      >
        {({ getFieldValue }) => {
          const targetType = getFieldValue('target_type')
          if (targetType === 'ORDER') {
            return (
              <Form.Item
                label="최소 구매 금액 (원)"
                name="min_order_amount"
                rules={[
                  { required: true, message: '최소 구매 금액을 입력하세요' },
                  { type: 'number', min: 0, message: '최소 구매 금액은 0원 이상이어야 합니다' }
                ]}
                tooltip="주문 금액이 이 금액 이상일 때 할인 정책이 적용됩니다"
              >
                <InputNumber
                  style={{ width: '100%' }}
                  placeholder="최소 구매 금액을 입력하세요"
                  min={0}
                  formatter={(value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                  parser={(value) => Number(value!.replace(/\$\s?|(,*)/g, '')) as never}
                />
              </Form.Item>
            )
          } else {
            return (
              <>
                <Form.Item
                  label={targetType === 'PRODUCT' ? '상품 ID' : '카테고리 ID'}
                  name="target_id"
                  rules={[
                    { required: true, message: `${targetType === 'PRODUCT' ? '상품' : '카테고리'} ID를 입력하세요` },
                    { type: 'number', min: 1, message: 'ID는 1 이상이어야 합니다' }
                  ]}
                >
                  <InputNumber
                    style={{ width: '100%' }}
                    placeholder={`${targetType === 'PRODUCT' ? '상품' : '카테고리'} ID를 입력하세요`}
                    min={1}
                  />
                </Form.Item>
                <Form.Item
                  label="최소 구매 금액 (원)"
                  name="min_order_amount"
                  rules={[
                    { type: 'number', min: 0, message: '최소 구매 금액은 0원 이상이어야 합니다' }
                  ]}
                >
                  <InputNumber
                    style={{ width: '100%' }}
                    placeholder="최소 구매 금액을 입력하세요 (선택사항)"
                    min={0}
                    formatter={(value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                    parser={(value) => Number(value!.replace(/\$\s?|(,*)/g, '')) as never}
                  />
                </Form.Item>
              </>
            )
          }
        }}
      </Form.Item>

      <Form.Item
        label="유효 기간"
        name="valid_period"
        rules={[{ required: true, message: '유효 기간을 선택하세요' }]}
      >
        <RangePicker
          style={{ width: '100%' }}
          showTime={{ format: 'HH:mm' }}
          format="YYYY-MM-DD HH:mm"
          placeholder={['시작일시', '종료일시']}
        />
      </Form.Item>

      <Form.Item
        label="상태"
        name="status"
        rules={[{ required: true, message: '상태를 선택하세요' }]}
      >
        <Select>
          <Option value="ACTIVE">활성</Option>
          <Option value="INACTIVE">비활성</Option>
          <Option value="EXPIRED">만료</Option>
        </Select>
      </Form.Item>
    </Form>
  )

  return (
    <div className="admin-discount-policy-manage">
      <div className="discount-policy-manage-container">
        <div className="policy-list-header">
          <h2>할인 정책 관리</h2>
        </div>

        <div className="policy-list-filters">
          <div className="filter-inputs">
            <Space size="middle">
              <Input
                placeholder="할인 정책명 검색"
                allowClear
                style={{ width: 250 }}
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
                onPressEnter={handleSearch}
              />
              <Select
                placeholder="상태 선택"
                allowClear
                style={{ width: 150 }}
                value={searchStatus}
                onChange={(value) => setSearchStatus(value)}
              >
                <Option value="ACTIVE">활성</Option>
                <Option value="INACTIVE">비활성</Option>
                <Option value="EXPIRED">만료</Option>
              </Select>
            </Space>
          </div>
          <div className="filter-actions">
            <Space>
              <Button onClick={handleReset}>초기화</Button>
              <Button type="primary" onClick={handleSearch}>
                검색
              </Button>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={handleRegister}
                style={{ backgroundColor: '#FFC107', borderColor: '#FFC107', color: '#343A40', fontWeight: 600 }}
              >
                할인 정책 등록
              </Button>
            </Space>
          </div>
        </div>

        <Table
          columns={columns}
          dataSource={policies}
          rowKey="discount_id"
          loading={loading}
          scroll={{ x: 'max-content' }}
          pagination={{
            current: currentPage,
            pageSize: pageSize,
            total: totalElements,
            showSizeChanger: true,
            showTotal: (total) => `총 ${total}개`,
            onChange: (page, size) => {
              fetchPolicies(searchKeyword, searchStatus, page, size)
            },
          }}
        />

        {/* 할인 정책 등록 모달 */}
        <Modal
          title="할인 정책 등록"
          open={isRegisterModalVisible}
          onCancel={handleRegisterModalClose}
          onOk={handleRegisterSave}
          okText="등록"
          cancelText="취소"
          okButtonProps={{
            style: { backgroundColor: '#FFC107', borderColor: '#FFC107', color: '#343A40', fontWeight: 600 }
          }}
          width={700}
        >
          {renderPolicyForm(policyForm)}
        </Modal>

        {/* 할인 정책 상세 조회 및 수정 모달 */}
        <Modal
          title={`할인 정책 상세 - ${selectedPolicy?.discount_name}`}
          open={isDetailModalVisible}
          onCancel={handleDetailModalClose}
          footer={[
            <Button key="cancel" onClick={handleDetailModalClose}>
              취소
            </Button>,
            <Button
              key="save"
              type="primary"
              onClick={handleDetailSave}
              style={{ backgroundColor: '#FFC107', borderColor: '#FFC107', color: '#343A40', fontWeight: 600 }}
            >
              저장
            </Button>
          ]}
          width={700}
        >
          {selectedPolicy && renderPolicyForm(detailForm)}
        </Modal>
      </div>
    </div>
  )
}

export default AdminDiscountPolicyManage

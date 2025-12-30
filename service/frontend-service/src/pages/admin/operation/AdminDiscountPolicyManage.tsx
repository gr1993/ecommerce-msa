import { useState, useEffect } from 'react'
import { Table, Card, Space, Input, Button, Select, Tag, Modal, Form, InputNumber, DatePicker, message, Tabs } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PlusOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import './AdminDiscountPolicyManage.css'

const { Option } = Select
const { RangePicker } = DatePicker

interface DiscountPolicy {
  discount_id: string
  discount_name: string
  discount_type: 'FIXED' | 'RATE'
  discount_value: number
  target_type: 'PRODUCT' | 'CATEGORY' | 'ORDER'
  target_id?: number
  target_name?: string
  min_order_amount: number
  max_discount_amount?: number
  valid_from: string
  valid_to: string
  status: 'ACTIVE' | 'INACTIVE' | 'EXPIRED'
  created_at: string
  updated_at: string
}

interface DiscountUsage {
  usage_id: string
  discount_id: string
  user_id: string
  user_name: string
  order_id: string
  order_number: string
  discount_amount: number
  used_at: string
}

function AdminDiscountPolicyManage() {
  const [policies, setPolicies] = useState<DiscountPolicy[]>([])
  const [filteredPolicies, setFilteredPolicies] = useState<DiscountPolicy[]>([])
  const [searchPolicyName, setSearchPolicyName] = useState('')
  const [searchStatus, setSearchStatus] = useState<string | undefined>(undefined)
  const [isRegisterModalVisible, setIsRegisterModalVisible] = useState(false)
  const [isDetailModalVisible, setIsDetailModalVisible] = useState(false)
  const [selectedPolicy, setSelectedPolicy] = useState<DiscountPolicy | null>(null)
  const [policyForm] = Form.useForm()
  const [detailForm] = Form.useForm()
  const [usageSearchText, setUsageSearchText] = useState('')

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

  // 할인 정책 데이터 로드 (샘플 데이터)
  useEffect(() => {
    // TODO: API 호출로 할인 정책 데이터 로드
    const samplePolicies: DiscountPolicy[] = [
      {
        discount_id: '1',
        discount_name: '신상품 10% 할인',
        discount_type: 'RATE',
        discount_value: 10,
        target_type: 'PRODUCT',
        target_id: 1,
        target_name: '노트북',
        min_order_amount: 0,
        max_discount_amount: 50000,
        valid_from: '2024-01-01 00:00:00',
        valid_to: '2024-12-31 23:59:59',
        status: 'ACTIVE',
        created_at: '2024-01-01 00:00:00',
        updated_at: '2024-01-15 10:00:00'
      },
      {
        discount_id: '2',
        discount_name: '의류 카테고리 5000원 할인',
        discount_type: 'FIXED',
        discount_value: 5000,
        target_type: 'CATEGORY',
        target_id: 2,
        target_name: '의류',
        min_order_amount: 30000,
        max_discount_amount: undefined,
        valid_from: '2024-01-15 00:00:00',
        valid_to: '2024-02-15 23:59:59',
        status: 'ACTIVE',
        created_at: '2024-01-15 00:00:00',
        updated_at: '2024-01-15 10:00:00'
      },
      {
        discount_id: '3',
        discount_name: '전체 주문 5% 할인',
        discount_type: 'RATE',
        discount_value: 5,
        target_type: 'ORDER',
        target_id: undefined,
        target_name: '전체',
        min_order_amount: 50000,
        max_discount_amount: 10000,
        valid_from: '2024-01-01 00:00:00',
        valid_to: '2024-03-31 23:59:59',
        status: 'ACTIVE',
        created_at: '2024-01-01 00:00:00',
        updated_at: '2024-01-01 00:00:00'
      },
      {
        discount_id: '4',
        discount_name: '여름 특가 20% 할인',
        discount_type: 'RATE',
        discount_value: 20,
        target_type: 'ORDER',
        target_id: undefined,
        target_name: '전체',
        min_order_amount: 100000,
        max_discount_amount: 30000,
        valid_from: '2024-06-01 00:00:00',
        valid_to: '2024-08-31 23:59:59',
        status: 'INACTIVE',
        created_at: '2024-05-20 00:00:00',
        updated_at: '2024-05-20 00:00:00'
      }
    ]
    setPolicies(samplePolicies)
  }, [])

  // 필터링된 데이터
  useEffect(() => {
    const filtered = policies.filter((policy) => {
      const nameMatch = !searchPolicyName || 
        policy.discount_name.toLowerCase().includes(searchPolicyName.toLowerCase())
      const statusMatch = !searchStatus || policy.status === searchStatus
      return nameMatch && statusMatch
    })
    setFilteredPolicies(filtered)
  }, [searchPolicyName, searchStatus, policies])

  const handleSearch = () => {
    // 검색 버튼 클릭 시 필터 적용 (현재는 실시간 필터링이므로 별도 로직 불필요)
  }

  const handleReset = () => {
    setSearchPolicyName('')
    setSearchStatus(undefined)
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
      
      const newPolicy: DiscountPolicy = {
        discount_id: `discount_${Date.now()}`,
        discount_name: values.discount_name,
        discount_type: values.discount_type,
        discount_value: values.discount_value,
        target_type: values.target_type,
        target_id: values.target_type === 'ORDER' ? undefined : values.target_id,
        target_name: values.target_type === 'ORDER' ? '전체' : values.target_name || undefined,
        min_order_amount: values.min_order_amount || 0,
        max_discount_amount: values.discount_type === 'RATE' ? values.max_discount_amount : undefined,
        valid_from: values.valid_period[0].format('YYYY-MM-DD HH:mm:ss'),
        valid_to: values.valid_period[1].format('YYYY-MM-DD HH:mm:ss'),
        status: values.status,
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString()
      }

      // TODO: API 호출로 할인 정책 등록
      setPolicies(prev => [newPolicy, ...prev])
      message.success('할인 정책이 등록되었습니다.')
      setIsRegisterModalVisible(false)
      policyForm.resetFields()
    } catch (error) {
      console.error('Validation failed:', error)
    }
  }

  const handleRegisterModalClose = () => {
    setIsRegisterModalVisible(false)
    policyForm.resetFields()
  }

  // 할인 정책 상세 조회
  const handlePolicyClick = (policy: DiscountPolicy) => {
    setSelectedPolicy(policy)
    setUsageSearchText('') // 검색어 초기화
    
    // 폼에 할인 정책 정보 설정
    detailForm.setFieldsValue({
      discount_name: policy.discount_name,
      discount_type: policy.discount_type,
      discount_value: policy.discount_value,
      target_type: policy.target_type,
      target_id: policy.target_id,
      target_name: policy.target_name,
      min_order_amount: policy.min_order_amount,
      max_discount_amount: policy.max_discount_amount,
      valid_period: [dayjs(policy.valid_from), dayjs(policy.valid_to)],
      status: policy.status
    })
    
    setIsDetailModalVisible(true)
  }

  const handleDetailModalClose = () => {
    setIsDetailModalVisible(false)
    setSelectedPolicy(null)
    setUsageSearchText('')
    detailForm.resetFields()
  }

  const handleDetailSave = async () => {
    if (!selectedPolicy) return

    try {
      const values = await detailForm.validateFields()
      
      // TODO: API 호출로 할인 정책 정보 업데이트
      setPolicies(prev =>
        prev.map(policy =>
          policy.discount_id === selectedPolicy.discount_id
            ? {
                ...policy,
                discount_name: values.discount_name,
                discount_type: values.discount_type,
                discount_value: values.discount_value,
                target_type: values.target_type,
                target_id: values.target_type === 'ORDER' ? undefined : values.target_id,
                target_name: values.target_type === 'ORDER' ? '전체' : values.target_name || undefined,
                min_order_amount: values.min_order_amount || 0,
                max_discount_amount: values.discount_type === 'RATE' ? values.max_discount_amount : undefined,
                valid_from: values.valid_period[0].format('YYYY-MM-DD HH:mm:ss'),
                valid_to: values.valid_period[1].format('YYYY-MM-DD HH:mm:ss'),
                status: values.status,
                updated_at: new Date().toISOString()
              }
            : policy
        )
      )

      message.success('할인 정책 정보가 수정되었습니다.')
      setIsDetailModalVisible(false)
      setSelectedPolicy(null)
      detailForm.resetFields()
    } catch (error) {
      console.error('Validation failed:', error)
    }
  }

  // 할인 정책 사용자 리스트 (샘플 데이터)
  const [discountUsages] = useState<DiscountUsage[]>([
    {
      usage_id: '1',
      discount_id: '3',
      user_id: '1',
      user_name: '홍길동',
      order_id: '1',
      order_number: 'ORD-2024-001',
      discount_amount: 5000,
      used_at: '2024-01-10 14:30:00'
    },
    {
      usage_id: '2',
      discount_id: '3',
      user_id: '2',
      user_name: '김철수',
      order_id: '2',
      order_number: 'ORD-2024-002',
      discount_amount: 7500,
      used_at: '2024-01-12 10:20:00'
    },
    {
      usage_id: '3',
      discount_id: '3',
      user_id: '3',
      user_name: '이영희',
      order_id: '3',
      order_number: 'ORD-2024-003',
      discount_amount: 10000,
      used_at: '2024-01-14 16:45:00'
    }
  ])

  const getDiscountUsages = (discountId: string) => {
    const usages = discountUsages.filter(usage => usage.discount_id === discountId)
    
    // 검색 필터 적용
    if (!usageSearchText) {
      return usages
    }
    
    return usages.filter(usage => 
      usage.user_name.toLowerCase().includes(usageSearchText.toLowerCase()) ||
      usage.order_number.toLowerCase().includes(usageSearchText.toLowerCase())
    )
  }

  const handleUsageSearch = () => {
    // 검색 버튼 클릭 시 필터 적용 (현재는 실시간 필터링이므로 별도 로직 불필요)
  }

  const handleUsageSearchReset = () => {
    setUsageSearchText('')
  }

  const usageColumns: ColumnsType<DiscountUsage> = [
    {
      title: '사용자명',
      dataIndex: 'user_name',
      key: 'user_name',
      width: 120,
    },
    {
      title: '주문 번호',
      dataIndex: 'order_number',
      key: 'order_number',
      width: 150,
    },
    {
      title: '할인 금액',
      dataIndex: 'discount_amount',
      key: 'discount_amount',
      render: (amount: number) => (
        <strong style={{ color: '#007BFF' }}>
          {amount.toLocaleString()}원
        </strong>
      ),
      align: 'right',
      width: 130,
    },
    {
      title: '사용 일시',
      dataIndex: 'used_at',
      key: 'used_at',
      sorter: (a, b) => new Date(a.used_at).getTime() - new Date(b.used_at).getTime(),
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
        return `${targetTypeMap[record.target_type]}${record.target_name ? `: ${record.target_name}` : ''}`
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
      filters: [
        { text: '활성', value: 'ACTIVE' },
        { text: '비활성', value: 'INACTIVE' },
        { text: '만료', value: 'EXPIRED' },
      ],
      onFilter: (value, record) => record.status === value,
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
                value={searchPolicyName}
                onChange={(e) => setSearchPolicyName(e.target.value)}
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
          dataSource={filteredPolicies}
          rowKey="discount_id"
          scroll={{ x: 'max-content' }}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total) => `총 ${total}개`,
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
          <Form
            form={policyForm}
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
                      { type: 'number', min: discountType === 'RATE' ? 1 : 1, max: discountType === 'RATE' ? 100 : undefined, message: discountType === 'RATE' ? '할인율은 1~100% 사이여야 합니다' : '할인 금액은 1원 이상이어야 합니다' }
                    ]}
                  >
                    <InputNumber
                      style={{ width: '100%' }}
                      placeholder={discountType === 'RATE' ? '할인율을 입력하세요 (1~100)' : '할인 금액을 입력하세요'}
                      min={1}
                      max={discountType === 'RATE' ? 100 : undefined}
                      formatter={discountType === 'RATE' ? (value) => `${value}%` : (value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                      parser={discountType === 'RATE' ? (value) => value!.replace('%', '') : (value) => value!.replace(/\$\s?|(,*)/g, '')}
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
                      parser={(value) => value!.replace(/\$\s?|(,*)/g, '')}
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
                        parser={(value) => value!.replace(/\$\s?|(,*)/g, '')}
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
                        label={targetType === 'PRODUCT' ? '상품명' : '카테고리명'}
                        name="target_name"
                        rules={[
                          { max: 200, message: `${targetType === 'PRODUCT' ? '상품명' : '카테고리명'}은 최대 200자까지 입력 가능합니다.` }
                        ]}
                      >
                        <Input placeholder={`${targetType === 'PRODUCT' ? '상품명' : '카테고리명'}을 입력하세요 (선택사항)`} maxLength={200} />
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
                          parser={(value) => value!.replace(/\$\s?|(,*)/g, '')}
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
          width={900}
        >
          {selectedPolicy && (
            <Tabs
              defaultActiveKey="info"
              items={[
                {
                  key: 'info',
                  label: '할인 정책 정보',
                  children: (
                    <Form
                      form={detailForm}
                      layout="vertical"
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
                                { type: 'number', min: discountType === 'RATE' ? 1 : 1, max: discountType === 'RATE' ? 100 : undefined, message: discountType === 'RATE' ? '할인율은 1~100% 사이여야 합니다' : '할인 금액은 1원 이상이어야 합니다' }
                              ]}
                            >
                              <InputNumber
                                style={{ width: '100%' }}
                                placeholder={discountType === 'RATE' ? '할인율을 입력하세요 (1~100)' : '할인 금액을 입력하세요'}
                                min={1}
                                max={discountType === 'RATE' ? 100 : undefined}
                                formatter={discountType === 'RATE' ? (value) => `${value}%` : (value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                                parser={discountType === 'RATE' ? (value) => value!.replace('%', '') : (value) => value!.replace(/\$\s?|(,*)/g, '')}
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
                                parser={(value) => value!.replace(/\$\s?|(,*)/g, '')}
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
                                  parser={(value) => value!.replace(/\$\s?|(,*)/g, '')}
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
                                  label={targetType === 'PRODUCT' ? '상품명' : '카테고리명'}
                                  name="target_name"
                                  rules={[
                                    { max: 200, message: `${targetType === 'PRODUCT' ? '상품명' : '카테고리명'}은 최대 200자까지 입력 가능합니다.` }
                                  ]}
                                >
                                  <Input placeholder={`${targetType === 'PRODUCT' ? '상품명' : '카테고리명'}을 입력하세요 (선택사항)`} maxLength={200} />
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
                                    parser={(value) => value!.replace(/\$\s?|(,*)/g, '')}
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
                },
                {
                  key: 'usage',
                  label: `사용 내역 (${getDiscountUsages(selectedPolicy.discount_id).length}건)`,
                  children: (
                    <div>
                      <div style={{ marginBottom: '1rem' }}>
                        <Space>
                          <Input
                            placeholder="사용자명 또는 주문 번호 검색"
                            allowClear
                            style={{ width: 250 }}
                            value={usageSearchText}
                            onChange={(e) => setUsageSearchText(e.target.value)}
                            onPressEnter={handleUsageSearch}
                          />
                          <Button onClick={handleUsageSearchReset}>초기화</Button>
                          <Button type="primary" onClick={handleUsageSearch}>
                            검색
                          </Button>
                        </Space>
                      </div>
                      <Table
                        columns={usageColumns}
                        dataSource={getDiscountUsages(selectedPolicy.discount_id)}
                        rowKey="usage_id"
                        pagination={{
                          pageSize: 10,
                          showSizeChanger: true,
                          showTotal: (total) => `총 ${total}건`,
                        }}
                        size="small"
                      />
                    </div>
                  )
                }
              ]}
            />
          )}
        </Modal>
      </div>
    </div>
  )
}

export default AdminDiscountPolicyManage


import { useState, useEffect } from 'react'
import { Table, Space, Input, Button, Select, Tag, Modal, Form, InputNumber, DatePicker, Switch, message, Tabs } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PlusOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import {
  getAdminCoupons,
  getAdminCouponDetail,
  createAdminCoupon,
  updateAdminCoupon,
} from '../../../api/couponApi'
import type { Coupon, CouponDetail, CouponUsage } from '../../../api/couponApi'
import './AdminCouponManage.css'

const { Option } = Select
const { RangePicker } = DatePicker

function AdminCouponManage() {
  const [coupons, setCoupons] = useState<Coupon[]>([])
  const [loading, setLoading] = useState(false)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [searchStatus, setSearchStatus] = useState<string | undefined>(undefined)
  const [currentPage, setCurrentPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [totalElements, setTotalElements] = useState(0)
  const [isRegisterModalVisible, setIsRegisterModalVisible] = useState(false)
  const [isDetailModalVisible, setIsDetailModalVisible] = useState(false)
  const [selectedCoupon, setSelectedCoupon] = useState<CouponDetail | null>(null)
  const [couponForm] = Form.useForm()
  const [detailForm] = Form.useForm()

  const discountTypeMap: Record<string, string> = {
    RATE: '정률 할인',
    FIXED: '정액 할인'
  }

  const statusMap: Record<string, { label: string; color: string }> = {
    ACTIVE: { label: '활성', color: 'green' },
    INACTIVE: { label: '비활성', color: 'red' },
    EXPIRED: { label: '만료', color: 'default' },
  }

  // 쿠폰 목록 조회
  const fetchCoupons = async (keyword?: string, status?: string, page: number = 1, size: number = pageSize) => {
    setLoading(true)
    try {
      const data = await getAdminCoupons(keyword || undefined, status || undefined, page - 1, size)
      setCoupons(data.content)
      setTotalElements(data.totalElements)
      setCurrentPage(page)
      setPageSize(size)
    } catch (error) {
      message.error(error instanceof Error ? error.message : '쿠폰 목록을 불러오는데 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchCoupons()
  }, [])

  const handleSearch = () => {
    fetchCoupons(searchKeyword, searchStatus, 1)
  }

  const handleReset = () => {
    setSearchKeyword('')
    setSearchStatus(undefined)
    fetchCoupons(undefined, undefined, 1)
  }

  const handleRegister = () => {
    couponForm.resetFields()
    setIsRegisterModalVisible(true)
  }

  const handleRegisterSave = async () => {
    try {
      const values = await couponForm.validateFields()

      await createAdminCoupon({
        couponCode: values.coupon_code,
        couponName: values.coupon_name,
        discountType: values.discount_type,
        discountValue: values.discount_value,
        minOrderAmount: values.min_order_amount || undefined,
        maxDiscountAmount: values.discount_type === 'RATE' ? values.max_discount_amount : undefined,
        validFrom: values.valid_period[0].format('YYYY-MM-DDTHH:mm:ss'),
        validTo: values.valid_period[1].format('YYYY-MM-DDTHH:mm:ss'),
        status: values.is_active ? 'ACTIVE' : 'INACTIVE',
      })

      message.success('쿠폰이 등록되었습니다.')
      setIsRegisterModalVisible(false)
      couponForm.resetFields()
      fetchCoupons(searchKeyword, searchStatus, 1)
    } catch (error) {
      if (error instanceof Error && error.message !== 'Validation failed') {
        message.error(error.message)
      }
    }
  }

  const handleRegisterModalClose = () => {
    setIsRegisterModalVisible(false)
    couponForm.resetFields()
  }

  // 쿠폰 상세 조회
  const handleCouponClick = async (coupon: Coupon) => {
    try {
      const detail = await getAdminCouponDetail(coupon.coupon_id)
      setSelectedCoupon(detail)

      detailForm.setFieldsValue({
        coupon_code: detail.coupon_code,
        coupon_name: detail.coupon_name,
        discount_type: detail.discount_type,
        discount_value: detail.discount_value,
        min_order_amount: detail.min_order_amount || undefined,
        max_discount_amount: detail.max_discount_amount || undefined,
        valid_period: [dayjs(detail.valid_from), dayjs(detail.valid_to)],
        is_active: detail.status === 'ACTIVE',
      })

      setIsDetailModalVisible(true)
    } catch (error) {
      message.error(error instanceof Error ? error.message : '쿠폰 상세 정보를 불러오는데 실패했습니다.')
    }
  }

  const handleDetailModalClose = () => {
    setIsDetailModalVisible(false)
    setSelectedCoupon(null)
    detailForm.resetFields()
  }

  const handleDetailSave = async () => {
    if (!selectedCoupon) return

    try {
      const values = await detailForm.validateFields()

      await updateAdminCoupon(selectedCoupon.coupon_id, {
        couponCode: values.coupon_code,
        couponName: values.coupon_name,
        discountType: values.discount_type,
        discountValue: values.discount_value,
        minOrderAmount: values.min_order_amount || undefined,
        maxDiscountAmount: values.discount_type === 'RATE' ? values.max_discount_amount : undefined,
        validFrom: values.valid_period[0].format('YYYY-MM-DDTHH:mm:ss'),
        validTo: values.valid_period[1].format('YYYY-MM-DDTHH:mm:ss'),
        status: values.is_active ? 'ACTIVE' : 'INACTIVE',
      })

      message.success('쿠폰 정보가 수정되었습니다.')
      setIsDetailModalVisible(false)
      setSelectedCoupon(null)
      detailForm.resetFields()
      fetchCoupons(searchKeyword, searchStatus, currentPage, pageSize)
    } catch (error) {
      if (error instanceof Error && error.message !== 'Validation failed') {
        message.error(error.message)
      }
    }
  }

  const couponStatusMap: Record<string, { label: string; color: string }> = {
    ISSUED: { label: '발급됨', color: 'blue' },
    USED: { label: '사용됨', color: 'green' },
    EXPIRED: { label: '만료됨', color: 'default' },
  }

  const usageColumns: ColumnsType<CouponUsage> = [
    {
      title: '발급 ID',
      dataIndex: 'user_coupon_id',
      key: 'user_coupon_id',
      width: 80,
    },
    {
      title: '사용자 ID',
      dataIndex: 'user_id',
      key: 'user_id',
      width: 100,
    },
    {
      title: '상태',
      dataIndex: 'coupon_status',
      key: 'coupon_status',
      render: (status: string) => {
        const info = couponStatusMap[status]
        return <Tag color={info?.color || 'default'}>{info?.label || status}</Tag>
      },
      width: 100,
    },
    {
      title: '발급 일시',
      dataIndex: 'issued_at',
      key: 'issued_at',
      sorter: (a, b) => new Date(a.issued_at).getTime() - new Date(b.issued_at).getTime(),
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
    {
      title: '사용 일시',
      dataIndex: 'used_at',
      key: 'used_at',
      render: (date: string | null) => {
        if (!date) return '-'
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

  const columns: ColumnsType<Coupon> = [
    {
      title: '쿠폰 코드',
      dataIndex: 'coupon_code',
      key: 'coupon_code',
      sorter: (a, b) => a.coupon_code.localeCompare(b.coupon_code),
      render: (text: string, record: Coupon) => (
        <a
          onClick={() => handleCouponClick(record)}
          style={{ color: '#007BFF', cursor: 'pointer' }}
        >
          {text}
        </a>
      ),
      width: 150,
    },
    {
      title: '쿠폰명',
      dataIndex: 'coupon_name',
      key: 'coupon_name',
      sorter: (a, b) => a.coupon_name.localeCompare(b.coupon_name),
      render: (text: string, record: Coupon) => (
        <a
          onClick={() => handleCouponClick(record)}
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
        { text: '정률 할인', value: 'RATE' },
        { text: '정액 할인', value: 'FIXED' },
      ],
      onFilter: (value, record) => record.discount_type === value,
      render: (type: string) => discountTypeMap[type] || type,
      width: 120,
    },
    {
      title: '할인 금액',
      key: 'discount',
      render: (_, record: Coupon) => {
        if (record.discount_type === 'RATE') {
          return `${record.discount_value}%${record.max_discount_amount ? ` (최대 ${record.max_discount_amount.toLocaleString()}원)` : ''}`
        } else {
          return `${record.discount_value.toLocaleString()}원`
        }
      },
      width: 180,
    },
    {
      title: '최소 구매 금액',
      dataIndex: 'min_order_amount',
      key: 'min_order_amount',
      render: (amount: number) => amount ? `${amount.toLocaleString()}원` : '-',
      align: 'right',
      width: 130,
    },
    {
      title: '유효 기간',
      key: 'valid_period',
      render: (_, record: Coupon) => {
        const from = new Date(record.valid_from).toLocaleDateString('ko-KR')
        const to = new Date(record.valid_to).toLocaleDateString('ko-KR')
        return `${from} ~ ${to}`
      },
      width: 200,
    },
    {
      title: '발급 수',
      dataIndex: 'issued_count',
      key: 'issued_count',
      align: 'center',
      width: 80,
    },
    {
      title: '상태',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        const info = statusMap[status]
        return <Tag color={info?.color || 'default'}>{info?.label || status}</Tag>
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
  const renderCouponForm = (form: ReturnType<typeof Form.useForm>[0]) => (
    <Form
      form={form}
      layout="vertical"
      initialValues={{
        discount_type: 'RATE',
        is_active: true
      }}
    >
      <Form.Item
        label="쿠폰 코드"
        name="coupon_code"
        rules={[
          { required: true, message: '쿠폰 코드를 입력하세요' },
          { max: 50, message: '쿠폰 코드는 최대 50자까지 입력 가능합니다.' }
        ]}
      >
        <Input placeholder="쿠폰 코드를 입력하세요 (예: WELCOME10)" maxLength={50} />
      </Form.Item>

      <Form.Item
        label="쿠폰명"
        name="coupon_name"
        rules={[
          { required: true, message: '쿠폰명을 입력하세요' },
          { max: 100, message: '쿠폰명은 최대 100자까지 입력 가능합니다.' }
        ]}
      >
        <Input placeholder="쿠폰명을 입력하세요" maxLength={100} />
      </Form.Item>

      <Form.Item
        label="할인 유형"
        name="discount_type"
        rules={[{ required: true, message: '할인 유형을 선택하세요' }]}
      >
        <Select>
          <Option value="RATE">정률 할인</Option>
          <Option value="FIXED">정액 할인</Option>
        </Select>
      </Form.Item>

      <Form.Item
        noStyle
        shouldUpdate={(prevValues, currentValues) => prevValues.discount_type !== currentValues.discount_type}
      >
        {({ getFieldValue }) => {
          const discountType = getFieldValue('discount_type')
          return (
            <>
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

              {discountType === 'RATE' && (
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
              )}
            </>
          )
        }}
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
        label="활성화"
        name="is_active"
        valuePropName="checked"
      >
        <Switch checkedChildren="활성" unCheckedChildren="비활성" />
      </Form.Item>
    </Form>
  )

  return (
    <div className="admin-coupon-manage">
      <div className="coupon-manage-container">
        <div className="coupon-list-header">
          <h2>쿠폰 관리</h2>
        </div>

        <div className="coupon-list-filters">
          <div className="filter-inputs">
            <Space size="middle">
              <Input
                placeholder="쿠폰 코드 또는 쿠폰명 검색"
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
                쿠폰 등록
              </Button>
            </Space>
          </div>
        </div>

        <Table
          columns={columns}
          dataSource={coupons}
          rowKey="coupon_id"
          loading={loading}
          scroll={{ x: 'max-content' }}
          pagination={{
            current: currentPage,
            pageSize: pageSize,
            total: totalElements,
            showSizeChanger: true,
            showTotal: (total) => `총 ${total}개`,
            onChange: (page, size) => {
              fetchCoupons(searchKeyword, searchStatus, page, size)
            },
          }}
        />

        {/* 쿠폰 등록 모달 */}
        <Modal
          title="쿠폰 등록"
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
          {renderCouponForm(couponForm)}
        </Modal>

        {/* 쿠폰 상세 조회 및 수정 모달 */}
        <Modal
          title={`쿠폰 상세 - ${selectedCoupon?.coupon_code}`}
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
          {selectedCoupon && (
            <Tabs
              defaultActiveKey="info"
              items={[
                {
                  key: 'info',
                  label: '쿠폰 정보',
                  children: renderCouponForm(detailForm),
                },
                {
                  key: 'usage',
                  label: `발급 내역 (${selectedCoupon.user_coupons.length}건)`,
                  children: (
                    <Table
                      columns={usageColumns}
                      dataSource={selectedCoupon.user_coupons}
                      rowKey="user_coupon_id"
                      pagination={{
                        pageSize: 10,
                        showSizeChanger: true,
                        showTotal: (total) => `총 ${total}건`,
                      }}
                      size="small"
                    />
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

export default AdminCouponManage

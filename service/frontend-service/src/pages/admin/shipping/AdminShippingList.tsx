import { useState, useEffect, useCallback } from 'react'
import { Table, Space, Input, Button, Select, Tag, Modal, Form, message } from 'antd'
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table'
import { PlusOutlined } from '@ant-design/icons'
import OrderDetailModal, { type Order, type OrderItem, type OrderShipping } from '../order/OrderDetailModal'
import { getAdminShippings, type AdminShippingResponse } from '../../../api/shippingApi'
import './AdminShippingList.css'

const { Option } = Select

function AdminShippingList() {
  const [shippings, setShippings] = useState<AdminShippingResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 20,
    total: 0
  })
  const [searchShippingStatus, setSearchShippingStatus] = useState<string | undefined>(undefined)
  const [searchTrackingNumber, setSearchTrackingNumber] = useState('')
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null)
  const [orderItems, setOrderItems] = useState<OrderItem[]>([])
  const [orderShipping, setOrderShipping] = useState<OrderShipping | null>(null)
  const [isModalVisible, setIsModalVisible] = useState(false)
  const [isTrackingModalVisible, setIsTrackingModalVisible] = useState(false)
  const [selectedShipping, setSelectedShipping] = useState<AdminShippingResponse | null>(null)
  const [trackingForm] = Form.useForm()

  const shippingStatusMap: Record<string, { label: string; color: string }> = {
    READY: { label: '배송 준비', color: 'blue' },
    SHIPPING: { label: '배송 중', color: 'orange' },
    DELIVERED: { label: '배송 완료', color: 'green' },
    RETURNED: { label: '반품', color: 'red' }
  }

  const deliveryServiceStatusMap: Record<string, { label: string; color: string }> = {
    NOT_SENT: { label: '미전송', color: 'default' },
    SENT: { label: '전송 완료', color: 'blue' },
    IN_TRANSIT: { label: '운송 중', color: 'orange' },
    DELIVERED: { label: '배송 완료', color: 'green' }
  }

  // 배송 데이터 로드
  const fetchShippings = useCallback(async (
    page: number = 0,
    size: number = 20,
    status?: string,
    trackingNum?: string
  ) => {
    setLoading(true)
    try {
      const data = await getAdminShippings(status, trackingNum, page, size)
      setShippings(data.content)
      setPagination({
        current: data.page + 1,
        pageSize: data.size,
        total: data.totalElements
      })
    } catch (error) {
      console.error('Failed to fetch shippings:', error)
      message.error(error instanceof Error ? error.message : '배송 목록을 불러오는데 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchShippings(0, pagination.pageSize, searchShippingStatus, searchTrackingNumber)
  }, [])

  const handleSearch = () => {
    fetchShippings(0, pagination.pageSize, searchShippingStatus, searchTrackingNumber)
  }

  const handleReset = () => {
    setSearchShippingStatus(undefined)
    setSearchTrackingNumber('')
    fetchShippings(0, pagination.pageSize)
  }

  const handleTableChange = (paginationConfig: TablePaginationConfig) => {
    const page = (paginationConfig.current || 1) - 1
    const size = paginationConfig.pageSize || 20
    fetchShippings(page, size, searchShippingStatus, searchTrackingNumber)
  }

  // 주문 상세 조회
  const handleOrderClick = async (orderId: number) => {
    // TODO: API 호출로 주문 상세 데이터 로드
    const shippingData = shippings.find(s => s.orderId === orderId)
    const sampleOrder: Order = {
      order_id: String(orderId),
      order_number: shippingData?.orderNumber || `ORD-${orderId}`,
      user_id: '1',
      user_name: '홍길동',
      order_status: 'SHIPPING',
      total_product_amount: 150000,
      total_discount_amount: 10000,
      total_payment_amount: 140000,
      ordered_at: '2024-01-15 10:30:00',
      updated_at: '2024-01-15 10:35:00'
    }

    const sampleOrderItems: OrderItem[] = [
      {
        order_item_id: '1',
        order_id: String(orderId),
        product_id: '1',
        product_name: '노트북',
        product_code: 'PRD-001',
        quantity: 1,
        unit_price: 150000,
        total_price: 150000,
        created_at: sampleOrder.ordered_at
      }
    ]

    const sampleShipping: OrderShipping = {
      shipping_id: shippingData?.shippingId ? String(shippingData.shippingId) : '1',
      order_id: String(orderId),
      receiver_name: shippingData?.receiverName || '홍길동',
      receiver_phone: shippingData?.receiverPhone || '010-1234-5678',
      address: shippingData?.address || '서울특별시 강남구 테헤란로 123',
      postal_code: shippingData?.postalCode,
      shipping_status: shippingData?.shippingStatus || 'READY',
      created_at: shippingData?.createdAt || sampleOrder.ordered_at
    }

    setSelectedOrder(sampleOrder)
    setOrderItems(sampleOrderItems)
    setOrderShipping(sampleShipping)
    setIsModalVisible(true)
  }

  const handleModalClose = () => {
    setIsModalVisible(false)
    setSelectedOrder(null)
    setOrderItems([])
    setOrderShipping(null)
  }

  const handleOrderSave = async (orderId: string, orderStatus: string, orderMemo: string) => {
    // TODO: API 호출로 주문 상태 및 메모 업데이트
    if (selectedOrder && selectedOrder.order_id === orderId) {
      setSelectedOrder({
        ...selectedOrder,
        order_status: orderStatus as Order['order_status'],
        order_memo: orderMemo
      })
    }
  }

  // 운송장 번호 등록 모달 열기
  const handleTrackingRegister = (shipping: AdminShippingResponse) => {
    setSelectedShipping(shipping)
    trackingForm.setFieldsValue({
      shipping_company: shipping.shippingCompany || '',
      tracking_number: ''
    })
    setIsTrackingModalVisible(true)
  }

  // 운송장 번호 등록 저장
  const handleTrackingSave = async () => {
    if (!selectedShipping) return

    try {
      const values = await trackingForm.validateFields()

      // TODO: API 호출로 운송장 번호 등록
      setShippings(prev =>
        prev.map(shipping =>
          shipping.shippingId === selectedShipping.shippingId
            ? {
                ...shipping,
                shippingCompany: values.shipping_company,
                trackingNumber: values.tracking_number,
                deliveryServiceStatus: 'SENT' as const,
                updatedAt: new Date().toISOString()
              }
            : shipping
        )
      )

      message.success('운송장 번호가 등록되었습니다.')
      setIsTrackingModalVisible(false)
      setSelectedShipping(null)
      trackingForm.resetFields()
    } catch (error) {
      console.error('Validation failed:', error)
    }
  }

  const handleTrackingModalClose = () => {
    setIsTrackingModalVisible(false)
    setSelectedShipping(null)
    trackingForm.resetFields()
  }

  const columns: ColumnsType<AdminShippingResponse> = [
    {
      title: '배송 ID',
      dataIndex: 'shippingId',
      key: 'shippingId',
      width: 100,
    },
    {
      title: '주문 번호',
      dataIndex: 'orderNumber',
      key: 'orderNumber',
      sorter: (a, b) => (a.orderNumber || '').localeCompare(b.orderNumber || ''),
      render: (text: string, record: AdminShippingResponse) => (
        <a
          onClick={() => handleOrderClick(record.orderId)}
          style={{ color: '#007BFF', cursor: 'pointer' }}
        >
          {text}
        </a>
      ),
      width: 180,
    },
    {
      title: '수령인',
      dataIndex: 'receiverName',
      key: 'receiverName',
      width: 100,
    },
    {
      title: '연락처',
      dataIndex: 'receiverPhone',
      key: 'receiverPhone',
      width: 130,
    },
    {
      title: '배송 주소',
      dataIndex: 'address',
      key: 'address',
      ellipsis: true,
      width: 200,
    },
    {
      title: '배송 상태',
      dataIndex: 'shippingStatus',
      key: 'shippingStatus',
      render: (status: string) => {
        const statusInfo = shippingStatusMap[status]
        return (
          <Tag color={statusInfo?.color || 'default'}>
            {statusInfo?.label || status}
          </Tag>
        )
      },
      width: 120,
    },
    {
      title: '배송사',
      dataIndex: 'shippingCompany',
      key: 'shippingCompany',
      render: (company: string | null) => company || <span style={{ color: '#999' }}>-</span>,
      width: 120,
    },
    {
      title: '운송장 번호',
      dataIndex: 'trackingNumber',
      key: 'trackingNumber',
      render: (tracking: string | null, record: AdminShippingResponse) => {
        if (tracking) {
          return tracking
        }
        return (
          <Button
            type="link"
            size="small"
            icon={<PlusOutlined />}
            onClick={() => handleTrackingRegister(record)}
            style={{ padding: 0 }}
          >
            등록
          </Button>
        )
      },
      width: 150,
    },
    {
      title: '배송사 연동 상태',
      dataIndex: 'deliveryServiceStatus',
      key: 'deliveryServiceStatus',
      render: (status: string | null) => {
        if (!status) return <span style={{ color: '#999' }}>-</span>
        const statusInfo = deliveryServiceStatusMap[status]
        return (
          <Tag color={statusInfo?.color || 'default'}>
            {statusInfo?.label || status}
          </Tag>
        )
      },
      width: 150,
    },
    {
      title: '수정 일시',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      sorter: (a, b) => new Date(a.updatedAt).getTime() - new Date(b.updatedAt).getTime(),
      render: (date: string) => {
        if (!date) return '-'
        return date
      },
      width: 160,
    },
  ]

  return (
    <div className="admin-shipping-list">
      <div className="admin-shipping-list-container">
        <div className="shipping-list-header">
          <h2>배송 관리</h2>
        </div>

        <div className="shipping-list-filters">
          <div className="filter-inputs">
            <Space size="middle">
              <Input
                placeholder="운송장 번호 검색"
                allowClear
                style={{ width: 200 }}
                value={searchTrackingNumber}
                onChange={(e) => setSearchTrackingNumber(e.target.value)}
                onPressEnter={handleSearch}
              />
              <Select
                placeholder="배송 상태 선택"
                allowClear
                style={{ width: 150 }}
                value={searchShippingStatus}
                onChange={(value) => setSearchShippingStatus(value)}
              >
                <Option value="READY">배송 준비</Option>
                <Option value="SHIPPING">배송 중</Option>
                <Option value="DELIVERED">배송 완료</Option>
                <Option value="RETURNED">반품</Option>
              </Select>
            </Space>
          </div>
          <div className="filter-actions">
            <Space>
              <Button onClick={handleReset}>초기화</Button>
              <Button type="primary" onClick={handleSearch}>
                검색
              </Button>
            </Space>
          </div>
        </div>

        <Table
          columns={columns}
          dataSource={shippings}
          rowKey="shippingId"
          loading={loading}
          scroll={{ x: 'max-content' }}
          onChange={handleTableChange}
          pagination={{
            current: pagination.current,
            pageSize: pagination.pageSize,
            total: pagination.total,
            showSizeChanger: true,
            showTotal: (total) => `총 ${total}개`,
          }}
        />

        {/* 주문 상세 모달 */}
        <OrderDetailModal
          open={isModalVisible}
          order={selectedOrder}
          orderItems={orderItems}
          orderShipping={orderShipping}
          onClose={handleModalClose}
          onSave={handleOrderSave}
        />

        {/* 운송장 번호 등록 모달 */}
        <Modal
          title="운송장 번호 등록"
          open={isTrackingModalVisible}
          onCancel={handleTrackingModalClose}
          onOk={handleTrackingSave}
          okText="저장"
          cancelText="취소"
          okButtonProps={{
            style: { backgroundColor: '#FFC107', borderColor: '#FFC107', color: '#343A40', fontWeight: 600 }
          }}
        >
          <Form
            form={trackingForm}
            layout="vertical"
          >
            <Form.Item
              label="배송사"
              name="shipping_company"
              rules={[{ required: true, message: '배송사를 선택하세요' }]}
            >
              <Select placeholder="배송사를 선택하세요">
                <Option value="04">CJ대한통운</Option>
                <Option value="05">한진택배</Option>
                <Option value="06">로젠택배</Option>
                <Option value="08">롯데택배</Option>
                <Option value="01">우체국택배</Option>
              </Select>
            </Form.Item>
            <Form.Item
              label="운송장 번호"
              name="tracking_number"
              rules={[
                { required: true, message: '운송장 번호를 입력하세요' },
                { max: 100, message: '운송장 번호는 최대 100자까지 입력 가능합니다.' }
              ]}
            >
              <Input
                placeholder="운송장 번호를 입력하세요"
                maxLength={100}
              />
            </Form.Item>
          </Form>
        </Modal>
      </div>
    </div>
  )
}

export default AdminShippingList


import { useState, useEffect } from 'react'
import { Table, Card, Space, Input, Button, Select, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import OrderDetailModal, { type Order, type OrderItem, type OrderShipping } from '../order/OrderDetailModal'
import './AdminShippingList.css'

const { Option } = Select

interface Shipping {
  shipping_id: string
  order_id: string
  order_number?: string
  receiver_name: string
  receiver_phone: string
  address: string
  postal_code?: string
  shipping_status: 'READY' | 'SHIPPING' | 'DELIVERED' | 'RETURNED'
  shipping_company?: string
  tracking_number?: string
  delivery_service_status?: 'NOT_SENT' | 'SENT' | 'IN_TRANSIT' | 'DELIVERED'
  created_at: string
  updated_at: string
}

function AdminShippingList() {
  const [shippings, setShippings] = useState<Shipping[]>([])
  const [filteredShippings, setFilteredShippings] = useState<Shipping[]>([])
  const [searchShippingStatus, setSearchShippingStatus] = useState<string | undefined>(undefined)
  const [searchTrackingNumber, setSearchTrackingNumber] = useState('')
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null)
  const [orderItems, setOrderItems] = useState<OrderItem[]>([])
  const [orderShipping, setOrderShipping] = useState<OrderShipping | null>(null)
  const [isModalVisible, setIsModalVisible] = useState(false)

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

  // 배송 데이터 로드 (샘플 데이터)
  useEffect(() => {
    // TODO: API 호출로 배송 데이터 로드
    const sampleShippings: Shipping[] = [
      {
        shipping_id: '1',
        order_id: '1',
        order_number: 'ORD-2024-001',
        receiver_name: '홍길동',
        receiver_phone: '010-1234-5678',
        address: '서울특별시 강남구 테헤란로 123',
        postal_code: '06234',
        shipping_status: 'SHIPPING',
        shipping_company: 'CJ대한통운',
        tracking_number: '1234567890123',
        delivery_service_status: 'IN_TRANSIT',
        created_at: '2024-01-15 10:30:00',
        updated_at: '2024-01-15 11:00:00'
      },
      {
        shipping_id: '2',
        order_id: '2',
        order_number: 'ORD-2024-002',
        receiver_name: '김철수',
        receiver_phone: '010-2345-6789',
        address: '서울특별시 서초구 서초대로 456',
        postal_code: '06511',
        shipping_status: 'DELIVERED',
        shipping_company: '한진택배',
        tracking_number: '9876543210987',
        delivery_service_status: 'DELIVERED',
        created_at: '2024-01-14 14:20:00',
        updated_at: '2024-01-15 09:00:00'
      },
      {
        shipping_id: '3',
        order_id: '3',
        order_number: 'ORD-2024-003',
        receiver_name: '이영희',
        receiver_phone: '010-3456-7890',
        address: '서울특별시 송파구 올림픽로 789',
        postal_code: '05510',
        shipping_status: 'READY',
        shipping_company: null,
        tracking_number: null,
        delivery_service_status: 'NOT_SENT',
        created_at: '2024-01-13 16:45:00',
        updated_at: '2024-01-13 16:45:00'
      },
      {
        shipping_id: '4',
        order_id: '4',
        order_number: 'ORD-2024-004',
        receiver_name: '박민수',
        receiver_phone: '010-4567-8901',
        address: '서울특별시 마포구 홍대로 321',
        postal_code: '04066',
        shipping_status: 'RETURNED',
        shipping_company: 'CJ대한통운',
        tracking_number: '1112223334445',
        delivery_service_status: 'IN_TRANSIT',
        created_at: '2024-01-12 11:20:00',
        updated_at: '2024-01-12 15:00:00'
      }
    ]
    setShippings(sampleShippings)
  }, [])

  // 필터링된 데이터
  useEffect(() => {
    const filtered = shippings.filter((shipping) => {
      const statusMatch = !searchShippingStatus || shipping.shipping_status === searchShippingStatus
      const trackingMatch = !searchTrackingNumber || 
        (shipping.tracking_number && shipping.tracking_number.includes(searchTrackingNumber))
      return statusMatch && trackingMatch
    })
    setFilteredShippings(filtered)
  }, [searchShippingStatus, searchTrackingNumber, shippings])

  const handleSearch = () => {
    // 검색 버튼 클릭 시 필터 적용 (현재는 실시간 필터링이므로 별도 로직 불필요)
  }

  const handleReset = () => {
    setSearchShippingStatus(undefined)
    setSearchTrackingNumber('')
  }

  // 주문 상세 조회
  const handleOrderClick = async (orderId: string) => {
    // TODO: API 호출로 주문 상세 데이터 로드
    const sampleOrder: Order = {
      order_id: orderId,
      order_number: filteredShippings.find(s => s.order_id === orderId)?.order_number || `ORD-${orderId}`,
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
        order_id: orderId,
        product_id: '1',
        product_name: '노트북',
        product_code: 'PRD-001',
        quantity: 1,
        unit_price: 150000,
        total_price: 150000,
        created_at: sampleOrder.ordered_at
      }
    ]

    const shippingData = filteredShippings.find(s => s.order_id === orderId)
    const sampleShipping: OrderShipping = {
      shipping_id: shippingData?.shipping_id || '1',
      order_id: orderId,
      receiver_name: shippingData?.receiver_name || '홍길동',
      receiver_phone: shippingData?.receiver_phone || '010-1234-5678',
      address: shippingData?.address || '서울특별시 강남구 테헤란로 123',
      postal_code: shippingData?.postal_code,
      shipping_status: shippingData?.shipping_status || 'READY',
      created_at: shippingData?.created_at || sampleOrder.ordered_at
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

  const columns: ColumnsType<Shipping> = [
    {
      title: '배송 ID',
      dataIndex: 'shipping_id',
      key: 'shipping_id',
      width: 100,
    },
    {
      title: '주문 번호',
      dataIndex: 'order_number',
      key: 'order_number',
      sorter: (a, b) => (a.order_number || '').localeCompare(b.order_number || ''),
      render: (text: string, record: Shipping) => (
        <a 
          onClick={() => handleOrderClick(record.order_id)}
          style={{ color: '#007BFF', cursor: 'pointer' }}
        >
          {text}
        </a>
      ),
      width: 150,
    },
    {
      title: '수령인',
      dataIndex: 'receiver_name',
      key: 'receiver_name',
      width: 100,
    },
    {
      title: '연락처',
      dataIndex: 'receiver_phone',
      key: 'receiver_phone',
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
      dataIndex: 'shipping_status',
      key: 'shipping_status',
      filters: [
        { text: '배송 준비', value: 'READY' },
        { text: '배송 중', value: 'SHIPPING' },
        { text: '배송 완료', value: 'DELIVERED' },
        { text: '반품', value: 'RETURNED' },
      ],
      onFilter: (value, record) => record.shipping_status === value,
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
      dataIndex: 'shipping_company',
      key: 'shipping_company',
      render: (company: string | null) => company || <span style={{ color: '#999' }}>-</span>,
      width: 120,
    },
    {
      title: '운송장 번호',
      dataIndex: 'tracking_number',
      key: 'tracking_number',
      render: (tracking: string | null) => tracking || <span style={{ color: '#999' }}>-</span>,
      width: 150,
    },
    {
      title: '배송사 연동 상태',
      dataIndex: 'delivery_service_status',
      key: 'delivery_service_status',
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
      dataIndex: 'updated_at',
      key: 'updated_at',
      sorter: (a, b) => new Date(a.updated_at).getTime() - new Date(b.updated_at).getTime(),
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
          dataSource={filteredShippings}
          rowKey="shipping_id"
          scroll={{ x: 'max-content' }}
          pagination={{
            pageSize: 10,
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
      </div>
    </div>
  )
}

export default AdminShippingList


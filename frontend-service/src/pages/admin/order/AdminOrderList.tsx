import { useState, useEffect } from 'react'
import { Table, Card, Space, Input, Button, Select, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import OrderDetailModal, { type Order, type OrderItem, type OrderShipping } from './OrderDetailModal'
import './AdminOrderList.css'

const { Option } = Select

function AdminOrderList() {
  const [orders, setOrders] = useState<Order[]>([])
  const [filteredOrders, setFilteredOrders] = useState<Order[]>([])
  const [searchOrderNumber, setSearchOrderNumber] = useState('')
  const [searchStatus, setSearchStatus] = useState<string | undefined>(undefined)
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null)
  const [orderItems, setOrderItems] = useState<OrderItem[]>([])
  const [orderShipping, setOrderShipping] = useState<OrderShipping | null>(null)
  const [isModalVisible, setIsModalVisible] = useState(false)

  const statusMap: Record<string, { label: string; color: string }> = {
    CREATED: { label: '주문 생성', color: 'blue' },
    PAID: { label: '결제 완료', color: 'green' },
    SHIPPING: { label: '배송 중', color: 'orange' },
    DELIVERED: { label: '배송 완료', color: 'cyan' },
    CANCELED: { label: '취소됨', color: 'red' }
  }

  // 주문 데이터 로드 (샘플 데이터)
  useEffect(() => {
    // TODO: API 호출로 주문 데이터 로드
    const sampleOrders: Order[] = [
      {
        order_id: '1',
        order_number: 'ORD-2024-001',
        user_id: '1',
        user_name: '홍길동',
        order_status: 'PAID',
        total_product_amount: 150000,
        total_discount_amount: 10000,
        total_payment_amount: 140000,
        ordered_at: '2024-01-15 10:30:00',
        updated_at: '2024-01-15 10:35:00'
      },
      {
        order_id: '2',
        order_number: 'ORD-2024-002',
        user_id: '2',
        user_name: '김철수',
        order_status: 'SHIPPING',
        total_product_amount: 250000,
        total_discount_amount: 0,
        total_payment_amount: 250000,
        ordered_at: '2024-01-14 14:20:00',
        updated_at: '2024-01-15 09:00:00'
      },
      {
        order_id: '3',
        order_number: 'ORD-2024-003',
        user_id: '3',
        user_name: '이영희',
        order_status: 'DELIVERED',
        total_product_amount: 80000,
        total_discount_amount: 5000,
        total_payment_amount: 75000,
        ordered_at: '2024-01-13 16:45:00',
        updated_at: '2024-01-14 10:00:00'
      },
      {
        order_id: '4',
        order_number: 'ORD-2024-004',
        user_id: '4',
        user_name: '박민수',
        order_status: 'CANCELED',
        total_product_amount: 120000,
        total_discount_amount: 0,
        total_payment_amount: 120000,
        ordered_at: '2024-01-12 11:20:00',
        updated_at: '2024-01-12 12:00:00'
      },
      {
        order_id: '5',
        order_number: 'ORD-2024-005',
        user_id: '5',
        user_name: '최지영',
        order_status: 'CREATED',
        total_product_amount: 300000,
        total_discount_amount: 20000,
        total_payment_amount: 280000,
        ordered_at: '2024-01-15 15:10:00',
        updated_at: '2024-01-15 15:10:00'
      }
    ]
    setOrders(sampleOrders)
  }, [])

  // 필터링된 데이터
  useEffect(() => {
    const filtered = orders.filter((order) => {
      const orderNumberMatch = !searchOrderNumber || 
        order.order_number.toLowerCase().includes(searchOrderNumber.toLowerCase())
      const statusMatch = !searchStatus || order.order_status === searchStatus
      return orderNumberMatch && statusMatch
    })
    setFilteredOrders(filtered)
  }, [searchOrderNumber, searchStatus, orders])

  const handleSearch = () => {
    // 검색 버튼 클릭 시 필터 적용 (현재는 실시간 필터링이므로 별도 로직 불필요)
  }

  const handleReset = () => {
    setSearchOrderNumber('')
    setSearchStatus(undefined)
  }

  // 주문 상세 조회
  const handleOrderClick = async (orderId: string) => {
    const order = orders.find(o => o.order_id === orderId)
    if (!order) return

    setSelectedOrder(order)
    
    // TODO: API 호출로 주문 상세 데이터 로드
    // 샘플 데이터
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
        created_at: order.ordered_at
      }
    ]

    const sampleShipping: OrderShipping = {
      shipping_id: '1',
      order_id: orderId,
      receiver_name: '홍길동',
      receiver_phone: '010-1234-5678',
      address: '서울특별시 강남구 테헤란로 123',
      postal_code: '06234',
      shipping_status: order.order_status === 'SHIPPING' ? 'SHIPPING' : 
                       order.order_status === 'DELIVERED' ? 'DELIVERED' : 'READY',
      created_at: order.ordered_at
    }

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

  const columns: ColumnsType<Order> = [
    {
      title: '주문 번호',
      dataIndex: 'order_number',
      key: 'order_number',
      sorter: (a, b) => a.order_number.localeCompare(b.order_number),
      render: (text: string, record: Order) => (
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
      title: '주문자',
      dataIndex: 'user_name',
      key: 'user_name',
      render: (name: string, record: Order) => name || `사용자 ID: ${record.user_id}`,
      width: 120,
    },
    {
      title: '주문 상태',
      dataIndex: 'order_status',
      key: 'order_status',
      filters: [
        { text: '주문 생성', value: 'CREATED' },
        { text: '결제 완료', value: 'PAID' },
        { text: '배송 중', value: 'SHIPPING' },
        { text: '배송 완료', value: 'DELIVERED' },
        { text: '취소됨', value: 'CANCELED' },
      ],
      onFilter: (value, record) => record.order_status === value,
      render: (status: string) => {
        const statusInfo = statusMap[status]
        return (
          <Tag color={statusInfo?.color || 'default'}>
            {statusInfo?.label || status}
          </Tag>
        )
      },
      width: 120,
    },
    {
      title: '상품 금액',
      dataIndex: 'total_product_amount',
      key: 'total_product_amount',
      render: (amount: number) => `${amount.toLocaleString()}원`,
      sorter: (a, b) => a.total_product_amount - b.total_product_amount,
      align: 'right',
      width: 120,
    },
    {
      title: '할인 금액',
      dataIndex: 'total_discount_amount',
      key: 'total_discount_amount',
      render: (amount: number) => `${amount.toLocaleString()}원`,
      sorter: (a, b) => a.total_discount_amount - b.total_discount_amount,
      align: 'right',
      width: 120,
    },
    {
      title: '결제 금액',
      dataIndex: 'total_payment_amount',
      key: 'total_payment_amount',
      render: (amount: number) => (
        <strong style={{ color: '#007BFF' }}>
          {amount.toLocaleString()}원
        </strong>
      ),
      sorter: (a, b) => a.total_payment_amount - b.total_payment_amount,
      align: 'right',
      width: 130,
    },
    {
      title: '주문 일시',
      dataIndex: 'ordered_at',
      key: 'ordered_at',
      sorter: (a, b) => new Date(a.ordered_at).getTime() - new Date(b.ordered_at).getTime(),
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
    <div className="admin-order-list">
      <div className="admin-order-list-container">
        <div className="order-list-header">
          <h2>주문 관리</h2>
        </div>

        <div className="order-list-filters">
          <div className="filter-inputs">
            <Space size="middle">
              <Input
                placeholder="주문 번호 검색"
                allowClear
                style={{ width: 200 }}
                value={searchOrderNumber}
                onChange={(e) => setSearchOrderNumber(e.target.value)}
                onPressEnter={handleSearch}
              />
              <Select
                placeholder="주문 상태 선택"
                allowClear
                style={{ width: 150 }}
                value={searchStatus}
                onChange={(value) => setSearchStatus(value)}
              >
                <Option value="CREATED">주문 생성</Option>
                <Option value="PAID">결제 완료</Option>
                <Option value="SHIPPING">배송 중</Option>
                <Option value="DELIVERED">배송 완료</Option>
                <Option value="CANCELED">취소됨</Option>
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
          dataSource={filteredOrders}
          rowKey="order_id"
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
        />
      </div>
    </div>
  )
}

export default AdminOrderList


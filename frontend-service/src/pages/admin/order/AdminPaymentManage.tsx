import { useState, useEffect } from 'react'
import { Table, Card, Space, Input, Button, Select, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import OrderDetailModal, { type Order, type OrderItem, type OrderShipping } from './OrderDetailModal'
import './AdminPaymentManage.css'

const { Option } = Select

interface OrderPayment {
  payment_id: string
  order_id: string
  order_number?: string
  payment_method: 'CARD' | 'BANK_TRANSFER' | 'KAKAO_PAY' | 'NAVER_PAY' | 'TOSS_PAY'
  payment_amount: number
  payment_status: 'READY' | 'PAID' | 'FAILED' | 'CANCELED'
  paid_at: string | null
  created_at: string
}

function AdminPaymentManage() {
  const [payments, setPayments] = useState<OrderPayment[]>([])
  const [filteredPayments, setFilteredPayments] = useState<OrderPayment[]>([])
  const [searchPaymentStatus, setSearchPaymentStatus] = useState<string | undefined>(undefined)
  const [searchPaymentMethod, setSearchPaymentMethod] = useState<string | undefined>(undefined)
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null)
  const [orderItems, setOrderItems] = useState<OrderItem[]>([])
  const [orderShipping, setOrderShipping] = useState<OrderShipping | null>(null)
  const [isModalVisible, setIsModalVisible] = useState(false)

  const paymentStatusMap: Record<string, { label: string; color: string }> = {
    READY: { label: '결제 대기', color: 'blue' },
    PAID: { label: '결제 완료', color: 'green' },
    FAILED: { label: '결제 실패', color: 'red' },
    CANCELED: { label: '결제 취소', color: 'orange' }
  }

  const paymentMethodMap: Record<string, string> = {
    CARD: '신용카드',
    BANK_TRANSFER: '계좌이체',
    KAKAO_PAY: '카카오페이',
    NAVER_PAY: '네이버페이',
    TOSS_PAY: '토스페이'
  }

  // 결제 데이터 로드 (샘플 데이터)
  useEffect(() => {
    // TODO: API 호출로 결제 데이터 로드
    const samplePayments: OrderPayment[] = [
      {
        payment_id: '1',
        order_id: '1',
        order_number: 'ORD-2024-001',
        payment_method: 'CARD',
        payment_amount: 140000,
        payment_status: 'PAID',
        paid_at: '2024-01-15 10:35:00',
        created_at: '2024-01-15 10:30:00'
      },
      {
        payment_id: '2',
        order_id: '2',
        order_number: 'ORD-2024-002',
        payment_method: 'KAKAO_PAY',
        payment_amount: 250000,
        payment_status: 'PAID',
        paid_at: '2024-01-14 14:25:00',
        created_at: '2024-01-14 14:20:00'
      },
      {
        payment_id: '3',
        order_id: '3',
        order_number: 'ORD-2024-003',
        payment_method: 'BANK_TRANSFER',
        payment_amount: 75000,
        payment_status: 'PAID',
        paid_at: '2024-01-13 16:50:00',
        created_at: '2024-01-13 16:45:00'
      },
      {
        payment_id: '4',
        order_id: '4',
        order_number: 'ORD-2024-004',
        payment_method: 'CARD',
        payment_amount: 120000,
        payment_status: 'CANCELED',
        paid_at: null,
        created_at: '2024-01-12 11:20:00'
      },
      {
        payment_id: '5',
        order_id: '5',
        order_number: 'ORD-2024-005',
        payment_method: 'TOSS_PAY',
        payment_amount: 280000,
        payment_status: 'READY',
        paid_at: null,
        created_at: '2024-01-15 15:10:00'
      },
      {
        payment_id: '6',
        order_id: '6',
        order_number: 'ORD-2024-006',
        payment_method: 'NAVER_PAY',
        payment_amount: 50000,
        payment_status: 'FAILED',
        paid_at: null,
        created_at: '2024-01-14 09:00:00'
      }
    ]
    setPayments(samplePayments)
  }, [])

  // 필터링된 데이터
  useEffect(() => {
    const filtered = payments.filter((payment) => {
      const statusMatch = !searchPaymentStatus || payment.payment_status === searchPaymentStatus
      const methodMatch = !searchPaymentMethod || payment.payment_method === searchPaymentMethod
      return statusMatch && methodMatch
    })
    setFilteredPayments(filtered)
  }, [searchPaymentStatus, searchPaymentMethod, payments])

  const handleSearch = () => {
    // 검색 버튼 클릭 시 필터 적용 (현재는 실시간 필터링이므로 별도 로직 불필요)
  }

  const handleReset = () => {
    setSearchPaymentStatus(undefined)
    setSearchPaymentMethod(undefined)
  }

  // 주문 상세 조회
  const handleOrderClick = async (orderId: string) => {
    // TODO: API 호출로 주문 상세 데이터 로드
    // 샘플 데이터
    const sampleOrder: Order = {
      order_id: orderId,
      order_number: filteredPayments.find(p => p.order_id === orderId)?.order_number || `ORD-${orderId}`,
      user_id: '1',
      user_name: '홍길동',
      order_status: 'PAID',
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

    const sampleShipping: OrderShipping = {
      shipping_id: '1',
      order_id: orderId,
      receiver_name: '홍길동',
      receiver_phone: '010-1234-5678',
      address: '서울특별시 강남구 테헤란로 123',
      postal_code: '06234',
      shipping_status: 'READY',
      created_at: sampleOrder.ordered_at
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

  const columns: ColumnsType<OrderPayment> = [
    {
      title: '결제 ID',
      dataIndex: 'payment_id',
      key: 'payment_id',
      width: 100,
    },
    {
      title: '주문 번호',
      dataIndex: 'order_number',
      key: 'order_number',
      sorter: (a, b) => (a.order_number || '').localeCompare(b.order_number || ''),
      render: (text: string, record: OrderPayment) => (
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
      title: '결제 수단',
      dataIndex: 'payment_method',
      key: 'payment_method',
      filters: [
        { text: '신용카드', value: 'CARD' },
        { text: '계좌이체', value: 'BANK_TRANSFER' },
        { text: '카카오페이', value: 'KAKAO_PAY' },
        { text: '네이버페이', value: 'NAVER_PAY' },
        { text: '토스페이', value: 'TOSS_PAY' },
      ],
      onFilter: (value, record) => record.payment_method === value,
      render: (method: string) => paymentMethodMap[method] || method,
      width: 120,
    },
    {
      title: '결제 금액',
      dataIndex: 'payment_amount',
      key: 'payment_amount',
      render: (amount: number) => (
        <strong style={{ color: '#007BFF' }}>
          {amount.toLocaleString()}원
        </strong>
      ),
      sorter: (a, b) => a.payment_amount - b.payment_amount,
      align: 'right',
      width: 130,
    },
    {
      title: '결제 상태',
      dataIndex: 'payment_status',
      key: 'payment_status',
      filters: [
        { text: '결제 대기', value: 'READY' },
        { text: '결제 완료', value: 'PAID' },
        { text: '결제 실패', value: 'FAILED' },
        { text: '결제 취소', value: 'CANCELED' },
      ],
      onFilter: (value, record) => record.payment_status === value,
      render: (status: string) => {
        const statusInfo = paymentStatusMap[status]
        return (
          <Tag color={statusInfo?.color || 'default'}>
            {statusInfo?.label || status}
          </Tag>
        )
      },
      width: 120,
    },
    {
      title: '결제 완료 일시',
      dataIndex: 'paid_at',
      key: 'paid_at',
      sorter: (a, b) => {
        const aTime = a.paid_at ? new Date(a.paid_at).getTime() : 0
        const bTime = b.paid_at ? new Date(b.paid_at).getTime() : 0
        return aTime - bTime
      },
      render: (paidAt: string | null) => {
        if (!paidAt) return <span style={{ color: '#999' }}>-</span>
        const dateObj = new Date(paidAt)
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
    <div className="admin-payment-manage">
      <div className="payment-manage-container">
        <div className="payment-list-header">
          <h2>결제 관리</h2>
        </div>

        <div className="payment-list-filters">
          <div className="filter-inputs">
            <Space size="middle">
              <Select
                placeholder="결제 상태 선택"
                allowClear
                style={{ width: 150 }}
                value={searchPaymentStatus}
                onChange={(value) => setSearchPaymentStatus(value)}
              >
                <Option value="READY">결제 대기</Option>
                <Option value="PAID">결제 완료</Option>
                <Option value="FAILED">결제 실패</Option>
                <Option value="CANCELED">결제 취소</Option>
              </Select>
              <Select
                placeholder="결제 수단 선택"
                allowClear
                style={{ width: 150 }}
                value={searchPaymentMethod}
                onChange={(value) => setSearchPaymentMethod(value)}
              >
                <Option value="CARD">신용카드</Option>
                <Option value="BANK_TRANSFER">계좌이체</Option>
                <Option value="KAKAO_PAY">카카오페이</Option>
                <Option value="NAVER_PAY">네이버페이</Option>
                <Option value="TOSS_PAY">토스페이</Option>
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
          dataSource={filteredPayments}
          rowKey="payment_id"
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

export default AdminPaymentManage


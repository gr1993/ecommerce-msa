import { useState, useEffect } from 'react'
import { Table, Space, Input, Button, Select, Tag, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import OrderDetailModal, { type Order, type OrderItem, type OrderShipping } from './OrderDetailModal'
import { getAdminOrders, getAdminOrderDetail, updateAdminOrder, cancelAdminOrder } from '../../../api/orderApi'
import './AdminOrderList.css'

const { Option } = Select

function AdminOrderList() {
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(false)
  const [searchOrderNumber, setSearchOrderNumber] = useState('')
  const [searchStatus, setSearchStatus] = useState<string | undefined>(undefined)
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null)
  const [orderItems, setOrderItems] = useState<OrderItem[]>([])
  const [orderShipping, setOrderShipping] = useState<OrderShipping | null>(null)
  const [isModalVisible, setIsModalVisible] = useState(false)
  const [currentPage, setCurrentPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [totalElements, setTotalElements] = useState(0)

  const statusMap: Record<string, { label: string; color: string }> = {
    CREATED: { label: '주문 생성', color: 'blue' },
    PAID: { label: '결제 완료', color: 'green' },
    FAILED: { label: '결제 실패', color: 'red' },
    SHIPPING: { label: '배송 중', color: 'orange' },
    DELIVERED: { label: '배송 완료', color: 'cyan' },
    CANCELED: { label: '취소됨', color: 'red' },
    RETURN_REQUESTED: { label: '반품 신청', color: 'volcano' },
    RETURN_APPROVED: { label: '반품 승인', color: 'gold' },
    RETURN_REJECTED: { label: '반품 거절', color: 'red' },
    RETURN_IN_TRANSIT: { label: '반품 회수 중', color: 'orange' },
    RETURNED: { label: '반품 완료', color: 'purple' },
    EXCHANGE_REQUESTED: { label: '교환 신청', color: 'volcano' },
    EXCHANGE_APPROVED: { label: '교환 승인', color: 'gold' },
    EXCHANGE_REJECTED: { label: '교환 거절', color: 'red' },
    EXCHANGE_COLLECTING: { label: '교환 회수 중', color: 'orange' },
    EXCHANGE_RETURN_COMPLETED: { label: '교환 회수 완료', color: 'lime' },
    EXCHANGE_SHIPPING: { label: '교환 배송 중', color: 'geekblue' },
    EXCHANGED: { label: '교환 완료', color: 'purple' },
  }

  const fetchOrders = async (orderNumber?: string, orderStatus?: string, page: number = 1, size: number = pageSize) => {
    setLoading(true)
    try {
      const data = await getAdminOrders(orderNumber || undefined, orderStatus || undefined, page - 1, size)
      setOrders(data.content)
      setTotalElements(data.totalElements)
      setCurrentPage(page)
      setPageSize(size)
    } catch (error) {
      message.error(error instanceof Error ? error.message : '주문 목록을 불러오는데 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  // 초기 데이터 로드
  useEffect(() => {
    fetchOrders()
  }, [])

  const handleSearch = () => {
    fetchOrders(searchOrderNumber, searchStatus, 1)
  }

  const handleReset = () => {
    setSearchOrderNumber('')
    setSearchStatus(undefined)
    fetchOrders(undefined, undefined, 1)
  }

  // 주문 상세 조회
  const handleOrderClick = async (orderId: string) => {
    try {
      const detail = await getAdminOrderDetail(orderId)
      setSelectedOrder(detail.order)
      setOrderItems(detail.orderItems)
      setOrderShipping(detail.orderShipping)
      setIsModalVisible(true)
    } catch (error) {
      message.error(error instanceof Error ? error.message : '주문 상세 정보를 불러오는데 실패했습니다.')
    }
  }

  const handleModalClose = () => {
    setIsModalVisible(false)
    setSelectedOrder(null)
    setOrderItems([])
    setOrderShipping(null)
  }

  const handleOrderSave = async (orderId: string, orderStatus: string, orderMemo: string) => {
    try {
      const result = await updateAdminOrder(orderId, orderStatus, orderMemo)

      setOrders(prev =>
        prev.map(order =>
          order.order_id === orderId ? result.order : order
        )
      )

      setSelectedOrder(result.order)
      setOrderItems(result.orderItems)
      setOrderShipping(result.orderShipping)
    } catch (error) {
      message.error(error instanceof Error ? error.message : '주문 수정에 실패했습니다.')
      throw error
    }
  }

  const handleCancelOrder = async (orderId: string, reason: string) => {
    await cancelAdminOrder(orderId, reason || undefined)
    // 목록 갱신
    fetchOrders(searchOrderNumber, searchStatus, currentPage, pageSize)
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
          dataSource={orders}
          rowKey="order_id"
          loading={loading}
          pagination={{
            current: currentPage,
            pageSize: pageSize,
            total: totalElements,
            showSizeChanger: true,
            showTotal: (total) => `총 ${total}개`,
            onChange: (page, size) => {
              fetchOrders(searchOrderNumber, searchStatus, page, size)
            },
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
          onCancelOrder={handleCancelOrder}
        />
      </div>
    </div>
  )
}

export default AdminOrderList

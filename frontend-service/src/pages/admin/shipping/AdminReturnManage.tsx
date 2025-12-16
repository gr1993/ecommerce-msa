import { useState, useEffect } from 'react'
import { Table, Card, Space, Input, Button, Select, Tag, Modal, Form, message, Popconfirm } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { CheckOutlined, DollarOutlined } from '@ant-design/icons'
import OrderDetailModal, { type Order, type OrderItem, type OrderShipping } from '../order/OrderDetailModal'
import './AdminReturnManage.css'

const { Option } = Select

interface Return {
  return_id: string
  order_id: string
  order_number?: string
  return_status: 'RETURN_REQUESTED' | 'RETURN_APPROVED' | 'RETURN_REJECTED' | 'RETURNED' | 'REFUNDED'
  return_reason?: string
  receiver_name: string
  receiver_phone: string
  return_address: string
  postal_code?: string
  created_at: string
  updated_at: string
}

function AdminReturnManage() {
  const [returns, setReturns] = useState<Return[]>([])
  const [filteredReturns, setFilteredReturns] = useState<Return[]>([])
  const [searchReturnStatus, setSearchReturnStatus] = useState<string | undefined>(undefined)
  const [searchOrderNumber, setSearchOrderNumber] = useState('')
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null)
  const [orderItems, setOrderItems] = useState<OrderItem[]>([])
  const [orderShipping, setOrderShipping] = useState<OrderShipping | null>(null)
  const [isModalVisible, setIsModalVisible] = useState(false)
  const [isApprovalModalVisible, setIsApprovalModalVisible] = useState(false)
  const [selectedReturn, setSelectedReturn] = useState<Return | null>(null)
  const [approvalForm] = Form.useForm()

  const returnStatusMap: Record<string, { label: string; color: string }> = {
    RETURN_REQUESTED: { label: '반품 요청', color: 'blue' },
    RETURN_APPROVED: { label: '반품 승인', color: 'green' },
    RETURN_REJECTED: { label: '반품 거절', color: 'red' },
    RETURNED: { label: '반품 완료', color: 'cyan' },
    REFUNDED: { label: '환불 완료', color: 'purple' }
  }

  // 반품 데이터 로드 (샘플 데이터)
  useEffect(() => {
    // TODO: API 호출로 반품 데이터 로드
    const sampleReturns: Return[] = [
      {
        return_id: '1',
        order_id: '1',
        order_number: 'ORD-2024-001',
        return_status: 'RETURN_REQUESTED',
        return_reason: '상품 불량',
        receiver_name: '홍길동',
        receiver_phone: '010-1234-5678',
        return_address: '서울특별시 강남구 테헤란로 123',
        postal_code: '06234',
        created_at: '2024-01-15 14:00:00',
        updated_at: '2024-01-15 14:00:00'
      },
      {
        return_id: '2',
        order_id: '2',
        order_number: 'ORD-2024-002',
        return_status: 'RETURN_APPROVED',
        return_reason: '단순 변심',
        receiver_name: '김철수',
        receiver_phone: '010-2345-6789',
        return_address: '서울특별시 서초구 서초대로 456',
        postal_code: '06511',
        created_at: '2024-01-14 16:00:00',
        updated_at: '2024-01-15 09:00:00'
      },
      {
        return_id: '3',
        order_id: '3',
        order_number: 'ORD-2024-003',
        return_status: 'RETURNED',
        return_reason: '배송 지연',
        receiver_name: '이영희',
        receiver_phone: '010-3456-7890',
        return_address: '서울특별시 송파구 올림픽로 789',
        postal_code: '05510',
        created_at: '2024-01-13 10:00:00',
        updated_at: '2024-01-14 15:00:00'
      },
      {
        return_id: '4',
        order_id: '4',
        order_number: 'ORD-2024-004',
        return_status: 'REFUNDED',
        return_reason: '상품 하자',
        receiver_name: '박민수',
        receiver_phone: '010-4567-8901',
        return_address: '서울특별시 마포구 홍대로 321',
        postal_code: '04066',
        created_at: '2024-01-12 11:00:00',
        updated_at: '2024-01-13 10:00:00'
      }
    ]
    setReturns(sampleReturns)
  }, [])

  // 필터링된 데이터
  useEffect(() => {
    const filtered = returns.filter((returnItem) => {
      const statusMatch = !searchReturnStatus || returnItem.return_status === searchReturnStatus
      const orderNumberMatch = !searchOrderNumber || 
        (returnItem.order_number && returnItem.order_number.toLowerCase().includes(searchOrderNumber.toLowerCase()))
      return statusMatch && orderNumberMatch
    })
    setFilteredReturns(filtered)
  }, [searchReturnStatus, searchOrderNumber, returns])

  const handleSearch = () => {
    // 검색 버튼 클릭 시 필터 적용 (현재는 실시간 필터링이므로 별도 로직 불필요)
  }

  const handleReset = () => {
    setSearchReturnStatus(undefined)
    setSearchOrderNumber('')
  }

  // 주문 상세 조회
  const handleOrderClick = async (orderId: string) => {
    // TODO: API 호출로 주문 상세 데이터 로드
    const sampleOrder: Order = {
      order_id: orderId,
      order_number: filteredReturns.find(r => r.order_id === orderId)?.order_number || `ORD-${orderId}`,
      user_id: '1',
      user_name: '홍길동',
      order_status: 'CANCELED',
      total_product_amount: 150000,
      total_discount_amount: 10000,
      total_payment_amount: 140000,
      ordered_at: '2024-01-15 10:30:00',
      updated_at: '2024-01-15 14:00:00'
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

    const returnData = filteredReturns.find(r => r.order_id === orderId)
    const sampleShipping: OrderShipping = {
      shipping_id: '1',
      order_id: orderId,
      receiver_name: returnData?.receiver_name || '홍길동',
      receiver_phone: returnData?.receiver_phone || '010-1234-5678',
      address: returnData?.return_address || '서울특별시 강남구 테헤란로 123',
      postal_code: returnData?.postal_code,
      shipping_status: 'RETURNED',
      created_at: returnData?.created_at || sampleOrder.ordered_at
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

  // 반품 승인 모달 열기
  const handleApprovalClick = (returnItem: Return) => {
    setSelectedReturn(returnItem)
    approvalForm.resetFields()
    setIsApprovalModalVisible(true)
  }

  // 반품 승인 저장
  const handleApprovalSave = async () => {
    if (!selectedReturn) return

    try {
      const values = await approvalForm.validateFields()
      
      // TODO: API 호출로 반품 승인 및 운송장 번호 등록
      setReturns(prev =>
        prev.map(returnItem =>
          returnItem.return_id === selectedReturn.return_id
            ? {
                ...returnItem,
                return_status: 'RETURN_APPROVED' as const,
                shipping_company: values.shipping_company,
                tracking_number: values.tracking_number,
                updated_at: new Date().toISOString()
              }
            : returnItem
        )
      )

      message.success('반품이 승인되었고 운송장 번호가 등록되었습니다.')
      setIsApprovalModalVisible(false)
      setSelectedReturn(null)
      approvalForm.resetFields()
    } catch (error) {
      console.error('Validation failed:', error)
    }
  }

  const handleApprovalModalClose = () => {
    setIsApprovalModalVisible(false)
    setSelectedReturn(null)
    approvalForm.resetFields()
  }

  // 환불 처리
  const handleRefund = (returnItem: Return) => {
    // TODO: API 호출로 환불 처리
    setReturns(prev =>
      prev.map(item =>
        item.return_id === returnItem.return_id
          ? {
              ...item,
              return_status: 'REFUNDED' as const,
              updated_at: new Date().toISOString()
            }
          : item
      )
    )
    message.success('환불이 완료되었습니다.')
  }

  const columns: ColumnsType<Return> = [
    {
      title: '반품 ID',
      dataIndex: 'return_id',
      key: 'return_id',
      width: 100,
    },
    {
      title: '주문 번호',
      dataIndex: 'order_number',
      key: 'order_number',
      sorter: (a, b) => (a.order_number || '').localeCompare(b.order_number || ''),
      render: (text: string, record: Return) => (
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
      title: '반품 상태',
      dataIndex: 'return_status',
      key: 'return_status',
      filters: [
        { text: '반품 요청', value: 'RETURN_REQUESTED' },
        { text: '반품 승인', value: 'RETURN_APPROVED' },
        { text: '반품 거절', value: 'RETURN_REJECTED' },
        { text: '반품 완료', value: 'RETURNED' },
        { text: '환불 완료', value: 'REFUNDED' },
      ],
      onFilter: (value, record) => record.return_status === value,
      render: (status: string, record: Return) => {
        const statusInfo = returnStatusMap[status]
        return (
          <Space>
            <Tag color={statusInfo?.color || 'default'}>
              {statusInfo?.label || status}
            </Tag>
            {status === 'RETURN_REQUESTED' && (
              <Button
                type="primary"
                size="small"
                icon={<CheckOutlined />}
                onClick={() => handleApprovalClick(record)}
                style={{ backgroundColor: '#FFC107', borderColor: '#FFC107', color: '#343A40', fontWeight: 600 }}
              >
                승인
              </Button>
            )}
            {status === 'RETURNED' && (
              <Popconfirm
                title="환불 확인"
                description="상품 회수를 확인해주세요. 정말 환불하시겠습니까?"
                onConfirm={() => handleRefund(record)}
                okText="확인"
                cancelText="취소"
                okButtonProps={{
                  style: { backgroundColor: '#FFC107', borderColor: '#FFC107', color: '#343A40', fontWeight: 600 }
                }}
              >
                <Button
                  type="primary"
                  size="small"
                  icon={<DollarOutlined />}
                  style={{ backgroundColor: '#28a745', borderColor: '#28a745' }}
                >
                  환불
                </Button>
              </Popconfirm>
            )}
          </Space>
        )
      },
      width: 220,
    },
    {
      title: '반품 사유',
      dataIndex: 'return_reason',
      key: 'return_reason',
      ellipsis: true,
      render: (reason: string | null) => reason || <span style={{ color: '#999' }}>-</span>,
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
      title: '반품 주소',
      dataIndex: 'return_address',
      key: 'return_address',
      ellipsis: true,
      width: 200,
    },
    {
      title: '요청 일시',
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
    <div className="admin-return-manage">
      <div className="return-manage-container">
        <div className="return-list-header">
          <h2>반품 관리</h2>
        </div>

        <div className="return-list-filters">
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
                placeholder="반품 상태 선택"
                allowClear
                style={{ width: 150 }}
                value={searchReturnStatus}
                onChange={(value) => setSearchReturnStatus(value)}
              >
                <Option value="RETURN_REQUESTED">반품 요청</Option>
                <Option value="RETURN_APPROVED">반품 승인</Option>
                <Option value="RETURN_REJECTED">반품 거절</Option>
                <Option value="RETURNED">반품 완료</Option>
                <Option value="REFUNDED">환불 완료</Option>
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
          dataSource={filteredReturns}
          rowKey="return_id"
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

        {/* 반품 승인 모달 */}
        <Modal
          title="반품 승인 및 운송장 번호 등록"
          open={isApprovalModalVisible}
          onCancel={handleApprovalModalClose}
          onOk={handleApprovalSave}
          okText="승인 및 저장"
          cancelText="취소"
          okButtonProps={{
            style: { backgroundColor: '#FFC107', borderColor: '#FFC107', color: '#343A40', fontWeight: 600 }
          }}
        >
          {selectedReturn && (
            <div style={{ marginBottom: '1rem' }}>
              <p><strong>주문 번호:</strong> {selectedReturn.order_number}</p>
              <p><strong>반품 사유:</strong> {selectedReturn.return_reason || '-'}</p>
            </div>
          )}
          <Form
            form={approvalForm}
            layout="vertical"
          >
            <Form.Item
              label="배송사"
              name="shipping_company"
              rules={[{ required: true, message: '배송사를 선택하세요' }]}
            >
              <Select placeholder="배송사를 선택하세요">
                <Option value="CJ대한통운">CJ대한통운</Option>
                <Option value="한진택배">한진택배</Option>
                <Option value="로젠택배">로젠택배</Option>
                <Option value="쿠팡배송">쿠팡배송</Option>
                <Option value="롯데택배">롯데택배</Option>
                <Option value="우체국택배">우체국택배</Option>
                <Option value="기타">기타</Option>
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

export default AdminReturnManage


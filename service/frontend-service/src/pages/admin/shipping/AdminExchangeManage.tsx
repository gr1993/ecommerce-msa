import { useState, useEffect } from 'react'
import { Table, Card, Space, Input, Button, Select, Tag, Modal, Form, message, Popconfirm } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { CheckOutlined, TruckOutlined, CheckCircleOutlined } from '@ant-design/icons'
import OrderDetailModal, { type Order, type OrderItem, type OrderShipping } from '../order/OrderDetailModal'
import './AdminExchangeManage.css'

const { Option } = Select

interface Exchange {
  exchange_id: string
  order_id: string
  order_number?: string
  exchange_status: 'EXCHANGE_REQUESTED' | 'EXCHANGE_APPROVED' | 'RETURNED' | 'EXCHANGE_SHIPPING' | 'EXCHANGED'
  exchange_reason?: string
  receiver_name: string
  receiver_phone: string
  exchange_address: string
  postal_code?: string
  return_shipping_company?: string
  return_tracking_number?: string
  exchange_shipping_company?: string
  exchange_tracking_number?: string
  created_at: string
  updated_at: string
}

function AdminExchangeManage() {
  const [exchanges, setExchanges] = useState<Exchange[]>([])
  const [filteredExchanges, setFilteredExchanges] = useState<Exchange[]>([])
  const [searchExchangeStatus, setSearchExchangeStatus] = useState<string | undefined>(undefined)
  const [searchOrderNumber, setSearchOrderNumber] = useState('')
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null)
  const [orderItems, setOrderItems] = useState<OrderItem[]>([])
  const [orderShipping, setOrderShipping] = useState<OrderShipping | null>(null)
  const [isModalVisible, setIsModalVisible] = useState(false)
  const [isApprovalModalVisible, setIsApprovalModalVisible] = useState(false)
  const [isShippingModalVisible, setIsShippingModalVisible] = useState(false)
  const [selectedExchange, setSelectedExchange] = useState<Exchange | null>(null)
  const [approvalForm] = Form.useForm()
  const [shippingForm] = Form.useForm()

  const exchangeStatusMap: Record<string, { label: string; color: string }> = {
    EXCHANGE_REQUESTED: { label: '교환 요청', color: 'blue' },
    EXCHANGE_APPROVED: { label: '교환 승인', color: 'green' },
    RETURNED: { label: '반품 완료', color: 'cyan' },
    EXCHANGE_SHIPPING: { label: '교환 배송 중', color: 'orange' },
    EXCHANGED: { label: '교환 완료', color: 'purple' }
  }

  // 교환 데이터 로드 (샘플 데이터)
  useEffect(() => {
    // TODO: API 호출로 교환 데이터 로드
    const sampleExchanges: Exchange[] = [
      {
        exchange_id: '1',
        order_id: '1',
        order_number: 'ORD-2024-001',
        exchange_status: 'EXCHANGE_REQUESTED',
        exchange_reason: '사이즈 불일치',
        receiver_name: '홍길동',
        receiver_phone: '010-1234-5678',
        exchange_address: '서울특별시 강남구 테헤란로 123',
        postal_code: '06234',
        created_at: '2024-01-15 14:00:00',
        updated_at: '2024-01-15 14:00:00'
      },
      {
        exchange_id: '2',
        order_id: '2',
        order_number: 'ORD-2024-002',
        exchange_status: 'EXCHANGE_APPROVED',
        exchange_reason: '색상 불일치',
        receiver_name: '김철수',
        receiver_phone: '010-2345-6789',
        exchange_address: '서울특별시 서초구 서초대로 456',
        postal_code: '06511',
        return_shipping_company: 'CJ대한통운',
        return_tracking_number: '1234567890123',
        created_at: '2024-01-14 16:00:00',
        updated_at: '2024-01-15 09:00:00'
      },
      {
        exchange_id: '3',
        order_id: '3',
        order_number: 'ORD-2024-003',
        exchange_status: 'RETURNED',
        exchange_reason: '상품 불량',
        receiver_name: '이영희',
        receiver_phone: '010-3456-7890',
        exchange_address: '서울특별시 송파구 올림픽로 789',
        postal_code: '05510',
        return_shipping_company: '한진택배',
        return_tracking_number: '9876543210987',
        created_at: '2024-01-13 10:00:00',
        updated_at: '2024-01-14 15:00:00'
      },
      {
        exchange_id: '4',
        order_id: '4',
        order_number: 'ORD-2024-004',
        exchange_status: 'EXCHANGE_SHIPPING',
        exchange_reason: '단순 변심',
        receiver_name: '박민수',
        receiver_phone: '010-4567-8901',
        exchange_address: '서울특별시 마포구 홍대로 321',
        postal_code: '04066',
        return_shipping_company: 'CJ대한통운',
        return_tracking_number: '1112223334445',
        exchange_shipping_company: '한진택배',
        exchange_tracking_number: '5556667778889',
        created_at: '2024-01-12 11:00:00',
        updated_at: '2024-01-15 10:00:00'
      }
    ]
    setExchanges(sampleExchanges)
  }, [])

  // 필터링된 데이터
  useEffect(() => {
    const filtered = exchanges.filter((exchange) => {
      const statusMatch = !searchExchangeStatus || exchange.exchange_status === searchExchangeStatus
      const orderNumberMatch = !searchOrderNumber || 
        (exchange.order_number && exchange.order_number.toLowerCase().includes(searchOrderNumber.toLowerCase()))
      return statusMatch && orderNumberMatch
    })
    setFilteredExchanges(filtered)
  }, [searchExchangeStatus, searchOrderNumber, exchanges])

  const handleSearch = () => {
    // 검색 버튼 클릭 시 필터 적용 (현재는 실시간 필터링이므로 별도 로직 불필요)
  }

  const handleReset = () => {
    setSearchExchangeStatus(undefined)
    setSearchOrderNumber('')
  }

  // 주문 상세 조회
  const handleOrderClick = async (orderId: string) => {
    // TODO: API 호출로 주문 상세 데이터 로드
    const sampleOrder: Order = {
      order_id: orderId,
      order_number: filteredExchanges.find(e => e.order_id === orderId)?.order_number || `ORD-${orderId}`,
      user_id: '1',
      user_name: '홍길동',
      order_status: 'SHIPPING',
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

    const exchangeData = filteredExchanges.find(e => e.order_id === orderId)
    const sampleShipping: OrderShipping = {
      shipping_id: '1',
      order_id: orderId,
      receiver_name: exchangeData?.receiver_name || '홍길동',
      receiver_phone: exchangeData?.receiver_phone || '010-1234-5678',
      address: exchangeData?.exchange_address || '서울특별시 강남구 테헤란로 123',
      postal_code: exchangeData?.postal_code,
      shipping_status: 'SHIPPING',
      created_at: exchangeData?.created_at || sampleOrder.ordered_at
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

  // 교환 승인 모달 열기
  const handleApprovalClick = (exchange: Exchange) => {
    setSelectedExchange(exchange)
    approvalForm.resetFields()
    setIsApprovalModalVisible(true)
  }

  // 교환 승인 저장 (반품 수거용 운송장 번호)
  const handleApprovalSave = async () => {
    if (!selectedExchange) return

    try {
      const values = await approvalForm.validateFields()
      
      // TODO: API 호출로 교환 승인 및 반품 수거용 운송장 번호 등록
      setExchanges(prev =>
        prev.map(exchange =>
          exchange.exchange_id === selectedExchange.exchange_id
            ? {
                ...exchange,
                exchange_status: 'EXCHANGE_APPROVED' as const,
                return_shipping_company: values.shipping_company,
                return_tracking_number: values.tracking_number,
                updated_at: new Date().toISOString()
              }
            : exchange
        )
      )

      message.success('교환이 승인되었고 반품 수거용 운송장 번호가 등록되었습니다.')
      setIsApprovalModalVisible(false)
      setSelectedExchange(null)
      approvalForm.resetFields()
    } catch (error) {
      console.error('Validation failed:', error)
    }
  }

  const handleApprovalModalClose = () => {
    setIsApprovalModalVisible(false)
    setSelectedExchange(null)
    approvalForm.resetFields()
  }

  // 교환 배송 모달 열기
  const handleShippingClick = (exchange: Exchange) => {
    setSelectedExchange(exchange)
    shippingForm.resetFields()
    setIsShippingModalVisible(true)
  }

  // 교환 배송 저장 (교환 상품 배송용 운송장 번호)
  const handleShippingSave = async () => {
    if (!selectedExchange) return

    try {
      const values = await shippingForm.validateFields()
      
      // TODO: API 호출로 교환 배송 및 교환 상품 배송용 운송장 번호 등록
      setExchanges(prev =>
        prev.map(exchange =>
          exchange.exchange_id === selectedExchange.exchange_id
            ? {
                ...exchange,
                exchange_status: 'EXCHANGE_SHIPPING' as const,
                exchange_shipping_company: values.shipping_company,
                exchange_tracking_number: values.tracking_number,
                updated_at: new Date().toISOString()
              }
            : exchange
        )
      )

      message.success('교환 상품 배송이 시작되었고 운송장 번호가 등록되었습니다.')
      setIsShippingModalVisible(false)
      setSelectedExchange(null)
      shippingForm.resetFields()
    } catch (error) {
      console.error('Validation failed:', error)
    }
  }

  const handleShippingModalClose = () => {
    setIsShippingModalVisible(false)
    setSelectedExchange(null)
    shippingForm.resetFields()
  }

  // 교환 완료 처리
  const handleExchangeComplete = (exchange: Exchange) => {
    // TODO: API 호출로 교환 완료 처리
    setExchanges(prev =>
      prev.map(item =>
        item.exchange_id === exchange.exchange_id
          ? {
              ...item,
              exchange_status: 'EXCHANGED' as const,
              updated_at: new Date().toISOString()
            }
          : item
      )
    )
    message.success('교환이 완료되었습니다.')
  }

  const columns: ColumnsType<Exchange> = [
    {
      title: '교환 ID',
      dataIndex: 'exchange_id',
      key: 'exchange_id',
      width: 100,
    },
    {
      title: '주문 번호',
      dataIndex: 'order_number',
      key: 'order_number',
      sorter: (a, b) => (a.order_number || '').localeCompare(b.order_number || ''),
      render: (text: string, record: Exchange) => (
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
      title: '교환 상태',
      dataIndex: 'exchange_status',
      key: 'exchange_status',
      filters: [
        { text: '교환 요청', value: 'EXCHANGE_REQUESTED' },
        { text: '교환 승인', value: 'EXCHANGE_APPROVED' },
        { text: '반품 완료', value: 'RETURNED' },
        { text: '교환 배송 중', value: 'EXCHANGE_SHIPPING' },
        { text: '교환 완료', value: 'EXCHANGED' },
      ],
      onFilter: (value, record) => record.exchange_status === value,
      render: (status: string, record: Exchange) => {
        const statusInfo = exchangeStatusMap[status]
        return (
          <Space>
            <Tag color={statusInfo?.color || 'default'}>
              {statusInfo?.label || status}
            </Tag>
            {status === 'EXCHANGE_REQUESTED' && (
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
              <Button
                type="primary"
                size="small"
                icon={<TruckOutlined />}
                onClick={() => handleShippingClick(record)}
                style={{ backgroundColor: '#007BFF', borderColor: '#007BFF' }}
              >
                배송
              </Button>
            )}
            {status === 'EXCHANGE_SHIPPING' && (
              <Popconfirm
                title="교환 완료 확인"
                description="교환 상품 배송이 완료되었나요? 정말 교환을 완료하시겠습니까?"
                onConfirm={() => handleExchangeComplete(record)}
                okText="확인"
                cancelText="취소"
                okButtonProps={{
                  style: { backgroundColor: '#FFC107', borderColor: '#FFC107', color: '#343A40', fontWeight: 600 }
                }}
              >
                <Button
                  type="primary"
                  size="small"
                  icon={<CheckCircleOutlined />}
                  style={{ backgroundColor: '#28a745', borderColor: '#28a745' }}
                >
                  완료
                </Button>
              </Popconfirm>
            )}
          </Space>
        )
      },
      width: 280,
    },
    {
      title: '교환 사유',
      dataIndex: 'exchange_reason',
      key: 'exchange_reason',
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
      title: '교환 주소',
      dataIndex: 'exchange_address',
      key: 'exchange_address',
      ellipsis: true,
      width: 200,
    },
    {
      title: '반품 운송장',
      key: 'return_tracking',
      render: (_, record: Exchange) => {
        if (record.return_tracking_number) {
          return (
            <div>
              <div style={{ fontSize: '12px', color: '#666' }}>{record.return_shipping_company}</div>
              <div>{record.return_tracking_number}</div>
            </div>
          )
        }
        return <span style={{ color: '#999' }}>-</span>
      },
      width: 150,
    },
    {
      title: '교환 운송장',
      key: 'exchange_tracking',
      render: (_, record: Exchange) => {
        if (record.exchange_tracking_number) {
          return (
            <div>
              <div style={{ fontSize: '12px', color: '#666' }}>{record.exchange_shipping_company}</div>
              <div>{record.exchange_tracking_number}</div>
            </div>
          )
        }
        return <span style={{ color: '#999' }}>-</span>
      },
      width: 150,
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
    <div className="admin-exchange-manage">
      <div className="exchange-manage-container">
        <div className="exchange-list-header">
          <h2>교환 관리</h2>
        </div>

        <div className="exchange-list-filters">
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
                placeholder="교환 상태 선택"
                allowClear
                style={{ width: 150 }}
                value={searchExchangeStatus}
                onChange={(value) => setSearchExchangeStatus(value)}
              >
                <Option value="EXCHANGE_REQUESTED">교환 요청</Option>
                <Option value="EXCHANGE_APPROVED">교환 승인</Option>
                <Option value="RETURNED">반품 완료</Option>
                <Option value="EXCHANGE_SHIPPING">교환 배송 중</Option>
                <Option value="EXCHANGED">교환 완료</Option>
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
          dataSource={filteredExchanges}
          rowKey="exchange_id"
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

        {/* 교환 승인 모달 (반품 수거용 운송장 번호) */}
        <Modal
          title="교환 승인 및 반품 수거용 운송장 번호 등록"
          open={isApprovalModalVisible}
          onCancel={handleApprovalModalClose}
          onOk={handleApprovalSave}
          okText="승인 및 저장"
          cancelText="취소"
          okButtonProps={{
            style: { backgroundColor: '#FFC107', borderColor: '#FFC107', color: '#343A40', fontWeight: 600 }
          }}
        >
          {selectedExchange && (
            <div style={{ marginBottom: '1rem' }}>
              <p><strong>주문 번호:</strong> {selectedExchange.order_number}</p>
              <p><strong>교환 사유:</strong> {selectedExchange.exchange_reason || '-'}</p>
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
              label="운송장 번호 (반품 수거용)"
              name="tracking_number"
              rules={[
                { required: true, message: '운송장 번호를 입력하세요' },
                { max: 100, message: '운송장 번호는 최대 100자까지 입력 가능합니다.' }
              ]}
            >
              <Input
                placeholder="반품 수거용 운송장 번호를 입력하세요"
                maxLength={100}
              />
            </Form.Item>
          </Form>
        </Modal>

        {/* 교환 배송 모달 (교환 상품 배송용 운송장 번호) */}
        <Modal
          title="교환 상품 배송 및 운송장 번호 등록"
          open={isShippingModalVisible}
          onCancel={handleShippingModalClose}
          onOk={handleShippingSave}
          okText="배송 시작 및 저장"
          cancelText="취소"
          okButtonProps={{
            style: { backgroundColor: '#007BFF', borderColor: '#007BFF' }
          }}
        >
          {selectedExchange && (
            <div style={{ marginBottom: '1rem' }}>
              <p><strong>주문 번호:</strong> {selectedExchange.order_number}</p>
              <p><strong>교환 사유:</strong> {selectedExchange.exchange_reason || '-'}</p>
            </div>
          )}
          <Form
            form={shippingForm}
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
              label="운송장 번호 (교환 상품 배송용)"
              name="tracking_number"
              rules={[
                { required: true, message: '운송장 번호를 입력하세요' },
                { max: 100, message: '운송장 번호는 최대 100자까지 입력 가능합니다.' }
              ]}
            >
              <Input
                placeholder="교환 상품 배송용 운송장 번호를 입력하세요"
                maxLength={100}
              />
            </Form.Item>
          </Form>
        </Modal>
      </div>
    </div>
  )
}

export default AdminExchangeManage


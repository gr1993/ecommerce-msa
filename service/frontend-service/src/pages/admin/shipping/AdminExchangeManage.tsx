import { useState, useEffect } from 'react'
import { Table, Card, Space, Input, Button, Select, Tag, Modal, Form, message, Popconfirm } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { CheckOutlined, TruckOutlined, CheckCircleOutlined, UnorderedListOutlined } from '@ant-design/icons'
import OrderDetailModal, { type Order, type OrderItem, type OrderShipping } from '../order/OrderDetailModal'
import ExchangeItemsModal from './ExchangeItemsModal'
import {
  getAdminExchanges,
  approveExchange,
  completeExchange,
  type AdminExchangeResponse,
  type ExchangeStatus,
  type ExchangeItemDto
} from '../../../api/shippingApi'
import { getAdminOrderDetail } from '../../../api/orderApi'
import './AdminExchangeManage.css'

const { Option } = Select

interface Exchange {
  exchange_id: number
  order_id: number
  order_number?: string
  exchange_status: ExchangeStatus
  exchange_items: ExchangeItemDto[]
  exchange_reason?: string
  receiver_name: string
  receiver_phone: string
  exchange_address: string
  postal_code?: string
  courier?: string
  tracking_number?: string
  created_at: string
  updated_at: string
}

function AdminExchangeManage() {
  const [exchanges, setExchanges] = useState<Exchange[]>([])
  const [loading, setLoading] = useState(false)
  const [searchExchangeStatus, setSearchExchangeStatus] = useState<string | undefined>(undefined)
  const [searchOrderNumber, setSearchOrderNumber] = useState('')
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null)
  const [orderItems, setOrderItems] = useState<OrderItem[]>([])
  const [orderShipping, setOrderShipping] = useState<OrderShipping | null>(null)
  const [isOrderModalVisible, setIsOrderModalVisible] = useState(false)
  const [isApprovalModalVisible, setIsApprovalModalVisible] = useState(false)
  const [isExchangeItemsModalVisible, setIsExchangeItemsModalVisible] = useState(false)
  const [selectedExchange, setSelectedExchange] = useState<Exchange | null>(null)
  const [approvalForm] = Form.useForm()
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })

  const exchangeStatusMap: Record<string, { label: string; color: string }> = {
    EXCHANGE_REQUESTED: { label: '교환 요청', color: 'blue' },
    EXCHANGE_APPROVED: { label: '교환 승인 (배송 중)', color: 'orange' },
    EXCHANGED: { label: '교환 완료', color: 'green' },
    EXCHANGE_REJECTED: { label: '교환 거절', color: 'red' }
  }

  // 교환 데이터 로드
  useEffect(() => {
    loadExchanges()
  }, [])

  const loadExchanges = async (page?: number) => {
    setLoading(true)
    try {
      const currentPage = page !== undefined ? page : pagination.current - 1
      const response = await getAdminExchanges(
        searchExchangeStatus,
        searchOrderNumber,
        currentPage,
        pagination.pageSize
      )

      const mappedExchanges: Exchange[] = response.content.map((item: AdminExchangeResponse) => ({
        exchange_id: item.exchangeId,
        order_id: item.orderId,
        order_number: item.orderNumber || `주문 #${item.orderId}`,
        exchange_status: item.exchangeStatus,
        exchange_items: item.exchangeItems,
        exchange_reason: item.reason,
        receiver_name: item.receiverName,
        receiver_phone: item.receiverPhone,
        exchange_address: item.address,
        postal_code: item.postalCode,
        courier: item.courier,
        tracking_number: item.trackingNumber,
        created_at: item.requestedAt,
        updated_at: item.updatedAt,
      }))

      setExchanges(mappedExchanges)
      setPagination(prev => ({
        ...prev,
        current: response.page + 1,
        total: response.totalElements,
      }))
    } catch (error) {
      console.error('교환 목록 조회 실패:', error)
      message.error(error instanceof Error ? error.message : '교환 목록 조회에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const handleSearch = () => {
    setPagination(prev => ({ ...prev, current: 1 }))
    loadExchanges(0)
  }

  const handleReset = () => {
    setSearchExchangeStatus(undefined)
    setSearchOrderNumber('')
    setPagination(prev => ({ ...prev, current: 1 }))
    loadExchanges(0)
  }

  // 주문 상세 조회
  const handleOrderClick = async (orderId: number) => {
    try {
      const { order, orderItems, orderShipping } = await getAdminOrderDetail(String(orderId))
      setSelectedOrder(order)
      setOrderItems(orderItems)
      setOrderShipping(orderShipping)
      setIsOrderModalVisible(true)
    } catch (error) {
      console.error('주문 상세 조회 실패:', error)
      message.error(error instanceof Error ? error.message : '주문 상세 조회에 실패했습니다.')
    }
  }

  const handleOrderModalClose = () => {
    setIsOrderModalVisible(false)
    setSelectedOrder(null)
    setOrderItems([])
    setOrderShipping(null)
  }

  // 교환 품목 모달 열기
  const handleExchangeItemsClick = (exchange: Exchange) => {
    setSelectedExchange(exchange)
    setIsExchangeItemsModalVisible(true)
  }

  const handleExchangeItemsModalClose = () => {
    setIsExchangeItemsModalVisible(false)
    setSelectedExchange(null)
  }

  const handleOrderSave = async (orderId: string, orderStatus: string, orderMemo: string) => {
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

  // 교환 승인 저장 (교환품 배송지 설정)
  const handleApprovalSave = async () => {
    if (!selectedExchange) return

    try {
      const values = await approvalForm.validateFields()

      await approveExchange(selectedExchange.exchange_id, {
        receiverName: values.receiver_name,
        receiverPhone: values.receiver_phone,
        exchangeAddress: values.exchange_address,
        postalCode: values.postal_code,
      })

      message.success('교환이 승인되었고 택배사를 통해 자동으로 운송장이 발급되었습니다.')
      setIsApprovalModalVisible(false)
      setSelectedExchange(null)
      approvalForm.resetFields()
      loadExchanges(pagination.current - 1)
    } catch (error) {
      console.error('교환 승인 실패:', error)
      message.error(error instanceof Error ? error.message : '교환 승인에 실패했습니다.')
    }
  }

  const handleApprovalModalClose = () => {
    setIsApprovalModalVisible(false)
    setSelectedExchange(null)
    approvalForm.resetFields()
  }

  // 교환 완료 처리
  const handleExchangeComplete = async (exchange: Exchange) => {
    try {
      await completeExchange(exchange.exchange_id)
      message.success('교환이 완료되었습니다.')
      loadExchanges(pagination.current - 1)
    } catch (error) {
      console.error('교환 완료 처리 실패:', error)
      message.error(error instanceof Error ? error.message : '교환 완료 처리에 실패했습니다.')
    }
  }

  // 교환 거절
  const handleExchangeReject = async (exchange: Exchange) => {
    try {
      // TODO: 교환 거절 API 호출
      message.success('교환이 거절되었습니다.')
      loadExchanges(pagination.current - 1)
    } catch (error) {
      console.error('교환 거절 실패:', error)
      message.error(error instanceof Error ? error.message : '교환 거절에 실패했습니다.')
    }
  }

  const columns: ColumnsType<Exchange> = [
    {
      title: '교환 ID',
      dataIndex: 'exchange_id',
      key: 'exchange_id',
      width: 100,
      render: (text: number, record: Exchange) => (
        <Space>
          <span>{text}</span>
          <Button
            type="link"
            size="small"
            icon={<UnorderedListOutlined />}
            onClick={() => handleExchangeItemsClick(record)}
            style={{ padding: 0 }}
            title="교환 품목 보기"
          />
        </Space>
      ),
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
        { text: '교환 완료', value: 'EXCHANGED' },
        { text: '교환 거절', value: 'EXCHANGE_REJECTED' },
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
              <Space>
                <Button
                  type="primary"
                  size="small"
                  icon={<CheckOutlined />}
                  onClick={() => handleApprovalClick(record)}
                  style={{ backgroundColor: '#28a745', borderColor: '#28a745' }}
                >
                  승인
                </Button>
                <Popconfirm
                  title="교환 거절 확인"
                  description="정말 교환을 거절하시겠습니까?"
                  onConfirm={() => handleExchangeReject(record)}
                  okText="확인"
                  cancelText="취소"
                  okButtonProps={{
                    danger: true
                  }}
                >
                  <Button
                    danger
                    size="small"
                  >
                    거절
                  </Button>
                </Popconfirm>
              </Space>
            )}
            {status === 'EXCHANGE_APPROVED' && (
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
                  style={{ backgroundColor: '#FFC107', borderColor: '#FFC107', color: '#343A40', fontWeight: 600 }}
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
      title: '운송장',
      key: 'tracking',
      render: (_, record: Exchange) => {
        if (record.tracking_number) {
          return (
            <div>
              <div style={{ fontSize: '12px', color: '#666' }}>{record.courier}</div>
              <div>{record.tracking_number}</div>
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
                style={{ width: 180 }}
                value={searchExchangeStatus}
                onChange={(value) => setSearchExchangeStatus(value)}
              >
                <Option value="EXCHANGE_REQUESTED">교환 요청</Option>
                <Option value="EXCHANGE_APPROVED">교환 승인 (배송 중)</Option>
                <Option value="EXCHANGED">교환 완료</Option>
                <Option value="EXCHANGE_REJECTED">교환 거절</Option>
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
          dataSource={exchanges}
          rowKey="exchange_id"
          loading={loading}
          scroll={{ x: 'max-content' }}
          pagination={{
            current: pagination.current,
            pageSize: pagination.pageSize,
            total: pagination.total,
            showSizeChanger: true,
            showTotal: (total) => `총 ${total}개`,
            onChange: (page, pageSize) => {
              setPagination(prev => ({ ...prev, current: page, pageSize }))
              loadExchanges(page - 1)
            },
          }}
        />

        {/* 주문 상세 모달 */}
        <OrderDetailModal
          open={isOrderModalVisible}
          order={selectedOrder}
          orderItems={orderItems}
          orderShipping={orderShipping}
          onClose={handleOrderModalClose}
          onSave={handleOrderSave}
        />

        {/* 교환 품목 모달 */}
        <ExchangeItemsModal
          open={isExchangeItemsModalVisible}
          exchangeId={selectedExchange?.exchange_id || null}
          exchangeItems={selectedExchange?.exchange_items || []}
          onClose={handleExchangeItemsModalClose}
        />

        {/* 교환 승인 모달 (교환품 배송지 설정) */}
        <Modal
          title="교환 승인 및 교환품 배송지 설정"
          open={isApprovalModalVisible}
          onCancel={handleApprovalModalClose}
          onOk={handleApprovalSave}
          okText="승인 및 저장"
          cancelText="취소"
          okButtonProps={{
            style: { backgroundColor: '#28a745', borderColor: '#28a745' }
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
              label="교환품 수령인"
              name="receiver_name"
              rules={[{ required: true, message: '수령인을 입력하세요' }]}
            >
              <Input placeholder="교환품 수령인 이름을 입력하세요" />
            </Form.Item>
            <Form.Item
              label="교환품 수령 연락처"
              name="receiver_phone"
              rules={[{ required: true, message: '수령 연락처를 입력하세요' }]}
            >
              <Input placeholder="교환품 수령 연락처를 입력하세요" />
            </Form.Item>
            <Form.Item
              label="교환품 배송 주소"
              name="exchange_address"
              rules={[{ required: true, message: '배송 주소를 입력하세요' }]}
            >
              <Input placeholder="교환품 배송 주소를 입력하세요" />
            </Form.Item>
            <Form.Item
              label="우편번호"
              name="postal_code"
            >
              <Input placeholder="우편번호를 입력하세요" />
            </Form.Item>
          </Form>
          <div style={{ marginTop: '16px', padding: '12px', background: '#e7f3ff', borderRadius: '4px', border: '1px solid #b3d9ff' }}>
            <p style={{ margin: 0, fontSize: '13px', color: '#0056b3' }}>
              ℹ️ 교환 승인 시 택배사 API를 통해 자동으로 운송장이 발급됩니다.
            </p>
          </div>
        </Modal>
      </div>
    </div>
  )
}

export default AdminExchangeManage


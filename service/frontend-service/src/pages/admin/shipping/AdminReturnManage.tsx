import { useState, useEffect } from 'react'
import { Table, Card, Space, Input, Button, Select, Tag, Modal, Form, message, Popconfirm, Spin } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { CheckOutlined, DollarOutlined } from '@ant-design/icons'
import OrderDetailModal, { type Order, type OrderItem, type OrderShipping } from '../order/OrderDetailModal'
import { getAdminReturns, approveReturn, completeReturn } from '../../../api/shippingApi'
import type { AdminReturnResponse } from '../../../api/shippingApi'
import './AdminReturnManage.css'

const { Option } = Select

function AdminReturnManage() {
  const [returns, setReturns] = useState<AdminReturnResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [searchReturnStatus, setSearchReturnStatus] = useState<string | undefined>(undefined)
  const [searchOrderNumber, setSearchOrderNumber] = useState('')
  const [currentPage, setCurrentPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [totalElements, setTotalElements] = useState(0)
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null)
  const [orderItems, setOrderItems] = useState<OrderItem[]>([])
  const [orderShipping, setOrderShipping] = useState<OrderShipping | null>(null)
  const [isModalVisible, setIsModalVisible] = useState(false)
  const [isApprovalModalVisible, setIsApprovalModalVisible] = useState(false)
  const [selectedReturn, setSelectedReturn] = useState<AdminReturnResponse | null>(null)
  const [approvalForm] = Form.useForm()

  const returnStatusMap: Record<string, { label: string; color: string }> = {
    RETURN_REQUESTED: { label: '반품 요청', color: 'blue' },
    RETURN_APPROVED: { label: '반품 승인', color: 'green' },
    RETURN_REJECTED: { label: '반품 거절', color: 'red' },
    RETURNED: { label: '반품 완료', color: 'cyan' }
  }

  // 반품 데이터 로드
  const fetchReturns = async () => {
    setLoading(true)
    try {
      const data = await getAdminReturns(
        searchReturnStatus,
        searchOrderNumber,
        currentPage,
        pageSize
      )
      setReturns(data.content)
      setTotalElements(data.totalElements)
    } catch (error) {
      console.error('반품 목록 조회 실패:', error)
      message.error(error instanceof Error ? error.message : '반품 목록을 불러오는데 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchReturns()
  }, [currentPage, pageSize])

  const handleSearch = () => {
    setCurrentPage(0)
    fetchReturns()
  }

  const handleReset = () => {
    setSearchReturnStatus(undefined)
    setSearchOrderNumber('')
    setCurrentPage(0)
    setTimeout(() => fetchReturns(), 0)
  }

  // 주문 상세 조회
  const handleOrderClick = async (orderId: number) => {
    // TODO: API 호출로 주문 상세 데이터 로드
    const returnData = returns.find(r => r.orderId === orderId)
    const sampleOrder: Order = {
      order_id: orderId.toString(),
      order_number: `ORD-${orderId}`,
      user_id: returnData?.userId.toString() || '1',
      user_name: '홍길동',
      order_status: 'CANCELED',
      total_product_amount: 150000,
      total_discount_amount: 10000,
      total_payment_amount: 140000,
      ordered_at: returnData?.requestedAt || '2024-01-15 10:30:00',
      updated_at: returnData?.updatedAt || '2024-01-15 14:00:00'
    }

    const sampleOrderItems: OrderItem[] = [
      {
        order_item_id: '1',
        order_id: orderId.toString(),
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
      order_id: orderId.toString(),
      receiver_name: returnData?.receiverName || '홍길동',
      receiver_phone: returnData?.receiverPhone || '010-1234-5678',
      address: returnData?.returnAddress || '서울특별시 강남구 테헤란로 123',
      postal_code: returnData?.postalCode,
      shipping_status: 'RETURNED',
      created_at: returnData?.requestedAt || sampleOrder.ordered_at
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
  const handleApprovalClick = (returnItem: AdminReturnResponse) => {
    setSelectedReturn(returnItem)
    approvalForm.setFieldsValue({
      receiverName: '물류센터',
      receiverPhone: '02-1234-5678',
      returnAddress: '서울특별시 강남구 물류센터로 1',
      postalCode: '06234'
    })
    setIsApprovalModalVisible(true)
  }

  // 반품 승인 저장
  const handleApprovalSave = async () => {
    if (!selectedReturn) return

    try {
      const values = await approvalForm.validateFields()

      await approveReturn(selectedReturn.returnId, {
        receiverName: values.receiverName,
        receiverPhone: values.receiverPhone,
        returnAddress: values.returnAddress,
        postalCode: values.postalCode
      })

      message.success('반품이 승인되었습니다.')
      setIsApprovalModalVisible(false)
      setSelectedReturn(null)
      approvalForm.resetFields()
      fetchReturns()
    } catch (error) {
      console.error('반품 승인 실패:', error)
      message.error(error instanceof Error ? error.message : '반품 승인에 실패했습니다.')
    }
  }

  const handleApprovalModalClose = () => {
    setIsApprovalModalVisible(false)
    setSelectedReturn(null)
    approvalForm.resetFields()
  }

  // 반품 완료 처리
  const handleCompleteReturn = async (returnItem: AdminReturnResponse) => {
    try {
      await completeReturn(returnItem.returnId)
      message.success('반품이 완료되었습니다.')
      fetchReturns()
    } catch (error) {
      console.error('반품 완료 처리 실패:', error)
      message.error(error instanceof Error ? error.message : '반품 완료 처리에 실패했습니다.')
    }
  }

  const columns: ColumnsType<AdminReturnResponse> = [
    {
      title: '반품 ID',
      dataIndex: 'returnId',
      key: 'returnId',
      width: 100,
    },
    {
      title: '주문 ID',
      dataIndex: 'orderId',
      key: 'orderId',
      sorter: (a, b) => a.orderId - b.orderId,
      render: (orderId: number, record: AdminReturnResponse) => (
        <a
          onClick={() => handleOrderClick(record.orderId)}
          style={{ color: '#007BFF', cursor: 'pointer' }}
        >
          {orderId}
        </a>
      ),
      width: 100,
    },
    {
      title: '반품 상태',
      dataIndex: 'returnStatus',
      key: 'returnStatus',
      filters: [
        { text: '반품 요청', value: 'RETURN_REQUESTED' },
        { text: '반품 승인', value: 'RETURN_APPROVED' },
        { text: '반품 거절', value: 'RETURN_REJECTED' },
        { text: '반품 완료', value: 'RETURNED' },
      ],
      onFilter: (value, record) => record.returnStatus === value,
      render: (status: string, record: AdminReturnResponse) => {
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
            {status === 'RETURN_APPROVED' && (
              <Popconfirm
                title="반품 완료 확인"
                description="상품 회수를 확인했습니까? 반품을 완료 처리하시겠습니까?"
                onConfirm={() => handleCompleteReturn(record)}
                okText="확인"
                cancelText="취소"
                okButtonProps={{
                  style: { backgroundColor: '#FFC107', borderColor: '#FFC107', color: '#343A40', fontWeight: 600 }
                }}
              >
                <Button
                  type="primary"
                  size="small"
                  icon={<CheckOutlined />}
                  style={{ backgroundColor: '#28a745', borderColor: '#28a745' }}
                >
                  완료
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
      dataIndex: 'reason',
      key: 'reason',
      ellipsis: true,
      render: (reason: string | null) => reason || <span style={{ color: '#999' }}>-</span>,
      width: 150,
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
      title: '반품 주소',
      dataIndex: 'returnAddress',
      key: 'returnAddress',
      ellipsis: true,
      width: 200,
    },
    {
      title: '요청 일시',
      dataIndex: 'requestedAt',
      key: 'requestedAt',
      sorter: (a, b) => new Date(a.requestedAt).getTime() - new Date(b.requestedAt).getTime(),
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
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      sorter: (a, b) => new Date(a.updatedAt).getTime() - new Date(b.updatedAt).getTime(),
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

        <Spin spinning={loading}>
          <Table
            columns={columns}
            dataSource={returns}
            rowKey="returnId"
            scroll={{ x: 'max-content' }}
            pagination={{
              current: currentPage + 1,
              pageSize: pageSize,
              total: totalElements,
              showSizeChanger: true,
              showTotal: (total) => `총 ${total}개`,
              onChange: (page, size) => {
                setCurrentPage(page - 1)
                setPageSize(size)
              },
            }}
          />
        </Spin>

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
          title="반품 승인 및 수거지 정보 설정"
          open={isApprovalModalVisible}
          onCancel={handleApprovalModalClose}
          onOk={handleApprovalSave}
          okText="승인"
          cancelText="취소"
          okButtonProps={{
            style: { backgroundColor: '#FFC107', borderColor: '#FFC107', color: '#343A40', fontWeight: 600 }
          }}
        >
          {selectedReturn && (
            <div style={{ marginBottom: '1rem', padding: '12px', backgroundColor: '#f5f5f5', borderRadius: '4px' }}>
              <p style={{ margin: '4px 0' }}><strong>주문 ID:</strong> {selectedReturn.orderId}</p>
              <p style={{ margin: '4px 0' }}><strong>반품 사유:</strong> {selectedReturn.reason || '-'}</p>
            </div>
          )}
          <Form
            form={approvalForm}
            layout="vertical"
          >
            <Form.Item
              label="수거지 수령인"
              name="receiverName"
              rules={[{ required: true, message: '수령인을 입력하세요' }]}
            >
              <Input
                placeholder="수령인을 입력하세요"
                maxLength={100}
              />
            </Form.Item>
            <Form.Item
              label="수거지 연락처"
              name="receiverPhone"
              rules={[
                { required: true, message: '연락처를 입력하세요' },
                { max: 20, message: '연락처는 최대 20자까지 입력 가능합니다.' }
              ]}
            >
              <Input
                placeholder="연락처를 입력하세요"
                maxLength={20}
              />
            </Form.Item>
            <Form.Item
              label="수거지 주소"
              name="returnAddress"
              rules={[
                { required: true, message: '수거지 주소를 입력하세요' },
                { max: 500, message: '주소는 최대 500자까지 입력 가능합니다.' }
              ]}
            >
              <Input.TextArea
                placeholder="수거지 주소를 입력하세요"
                maxLength={500}
                rows={3}
              />
            </Form.Item>
            <Form.Item
              label="우편번호"
              name="postalCode"
              rules={[
                { max: 20, message: '우편번호는 최대 20자까지 입력 가능합니다.' }
              ]}
            >
              <Input
                placeholder="우편번호를 입력하세요 (선택)"
                maxLength={20}
              />
            </Form.Item>
          </Form>
        </Modal>
      </div>
    </div>
  )
}

export default AdminReturnManage


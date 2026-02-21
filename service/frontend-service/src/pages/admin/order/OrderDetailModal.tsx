import { useState, useEffect } from 'react'
import { Modal, Card, Descriptions, Table, Tag, Button, Form, Select, Input, message, Space } from 'antd'
import { StopOutlined } from '@ant-design/icons'

const { TextArea } = Input
const { Option } = Select

interface Order {
  order_id: string
  order_number: string
  user_id: string
  user_name?: string
  order_status: 'CREATED' | 'PAID' | 'SHIPPING' | 'DELIVERED' | 'CANCELED'
  total_product_amount: number
  total_discount_amount: number
  total_payment_amount: number
  ordered_at: string
  updated_at: string
  order_memo?: string
}

interface OrderItem {
  order_item_id: string
  order_id: string
  product_id: string
  product_name: string
  product_code: string
  quantity: number
  unit_price: number
  total_price: number
  created_at: string
}

interface OrderShipping {
  shipping_id: string
  order_id: string
  receiver_name: string
  receiver_phone: string
  address: string
  postal_code?: string
  shipping_status: 'READY' | 'SHIPPING' | 'DELIVERED' | 'RETURNED'
  created_at: string
}

interface OrderDetailModalProps {
  open: boolean
  order: Order | null
  orderItems: OrderItem[]
  orderShipping: OrderShipping | null
  onClose: () => void
  onSave?: (orderId: string, orderStatus: string, orderMemo: string) => void
  onCancelOrder?: (orderId: string, reason: string) => Promise<void>
}

const shippingStatusMap: Record<string, { label: string; color: string }> = {
  READY: { label: '배송 준비', color: 'blue' },
  SHIPPING: { label: '배송 중', color: 'orange' },
  DELIVERED: { label: '배송 완료', color: 'green' }
}

const CANCELLABLE_STATUSES = ['CREATED', 'PAID']

function OrderDetailModal({ open, order, orderItems, orderShipping, onClose, onSave, onCancelOrder }: OrderDetailModalProps) {
  const [form] = Form.useForm()
  const [isSaving, setIsSaving] = useState(false)
  const [isCancelModalOpen, setIsCancelModalOpen] = useState(false)
  const [cancelReason, setCancelReason] = useState('')
  const [isCancelling, setIsCancelling] = useState(false)

  useEffect(() => {
    if (order) {
      form.setFieldsValue({
        order_status: order.order_status,
        order_memo: order.order_memo || ''
      })
    }
  }, [order, form])

  const handleSave = async () => {
    if (!order) return

    try {
      const values = await form.validateFields()
      setIsSaving(true)

      if (onSave) {
        await onSave(order.order_id, values.order_status, values.order_memo || '')
      }

      message.success('주문 정보가 저장되었습니다.')
      onClose()
    } catch (error) {
      console.error('Validation failed:', error)
    } finally {
      setIsSaving(false)
    }
  }

  const handleCancel = () => {
    form.resetFields()
    onClose()
  }

  const handleCancelOrderConfirm = async () => {
    if (!order || !onCancelOrder) return
    setIsCancelling(true)
    try {
      await onCancelOrder(order.order_id, cancelReason)
      message.success('주문이 취소되었습니다.')
      setIsCancelModalOpen(false)
      onClose()
    } catch (error) {
      message.error(error instanceof Error ? error.message : '주문 취소에 실패했습니다.')
    } finally {
      setIsCancelling(false)
    }
  }

  const isCancellable = order ? CANCELLABLE_STATUSES.includes(order.order_status) : false

  return (
    <>
      <Modal
        title={`주문 상세 - ${order?.order_number}`}
        open={open}
        onCancel={handleCancel}
        footer={[
          isCancellable && onCancelOrder && (
            <Button
              key="cancel-order"
              danger
              icon={<StopOutlined />}
              onClick={() => {
                setCancelReason('')
                setIsCancelModalOpen(true)
              }}
            >
              주문 취소
            </Button>
          ),
          <Button key="cancel" onClick={handleCancel}>
            닫기
          </Button>,
          <Button
            key="save"
            type="primary"
            onClick={handleSave}
            loading={isSaving}
            style={{ backgroundColor: '#FFC107', borderColor: '#FFC107', color: '#343A40', fontWeight: 600 }}
          >
            저장
          </Button>
        ].filter(Boolean)}
        width={900}
      >
        {order && (
          <Form form={form} layout="vertical">
            {/* 주문 정보 */}
            <Card title="주문 정보" size="small" style={{ marginBottom: 16 }}>
              <Descriptions column={2} bordered size="small">
                <Descriptions.Item label="주문 번호">{order.order_number}</Descriptions.Item>
                <Descriptions.Item label="주문 상태">
                  <Form.Item
                    name="order_status"
                    style={{ margin: 0 }}
                  >
                    <Select style={{ width: 150 }}>
                      <Option value="CREATED">주문 생성</Option>
                      <Option value="PAID">결제 완료</Option>
                      <Option value="SHIPPING">배송 중</Option>
                      <Option value="DELIVERED">배송 완료</Option>
                      <Option value="CANCELED">취소됨</Option>
                    </Select>
                  </Form.Item>
                </Descriptions.Item>
                <Descriptions.Item label="주문자">
                  {order.user_name || `사용자 ID: ${order.user_id}`}
                </Descriptions.Item>
                <Descriptions.Item label="주문 일시">
                  {new Date(order.ordered_at).toLocaleString('ko-KR')}
                </Descriptions.Item>
                <Descriptions.Item label="상품 총 금액">
                  {order.total_product_amount.toLocaleString()}원
                </Descriptions.Item>
                <Descriptions.Item label="할인 금액">
                  {order.total_discount_amount.toLocaleString()}원
                </Descriptions.Item>
                <Descriptions.Item label="최종 결제 금액" span={2}>
                  <strong style={{ color: '#007BFF', fontSize: '16px' }}>
                    {order.total_payment_amount.toLocaleString()}원
                  </strong>
                </Descriptions.Item>
              </Descriptions>
            </Card>

            {/* 주문 메모 */}
            <Card title="주문 메모" size="small" style={{ marginBottom: 16 }}>
              <Form.Item
                name="order_memo"
                rules={[{ max: 1000, message: '메모는 최대 1000자까지 입력 가능합니다.' }]}
              >
                <TextArea
                  rows={4}
                  placeholder="주문 메모를 입력하세요 (최대 1000자)"
                  maxLength={1000}
                  showCount
                />
              </Form.Item>
            </Card>

            {/* 주문 상품 목록 */}
            <Card title="주문 상품" size="small" style={{ marginBottom: 16 }}>
              <Table
                columns={[
                  {
                    title: '상품명',
                    dataIndex: 'product_name',
                    key: 'product_name',
                  },
                  {
                    title: '상품 코드',
                    dataIndex: 'product_code',
                    key: 'product_code',
                  },
                  {
                    title: '수량',
                    dataIndex: 'quantity',
                    key: 'quantity',
                    align: 'right',
                  },
                  {
                    title: '단가',
                    dataIndex: 'unit_price',
                    key: 'unit_price',
                    render: (price: number) => `${price.toLocaleString()}원`,
                    align: 'right',
                  },
                  {
                    title: '총 금액',
                    dataIndex: 'total_price',
                    key: 'total_price',
                    render: (price: number) => (
                      <strong>{price.toLocaleString()}원</strong>
                    ),
                    align: 'right',
                  },
                ]}
                dataSource={orderItems}
                rowKey="order_item_id"
                pagination={false}
                size="small"
              />
            </Card>

            {/* 배송 정보 */}
            {orderShipping && (
              <Card title="배송 정보" size="small">
                <Descriptions column={1} bordered size="small">
                  <Descriptions.Item label="수령인">{orderShipping.receiver_name}</Descriptions.Item>
                  <Descriptions.Item label="연락처">{orderShipping.receiver_phone}</Descriptions.Item>
                  <Descriptions.Item label="우편번호">{orderShipping.postal_code || '-'}</Descriptions.Item>
                  <Descriptions.Item label="배송 주소">{orderShipping.address}</Descriptions.Item>
                  <Descriptions.Item label="배송 상태">
                    <Tag color={shippingStatusMap[orderShipping.shipping_status]?.color}>
                      {shippingStatusMap[orderShipping.shipping_status]?.label}
                    </Tag>
                  </Descriptions.Item>
                </Descriptions>
              </Card>
            )}
          </Form>
        )}
      </Modal>

      {/* 주문 취소 확인 모달 */}
      <Modal
        title="주문 취소"
        open={isCancelModalOpen}
        onCancel={() => setIsCancelModalOpen(false)}
        footer={[
          <Button key="back" onClick={() => setIsCancelModalOpen(false)}>
            닫기
          </Button>,
          <Button
            key="confirm"
            type="primary"
            danger
            loading={isCancelling}
            onClick={handleCancelOrderConfirm}
          >
            취소 확인
          </Button>
        ]}
      >
        {order && (
          <Space direction="vertical" style={{ width: '100%' }} size={12}>
            <p>
              <strong>{order.order_number}</strong> 주문을 취소하시겠습니까?
            </p>
            <p style={{ color: '#888', fontSize: '0.9rem' }}>
              취소 후 결제 환불은 결제 서비스를 통해 자동 처리됩니다.
            </p>
            <div>
              <label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>
                취소 사유 <span style={{ color: '#999', fontWeight: 400 }}>(선택)</span>
              </label>
              <TextArea
                rows={3}
                placeholder="취소 사유를 입력하세요."
                maxLength={200}
                showCount
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
              />
            </div>
          </Space>
        )}
      </Modal>
    </>
  )
}

export default OrderDetailModal
export type { Order, OrderItem, OrderShipping }

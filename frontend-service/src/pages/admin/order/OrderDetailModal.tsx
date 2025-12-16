import { Modal, Card, Descriptions, Table, Tag, Button } from 'antd'

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
  shipping_status: 'READY' | 'SHIPPING' | 'DELIVERED'
  created_at: string
}

interface OrderDetailModalProps {
  open: boolean
  order: Order | null
  orderItems: OrderItem[]
  orderShipping: OrderShipping | null
  onClose: () => void
}

const statusMap: Record<string, { label: string; color: string }> = {
  CREATED: { label: '주문 생성', color: 'blue' },
  PAID: { label: '결제 완료', color: 'green' },
  SHIPPING: { label: '배송 중', color: 'orange' },
  DELIVERED: { label: '배송 완료', color: 'cyan' },
  CANCELED: { label: '취소됨', color: 'red' }
}

const shippingStatusMap: Record<string, { label: string; color: string }> = {
  READY: { label: '배송 준비', color: 'blue' },
  SHIPPING: { label: '배송 중', color: 'orange' },
  DELIVERED: { label: '배송 완료', color: 'green' }
}

function OrderDetailModal({ open, order, orderItems, orderShipping, onClose }: OrderDetailModalProps) {
  return (
    <Modal
      title={`주문 상세 - ${order?.order_number}`}
      open={open}
      onCancel={onClose}
      footer={[
        <Button key="close" onClick={onClose}>
          닫기
        </Button>
      ]}
      width={900}
    >
      {order && (
        <>
          {/* 주문 정보 */}
          <Card title="주문 정보" size="small" style={{ marginBottom: 16 }}>
            <Descriptions column={2} bordered size="small">
              <Descriptions.Item label="주문 번호">{order.order_number}</Descriptions.Item>
              <Descriptions.Item label="주문 상태">
                <Tag color={statusMap[order.order_status]?.color}>
                  {statusMap[order.order_status]?.label}
                </Tag>
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
        </>
      )}
    </Modal>
  )
}

export default OrderDetailModal
export type { Order, OrderItem, OrderShipping }


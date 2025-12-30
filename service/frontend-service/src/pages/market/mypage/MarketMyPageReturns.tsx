import { useState, useEffect } from 'react'
import { Card, Table, Tag, Button, Space, Empty, message, Modal, Form, Input, Select, Descriptions, Tabs } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { UndoOutlined, EyeOutlined, SwapOutlined } from '@ant-design/icons'
import './MarketMyPageReturns.css'

const { TabPane } = Tabs
const { TextArea } = Input
const { Option } = Select

interface OrderItem {
  order_item_id: string
  product_id: string
  product_name: string
  product_code: string
  quantity: number
  price: number
  total_price: number
}

interface ReturnRefund {
  return_id: string
  order_id: string
  order_number: string
  return_status: 'RETURN_REQUESTED' | 'RETURN_APPROVED' | 'RETURN_REJECTED' | 'RETURNED' | 'REFUNDED'
  return_reason?: string
  return_reason_detail?: string
  refund_amount?: number
  refund_method?: string
  return_shipping_company?: string
  return_tracking_number?: string
  created_at: string
  updated_at: string
  items: OrderItem[]
}

interface ReturnableOrder {
  order_id: string
  order_number: string
  order_status: 'DELIVERED' | 'SHIPPING'
  total_amount: number
  ordered_at: string
  delivered_at?: string
  items: OrderItem[]
}

interface Exchange {
  exchange_id: string
  order_id: string
  order_number: string
  exchange_status: 'EXCHANGE_REQUESTED' | 'EXCHANGE_APPROVED' | 'RETURNED' | 'EXCHANGE_SHIPPING' | 'EXCHANGED'
  exchange_reason?: string
  exchange_reason_detail?: string
  exchange_product_name?: string
  exchange_product_code?: string
  return_shipping_company?: string
  return_tracking_number?: string
  exchange_shipping_company?: string
  exchange_tracking_number?: string
  created_at: string
  updated_at: string
  items: OrderItem[]
}

function MarketMyPageReturns() {
  const [returnRefunds, setReturnRefunds] = useState<ReturnRefund[]>([])
  const [exchanges, setExchanges] = useState<Exchange[]>([])
  const [returnableOrders, setReturnableOrders] = useState<ReturnableOrder[]>([])
  const [exchangeableOrders, setExchangeableOrders] = useState<ReturnableOrder[]>([])
  const [loading, setLoading] = useState(false)
  const [selectedOrder, setSelectedOrder] = useState<ReturnableOrder | null>(null)
  const [selectedExchangeOrder, setSelectedExchangeOrder] = useState<ReturnableOrder | null>(null)
  const [isRequestModalVisible, setIsRequestModalVisible] = useState(false)
  const [isExchangeModalVisible, setIsExchangeModalVisible] = useState(false)
  const [isDetailModalVisible, setIsDetailModalVisible] = useState(false)
  const [isExchangeDetailModalVisible, setIsExchangeDetailModalVisible] = useState(false)
  const [selectedReturn, setSelectedReturn] = useState<ReturnRefund | null>(null)
  const [selectedExchange, setSelectedExchange] = useState<Exchange | null>(null)
  const [requestForm] = Form.useForm()
  const [exchangeForm] = Form.useForm()
  const [activeTab, setActiveTab] = useState('history')

  useEffect(() => {
    loadReturnRefunds()
    loadExchanges()
    loadReturnableOrders()
    loadExchangeableOrders()
  }, [])

  const loadReturnRefunds = async () => {
    setLoading(true)
    try {
      // TODO: API 호출로 반품/환불 내역 가져오기
      await new Promise(resolve => setTimeout(resolve, 500))
      
      const sampleReturns: ReturnRefund[] = [
        {
          return_id: '1',
          order_id: '1',
          order_number: 'ORD-20240101-001',
          return_status: 'RETURN_REQUESTED',
          return_reason: '단순 변심',
          return_reason_detail: '다른 상품으로 변경하고 싶습니다.',
          refund_amount: 1200000,
          created_at: '2024-01-15 14:00:00',
          updated_at: '2024-01-15 14:00:00',
          items: [
            {
              order_item_id: '1',
              product_id: '1',
              product_name: '프리미엄 노트북',
              product_code: 'PRD-001',
              quantity: 1,
              price: 1200000,
              total_price: 1200000
            }
          ]
        },
        {
          return_id: '2',
          order_id: '2',
          order_number: 'ORD-20240102-002',
          return_status: 'REFUNDED',
          return_reason: '상품 불량',
          return_reason_detail: '화면에 불량이 있습니다.',
          refund_amount: 800000,
          refund_method: '신용카드',
          created_at: '2024-01-10 10:00:00',
          updated_at: '2024-01-12 15:00:00',
          items: [
            {
              order_item_id: '2',
              product_id: '2',
              product_name: '최신 스마트폰',
              product_code: 'PRD-002',
              quantity: 1,
              price: 800000,
              total_price: 800000
            }
          ]
        }
      ]
      
      setReturnRefunds(sampleReturns)
    } catch (error) {
      message.error('반품/환불 내역을 불러오는데 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const loadExchanges = async () => {
    setLoading(true)
    try {
      // TODO: API 호출로 교환 내역 가져오기
      await new Promise(resolve => setTimeout(resolve, 500))
      
      const sampleExchanges: Exchange[] = [
        {
          exchange_id: '1',
          order_id: '3',
          order_number: 'ORD-20240103-003',
          exchange_status: 'EXCHANGE_REQUESTED',
          exchange_reason: '사이즈 불일치',
          exchange_reason_detail: '다른 사이즈로 교환하고 싶습니다.',
          exchange_product_name: '무선 이어폰 (대형)',
          exchange_product_code: 'PRD-004-L',
          created_at: '2024-01-16 10:00:00',
          updated_at: '2024-01-16 10:00:00',
          items: [
            {
              order_item_id: '3',
              product_id: '4',
              product_name: '무선 이어폰',
              product_code: 'PRD-004',
              quantity: 1,
              price: 150000,
              total_price: 150000
            }
          ]
        },
        {
          exchange_id: '2',
          order_id: '4',
          order_number: 'ORD-20240104-004',
          exchange_status: 'EXCHANGED',
          exchange_reason: '색상 불일치',
          exchange_reason_detail: '다른 색상으로 교환하고 싶습니다.',
          exchange_product_name: '프리미엄 노트북 (실버)',
          exchange_product_code: 'PRD-001-S',
          return_shipping_company: 'CJ대한통운',
          return_tracking_number: '1111111111111',
          exchange_shipping_company: 'CJ대한통운',
          exchange_tracking_number: '2222222222222',
          created_at: '2024-01-10 14:00:00',
          updated_at: '2024-01-13 16:00:00',
          items: [
            {
              order_item_id: '4',
              product_id: '1',
              product_name: '프리미엄 노트북',
              product_code: 'PRD-001',
              quantity: 1,
              price: 1200000,
              total_price: 1200000
            }
          ]
        }
      ]
      
      setExchanges(sampleExchanges)
    } catch (error) {
      message.error('교환 내역을 불러오는데 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const loadReturnableOrders = async () => {
    try {
      // TODO: API 호출로 반품 신청 가능한 주문 가져오기
      await new Promise(resolve => setTimeout(resolve, 300))
      
      const sampleOrders: ReturnableOrder[] = [
        {
          order_id: '5',
          order_number: 'ORD-20240105-005',
          order_status: 'DELIVERED',
          total_amount: 200000,
          ordered_at: '2024-01-05 11:00:00',
          delivered_at: '2024-01-07 15:00:00',
          items: [
            {
              order_item_id: '5',
              product_id: '6',
              product_name: '프리미엄 티셔츠',
              product_code: 'PRD-005',
              quantity: 1,
              price: 200000,
              total_price: 200000
            }
          ]
        }
      ]
      
      setReturnableOrders(sampleOrders)
    } catch (error) {
      message.error('반품 신청 가능한 주문을 불러오는데 실패했습니다.')
    }
  }

  const loadExchangeableOrders = async () => {
    try {
      // TODO: API 호출로 교환 신청 가능한 주문 가져오기
      await new Promise(resolve => setTimeout(resolve, 300))
      
      const sampleOrders: ReturnableOrder[] = [
        {
          order_id: '6',
          order_number: 'ORD-20240106-006',
          order_status: 'DELIVERED',
          total_amount: 180000,
          ordered_at: '2024-01-06 09:00:00',
          delivered_at: '2024-01-08 12:00:00',
          items: [
            {
              order_item_id: '6',
              product_id: '7',
              product_name: '스타일리시한 바지',
              product_code: 'PRD-006',
              quantity: 1,
              price: 180000,
              total_price: 180000
            }
          ]
        }
      ]
      
      setExchangeableOrders(sampleOrders)
    } catch (error) {
      message.error('교환 신청 가능한 주문을 불러오는데 실패했습니다.')
    }
  }

  const statusMap: Record<string, { label: string; color: string }> = {
    RETURN_REQUESTED: { label: '반품 요청', color: 'blue' },
    RETURN_APPROVED: { label: '반품 승인', color: 'green' },
    RETURN_REJECTED: { label: '반품 거절', color: 'red' },
    RETURNED: { label: '반품 완료', color: 'cyan' },
    REFUNDED: { label: '환불 완료', color: 'purple' }
  }

  const exchangeStatusMap: Record<string, { label: string; color: string }> = {
    EXCHANGE_REQUESTED: { label: '교환 요청', color: 'blue' },
    EXCHANGE_APPROVED: { label: '교환 승인', color: 'green' },
    RETURNED: { label: '반품 완료', color: 'cyan' },
    EXCHANGE_SHIPPING: { label: '교환 배송 중', color: 'orange' },
    EXCHANGED: { label: '교환 완료', color: 'purple' }
  }

  const handleRequestReturn = (order: ReturnableOrder) => {
    setSelectedOrder(order)
    requestForm.resetFields()
    requestForm.setFieldsValue({
      order_id: order.order_id,
      return_reason: undefined,
      return_reason_detail: ''
    })
    setIsRequestModalVisible(true)
  }

  const handleSubmitReturn = async (values: any) => {
    try {
      // TODO: API 호출로 반품 신청
      await new Promise(resolve => setTimeout(resolve, 1000))
      
      message.success('반품 신청이 완료되었습니다.')
      setIsRequestModalVisible(false)
      loadReturnRefunds()
      loadReturnableOrders()
    } catch (error) {
      message.error('반품 신청에 실패했습니다.')
    }
  }

  const handleViewDetail = (returnRefund: ReturnRefund) => {
    setSelectedReturn(returnRefund)
    setIsDetailModalVisible(true)
  }

  const handleRequestExchange = (order: ReturnableOrder) => {
    setSelectedExchangeOrder(order)
    exchangeForm.resetFields()
    exchangeForm.setFieldsValue({
      order_id: order.order_id,
      exchange_reason: undefined,
      exchange_reason_detail: '',
      exchange_product_name: '',
      exchange_product_code: ''
    })
    setIsExchangeModalVisible(true)
  }

  const handleSubmitExchange = async (values: any) => {
    try {
      // TODO: API 호출로 교환 신청
      await new Promise(resolve => setTimeout(resolve, 1000))
      
      message.success('교환 신청이 완료되었습니다.')
      setIsExchangeModalVisible(false)
      loadExchanges()
      loadExchangeableOrders()
    } catch (error) {
      message.error('교환 신청에 실패했습니다.')
    }
  }

  const handleViewExchangeDetail = (exchange: Exchange) => {
    setSelectedExchange(exchange)
    setIsExchangeDetailModalVisible(true)
  }

  const returnRefundColumns: ColumnsType<ReturnRefund> = [
    {
      title: '주문번호',
      dataIndex: 'order_number',
      key: 'order_number',
      width: 180,
      render: (text: string) => <strong>{text}</strong>
    },
    {
      title: '신청일시',
      dataIndex: 'created_at',
      key: 'created_at',
      width: 180,
      render: (text: string) => text.split(' ')[0]
    },
    {
      title: '반품 상품',
      key: 'items',
      render: (_, record: ReturnRefund) => (
        <div>
          {record.items.length > 0 && (
            <div>
              <div>{record.items[0].product_name}</div>
              {record.items.length > 1 && (
                <div style={{ color: '#999', fontSize: '0.9rem' }}>
                  외 {record.items.length - 1}개
                </div>
              )}
            </div>
          )}
        </div>
      )
    },
    {
      title: '반품 사유',
      dataIndex: 'return_reason',
      key: 'return_reason',
      width: 120
    },
    {
      title: '환불금액',
      dataIndex: 'refund_amount',
      key: 'refund_amount',
      width: 120,
      align: 'right',
      render: (amount: number | undefined) => 
        amount ? <strong>{amount.toLocaleString()}원</strong> : <span style={{ color: '#999' }}>-</span>
    },
    {
      title: '상태',
      dataIndex: 'return_status',
      key: 'return_status',
      width: 120,
      render: (status: string) => {
        const statusInfo = statusMap[status] || { label: status, color: 'default' }
        return <Tag color={statusInfo.color}>{statusInfo.label}</Tag>
      }
    },
    {
      title: '관리',
      key: 'action',
      width: 100,
      render: (_, record: ReturnRefund) => (
        <Button
          type="link"
          icon={<EyeOutlined />}
          onClick={() => handleViewDetail(record)}
        >
          상세보기
        </Button>
      )
    }
  ]

  const returnableOrderColumns: ColumnsType<ReturnableOrder> = [
    {
      title: '주문번호',
      dataIndex: 'order_number',
      key: 'order_number',
      width: 180,
      render: (text: string) => <strong>{text}</strong>
    },
    {
      title: '주문일시',
      dataIndex: 'ordered_at',
      key: 'ordered_at',
      width: 180,
      render: (text: string) => text.split(' ')[0]
    },
    {
      title: '배송완료일',
      dataIndex: 'delivered_at',
      key: 'delivered_at',
      width: 180,
      render: (text: string | undefined) => text ? text.split(' ')[0] : <span style={{ color: '#999' }}>-</span>
    },
    {
      title: '주문 상품',
      key: 'items',
      render: (_, record: ReturnableOrder) => (
        <div>
          {record.items.length > 0 && (
            <div>
              <div>{record.items[0].product_name}</div>
              {record.items.length > 1 && (
                <div style={{ color: '#999', fontSize: '0.9rem' }}>
                  외 {record.items.length - 1}개
                </div>
              )}
            </div>
          )}
        </div>
      )
    },
    {
      title: '주문금액',
      dataIndex: 'total_amount',
      key: 'total_amount',
      width: 120,
      align: 'right',
      render: (amount: number) => <strong>{amount.toLocaleString()}원</strong>
    },
    {
      title: '관리',
      key: 'action',
      width: 120,
      render: (_, record: ReturnableOrder) => (
        <Button
          type="primary"
          icon={<UndoOutlined />}
          onClick={() => handleRequestReturn(record)}
        >
          반품신청
        </Button>
      )
    }
  ]

  const exchangeColumns: ColumnsType<Exchange> = [
    {
      title: '주문번호',
      dataIndex: 'order_number',
      key: 'order_number',
      width: 180,
      render: (text: string) => <strong>{text}</strong>
    },
    {
      title: '신청일시',
      dataIndex: 'created_at',
      key: 'created_at',
      width: 180,
      render: (text: string) => text.split(' ')[0]
    },
    {
      title: '교환 상품',
      key: 'items',
      render: (_, record: Exchange) => (
        <div>
          {record.items.length > 0 && (
            <div>
              <div>{record.items[0].product_name}</div>
              {record.items.length > 1 && (
                <div style={{ color: '#999', fontSize: '0.9rem' }}>
                  외 {record.items.length - 1}개
                </div>
              )}
            </div>
          )}
        </div>
      )
    },
    {
      title: '교환 상품',
      key: 'exchange_product',
      render: (_, record: Exchange) => (
        <div>
          {record.exchange_product_name ? (
            <div>
              <div>{record.exchange_product_name}</div>
              {record.exchange_product_code && (
                <div style={{ color: '#999', fontSize: '0.9rem' }}>
                  {record.exchange_product_code}
                </div>
              )}
            </div>
          ) : (
            <span style={{ color: '#999' }}>-</span>
          )}
        </div>
      )
    },
    {
      title: '교환 사유',
      dataIndex: 'exchange_reason',
      key: 'exchange_reason',
      width: 120
    },
    {
      title: '상태',
      dataIndex: 'exchange_status',
      key: 'exchange_status',
      width: 120,
      render: (status: string) => {
        const statusInfo = exchangeStatusMap[status] || { label: status, color: 'default' }
        return <Tag color={statusInfo.color}>{statusInfo.label}</Tag>
      }
    },
    {
      title: '관리',
      key: 'action',
      width: 100,
      render: (_, record: Exchange) => (
        <Button
          type="link"
          icon={<EyeOutlined />}
          onClick={() => handleViewExchangeDetail(record)}
        >
          상세보기
        </Button>
      )
    }
  ]

  const exchangeableOrderColumns: ColumnsType<ReturnableOrder> = [
    {
      title: '주문번호',
      dataIndex: 'order_number',
      key: 'order_number',
      width: 180,
      render: (text: string) => <strong>{text}</strong>
    },
    {
      title: '주문일시',
      dataIndex: 'ordered_at',
      key: 'ordered_at',
      width: 180,
      render: (text: string) => text.split(' ')[0]
    },
    {
      title: '배송완료일',
      dataIndex: 'delivered_at',
      key: 'delivered_at',
      width: 180,
      render: (text: string | undefined) => text ? text.split(' ')[0] : <span style={{ color: '#999' }}>-</span>
    },
    {
      title: '주문 상품',
      key: 'items',
      render: (_, record: ReturnableOrder) => (
        <div>
          {record.items.length > 0 && (
            <div>
              <div>{record.items[0].product_name}</div>
              {record.items.length > 1 && (
                <div style={{ color: '#999', fontSize: '0.9rem' }}>
                  외 {record.items.length - 1}개
                </div>
              )}
            </div>
          )}
        </div>
      )
    },
    {
      title: '주문금액',
      dataIndex: 'total_amount',
      key: 'total_amount',
      width: 120,
      align: 'right',
      render: (amount: number) => <strong>{amount.toLocaleString()}원</strong>
    },
    {
      title: '관리',
      key: 'action',
      width: 120,
      render: (_, record: ReturnableOrder) => (
        <Button
          type="primary"
          icon={<SwapOutlined />}
          onClick={() => handleRequestExchange(record)}
        >
          교환신청
        </Button>
      )
    }
  ]

  return (
    <div className="market-mypage-returns">
      <Card title="반품/교환" className="returns-card">
        <Tabs activeKey={activeTab} onChange={setActiveTab}>
          <TabPane tab="반품 내역" key="history">
            {returnRefunds.length === 0 ? (
              <Empty description="반품/환불 내역이 없습니다." />
            ) : (
              <Table
                columns={returnRefundColumns}
                dataSource={returnRefunds}
                rowKey="return_id"
                loading={loading}
                pagination={{
                  pageSize: 10,
                  showSizeChanger: true,
                  showTotal: (total) => `총 ${total}건`
                }}
              />
            )}
          </TabPane>
          <TabPane tab="반품 신청" key="request">
            {returnableOrders.length === 0 ? (
              <Empty description="반품 신청 가능한 주문이 없습니다." />
            ) : (
              <Table
                columns={returnableOrderColumns}
                dataSource={returnableOrders}
                rowKey="order_id"
                loading={loading}
                pagination={{
                  pageSize: 10,
                  showSizeChanger: true,
                  showTotal: (total) => `총 ${total}건`
                }}
              />
            )}
          </TabPane>
          <TabPane tab="교환 내역" key="exchange-history">
            {exchanges.length === 0 ? (
              <Empty description="교환 내역이 없습니다." />
            ) : (
              <Table
                columns={exchangeColumns}
                dataSource={exchanges}
                rowKey="exchange_id"
                loading={loading}
                pagination={{
                  pageSize: 10,
                  showSizeChanger: true,
                  showTotal: (total) => `총 ${total}건`
                }}
              />
            )}
          </TabPane>
          <TabPane tab="교환 신청" key="exchange-request">
            {exchangeableOrders.length === 0 ? (
              <Empty description="교환 신청 가능한 주문이 없습니다." />
            ) : (
              <Table
                columns={exchangeableOrderColumns}
                dataSource={exchangeableOrders}
                rowKey="order_id"
                loading={loading}
                pagination={{
                  pageSize: 10,
                  showSizeChanger: true,
                  showTotal: (total) => `총 ${total}건`
                }}
              />
            )}
          </TabPane>
        </Tabs>
      </Card>

      {/* 반품 신청 모달 */}
      <Modal
        title="반품 신청"
        open={isRequestModalVisible}
        onCancel={() => setIsRequestModalVisible(false)}
        footer={null}
        width={600}
      >
        {selectedOrder && (
          <Form
            form={requestForm}
            layout="vertical"
            onFinish={handleSubmitReturn}
          >
            <Form.Item name="order_id" hidden>
              <Input />
            </Form.Item>

            <Descriptions column={1} bordered style={{ marginBottom: '1.5rem' }}>
              <Descriptions.Item label="주문번호">
                {selectedOrder.order_number}
              </Descriptions.Item>
              <Descriptions.Item label="주문 상품">
                {selectedOrder.items.map((item) => (
                  <div key={item.order_item_id}>
                    {item.product_name} ({item.product_code}) × {item.quantity}개
                  </div>
                ))}
              </Descriptions.Item>
              <Descriptions.Item label="주문금액">
                {selectedOrder.total_amount.toLocaleString()}원
              </Descriptions.Item>
            </Descriptions>

            <Form.Item
              name="return_reason"
              label="반품 사유"
              rules={[{ required: true, message: '반품 사유를 선택해주세요.' }]}
            >
              <Select placeholder="반품 사유를 선택해주세요" size="large">
                <Option value="단순 변심">단순 변심</Option>
                <Option value="상품 불량">상품 불량</Option>
                <Option value="상품 하자">상품 하자</Option>
                <Option value="배송 지연">배송 지연</Option>
                <Option value="상품 불일치">상품 불일치</Option>
                <Option value="기타">기타</Option>
              </Select>
            </Form.Item>

            <Form.Item
              name="return_reason_detail"
              label="상세 사유"
              rules={[{ required: true, message: '상세 사유를 입력해주세요.' }]}
            >
              <TextArea
                rows={4}
                placeholder="반품 사유를 자세히 입력해주세요"
              />
            </Form.Item>

            <Form.Item>
              <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
                <Button onClick={() => setIsRequestModalVisible(false)}>
                  취소
                </Button>
                <Button type="primary" htmlType="submit">
                  반품 신청
                </Button>
              </Space>
            </Form.Item>
          </Form>
        )}
      </Modal>

      {/* 반품/환불 상세 모달 */}
      <Modal
        title="반품/환불 상세 정보"
        open={isDetailModalVisible}
        onCancel={() => setIsDetailModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setIsDetailModalVisible(false)}>
            닫기
          </Button>
        ]}
        width={700}
      >
        {selectedReturn && (
          <Descriptions column={1} bordered>
            <Descriptions.Item label="주문번호">
              {selectedReturn.order_number}
            </Descriptions.Item>
            <Descriptions.Item label="상태">
              <Tag color={statusMap[selectedReturn.return_status]?.color}>
                {statusMap[selectedReturn.return_status]?.label}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="반품 상품">
              {selectedReturn.items.map((item) => (
                <div key={item.order_item_id} style={{ marginBottom: '0.5rem' }}>
                  {item.product_name} ({item.product_code}) × {item.quantity}개
                </div>
              ))}
            </Descriptions.Item>
            <Descriptions.Item label="반품 사유">
              {selectedReturn.return_reason}
            </Descriptions.Item>
            {selectedReturn.return_reason_detail && (
              <Descriptions.Item label="상세 사유">
                {selectedReturn.return_reason_detail}
              </Descriptions.Item>
            )}
            {selectedReturn.refund_amount && (
              <Descriptions.Item label="환불금액">
                <strong>{selectedReturn.refund_amount.toLocaleString()}원</strong>
              </Descriptions.Item>
            )}
            {selectedReturn.refund_method && (
              <Descriptions.Item label="환불 방법">
                {selectedReturn.refund_method}
              </Descriptions.Item>
            )}
            {selectedReturn.return_tracking_number && (
              <Descriptions.Item label="반품 운송장 번호">
                {selectedReturn.return_tracking_number}
                {selectedReturn.return_shipping_company && (
                  <span style={{ marginLeft: '0.5rem', color: '#999' }}>
                    ({selectedReturn.return_shipping_company})
                  </span>
                )}
              </Descriptions.Item>
            )}
            <Descriptions.Item label="신청일시">
              {selectedReturn.created_at}
            </Descriptions.Item>
            <Descriptions.Item label="최종 수정일시">
              {selectedReturn.updated_at}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>

      {/* 교환 신청 모달 */}
      <Modal
        title="교환 신청"
        open={isExchangeModalVisible}
        onCancel={() => setIsExchangeModalVisible(false)}
        footer={null}
        width={600}
      >
        {selectedExchangeOrder && (
          <Form
            form={exchangeForm}
            layout="vertical"
            onFinish={handleSubmitExchange}
          >
            <Form.Item name="order_id" hidden>
              <Input />
            </Form.Item>

            <Descriptions column={1} bordered style={{ marginBottom: '1.5rem' }}>
              <Descriptions.Item label="주문번호">
                {selectedExchangeOrder.order_number}
              </Descriptions.Item>
              <Descriptions.Item label="주문 상품">
                {selectedExchangeOrder.items.map((item) => (
                  <div key={item.order_item_id}>
                    {item.product_name} ({item.product_code}) × {item.quantity}개
                  </div>
                ))}
              </Descriptions.Item>
              <Descriptions.Item label="주문금액">
                {selectedExchangeOrder.total_amount.toLocaleString()}원
              </Descriptions.Item>
            </Descriptions>

            <Form.Item
              name="exchange_reason"
              label="교환 사유"
              rules={[{ required: true, message: '교환 사유를 선택해주세요.' }]}
            >
              <Select placeholder="교환 사유를 선택해주세요" size="large">
                <Option value="사이즈 불일치">사이즈 불일치</Option>
                <Option value="색상 불일치">색상 불일치</Option>
                <Option value="상품 불량">상품 불량</Option>
                <Option value="상품 하자">상품 하자</Option>
                <Option value="다른 상품으로 변경">다른 상품으로 변경</Option>
                <Option value="기타">기타</Option>
              </Select>
            </Form.Item>

            <Form.Item
              name="exchange_reason_detail"
              label="상세 사유"
              rules={[{ required: true, message: '상세 사유를 입력해주세요.' }]}
            >
              <TextArea
                rows={4}
                placeholder="교환 사유를 자세히 입력해주세요"
              />
            </Form.Item>

            <Form.Item
              name="exchange_product_name"
              label="교환 받을 상품명"
              rules={[{ required: true, message: '교환 받을 상품명을 입력해주세요.' }]}
            >
              <Input placeholder="교환 받을 상품명을 입력해주세요" size="large" />
            </Form.Item>

            <Form.Item
              name="exchange_product_code"
              label="교환 받을 상품 코드 (선택사항)"
            >
              <Input placeholder="교환 받을 상품 코드를 입력해주세요" size="large" />
            </Form.Item>

            <Form.Item>
              <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
                <Button onClick={() => setIsExchangeModalVisible(false)}>
                  취소
                </Button>
                <Button type="primary" htmlType="submit">
                  교환 신청
                </Button>
              </Space>
            </Form.Item>
          </Form>
        )}
      </Modal>

      {/* 교환 상세 모달 */}
      <Modal
        title="교환 상세 정보"
        open={isExchangeDetailModalVisible}
        onCancel={() => setIsExchangeDetailModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setIsExchangeDetailModalVisible(false)}>
            닫기
          </Button>
        ]}
        width={700}
      >
        {selectedExchange && (
          <Descriptions column={1} bordered>
            <Descriptions.Item label="주문번호">
              {selectedExchange.order_number}
            </Descriptions.Item>
            <Descriptions.Item label="상태">
              <Tag color={exchangeStatusMap[selectedExchange.exchange_status]?.color}>
                {exchangeStatusMap[selectedExchange.exchange_status]?.label}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="교환 전 상품">
              {selectedExchange.items.map((item) => (
                <div key={item.order_item_id} style={{ marginBottom: '0.5rem' }}>
                  {item.product_name} ({item.product_code}) × {item.quantity}개
                </div>
              ))}
            </Descriptions.Item>
            {selectedExchange.exchange_product_name && (
              <Descriptions.Item label="교환 후 상품">
                {selectedExchange.exchange_product_name}
                {selectedExchange.exchange_product_code && (
                  <span style={{ marginLeft: '0.5rem', color: '#999' }}>
                    ({selectedExchange.exchange_product_code})
                  </span>
                )}
              </Descriptions.Item>
            )}
            <Descriptions.Item label="교환 사유">
              {selectedExchange.exchange_reason}
            </Descriptions.Item>
            {selectedExchange.exchange_reason_detail && (
              <Descriptions.Item label="상세 사유">
                {selectedExchange.exchange_reason_detail}
              </Descriptions.Item>
            )}
            {selectedExchange.return_tracking_number && (
              <Descriptions.Item label="반품 운송장 번호">
                {selectedExchange.return_tracking_number}
                {selectedExchange.return_shipping_company && (
                  <span style={{ marginLeft: '0.5rem', color: '#999' }}>
                    ({selectedExchange.return_shipping_company})
                  </span>
                )}
              </Descriptions.Item>
            )}
            {selectedExchange.exchange_tracking_number && (
              <Descriptions.Item label="교환 배송 운송장 번호">
                {selectedExchange.exchange_tracking_number}
                {selectedExchange.exchange_shipping_company && (
                  <span style={{ marginLeft: '0.5rem', color: '#999' }}>
                    ({selectedExchange.exchange_shipping_company})
                  </span>
                )}
              </Descriptions.Item>
            )}
            <Descriptions.Item label="신청일시">
              {selectedExchange.created_at}
            </Descriptions.Item>
            <Descriptions.Item label="최종 수정일시">
              {selectedExchange.updated_at}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  )
}

export default MarketMyPageReturns


import { useState, useEffect } from 'react'
import { Card, Table, Tag, Button, Space, Empty, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { EyeOutlined } from '@ant-design/icons'
import './MarketMyPageOrders.css'

interface OrderItem {
  order_item_id: string
  product_id: string
  product_name: string
  product_code: string
  quantity: number
  price: number
  total_price: number
}

interface Order {
  order_id: string
  order_number: string
  order_status: 'CREATED' | 'PAID' | 'SHIPPING' | 'DELIVERED' | 'CANCELED'
  total_amount: number
  ordered_at: string
  items: OrderItem[]
}

function MarketMyPageOrders() {
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    loadOrders()
  }, [])

  const loadOrders = async () => {
    setLoading(true)
    try {
      // TODO: API 호출로 주문 내역 가져오기
      // 임시 샘플 데이터
      await new Promise(resolve => setTimeout(resolve, 500))
      
      const sampleOrders: Order[] = [
        {
          order_id: '1',
          order_number: 'ORD-20240101-001',
          order_status: 'DELIVERED',
          total_amount: 1200000,
          ordered_at: '2024-01-01 10:30:00',
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
          order_id: '2',
          order_number: 'ORD-20240102-002',
          order_status: 'SHIPPING',
          total_amount: 800000,
          ordered_at: '2024-01-02 14:20:00',
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
        },
        {
          order_id: '3',
          order_number: 'ORD-20240103-003',
          order_status: 'PAID',
          total_amount: 150000,
          ordered_at: '2024-01-03 09:15:00',
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
        }
      ]
      
      setOrders(sampleOrders)
    } catch (error) {
      message.error('주문 내역을 불러오는데 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const statusMap: Record<string, { label: string; color: string }> = {
    CREATED: { label: '주문 생성', color: 'blue' },
    PAID: { label: '결제 완료', color: 'green' },
    SHIPPING: { label: '배송 중', color: 'orange' },
    DELIVERED: { label: '배송 완료', color: 'cyan' },
    CANCELED: { label: '취소됨', color: 'red' }
  }

  const handleViewDetail = (orderId: string) => {
    // TODO: 주문 상세 페이지로 이동
    message.info('주문 상세 페이지는 준비 중입니다.')
  }

  const columns: ColumnsType<Order> = [
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
      render: (text: string) => text.replace(' ', '\n').split('\n')[0] + '\n' + text.split(' ')[1]
    },
    {
      title: '주문 상품',
      key: 'items',
      render: (_, record: Order) => (
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
      render: (amount: number) => (
        <strong>{amount.toLocaleString()}원</strong>
      )
    },
    {
      title: '주문상태',
      dataIndex: 'order_status',
      key: 'order_status',
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
      render: (_, record: Order) => (
        <Button
          type="link"
          icon={<EyeOutlined />}
          onClick={() => handleViewDetail(record.order_id)}
        >
          상세보기
        </Button>
      )
    }
  ]

  return (
    <div className="market-mypage-orders">
      <Card title="주문 내역" className="orders-card">
        {orders.length === 0 ? (
          <Empty description="주문 내역이 없습니다." />
        ) : (
          <Table
            columns={columns}
            dataSource={orders}
            rowKey="order_id"
            loading={loading}
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              showTotal: (total) => `총 ${total}건`
            }}
          />
        )}
      </Card>
    </div>
  )
}

export default MarketMyPageOrders


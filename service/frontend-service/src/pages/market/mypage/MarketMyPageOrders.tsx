import { useState, useEffect } from 'react'
import { Card, Table, Tag, Button, Empty, message } from 'antd'
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table'
import { EyeOutlined } from '@ant-design/icons'
import { getMyOrders } from '../../../api/orderApi'
import type { MyOrderResponse } from '../../../api/orderApi'
import './MarketMyPageOrders.css'

function MarketMyPageOrders() {
  const [orders, setOrders] = useState<MyOrderResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [totalElements, setTotalElements] = useState(0)
  const [currentPage, setCurrentPage] = useState(1)
  const [pageSize, setPageSize] = useState(10)

  useEffect(() => {
    loadOrders(currentPage, pageSize)
  }, [currentPage, pageSize])

  const loadOrders = async (page: number, size: number) => {
    setLoading(true)
    try {
      const data = await getMyOrders(page - 1, size)
      setOrders(data.content)
      setTotalElements(data.totalElements)
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

  const handleViewDetail = (_orderId: number) => {
    // TODO: 주문 상세 페이지로 이동
    message.info('주문 상세 페이지는 준비 중입니다.')
  }

  const handleTableChange = (pagination: TablePaginationConfig) => {
    setCurrentPage(pagination.current || 1)
    setPageSize(pagination.pageSize || 10)
  }

  const columns: ColumnsType<MyOrderResponse> = [
    {
      title: '주문번호',
      dataIndex: 'orderNumber',
      key: 'orderNumber',
      width: 180,
      render: (text: string) => <strong>{text}</strong>
    },
    {
      title: '주문일시',
      dataIndex: 'orderedAt',
      key: 'orderedAt',
      width: 180,
      render: (text: string) => text.replace(' ', '\n').split('\n')[0] + '\n' + text.split(' ')[1]
    },
    {
      title: '주문 상품',
      key: 'items',
      render: (_, record: MyOrderResponse) => (
        <div>
          {record.items.length > 0 && (
            <div>
              <div>{record.items[0].productName}</div>
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
      dataIndex: 'totalPaymentAmount',
      key: 'totalPaymentAmount',
      width: 120,
      align: 'right',
      render: (amount: number) => (
        <strong>{amount.toLocaleString()}원</strong>
      )
    },
    {
      title: '주문상태',
      dataIndex: 'orderStatus',
      key: 'orderStatus',
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
      render: (_, record: MyOrderResponse) => (
        <Button
          type="link"
          icon={<EyeOutlined />}
          onClick={() => handleViewDetail(record.orderId)}
        >
          상세보기
        </Button>
      )
    }
  ]

  return (
    <div className="market-mypage-orders">
      <Card title="주문 내역" className="orders-card">
        {orders.length === 0 && !loading ? (
          <Empty description="주문 내역이 없습니다." />
        ) : (
          <Table
            columns={columns}
            dataSource={orders}
            rowKey="orderId"
            loading={loading}
            onChange={handleTableChange}
            pagination={{
              current: currentPage,
              pageSize: pageSize,
              total: totalElements,
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

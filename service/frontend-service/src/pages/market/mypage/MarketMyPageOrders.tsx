import { useState, useEffect } from 'react'
import { Card, Table, Tag, Button, Empty, message, Modal, Space, Input } from 'antd'
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table'
import { EyeOutlined, StopOutlined } from '@ant-design/icons'
import { getMyOrders, cancelOrder } from '../../../api/orderApi'
import type { MyOrderResponse } from '../../../api/orderApi'
import './MarketMyPageOrders.css'

const { TextArea } = Input

const CANCELLABLE_STATUSES = ['CREATED', 'PAID']

function MarketMyPageOrders() {
  const [orders, setOrders] = useState<MyOrderResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [totalElements, setTotalElements] = useState(0)
  const [currentPage, setCurrentPage] = useState(1)
  const [pageSize, setPageSize] = useState(10)

  const [cancelTargetOrder, setCancelTargetOrder] = useState<MyOrderResponse | null>(null)
  const [cancelReason, setCancelReason] = useState('')
  const [isCancelling, setIsCancelling] = useState(false)

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
    message.info('주문 상세 페이지는 준비 중입니다.')
  }

  const handleCancelClick = (order: MyOrderResponse) => {
    setCancelTargetOrder(order)
    setCancelReason('')
  }

  const handleCancelConfirm = async () => {
    if (!cancelTargetOrder) return
    setIsCancelling(true)
    try {
      await cancelOrder(cancelTargetOrder.orderId, cancelReason || undefined)
      message.success('주문이 취소되었습니다.')
      setCancelTargetOrder(null)
      loadOrders(currentPage, pageSize)
    } catch (error) {
      message.error(error instanceof Error ? error.message : '주문 취소에 실패했습니다.')
    } finally {
      setIsCancelling(false)
    }
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
      width: 160,
      render: (_, record: MyOrderResponse) => (
        <Space>
          <Button
            type="link"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetail(record.orderId)}
          >
            상세보기
          </Button>
          {CANCELLABLE_STATUSES.includes(record.orderStatus) && (
            <Button
              type="link"
              danger
              icon={<StopOutlined />}
              onClick={() => handleCancelClick(record)}
            >
              취소
            </Button>
          )}
        </Space>
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

      {/* 주문 취소 확인 모달 */}
      <Modal
        title="주문 취소"
        open={!!cancelTargetOrder}
        onCancel={() => setCancelTargetOrder(null)}
        footer={[
          <Button key="back" onClick={() => setCancelTargetOrder(null)}>
            닫기
          </Button>,
          <Button
            key="confirm"
            type="primary"
            danger
            loading={isCancelling}
            onClick={handleCancelConfirm}
          >
            취소 확인
          </Button>
        ]}
      >
        {cancelTargetOrder && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <p>
              <strong>{cancelTargetOrder.orderNumber}</strong> 주문을 취소하시겠습니까?
            </p>
            <p style={{ color: '#888', fontSize: '0.9rem' }}>
              결제 취소는 영업일 기준 3~5일 내 처리됩니다.
            </p>
            <div>
              <label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>
                취소 사유 <span style={{ color: '#999', fontWeight: 400 }}>(선택)</span>
              </label>
              <TextArea
                rows={3}
                placeholder="취소 사유를 입력해 주세요. (예: 단순 변심, 주문 실수 등)"
                maxLength={200}
                showCount
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
              />
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}

export default MarketMyPageOrders

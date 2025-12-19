import { useState, useEffect } from 'react'
import { Card, Table, Tag, Button, Space, Empty, message, Statistic, Tabs } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { DollarOutlined, PlusOutlined, MinusOutlined, EyeOutlined } from '@ant-design/icons'
import './MarketMyPagePoints.css'

const { TabPane } = Tabs

interface PointTransaction {
  transaction_id: string
  transaction_type: 'EARNED' | 'USED' | 'EXPIRED' | 'REFUNDED'
  points: number
  description: string
  order_id?: string
  order_number?: string
  expired_at?: string
  created_at: string
}

function MarketMyPagePoints() {
  const [currentPoints, setCurrentPoints] = useState<number>(0)
  const [totalEarned, setTotalEarned] = useState<number>(0)
  const [totalUsed, setTotalUsed] = useState<number>(0)
  const [expiringSoon, setExpiringSoon] = useState<number>(0)
  const [transactions, setTransactions] = useState<PointTransaction[]>([])
  const [loading, setLoading] = useState(false)
  const [activeTab, setActiveTab] = useState('all')

  useEffect(() => {
    loadPoints()
    loadTransactions()
  }, [])

  const loadPoints = async () => {
    try {
      // TODO: API 호출로 포인트 정보 가져오기
      await new Promise(resolve => setTimeout(resolve, 300))
      
      // 임시 샘플 데이터
      setCurrentPoints(12500)
      setTotalEarned(50000)
      setTotalUsed(37500)
      setExpiringSoon(5000)
    } catch (error) {
      message.error('포인트 정보를 불러오는데 실패했습니다.')
    }
  }

  const loadTransactions = async () => {
    setLoading(true)
    try {
      // TODO: API 호출로 포인트 내역 가져오기
      await new Promise(resolve => setTimeout(resolve, 500))
      
      const sampleTransactions: PointTransaction[] = [
        {
          transaction_id: '1',
          transaction_type: 'EARNED',
          points: 1200,
          description: '주문 적립 (ORD-20240101-001)',
          order_id: '1',
          order_number: 'ORD-20240101-001',
          created_at: '2024-01-01 10:30:00',
          expired_at: '2025-01-01 23:59:59'
        },
        {
          transaction_id: '2',
          transaction_type: 'EARNED',
          points: 800,
          description: '주문 적립 (ORD-20240102-002)',
          order_id: '2',
          order_number: 'ORD-20240102-002',
          created_at: '2024-01-02 14:20:00',
          expired_at: '2025-01-02 23:59:59'
        },
        {
          transaction_id: '3',
          transaction_type: 'USED',
          points: -5000,
          description: '주문 시 포인트 사용 (ORD-20240103-003)',
          order_id: '3',
          order_number: 'ORD-20240103-003',
          created_at: '2024-01-03 09:15:00'
        },
        {
          transaction_id: '4',
          transaction_type: 'EARNED',
          points: 150,
          description: '리뷰 작성 적립',
          created_at: '2024-01-04 11:00:00',
          expired_at: '2025-01-04 23:59:59'
        },
        {
          transaction_id: '5',
          transaction_type: 'EXPIRED',
          points: -2000,
          description: '포인트 만료',
          created_at: '2024-01-05 00:00:00'
        },
        {
          transaction_id: '6',
          transaction_type: 'REFUNDED',
          points: 5000,
          description: '주문 취소로 인한 포인트 환불 (ORD-20240103-003)',
          order_id: '3',
          order_number: 'ORD-20240103-003',
          created_at: '2024-01-06 10:00:00',
          expired_at: '2025-01-06 23:59:59'
        }
      ]
      
      setTransactions(sampleTransactions)
    } catch (error) {
      message.error('포인트 내역을 불러오는데 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const getTransactionTypeInfo = (type: string) => {
    const typeMap: Record<string, { label: string; color: string; icon: JSX.Element }> = {
      EARNED: { 
        label: '적립', 
        color: 'green',
        icon: <PlusOutlined />
      },
      USED: { 
        label: '사용', 
        color: 'red',
        icon: <MinusOutlined />
      },
      EXPIRED: { 
        label: '만료', 
        color: 'default',
        icon: <MinusOutlined />
      },
      REFUNDED: { 
        label: '환불', 
        color: 'blue',
        icon: <PlusOutlined />
      }
    }
    return typeMap[type] || { label: type, color: 'default', icon: <DollarOutlined /> }
  }

  const getFilteredTransactions = () => {
    if (activeTab === 'earned') {
      return transactions.filter(t => t.transaction_type === 'EARNED' || t.transaction_type === 'REFUNDED')
    } else if (activeTab === 'used') {
      return transactions.filter(t => t.transaction_type === 'USED' || t.transaction_type === 'EXPIRED')
    }
    return transactions
  }

  const columns: ColumnsType<PointTransaction> = [
    {
      title: '일시',
      dataIndex: 'created_at',
      key: 'created_at',
      width: 180,
      render: (text: string) => text.split(' ')[0] + '\n' + text.split(' ')[1]
    },
    {
      title: '유형',
      dataIndex: 'transaction_type',
      key: 'transaction_type',
      width: 120,
      render: (type: string) => {
        const typeInfo = getTransactionTypeInfo(type)
        return (
          <Tag color={typeInfo.color} icon={typeInfo.icon}>
            {typeInfo.label}
          </Tag>
        )
      }
    },
    {
      title: '내용',
      dataIndex: 'description',
      key: 'description',
      render: (text: string, record: PointTransaction) => (
        <div>
          <div>{text}</div>
          {record.order_number && (
            <div style={{ color: '#999', fontSize: '0.85rem', marginTop: '0.25rem' }}>
              주문번호: {record.order_number}
            </div>
          )}
        </div>
      )
    },
    {
      title: '포인트',
      dataIndex: 'points',
      key: 'points',
      width: 150,
      align: 'right',
      render: (points: number, record: PointTransaction) => {
        const isPositive = points > 0
        return (
          <div style={{ 
            color: isPositive ? '#52c41a' : '#ff4d4f',
            fontWeight: 600,
            fontSize: '1rem'
          }}>
            {isPositive ? '+' : ''}{points.toLocaleString()}P
          </div>
        )
      }
    },
    {
      title: '만료일',
      dataIndex: 'expired_at',
      key: 'expired_at',
      width: 180,
      render: (expiredAt: string | undefined) => 
        expiredAt ? (
          <div>
            <div>{expiredAt.split(' ')[0]}</div>
            <div style={{ color: '#999', fontSize: '0.85rem' }}>
              {expiredAt.split(' ')[1]}
            </div>
          </div>
        ) : (
          <span style={{ color: '#999' }}>-</span>
        )
    }
  ]

  const filteredTransactions = getFilteredTransactions()

  return (
    <div className="market-mypage-points">
      <Card title="포인트" className="points-card">
        {/* 포인트 요약 */}
        <div className="points-summary">
          <Card className="summary-card">
            <Statistic
              title="현재 보유 포인트"
              value={currentPoints}
              suffix="P"
              valueStyle={{ color: '#667eea', fontSize: '2rem', fontWeight: 700 }}
              prefix={<DollarOutlined />}
            />
          </Card>
          <Card className="summary-card">
            <Statistic
              title="총 적립 포인트"
              value={totalEarned}
              suffix="P"
              valueStyle={{ color: '#52c41a' }}
              prefix={<PlusOutlined />}
            />
          </Card>
          <Card className="summary-card">
            <Statistic
              title="총 사용 포인트"
              value={totalUsed}
              suffix="P"
              valueStyle={{ color: '#ff4d4f' }}
              prefix={<MinusOutlined />}
            />
          </Card>
          <Card className="summary-card warning-card">
            <Statistic
              title="만료 예정 포인트"
              value={expiringSoon}
              suffix="P"
              valueStyle={{ color: '#faad14' }}
              prefix={<DollarOutlined />}
            />
            <div className="expiry-notice">
              <small>30일 이내 만료 예정</small>
            </div>
          </Card>
        </div>

        <Tabs activeKey={activeTab} onChange={setActiveTab} style={{ marginTop: '2rem' }}>
          <TabPane 
            tab={`전체 내역 (${transactions.length})`} 
            key="all"
          >
            {filteredTransactions.length === 0 ? (
              <Empty description="포인트 내역이 없습니다." />
            ) : (
              <Table
                columns={columns}
                dataSource={filteredTransactions}
                rowKey="transaction_id"
                loading={loading}
                pagination={{
                  pageSize: 10,
                  showSizeChanger: true,
                  showTotal: (total) => `총 ${total}건`
                }}
              />
            )}
          </TabPane>
          <TabPane 
            tab={`적립 내역 (${transactions.filter(t => t.transaction_type === 'EARNED' || t.transaction_type === 'REFUNDED').length})`} 
            key="earned"
          >
            {filteredTransactions.length === 0 ? (
              <Empty description="적립 내역이 없습니다." />
            ) : (
              <Table
                columns={columns}
                dataSource={filteredTransactions}
                rowKey="transaction_id"
                loading={loading}
                pagination={{
                  pageSize: 10,
                  showSizeChanger: true,
                  showTotal: (total) => `총 ${total}건`
                }}
              />
            )}
          </TabPane>
          <TabPane 
            tab={`사용 내역 (${transactions.filter(t => t.transaction_type === 'USED' || t.transaction_type === 'EXPIRED').length})`} 
            key="used"
          >
            {filteredTransactions.length === 0 ? (
              <Empty description="사용 내역이 없습니다." />
            ) : (
              <Table
                columns={columns}
                dataSource={filteredTransactions}
                rowKey="transaction_id"
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
    </div>
  )
}

export default MarketMyPagePoints


import { useState, useEffect } from 'react'
import { Card, Table, Tag, Button, Space, Empty, message, Modal, Descriptions, Tabs, Input } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { GiftOutlined, EyeOutlined, CopyOutlined, CheckCircleOutlined, CloseCircleOutlined, ClockCircleOutlined } from '@ant-design/icons'
import './MarketMyPageCoupons.css'

const { TabPane } = Tabs

interface UserCoupon {
  user_coupon_id: string
  coupon_id: string
  coupon_code: string
  coupon_name: string
  coupon_type: 'PERCENTAGE' | 'FIXED_AMOUNT'
  discount_value: number
  min_purchase_amount?: number
  max_discount_amount?: number
  valid_from: string
  valid_to: string
  status: 'AVAILABLE' | 'USED' | 'EXPIRED'
  used_at?: string
  used_order_id?: string
  used_order_number?: string
  obtained_at: string
}

function MarketMyPageCoupons() {
  const [coupons, setCoupons] = useState<UserCoupon[]>([])
  const [loading, setLoading] = useState(false)
  const [selectedCoupon, setSelectedCoupon] = useState<UserCoupon | null>(null)
  const [isDetailModalVisible, setIsDetailModalVisible] = useState(false)
  const [activeTab, setActiveTab] = useState('available')
  const [couponCodeInput, setCouponCodeInput] = useState('')

  useEffect(() => {
    loadCoupons()
  }, [])

  const loadCoupons = async () => {
    setLoading(true)
    try {
      // TODO: API 호출로 쿠폰 목록 가져오기
      await new Promise(resolve => setTimeout(resolve, 500))
      
      const sampleCoupons: UserCoupon[] = [
        {
          user_coupon_id: '1',
          coupon_id: '1',
          coupon_code: 'WELCOME10',
          coupon_name: '신규 가입 환영 쿠폰',
          coupon_type: 'PERCENTAGE',
          discount_value: 10,
          min_purchase_amount: 10000,
          max_discount_amount: 5000,
          valid_from: '2024-01-01 00:00:00',
          valid_to: '2024-12-31 23:59:59',
          status: 'AVAILABLE',
          obtained_at: '2024-01-01 10:00:00'
        },
        {
          user_coupon_id: '2',
          coupon_id: '2',
          coupon_code: 'SAVE5000',
          coupon_name: '5천원 할인 쿠폰',
          coupon_type: 'FIXED_AMOUNT',
          discount_value: 5000,
          min_purchase_amount: 30000,
          valid_from: '2024-01-15 00:00:00',
          valid_to: '2024-02-15 23:59:59',
          status: 'AVAILABLE',
          obtained_at: '2024-01-15 14:00:00'
        },
        {
          user_coupon_id: '3',
          coupon_id: '3',
          coupon_code: 'SUMMER20',
          coupon_name: '여름 특가 20% 할인',
          coupon_type: 'PERCENTAGE',
          discount_value: 20,
          min_purchase_amount: 50000,
          max_discount_amount: 20000,
          valid_from: '2024-06-01 00:00:00',
          valid_to: '2024-08-31 23:59:59',
          status: 'EXPIRED',
          obtained_at: '2024-05-20 10:00:00'
        },
        {
          user_coupon_id: '4',
          coupon_id: '4',
          coupon_code: 'BIRTHDAY15',
          coupon_name: '생일 축하 15% 할인',
          coupon_type: 'PERCENTAGE',
          discount_value: 15,
          min_purchase_amount: 20000,
          valid_from: '2024-01-10 00:00:00',
          valid_to: '2024-02-10 23:59:59',
          status: 'USED',
          used_at: '2024-01-12 15:30:00',
          used_order_id: '1',
          used_order_number: 'ORD-20240112-001',
          obtained_at: '2024-01-10 09:00:00'
        }
      ]
      
      setCoupons(sampleCoupons)
    } catch (error) {
      message.error('쿠폰 목록을 불러오는데 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const handleRegisterCoupon = async () => {
    if (!couponCodeInput.trim()) {
      message.warning('쿠폰 코드를 입력해주세요.')
      return
    }

    try {
      // TODO: API 호출로 쿠폰 등록
      await new Promise(resolve => setTimeout(resolve, 1000))
      
      message.success('쿠폰이 등록되었습니다.')
      setCouponCodeInput('')
      loadCoupons()
    } catch (error) {
      message.error('쿠폰 등록에 실패했습니다. 쿠폰 코드를 확인해주세요.')
    }
  }

  const handleCopyCouponCode = (couponCode: string) => {
    navigator.clipboard.writeText(couponCode)
    message.success('쿠폰 코드가 복사되었습니다.')
  }

  const handleViewDetail = (coupon: UserCoupon) => {
    setSelectedCoupon(coupon)
    setIsDetailModalVisible(true)
  }

  const getStatusInfo = (status: string) => {
    const statusMap: Record<string, { label: string; color: string; icon: JSX.Element }> = {
      AVAILABLE: { 
        label: '사용 가능', 
        color: 'green',
        icon: <CheckCircleOutlined />
      },
      USED: { 
        label: '사용 완료', 
        color: 'default',
        icon: <CloseCircleOutlined />
      },
      EXPIRED: { 
        label: '만료됨', 
        color: 'red',
        icon: <ClockCircleOutlined />
      }
    }
    return statusMap[status] || { label: status, color: 'default', icon: <ClockCircleOutlined /> }
  }

  const getFilteredCoupons = () => {
    if (activeTab === 'available') {
      return coupons.filter(c => c.status === 'AVAILABLE')
    } else if (activeTab === 'used') {
      return coupons.filter(c => c.status === 'USED')
    } else if (activeTab === 'expired') {
      return coupons.filter(c => c.status === 'EXPIRED')
    }
    return coupons
  }

  const columns: ColumnsType<UserCoupon> = [
    {
      title: '쿠폰명',
      key: 'coupon_name',
      render: (_, record: UserCoupon) => (
        <div>
          <div style={{ fontWeight: 600 }}>{record.coupon_name}</div>
          <div style={{ color: '#999', fontSize: '0.9rem' }}>
            {record.coupon_code}
          </div>
        </div>
      )
    },
    {
      title: '할인 내용',
      key: 'discount',
      render: (_, record: UserCoupon) => {
        if (record.coupon_type === 'PERCENTAGE') {
          return (
            <div>
              <strong style={{ color: '#667eea', fontSize: '1.1rem' }}>
                {record.discount_value}%
              </strong>
              {record.max_discount_amount && (
                <div style={{ color: '#999', fontSize: '0.85rem' }}>
                  최대 {record.max_discount_amount.toLocaleString()}원
                </div>
              )}
            </div>
          )
        } else {
          return (
            <strong style={{ color: '#667eea', fontSize: '1.1rem' }}>
              {record.discount_value.toLocaleString()}원
            </strong>
          )
        }
      }
    },
    {
      title: '사용 조건',
      key: 'condition',
      render: (_, record: UserCoupon) => (
        <div>
          {record.min_purchase_amount ? (
            <div>
              {record.min_purchase_amount.toLocaleString()}원 이상 구매 시
            </div>
          ) : (
            <div style={{ color: '#999' }}>조건 없음</div>
          )}
        </div>
      )
    },
    {
      title: '유효기간',
      key: 'valid_period',
      render: (_, record: UserCoupon) => (
        <div>
          <div>{record.valid_from.split(' ')[0]}</div>
          <div style={{ color: '#999' }}>~ {record.valid_to.split(' ')[0]}</div>
        </div>
      )
    },
    {
      title: '상태',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (status: string) => {
        const statusInfo = getStatusInfo(status)
        return (
          <Tag color={statusInfo.color} icon={statusInfo.icon}>
            {statusInfo.label}
          </Tag>
        )
      }
    },
    {
      title: '관리',
      key: 'action',
      width: 150,
      render: (_, record: UserCoupon) => (
        <Space>
          <Button
            type="link"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetail(record)}
          >
            상세보기
          </Button>
          {record.status === 'AVAILABLE' && (
            <Button
              type="link"
              icon={<CopyOutlined />}
              onClick={() => handleCopyCouponCode(record.coupon_code)}
            >
              코드복사
            </Button>
          )}
        </Space>
      )
    }
  ]

  const filteredCoupons = getFilteredCoupons()

  return (
    <div className="market-mypage-coupons">
      <Card title="쿠폰" className="coupons-card">
        {/* 쿠폰 등록 */}
        <Card 
          type="inner" 
          title="쿠폰 등록" 
          className="coupon-register-card"
          style={{ marginBottom: '1.5rem' }}
        >
          <Space.Compact style={{ width: '100%' }}>
            <Input
              placeholder="쿠폰 코드를 입력하세요"
              size="large"
              value={couponCodeInput}
              onChange={(e) => setCouponCodeInput(e.target.value)}
              onPressEnter={handleRegisterCoupon}
            />
            <Button 
              type="primary" 
              size="large"
              icon={<GiftOutlined />}
              onClick={handleRegisterCoupon}
            >
              등록
            </Button>
          </Space.Compact>
        </Card>

        <Tabs activeKey={activeTab} onChange={setActiveTab}>
          <TabPane 
            tab={`사용 가능 (${coupons.filter(c => c.status === 'AVAILABLE').length})`} 
            key="available"
          >
            {filteredCoupons.length === 0 ? (
              <Empty description="사용 가능한 쿠폰이 없습니다." />
            ) : (
              <Table
                columns={columns}
                dataSource={filteredCoupons}
                rowKey="user_coupon_id"
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
            tab={`사용 완료 (${coupons.filter(c => c.status === 'USED').length})`} 
            key="used"
          >
            {filteredCoupons.length === 0 ? (
              <Empty description="사용한 쿠폰이 없습니다." />
            ) : (
              <Table
                columns={columns}
                dataSource={filteredCoupons}
                rowKey="user_coupon_id"
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
            tab={`만료됨 (${coupons.filter(c => c.status === 'EXPIRED').length})`} 
            key="expired"
          >
            {filteredCoupons.length === 0 ? (
              <Empty description="만료된 쿠폰이 없습니다." />
            ) : (
              <Table
                columns={columns}
                dataSource={filteredCoupons}
                rowKey="user_coupon_id"
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

      {/* 쿠폰 상세 모달 */}
      <Modal
        title="쿠폰 상세 정보"
        open={isDetailModalVisible}
        onCancel={() => setIsDetailModalVisible(false)}
        footer={[
          selectedCoupon?.status === 'AVAILABLE' && (
            <Button
              key="copy"
              icon={<CopyOutlined />}
              onClick={() => {
                if (selectedCoupon) {
                  handleCopyCouponCode(selectedCoupon.coupon_code)
                }
              }}
            >
              쿠폰 코드 복사
            </Button>
          ),
          <Button key="close" onClick={() => setIsDetailModalVisible(false)}>
            닫기
          </Button>
        ].filter(Boolean)}
        width={600}
      >
        {selectedCoupon && (
          <Descriptions column={1} bordered>
            <Descriptions.Item label="쿠폰명">
              {selectedCoupon.coupon_name}
            </Descriptions.Item>
            <Descriptions.Item label="쿠폰 코드">
              <Space>
                <strong>{selectedCoupon.coupon_code}</strong>
                {selectedCoupon.status === 'AVAILABLE' && (
                  <Button
                    type="link"
                    size="small"
                    icon={<CopyOutlined />}
                    onClick={() => handleCopyCouponCode(selectedCoupon.coupon_code)}
                  >
                    복사
                  </Button>
                )}
              </Space>
            </Descriptions.Item>
            <Descriptions.Item label="할인 내용">
              {selectedCoupon.coupon_type === 'PERCENTAGE' ? (
                <div>
                  <strong style={{ color: '#667eea', fontSize: '1.2rem' }}>
                    {selectedCoupon.discount_value}% 할인
                  </strong>
                  {selectedCoupon.max_discount_amount && (
                    <div style={{ marginTop: '0.5rem', color: '#999' }}>
                      최대 할인 금액: {selectedCoupon.max_discount_amount.toLocaleString()}원
                    </div>
                  )}
                </div>
              ) : (
                <strong style={{ color: '#667eea', fontSize: '1.2rem' }}>
                  {selectedCoupon.discount_value.toLocaleString()}원 할인
                </strong>
              )}
            </Descriptions.Item>
            <Descriptions.Item label="사용 조건">
              {selectedCoupon.min_purchase_amount ? (
                <div>
                  {selectedCoupon.min_purchase_amount.toLocaleString()}원 이상 구매 시 사용 가능
                </div>
              ) : (
                <div>조건 없음</div>
              )}
            </Descriptions.Item>
            <Descriptions.Item label="유효기간">
              <div>
                <div>{selectedCoupon.valid_from}</div>
                <div>~ {selectedCoupon.valid_to}</div>
              </div>
            </Descriptions.Item>
            <Descriptions.Item label="상태">
              {(() => {
                const statusInfo = getStatusInfo(selectedCoupon.status)
                return (
                  <Tag color={statusInfo.color} icon={statusInfo.icon}>
                    {statusInfo.label}
                  </Tag>
                )
              })()}
            </Descriptions.Item>
            {selectedCoupon.status === 'USED' && selectedCoupon.used_order_number && (
              <>
                <Descriptions.Item label="사용 주문번호">
                  {selectedCoupon.used_order_number}
                </Descriptions.Item>
                <Descriptions.Item label="사용일시">
                  {selectedCoupon.used_at}
                </Descriptions.Item>
              </>
            )}
            <Descriptions.Item label="발급일시">
              {selectedCoupon.obtained_at}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  )
}

export default MarketMyPageCoupons


import { useState, useEffect } from 'react'
import { Card, Table, Tag, Button, Space, Empty, message, Modal, Descriptions, Tabs, Input } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { GiftOutlined, EyeOutlined, CopyOutlined, CheckCircleOutlined, CloseCircleOutlined, ClockCircleOutlined } from '@ant-design/icons'
import { API_BASE_URL } from '../../../config/env'
import { userFetch } from '../../../utils/apiHelper'
import './MarketMyPageCoupons.css'

const { TabPane } = Tabs

interface UserCoupon {
  userCouponId: number
  couponId: number
  couponCode: string
  couponName: string
  discountType: 'RATE' | 'FIXED'
  discountValue: number
  minOrderAmount?: number
  maxDiscountAmount?: number
  validFrom: string
  validTo: string
  couponStatus: 'ISSUED' | 'USED' | 'EXPIRED'
  usedAt?: string
  issuedAt: string
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
      const response = await userFetch(`${API_BASE_URL}/api/promotion/coupons`)

      if (!response.ok) {
        throw new Error('쿠폰 목록을 불러오는데 실패했습니다.')
      }

      const data: UserCoupon[] = await response.json()
      setCoupons(data)
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
      const response = await userFetch(`${API_BASE_URL}/api/promotion/coupon`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ couponCode: couponCodeInput.trim() }),
      })

      if (!response.ok) {
        const errorData = await response.json().catch(() => null)
        if (response.status === 404) {
          throw new Error(errorData?.message || '존재하지 않는 쿠폰 코드입니다.')
        }
        throw new Error(errorData?.message || '쿠폰 등록에 실패했습니다.')
      }

      message.success('쿠폰이 등록되었습니다.')
      setCouponCodeInput('')
      loadCoupons()
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '쿠폰 등록에 실패했습니다. 쿠폰 코드를 확인해주세요.'
      message.error(errorMessage)
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
      ISSUED: {
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
      return coupons.filter(c => c.couponStatus === 'ISSUED')
    } else if (activeTab === 'used') {
      return coupons.filter(c => c.couponStatus === 'USED')
    } else if (activeTab === 'expired') {
      return coupons.filter(c => c.couponStatus === 'EXPIRED')
    }
    return coupons
  }

  const columns: ColumnsType<UserCoupon> = [
    {
      title: '쿠폰명',
      key: 'couponName',
      render: (_, record: UserCoupon) => (
        <div>
          <div style={{ fontWeight: 600 }}>{record.couponName}</div>
          <div style={{ color: '#999', fontSize: '0.9rem' }}>
            {record.couponCode}
          </div>
        </div>
      )
    },
    {
      title: '할인 내용',
      key: 'discount',
      render: (_, record: UserCoupon) => {
        if (record.discountType === 'RATE') {
          return (
            <div>
              <strong style={{ color: '#667eea', fontSize: '1.1rem' }}>
                {record.discountValue}%
              </strong>
              {record.maxDiscountAmount && (
                <div style={{ color: '#999', fontSize: '0.85rem' }}>
                  최대 {record.maxDiscountAmount.toLocaleString()}원
                </div>
              )}
            </div>
          )
        } else {
          return (
            <strong style={{ color: '#667eea', fontSize: '1.1rem' }}>
              {record.discountValue.toLocaleString()}원
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
          {record.minOrderAmount ? (
            <div>
              {record.minOrderAmount.toLocaleString()}원 이상 구매 시
            </div>
          ) : (
            <div style={{ color: '#999' }}>조건 없음</div>
          )}
        </div>
      )
    },
    {
      title: '유효기간',
      key: 'validPeriod',
      render: (_, record: UserCoupon) => (
        <div>
          <div>{record.validFrom.split(' ')[0]}</div>
          <div style={{ color: '#999' }}>~ {record.validTo.split(' ')[0]}</div>
        </div>
      )
    },
    {
      title: '상태',
      dataIndex: 'couponStatus',
      key: 'couponStatus',
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
          {record.couponStatus === 'ISSUED' && (
            <Button
              type="link"
              icon={<CopyOutlined />}
              onClick={() => handleCopyCouponCode(record.couponCode)}
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
            tab={`사용 가능 (${coupons.filter(c => c.couponStatus === 'ISSUED').length})`}
            key="available"
          >
            {filteredCoupons.length === 0 ? (
              <Empty description="사용 가능한 쿠폰이 없습니다." />
            ) : (
              <Table
                columns={columns}
                dataSource={filteredCoupons}
                rowKey="userCouponId"
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
            tab={`사용 완료 (${coupons.filter(c => c.couponStatus === 'USED').length})`}
            key="used"
          >
            {filteredCoupons.length === 0 ? (
              <Empty description="사용한 쿠폰이 없습니다." />
            ) : (
              <Table
                columns={columns}
                dataSource={filteredCoupons}
                rowKey="userCouponId"
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
            tab={`만료됨 (${coupons.filter(c => c.couponStatus === 'EXPIRED').length})`}
            key="expired"
          >
            {filteredCoupons.length === 0 ? (
              <Empty description="만료된 쿠폰이 없습니다." />
            ) : (
              <Table
                columns={columns}
                dataSource={filteredCoupons}
                rowKey="userCouponId"
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
          selectedCoupon?.couponStatus === 'ISSUED' && (
            <Button
              key="copy"
              icon={<CopyOutlined />}
              onClick={() => {
                if (selectedCoupon) {
                  handleCopyCouponCode(selectedCoupon.couponCode)
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
              {selectedCoupon.couponName}
            </Descriptions.Item>
            <Descriptions.Item label="쿠폰 코드">
              <Space>
                <strong>{selectedCoupon.couponCode}</strong>
                {selectedCoupon.couponStatus === 'ISSUED' && (
                  <Button
                    type="link"
                    size="small"
                    icon={<CopyOutlined />}
                    onClick={() => handleCopyCouponCode(selectedCoupon.couponCode)}
                  >
                    복사
                  </Button>
                )}
              </Space>
            </Descriptions.Item>
            <Descriptions.Item label="할인 내용">
              {selectedCoupon.discountType === 'RATE' ? (
                <div>
                  <strong style={{ color: '#667eea', fontSize: '1.2rem' }}>
                    {selectedCoupon.discountValue}% 할인
                  </strong>
                  {selectedCoupon.maxDiscountAmount && (
                    <div style={{ marginTop: '0.5rem', color: '#999' }}>
                      최대 할인 금액: {selectedCoupon.maxDiscountAmount.toLocaleString()}원
                    </div>
                  )}
                </div>
              ) : (
                <strong style={{ color: '#667eea', fontSize: '1.2rem' }}>
                  {selectedCoupon.discountValue.toLocaleString()}원 할인
                </strong>
              )}
            </Descriptions.Item>
            <Descriptions.Item label="사용 조건">
              {selectedCoupon.minOrderAmount ? (
                <div>
                  {selectedCoupon.minOrderAmount.toLocaleString()}원 이상 구매 시 사용 가능
                </div>
              ) : (
                <div>조건 없음</div>
              )}
            </Descriptions.Item>
            <Descriptions.Item label="유효기간">
              <div>
                <div>{selectedCoupon.validFrom}</div>
                <div>~ {selectedCoupon.validTo}</div>
              </div>
            </Descriptions.Item>
            <Descriptions.Item label="상태">
              {(() => {
                const statusInfo = getStatusInfo(selectedCoupon.couponStatus)
                return (
                  <Tag color={statusInfo.color} icon={statusInfo.icon}>
                    {statusInfo.label}
                  </Tag>
                )
              })()}
            </Descriptions.Item>
            {selectedCoupon.couponStatus === 'USED' && selectedCoupon.usedAt && (
              <Descriptions.Item label="사용일시">
                {selectedCoupon.usedAt}
              </Descriptions.Item>
            )}
            <Descriptions.Item label="발급일시">
              {selectedCoupon.issuedAt}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  )
}

export default MarketMyPageCoupons


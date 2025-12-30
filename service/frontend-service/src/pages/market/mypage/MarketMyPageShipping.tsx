import { useState, useEffect } from 'react'
import { Card, Table, Tag, Button, Space, Empty, message, Descriptions, Modal } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { EyeOutlined, TruckOutlined, CopyOutlined } from '@ant-design/icons'
import './MarketMyPageShipping.css'

interface ShippingItem {
  shipping_item_id: string
  product_id: string
  product_name: string
  product_code: string
  quantity: number
}

interface Shipping {
  shipping_id: string
  order_id: string
  order_number: string
  shipping_status: 'READY' | 'SHIPPING' | 'DELIVERED' | 'RETURNED'
  shipping_company?: string
  tracking_number?: string
  receiver_name: string
  receiver_phone: string
  address: string
  postal_code?: string
  delivery_service_status?: 'NOT_SENT' | 'SENT' | 'IN_TRANSIT' | 'DELIVERED'
  shipped_at?: string
  delivered_at?: string
  created_at: string
  updated_at: string
  items: ShippingItem[]
}

function MarketMyPageShipping() {
  const [shippings, setShippings] = useState<Shipping[]>([])
  const [loading, setLoading] = useState(false)
  const [selectedShipping, setSelectedShipping] = useState<Shipping | null>(null)
  const [isDetailModalVisible, setIsDetailModalVisible] = useState(false)

  useEffect(() => {
    loadShippings()
  }, [])

  const loadShippings = async () => {
    setLoading(true)
    try {
      // TODO: API 호출로 배송 조회 데이터 가져오기
      // 임시 샘플 데이터
      await new Promise(resolve => setTimeout(resolve, 500))
      
      const sampleShippings: Shipping[] = [
        {
          shipping_id: '1',
          order_id: '1',
          order_number: 'ORD-20240101-001',
          shipping_status: 'SHIPPING',
          shipping_company: 'CJ대한통운',
          tracking_number: '1234567890123',
          receiver_name: '홍길동',
          receiver_phone: '010-1234-5678',
          address: '서울특별시 강남구 테헤란로 123',
          postal_code: '06234',
          delivery_service_status: 'IN_TRANSIT',
          shipped_at: '2024-01-15 10:30:00',
          created_at: '2024-01-01 10:30:00',
          updated_at: '2024-01-15 11:00:00',
          items: [
            {
              shipping_item_id: '1',
              product_id: '1',
              product_name: '프리미엄 노트북',
              product_code: 'PRD-001',
              quantity: 1
            }
          ]
        },
        {
          shipping_id: '2',
          order_id: '2',
          order_number: 'ORD-20240102-002',
          shipping_status: 'DELIVERED',
          shipping_company: '한진택배',
          tracking_number: '9876543210987',
          receiver_name: '홍길동',
          receiver_phone: '010-1234-5678',
          address: '서울특별시 강남구 테헤란로 123',
          postal_code: '06234',
          delivery_service_status: 'DELIVERED',
          shipped_at: '2024-01-14 14:20:00',
          delivered_at: '2024-01-15 09:00:00',
          created_at: '2024-01-02 14:20:00',
          updated_at: '2024-01-15 09:00:00',
          items: [
            {
              shipping_item_id: '2',
              product_id: '2',
              product_name: '최신 스마트폰',
              product_code: 'PRD-002',
              quantity: 1
            }
          ]
        },
        {
          shipping_id: '3',
          order_id: '3',
          order_number: 'ORD-20240103-003',
          shipping_status: 'READY',
          receiver_name: '홍길동',
          receiver_phone: '010-1234-5678',
          address: '서울특별시 강남구 테헤란로 123',
          postal_code: '06234',
          delivery_service_status: 'NOT_SENT',
          created_at: '2024-01-03 09:15:00',
          updated_at: '2024-01-03 09:15:00',
          items: [
            {
              shipping_item_id: '3',
              product_id: '4',
              product_name: '무선 이어폰',
              product_code: 'PRD-004',
              quantity: 1
            }
          ]
        }
      ]
      
      setShippings(sampleShippings)
    } catch (error) {
      message.error('배송 조회 정보를 불러오는데 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const statusMap: Record<string, { label: string; color: string }> = {
    READY: { label: '배송 준비', color: 'blue' },
    SHIPPING: { label: '배송 중', color: 'orange' },
    DELIVERED: { label: '배송 완료', color: 'green' },
    RETURNED: { label: '반품', color: 'red' }
  }

  const deliveryStatusMap: Record<string, { label: string; color: string }> = {
    NOT_SENT: { label: '미전송', color: 'default' },
    SENT: { label: '전송 완료', color: 'blue' },
    IN_TRANSIT: { label: '운송 중', color: 'orange' },
    DELIVERED: { label: '배송 완료', color: 'green' }
  }

  const handleViewDetail = (shipping: Shipping) => {
    setSelectedShipping(shipping)
    setIsDetailModalVisible(true)
  }

  const handleTracking = (trackingNumber: string, company?: string) => {
    if (!trackingNumber) {
      message.warning('운송장 번호가 없습니다.')
      return
    }

    // TODO: 배송 추적 페이지로 이동 또는 새 창 열기
    // 각 배송사별 추적 URL로 이동
    let trackingUrl = ''
    if (company === 'CJ대한통운') {
      trackingUrl = `https://www.cjlogistics.com/ko/tool/parcel/tracking?gnbInvcNo=${trackingNumber}`
    } else if (company === '한진택배') {
      trackingUrl = `https://www.hanjin.co.kr/kor/CMS/DeliveryMgr/WaybillNum.do?mCode=MN038&schLang=KR&wblnumText2=${trackingNumber}`
    } else {
      message.info('배송 추적 기능은 준비 중입니다.')
      return
    }

    window.open(trackingUrl, '_blank')
  }

  const handleCopyTrackingNumber = (trackingNumber: string) => {
    navigator.clipboard.writeText(trackingNumber)
    message.success('운송장 번호가 복사되었습니다.')
  }

  const columns: ColumnsType<Shipping> = [
    {
      title: '주문번호',
      dataIndex: 'order_number',
      key: 'order_number',
      width: 180,
      render: (text: string) => <strong>{text}</strong>
    },
    {
      title: '배송 상품',
      key: 'items',
      render: (_, record: Shipping) => (
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
      title: '배송 상태',
      dataIndex: 'shipping_status',
      key: 'shipping_status',
      width: 120,
      render: (status: string) => {
        const statusInfo = statusMap[status] || { label: status, color: 'default' }
        return <Tag color={statusInfo.color}>{statusInfo.label}</Tag>
      }
    },
    {
      title: '배송사',
      dataIndex: 'shipping_company',
      key: 'shipping_company',
      width: 120,
      render: (company: string | undefined) => company || <span style={{ color: '#999' }}>-</span>
    },
    {
      title: '운송장 번호',
      dataIndex: 'tracking_number',
      key: 'tracking_number',
      width: 180,
      render: (trackingNumber: string | undefined, record: Shipping) => {
        if (!trackingNumber) {
          return <span style={{ color: '#999' }}>-</span>
        }
        return (
          <Space>
            <span>{trackingNumber}</span>
            <Button
              type="text"
              size="small"
              icon={<CopyOutlined />}
              onClick={(e) => {
                e.stopPropagation()
                handleCopyTrackingNumber(trackingNumber)
              }}
            />
          </Space>
        )
      }
    },
    {
      title: '관리',
      key: 'action',
      width: 150,
      render: (_, record: Shipping) => (
        <Space>
          <Button
            type="link"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetail(record)}
          >
            상세보기
          </Button>
          {record.tracking_number && (
            <Button
              type="link"
              icon={<TruckOutlined />}
              onClick={() => handleTracking(record.tracking_number!, record.shipping_company)}
            >
              배송추적
            </Button>
          )}
        </Space>
      )
    }
  ]

  return (
    <div className="market-mypage-shipping">
      <Card title="배송 조회" className="shipping-card">
        {shippings.length === 0 ? (
          <Empty description="배송 내역이 없습니다." />
        ) : (
          <Table
            columns={columns}
            dataSource={shippings}
            rowKey="shipping_id"
            loading={loading}
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              showTotal: (total) => `총 ${total}건`
            }}
          />
        )}
      </Card>

      <Modal
        title="배송 상세 정보"
        open={isDetailModalVisible}
        onCancel={() => setIsDetailModalVisible(false)}
        footer={[
          selectedShipping?.tracking_number && (
            <Button
              key="tracking"
              type="primary"
              icon={<TruckOutlined />}
              onClick={() => {
                if (selectedShipping) {
                  handleTracking(selectedShipping.tracking_number!, selectedShipping.shipping_company)
                }
              }}
            >
              배송 추적
            </Button>
          ),
          <Button key="close" onClick={() => setIsDetailModalVisible(false)}>
            닫기
          </Button>
        ].filter(Boolean)}
        width={700}
      >
        {selectedShipping && (
          <Descriptions column={1} bordered>
            <Descriptions.Item label="주문번호">
              {selectedShipping.order_number}
            </Descriptions.Item>
            <Descriptions.Item label="배송 상태">
              <Tag color={statusMap[selectedShipping.shipping_status]?.color}>
                {statusMap[selectedShipping.shipping_status]?.label}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="배송 상품">
              {selectedShipping.items.map((item) => (
                <div key={item.shipping_item_id} style={{ marginBottom: '0.5rem' }}>
                  {item.product_name} ({item.product_code}) × {item.quantity}개
                </div>
              ))}
            </Descriptions.Item>
            <Descriptions.Item label="수령인">
              {selectedShipping.receiver_name}
            </Descriptions.Item>
            <Descriptions.Item label="연락처">
              {selectedShipping.receiver_phone}
            </Descriptions.Item>
            <Descriptions.Item label="배송 주소">
              [{selectedShipping.postal_code || '-'}] {selectedShipping.address}
            </Descriptions.Item>
            {selectedShipping.shipping_company && (
              <Descriptions.Item label="배송사">
                {selectedShipping.shipping_company}
              </Descriptions.Item>
            )}
            {selectedShipping.tracking_number && (
              <Descriptions.Item label="운송장 번호">
                <Space>
                  {selectedShipping.tracking_number}
                  <Button
                    type="text"
                    size="small"
                    icon={<CopyOutlined />}
                    onClick={() => handleCopyTrackingNumber(selectedShipping.tracking_number!)}
                  />
                </Space>
              </Descriptions.Item>
            )}
            {selectedShipping.shipped_at && (
              <Descriptions.Item label="발송일시">
                {selectedShipping.shipped_at}
              </Descriptions.Item>
            )}
            {selectedShipping.delivered_at && (
              <Descriptions.Item label="배송완료일시">
                {selectedShipping.delivered_at}
              </Descriptions.Item>
            )}
            {selectedShipping.delivery_service_status && (
              <Descriptions.Item label="배송 서비스 상태">
                <Tag color={deliveryStatusMap[selectedShipping.delivery_service_status]?.color}>
                  {deliveryStatusMap[selectedShipping.delivery_service_status]?.label}
                </Tag>
              </Descriptions.Item>
            )}
          </Descriptions>
        )}
      </Modal>
    </div>
  )
}

export default MarketMyPageShipping


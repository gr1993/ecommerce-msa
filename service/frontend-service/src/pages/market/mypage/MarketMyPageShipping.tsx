import { useState, useEffect } from 'react'
import { Card, Table, Tag, Button, Space, Empty, message, Descriptions, Modal } from 'antd'
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table'
import { EyeOutlined, TruckOutlined, CopyOutlined } from '@ant-design/icons'
import { getMyShippings, type MarketShippingResponse } from '../../../api/shippingApi'
import './MarketMyPageShipping.css'

function MarketMyPageShipping() {
  const [shippings, setShippings] = useState<MarketShippingResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })
  const [selectedShipping, setSelectedShipping] = useState<MarketShippingResponse | null>(null)
  const [isDetailModalVisible, setIsDetailModalVisible] = useState(false)

  useEffect(() => {
    loadShippings(0, pagination.pageSize)
  }, [])

  const loadShippings = async (page: number, size: number) => {
    setLoading(true)
    try {
      const data = await getMyShippings(page, size)
      setShippings(data.content)
      setPagination({
        current: data.page + 1,
        pageSize: data.size,
        total: data.totalElements,
      })
    } catch (error) {
      message.error(error instanceof Error ? error.message : '배송 조회 정보를 불러오는데 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const handleTableChange = (paginationConfig: TablePaginationConfig) => {
    const page = (paginationConfig.current || 1) - 1
    const size = paginationConfig.pageSize || 10
    loadShippings(page, size)
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

  const handleViewDetail = (shipping: MarketShippingResponse) => {
    setSelectedShipping(shipping)
    setIsDetailModalVisible(true)
  }

  const handleTracking = (trackingNumber: string, company?: string) => {
    if (!trackingNumber) {
      message.warning('운송장 번호가 없습니다.')
      return
    }

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

  const columns: ColumnsType<MarketShippingResponse> = [
    {
      title: '주문번호',
      dataIndex: 'orderNumber',
      key: 'orderNumber',
      width: 180,
      render: (text: string) => <strong>{text}</strong>
    },
    {
      title: '배송 상태',
      dataIndex: 'shippingStatus',
      key: 'shippingStatus',
      width: 120,
      render: (status: string) => {
        const statusInfo = statusMap[status] || { label: status, color: 'default' }
        return <Tag color={statusInfo.color}>{statusInfo.label}</Tag>
      }
    },
    {
      title: '배송사',
      dataIndex: 'shippingCompany',
      key: 'shippingCompany',
      width: 120,
      render: (company: string | undefined) => company || <span style={{ color: '#999' }}>-</span>
    },
    {
      title: '운송장 번호',
      dataIndex: 'trackingNumber',
      key: 'trackingNumber',
      width: 180,
      render: (trackingNumber: string | undefined) => {
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
      title: '수정 일시',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: 160,
      render: (date: string) => date || '-'
    },
    {
      title: '관리',
      key: 'action',
      width: 150,
      render: (_, record: MarketShippingResponse) => (
        <Space>
          <Button
            type="link"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetail(record)}
          >
            상세보기
          </Button>
          {record.trackingNumber && (
            <Button
              type="link"
              icon={<TruckOutlined />}
              onClick={() => handleTracking(record.trackingNumber!, record.shippingCompany)}
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
        {shippings.length === 0 && !loading ? (
          <Empty description="배송 내역이 없습니다." />
        ) : (
          <Table
            columns={columns}
            dataSource={shippings}
            rowKey="shippingId"
            loading={loading}
            onChange={handleTableChange}
            pagination={{
              current: pagination.current,
              pageSize: pagination.pageSize,
              total: pagination.total,
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
          selectedShipping?.trackingNumber && (
            <Button
              key="tracking"
              type="primary"
              icon={<TruckOutlined />}
              onClick={() => {
                if (selectedShipping) {
                  handleTracking(selectedShipping.trackingNumber!, selectedShipping.shippingCompany)
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
              {selectedShipping.orderNumber}
            </Descriptions.Item>
            <Descriptions.Item label="배송 상태">
              <Tag color={statusMap[selectedShipping.shippingStatus]?.color}>
                {statusMap[selectedShipping.shippingStatus]?.label}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="수령인">
              {selectedShipping.receiverName}
            </Descriptions.Item>
            <Descriptions.Item label="연락처">
              {selectedShipping.receiverPhone}
            </Descriptions.Item>
            <Descriptions.Item label="배송 주소">
              [{selectedShipping.postalCode || '-'}] {selectedShipping.address}
            </Descriptions.Item>
            {selectedShipping.shippingCompany && (
              <Descriptions.Item label="배송사">
                {selectedShipping.shippingCompany}
              </Descriptions.Item>
            )}
            {selectedShipping.trackingNumber && (
              <Descriptions.Item label="운송장 번호">
                <Space>
                  {selectedShipping.trackingNumber}
                  <Button
                    type="text"
                    size="small"
                    icon={<CopyOutlined />}
                    onClick={() => handleCopyTrackingNumber(selectedShipping.trackingNumber!)}
                  />
                </Space>
              </Descriptions.Item>
            )}
            {selectedShipping.deliveryServiceStatus && (
              <Descriptions.Item label="배송 서비스 상태">
                <Tag color={deliveryStatusMap[selectedShipping.deliveryServiceStatus]?.color}>
                  {deliveryStatusMap[selectedShipping.deliveryServiceStatus]?.label}
                </Tag>
              </Descriptions.Item>
            )}
            <Descriptions.Item label="등록 일시">
              {selectedShipping.createdAt}
            </Descriptions.Item>
            <Descriptions.Item label="수정 일시">
              {selectedShipping.updatedAt}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  )
}

export default MarketMyPageShipping

import { Modal, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { ExchangeItemDto } from '../../../api/shippingApi'
import './ExchangeItemsModal.css'

interface ExchangeItemsModalProps {
  open: boolean
  exchangeId: number | null
  exchangeItems: ExchangeItemDto[]
  onClose: () => void
}

function ExchangeItemsModal({ open, exchangeId, exchangeItems, onClose }: ExchangeItemsModalProps) {

  const columns: ColumnsType<ExchangeItemDto> = [
    {
      title: '주문 상품 ID',
      dataIndex: 'orderItemId',
      key: 'orderItemId',
      width: 120,
    },
    {
      title: '원래 옵션 ID',
      dataIndex: 'originalOptionId',
      key: 'originalOptionId',
      width: 120,
      render: (id: number) => <span style={{ color: '#666' }}>옵션 #{id}</span>,
    },
    {
      title: '교환 옵션 ID',
      dataIndex: 'newOptionId',
      key: 'newOptionId',
      width: 120,
      render: (id: number) => <span style={{ color: '#666' }}>옵션 #{id}</span>,
    },
    {
      title: '교환 수량',
      dataIndex: 'quantity',
      key: 'quantity',
      align: 'center',
      width: 100,
      render: (quantity: number) => `${quantity}개`,
    },
  ]

  return (
    <Modal
      title={
        <div>
          교환 품목 상세
          {exchangeId && (
            <span style={{ marginLeft: '10px', fontSize: '14px', color: '#666' }}>
              (교환 ID: {exchangeId})
            </span>
          )}
        </div>
      }
      open={open}
      onCancel={onClose}
      footer={null}
      width={600}
      className="exchange-items-modal"
    >
      <Table
        columns={columns}
        dataSource={exchangeItems}
        rowKey={(record) => `${record.orderItemId}-${record.originalOptionId}-${record.newOptionId}`}
        pagination={false}
        locale={{
          emptyText: '교환 품목이 없습니다.',
        }}
      />
      <div style={{ marginTop: '16px', padding: '12px', background: '#f0f2f5', borderRadius: '4px' }}>
        <p style={{ margin: 0, fontSize: '13px', color: '#666' }}>
          ℹ️ 상품명과 옵션명은 백엔드 API 수정 후 표시됩니다.
        </p>
      </div>
    </Modal>
  )
}

export default ExchangeItemsModal

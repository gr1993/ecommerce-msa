import { useState, useEffect } from 'react'
import { Table, Card, Space, Input, Button, Select, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PlusOutlined } from '@ant-design/icons'
import './AdminCouponManage.css'

const { Option } = Select

interface Coupon {
  coupon_id: string
  coupon_code: string
  coupon_name: string
  coupon_type: 'PERCENTAGE' | 'FIXED_AMOUNT'
  discount_value: number
  min_purchase_amount?: number
  max_discount_amount?: number
  valid_from: string
  valid_to: string
  usage_limit?: number
  used_count: number
  is_active: boolean
  created_at: string
  updated_at: string
}

function AdminCouponManage() {
  const [coupons, setCoupons] = useState<Coupon[]>([])
  const [filteredCoupons, setFilteredCoupons] = useState<Coupon[]>([])
  const [searchCouponCode, setSearchCouponCode] = useState('')
  const [searchIsActive, setSearchIsActive] = useState<string | undefined>(undefined)

  const couponTypeMap: Record<string, string> = {
    PERCENTAGE: '퍼센트 할인',
    FIXED_AMOUNT: '정액 할인'
  }

  // 쿠폰 데이터 로드 (샘플 데이터)
  useEffect(() => {
    // TODO: API 호출로 쿠폰 데이터 로드
    const sampleCoupons: Coupon[] = [
      {
        coupon_id: '1',
        coupon_code: 'WELCOME10',
        coupon_name: '신규 가입 환영 쿠폰',
        coupon_type: 'PERCENTAGE',
        discount_value: 10,
        min_purchase_amount: 10000,
        max_discount_amount: 5000,
        valid_from: '2024-01-01 00:00:00',
        valid_to: '2024-12-31 23:59:59',
        usage_limit: 1000,
        used_count: 245,
        is_active: true,
        created_at: '2024-01-01 00:00:00',
        updated_at: '2024-01-15 10:00:00'
      },
      {
        coupon_id: '2',
        coupon_code: 'SAVE5000',
        coupon_name: '5천원 할인 쿠폰',
        coupon_type: 'FIXED_AMOUNT',
        discount_value: 5000,
        min_purchase_amount: 30000,
        valid_from: '2024-01-15 00:00:00',
        valid_to: '2024-02-15 23:59:59',
        usage_limit: 500,
        used_count: 123,
        is_active: true,
        created_at: '2024-01-15 00:00:00',
        updated_at: '2024-01-15 10:00:00'
      },
      {
        coupon_id: '3',
        coupon_code: 'SUMMER20',
        coupon_name: '여름 특가 20% 할인',
        coupon_type: 'PERCENTAGE',
        discount_value: 20,
        min_purchase_amount: 50000,
        max_discount_amount: 20000,
        valid_from: '2024-06-01 00:00:00',
        valid_to: '2024-08-31 23:59:59',
        usage_limit: null,
        used_count: 0,
        is_active: false,
        created_at: '2024-05-20 00:00:00',
        updated_at: '2024-05-20 00:00:00'
      }
    ]
    setCoupons(sampleCoupons)
  }, [])

  // 필터링된 데이터
  useEffect(() => {
    const filtered = coupons.filter((coupon) => {
      const codeMatch = !searchCouponCode || 
        coupon.coupon_code.toLowerCase().includes(searchCouponCode.toLowerCase()) ||
        coupon.coupon_name.toLowerCase().includes(searchCouponCode.toLowerCase())
      const activeMatch = searchIsActive === undefined || 
        (searchIsActive === 'true' ? coupon.is_active : !coupon.is_active)
      return codeMatch && activeMatch
    })
    setFilteredCoupons(filtered)
  }, [searchCouponCode, searchIsActive, coupons])

  const handleSearch = () => {
    // 검색 버튼 클릭 시 필터 적용 (현재는 실시간 필터링이므로 별도 로직 불필요)
  }

  const handleReset = () => {
    setSearchCouponCode('')
    setSearchIsActive(undefined)
  }

  const handleRegister = () => {
    // TODO: 쿠폰 등록 페이지로 이동
    console.log('쿠폰 등록')
  }

  const columns: ColumnsType<Coupon> = [
    {
      title: '쿠폰 코드',
      dataIndex: 'coupon_code',
      key: 'coupon_code',
      sorter: (a, b) => a.coupon_code.localeCompare(b.coupon_code),
      width: 150,
    },
    {
      title: '쿠폰명',
      dataIndex: 'coupon_name',
      key: 'coupon_name',
      sorter: (a, b) => a.coupon_name.localeCompare(b.coupon_name),
      width: 200,
    },
    {
      title: '할인 유형',
      dataIndex: 'coupon_type',
      key: 'coupon_type',
      filters: [
        { text: '퍼센트 할인', value: 'PERCENTAGE' },
        { text: '정액 할인', value: 'FIXED_AMOUNT' },
      ],
      onFilter: (value, record) => record.coupon_type === value,
      render: (type: string) => couponTypeMap[type] || type,
      width: 120,
    },
    {
      title: '할인 금액',
      key: 'discount',
      render: (_, record: Coupon) => {
        if (record.coupon_type === 'PERCENTAGE') {
          return `${record.discount_value}%${record.max_discount_amount ? ` (최대 ${record.max_discount_amount.toLocaleString()}원)` : ''}`
        } else {
          return `${record.discount_value.toLocaleString()}원`
        }
      },
      width: 180,
    },
    {
      title: '최소 구매 금액',
      dataIndex: 'min_purchase_amount',
      key: 'min_purchase_amount',
      render: (amount: number | null) => amount ? `${amount.toLocaleString()}원` : '-',
      align: 'right',
      width: 130,
    },
    {
      title: '유효 기간',
      key: 'valid_period',
      render: (_, record: Coupon) => {
        const from = new Date(record.valid_from).toLocaleDateString('ko-KR')
        const to = new Date(record.valid_to).toLocaleDateString('ko-KR')
        return `${from} ~ ${to}`
      },
      width: 200,
    },
    {
      title: '사용 횟수',
      key: 'usage',
      render: (_, record: Coupon) => {
        if (record.usage_limit) {
          return `${record.used_count} / ${record.usage_limit}`
        }
        return `${record.used_count} / 무제한`
      },
      width: 120,
    },
    {
      title: '상태',
      dataIndex: 'is_active',
      key: 'is_active',
      filters: [
        { text: '활성', value: 'true' },
        { text: '비활성', value: 'false' },
      ],
      onFilter: (value, record) => {
        if (value === 'true') return record.is_active
        return !record.is_active
      },
      render: (isActive: boolean) => (
        <Tag color={isActive ? 'green' : 'red'}>
          {isActive ? '활성' : '비활성'}
        </Tag>
      ),
      width: 100,
    },
    {
      title: '생성 일시',
      dataIndex: 'created_at',
      key: 'created_at',
      sorter: (a, b) => new Date(a.created_at).getTime() - new Date(b.created_at).getTime(),
      render: (date: string) => {
        const dateObj = new Date(date)
        return dateObj.toLocaleString('ko-KR', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit'
        })
      },
      width: 160,
    },
  ]

  return (
    <div className="admin-coupon-manage">
      <div className="coupon-manage-container">
        <div className="coupon-list-header">
          <h2>쿠폰 관리</h2>
        </div>

        <div className="coupon-list-filters">
          <div className="filter-inputs">
            <Space size="middle">
              <Input
                placeholder="쿠폰 코드 또는 쿠폰명 검색"
                allowClear
                style={{ width: 250 }}
                value={searchCouponCode}
                onChange={(e) => setSearchCouponCode(e.target.value)}
                onPressEnter={handleSearch}
              />
              <Select
                placeholder="상태 선택"
                allowClear
                style={{ width: 150 }}
                value={searchIsActive}
                onChange={(value) => setSearchIsActive(value)}
              >
                <Option value="true">활성</Option>
                <Option value="false">비활성</Option>
              </Select>
            </Space>
          </div>
          <div className="filter-actions">
            <Space>
              <Button onClick={handleReset}>초기화</Button>
              <Button type="primary" onClick={handleSearch}>
                검색
              </Button>
              <Button 
                type="primary" 
                icon={<PlusOutlined />}
                onClick={handleRegister}
                style={{ backgroundColor: '#FFC107', borderColor: '#FFC107', color: '#343A40', fontWeight: 600 }}
              >
                쿠폰 등록
              </Button>
            </Space>
          </div>
        </div>

        <Table
          columns={columns}
          dataSource={filteredCoupons}
          rowKey="coupon_id"
          scroll={{ x: 'max-content' }}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total) => `총 ${total}개`,
          }}
        />
      </div>
    </div>
  )
}

export default AdminCouponManage


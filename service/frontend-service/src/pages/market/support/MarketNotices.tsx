import { useState, useEffect } from 'react'
import { Card, List, Empty, Input, Button, Space, Tag, Modal, message, Divider } from 'antd'
import { SearchOutlined, EyeOutlined, PushpinOutlined } from '@ant-design/icons'
import './MarketNotices.css'

interface Notice {
  notice_id: string
  title: string
  content: string
  author_name: string
  is_pinned: boolean
  is_displayed: boolean
  view_count: number
  created_at: string
  updated_at: string
}

function MarketNotices() {
  const [notices, setNotices] = useState<Notice[]>([])
  const [filteredNotices, setFilteredNotices] = useState<Notice[]>([])
  const [loading, setLoading] = useState(false)
  const [searchText, setSearchText] = useState('')
  const [selectedNotice, setSelectedNotice] = useState<Notice | null>(null)
  const [isDetailModalVisible, setIsDetailModalVisible] = useState(false)

  useEffect(() => {
    loadNotices()
  }, [])

  useEffect(() => {
    filterNotices()
  }, [searchText, notices])

  const loadNotices = async () => {
    setLoading(true)
    try {
      // TODO: API 호출로 공지사항 목록 가져오기 (is_displayed가 true인 것만)
      await new Promise(resolve => setTimeout(resolve, 500))
      
      const sampleNotices: Notice[] = [
        {
          notice_id: '1',
          title: '신규 상품 출시 안내',
          content: '<p>안녕하세요. 새로운 상품이 출시되었습니다.</p><p>많은 관심 부탁드립니다.</p>',
          author_name: '관리자',
          is_pinned: true,
          is_displayed: true,
          view_count: 150,
          created_at: '2024-01-01 10:00:00',
          updated_at: '2024-01-01 10:00:00'
        },
        {
          notice_id: '2',
          title: '배송 지연 안내',
          content: '<p>설 명절 기간으로 인해 배송이 지연될 수 있습니다.</p><p>양해 부탁드립니다.</p>',
          author_name: '관리자',
          is_pinned: false,
          is_displayed: true,
          view_count: 89,
          created_at: '2024-01-15 14:30:00',
          updated_at: '2024-01-15 14:30:00'
        },
        {
          notice_id: '3',
          title: '시스템 점검 안내',
          content: '<p>시스템 점검으로 인해 일시적으로 서비스가 중단됩니다.</p><p>점검 시간: 2024-01-20 02:00 ~ 04:00</p>',
          author_name: '관리자',
          is_pinned: false,
          is_displayed: true,
          view_count: 45,
          created_at: '2024-01-20 09:00:00',
          updated_at: '2024-01-20 09:00:00'
        },
        {
          notice_id: '4',
          title: '회원 등급 혜택 안내',
          content: '<p>VIP 회원에게는 추가 할인 혜택이 제공됩니다.</p>',
          author_name: '관리자',
          is_pinned: false,
          is_displayed: true,
          view_count: 120,
          created_at: '2024-01-10 11:00:00',
          updated_at: '2024-01-10 11:00:00'
        }
      ]
      
      // is_displayed가 true인 것만 필터링
      const displayedNotices = sampleNotices.filter(notice => notice.is_displayed)
      setNotices(displayedNotices)
    } catch (error) {
      message.error('공지사항을 불러오는데 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const filterNotices = () => {
    let filtered = notices
    
    if (searchText.trim()) {
      filtered = notices.filter(notice =>
        notice.title.toLowerCase().includes(searchText.toLowerCase())
      )
    }
    
    // 고정 공지사항을 먼저 정렬
    const sorted = filtered.sort((a, b) => {
      if (a.is_pinned && !b.is_pinned) return -1
      if (!a.is_pinned && b.is_pinned) return 1
      return new Date(b.created_at).getTime() - new Date(a.created_at).getTime()
    })
    
    setFilteredNotices(sorted)
  }

  const handleNoticeClick = async (notice: Notice) => {
    setSelectedNotice(notice)
    setIsDetailModalVisible(true)
    
    // TODO: API 호출로 조회수 증가
    setNotices(prev =>
      prev.map(n =>
        n.notice_id === notice.notice_id
          ? { ...n, view_count: n.view_count + 1 }
          : n
      )
    )
  }

  return (
    <div className="market-notices">
      <div className="notices-container">
        <Card title="공지사항" className="notices-card">
          <div className="notices-search">
            <Input
              placeholder="공지사항 제목으로 검색"
              prefix={<SearchOutlined />}
              size="large"
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              allowClear
            />
          </div>

          {filteredNotices.length === 0 ? (
            <Empty description="공지사항이 없습니다." />
          ) : (
            <List
              dataSource={filteredNotices}
              loading={loading}
              renderItem={(notice) => (
                <List.Item
                  className={`notice-item ${notice.is_pinned ? 'pinned' : ''}`}
                  onClick={() => handleNoticeClick(notice)}
                  style={{ cursor: 'pointer' }}
                >
                  <div className="notice-item-content">
                    <div className="notice-header">
                      <Space>
                        {notice.is_pinned && (
                          <Tag color="red" icon={<PushpinOutlined />}>
                            고정
                          </Tag>
                        )}
                        <h3 className="notice-title">{notice.title}</h3>
                      </Space>
                    </div>
                    <div className="notice-meta">
                      <Space split={<span style={{ color: '#d9d9d9' }}>|</span>}>
                        <span>{notice.author_name}</span>
                        <span>{notice.created_at.split(' ')[0]}</span>
                        <span>
                          <EyeOutlined /> {notice.view_count}
                        </span>
                      </Space>
                    </div>
                  </div>
                </List.Item>
              )}
            />
          )}
        </Card>
      </div>

      {/* 공지사항 상세 모달 */}
      <Modal
        title={selectedNotice?.is_pinned ? (
          <Space>
            <Tag color="red" icon={<PushpinOutlined />}>고정</Tag>
            <span>{selectedNotice?.title}</span>
          </Space>
        ) : (
          selectedNotice?.title
        )}
        open={isDetailModalVisible}
        onCancel={() => setIsDetailModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setIsDetailModalVisible(false)}>
            닫기
          </Button>
        ]}
        width={800}
      >
        {selectedNotice && (
          <div className="notice-detail">
            <div className="notice-detail-meta">
              <Space split={<span style={{ color: '#d9d9d9' }}>|</span>}>
                <span>작성자: {selectedNotice.author_name}</span>
                <span>작성일: {selectedNotice.created_at}</span>
                <span>조회수: {selectedNotice.view_count}</span>
              </Space>
            </div>
            <Divider />
            <div 
              className="notice-detail-content"
              dangerouslySetInnerHTML={{ __html: selectedNotice.content }}
            />
          </div>
        )}
      </Modal>
    </div>
  )
}

export default MarketNotices


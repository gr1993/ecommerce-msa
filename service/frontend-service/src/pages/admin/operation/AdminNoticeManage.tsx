import { useState, useEffect, useRef } from 'react'
import { Table, Space, Input, Button, Select, Tag, Modal, Form, Switch, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PlusOutlined } from '@ant-design/icons'
import ReactQuill from 'react-quill-new'
import 'react-quill-new/dist/quill.snow.css'
import './AdminNoticeManage.css'

const { Option } = Select

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

function AdminNoticeManage() {
  const [notices, setNotices] = useState<Notice[]>([])
  const [filteredNotices, setFilteredNotices] = useState<Notice[]>([])
  const [searchTitle, setSearchTitle] = useState('')
  const [searchDisplayed, setSearchDisplayed] = useState<string | undefined>(undefined)
  const [isRegisterModalVisible, setIsRegisterModalVisible] = useState(false)
  const [isDetailModalVisible, setIsDetailModalVisible] = useState(false)
  const [selectedNotice, setSelectedNotice] = useState<Notice | null>(null)
  const [noticeForm] = Form.useForm()
  const [detailForm] = Form.useForm()
  const quillRef = useRef<any>(null)
  const detailQuillRef = useRef<any>(null)

  // 공지사항 데이터 로드 (샘플 데이터)
  useEffect(() => {
    // TODO: API 호출로 공지사항 데이터 로드
    const sampleNotices: Notice[] = [
      {
        notice_id: '1',
        title: '신규 상품 출시 안내',
        content: '<p>안녕하세요. 새로운 상품이 출시되었습니다.</p>',
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
        content: '<p>설 명절 기간으로 인해 배송이 지연될 수 있습니다.</p>',
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
        content: '<p>시스템 점검으로 인해 일시적으로 서비스가 중단됩니다.</p>',
        author_name: '관리자',
        is_pinned: false,
        is_displayed: false,
        view_count: 45,
        created_at: '2024-01-20 09:00:00',
        updated_at: '2024-01-20 09:00:00'
      }
    ]
    setNotices(sampleNotices)
  }, [])

  // 필터링된 데이터
  useEffect(() => {
    const filtered = notices.filter((notice) => {
      const titleMatch = !searchTitle || 
        notice.title.toLowerCase().includes(searchTitle.toLowerCase())
      const displayedMatch = !searchDisplayed || 
        (searchDisplayed === 'true' ? notice.is_displayed : !notice.is_displayed)
      return titleMatch && displayedMatch
    })
    // 고정 공지사항을 먼저 정렬
    const sorted = filtered.sort((a, b) => {
      if (a.is_pinned && !b.is_pinned) return -1
      if (!a.is_pinned && b.is_pinned) return 1
      return new Date(b.created_at).getTime() - new Date(a.created_at).getTime()
    })
    setFilteredNotices(sorted)
  }, [searchTitle, searchDisplayed, notices])

  const handleSearch = () => {
    // 검색 버튼 클릭 시 필터 적용 (현재는 실시간 필터링이므로 별도 로직 불필요)
  }

  const handleReset = () => {
    setSearchTitle('')
    setSearchDisplayed(undefined)
  }

  // 이미지를 base64로 변환하여 에디터에 삽입
  const imageHandler = (quillInstance: any) => {
    const input = document.createElement('input')
    input.setAttribute('type', 'file')
    input.setAttribute('accept', 'image/*')
    input.click()

    input.onchange = async () => {
      const file = input.files?.[0]
      if (!file) return

      // 파일 크기 제한 (5MB)
      if (file.size > 5 * 1024 * 1024) {
        message.error('이미지 파일 크기는 5MB 이하여야 합니다.')
        return
      }

      const reader = new FileReader()
      reader.onload = () => {
        const base64 = reader.result as string
        const range = quillInstance.getEditor().getSelection(true)
        quillInstance.getEditor().insertEmbed(range.index, 'image', base64)
      }
      reader.readAsDataURL(file)
    }
  }


  // Quill 모듈 설정
  const modules = {
    toolbar: {
      container: [
        [{ 'header': [1, 2, 3, false] }],
        ['bold', 'italic', 'underline', 'strike'],
        [{ 'list': 'ordered'}, { 'list': 'bullet' }],
        [{ 'align': [] }],
        ['link', 'image'],
        [{ 'color': [] }, { 'background': [] }],
        ['clean']
      ],
      handlers: {
        image: function(this: any) {
          if (quillRef.current) {
            imageHandler(quillRef.current)
          }
        }
      }
    }
  }

  const detailModules = {
    toolbar: {
      container: [
        [{ 'header': [1, 2, 3, false] }],
        ['bold', 'italic', 'underline', 'strike'],
        [{ 'list': 'ordered'}, { 'list': 'bullet' }],
        [{ 'align': [] }],
        ['link', 'image'],
        [{ 'color': [] }, { 'background': [] }],
        ['clean']
      ],
      handlers: {
        image: function(this: any) {
          if (detailQuillRef.current) {
            imageHandler(detailQuillRef.current)
          }
        }
      }
    }
  }

  const formats = [
    'header',
    'bold', 'italic', 'underline', 'strike',
    'list', 'bullet',
    'align',
    'link', 'image',
    'color', 'background'
  ]

  const handleRegister = () => {
    noticeForm.resetFields()
    noticeForm.setFieldsValue({
      is_pinned: false,
      is_displayed: true
    })
    setIsRegisterModalVisible(true)
  }

  const handleRegisterSave = async () => {
    try {
      const values = await noticeForm.validateFields()
      const content = quillRef.current?.getEditor().root.innerHTML || ''
      
      if (!content || content === '<p><br></p>') {
        message.error('내용을 입력하세요.')
        return
      }

      const newNotice: Notice = {
        notice_id: `notice_${Date.now()}`,
        title: values.title,
        content: content,
        author_name: '관리자', // TODO: 실제 로그인한 관리자 정보 사용
        is_pinned: values.is_pinned || false,
        is_displayed: values.is_displayed !== undefined ? values.is_displayed : true,
        view_count: 0,
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString()
      }

      // TODO: API 호출로 공지사항 등록
      setNotices(prev => [newNotice, ...prev])
      message.success('공지사항이 등록되었습니다.')
      setIsRegisterModalVisible(false)
      noticeForm.resetFields()
      if (quillRef.current) {
        quillRef.current.getEditor().setText('')
      }
    } catch (error) {
      console.error('Validation failed:', error)
    }
  }

  const handleRegisterModalClose = () => {
    setIsRegisterModalVisible(false)
    noticeForm.resetFields()
    if (quillRef.current) {
      quillRef.current.getEditor().setText('')
    }
  }

  // 공지사항 상세 조회 및 수정
  const handleNoticeClick = (notice: Notice) => {
    setSelectedNotice(notice)
    
    detailForm.setFieldsValue({
      title: notice.title,
      is_pinned: notice.is_pinned,
      is_displayed: notice.is_displayed
    })
    
    if (detailQuillRef.current) {
      detailQuillRef.current.getEditor().root.innerHTML = notice.content
    }
    
    setIsDetailModalVisible(true)
  }

  const handleDetailModalClose = () => {
    setIsDetailModalVisible(false)
    setSelectedNotice(null)
    detailForm.resetFields()
    if (detailQuillRef.current) {
      detailQuillRef.current.getEditor().setText('')
    }
  }

  const handleDetailSave = async () => {
    if (!selectedNotice) return

    try {
      const values = await detailForm.validateFields()
      const content = detailQuillRef.current?.getEditor().root.innerHTML || ''
      
      if (!content || content === '<p><br></p>') {
        message.error('내용을 입력하세요.')
        return
      }

      // TODO: API 호출로 공지사항 정보 업데이트
      setNotices(prev =>
        prev.map(notice =>
          notice.notice_id === selectedNotice.notice_id
            ? {
                ...notice,
                title: values.title,
                content: content,
                is_pinned: values.is_pinned || false,
                is_displayed: values.is_displayed !== undefined ? values.is_displayed : true,
                updated_at: new Date().toISOString()
              }
            : notice
        )
      )

      message.success('공지사항 정보가 수정되었습니다.')
      setIsDetailModalVisible(false)
      setSelectedNotice(null)
      detailForm.resetFields()
      if (detailQuillRef.current) {
        detailQuillRef.current.getEditor().setText('')
      }
    } catch (error) {
      console.error('Validation failed:', error)
    }
  }

  const handleToggleDisplay = (notice: Notice) => {
    // TODO: API 호출로 전시 여부 업데이트
    setNotices(prev =>
      prev.map(n =>
        n.notice_id === notice.notice_id
          ? { ...n, is_displayed: !n.is_displayed, updated_at: new Date().toISOString() }
          : n
      )
    )
    message.success(`공지사항이 ${notice.is_displayed ? '비전시' : '전시'} 상태로 변경되었습니다.`)
  }

  const columns: ColumnsType<Notice> = [
    {
      title: '제목',
      dataIndex: 'title',
      key: 'title',
      sorter: (a, b) => a.title.localeCompare(b.title),
      render: (text: string, record: Notice) => (
        <Space>
          {record.is_pinned && <Tag color="red">고정</Tag>}
          <a 
            onClick={() => handleNoticeClick(record)}
            style={{ color: '#007BFF', cursor: 'pointer' }}
          >
            {text}
          </a>
        </Space>
      ),
      width: 300,
    },
    {
      title: '작성자',
      dataIndex: 'author_name',
      key: 'author_name',
      width: 100,
    },
    {
      title: '조회수',
      dataIndex: 'view_count',
      key: 'view_count',
      align: 'right',
      width: 80,
    },
    {
      title: '전시 여부',
      dataIndex: 'is_displayed',
      key: 'is_displayed',
      filters: [
        { text: '전시', value: 'true' },
        { text: '비전시', value: 'false' },
      ],
      onFilter: (value, record) => {
        if (value === 'true') return record.is_displayed
        return !record.is_displayed
      },
      render: (isDisplayed: boolean, record: Notice) => (
        <Switch
          checked={isDisplayed}
          onChange={() => handleToggleDisplay(record)}
          checkedChildren="전시"
          unCheckedChildren="비전시"
        />
      ),
      width: 100,
    },
    {
      title: '작성 일시',
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
    {
      title: '수정 일시',
      dataIndex: 'updated_at',
      key: 'updated_at',
      sorter: (a, b) => new Date(a.updated_at).getTime() - new Date(b.updated_at).getTime(),
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
    <div className="admin-notice-manage">
      <div className="notice-manage-container">
        <div className="notice-list-header">
          <h2>공지사항 관리</h2>
        </div>

        <div className="notice-list-filters">
          <div className="filter-inputs">
            <Space size="middle">
              <Input
                placeholder="제목 검색"
                allowClear
                style={{ width: 250 }}
                value={searchTitle}
                onChange={(e) => setSearchTitle(e.target.value)}
                onPressEnter={handleSearch}
              />
              <Select
                placeholder="전시 여부 선택"
                allowClear
                style={{ width: 150 }}
                value={searchDisplayed}
                onChange={(value) => setSearchDisplayed(value)}
              >
                <Option value="true">전시</Option>
                <Option value="false">비전시</Option>
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
                공지사항 등록
              </Button>
            </Space>
          </div>
        </div>

        <Table
          columns={columns}
          dataSource={filteredNotices}
          rowKey="notice_id"
          scroll={{ x: 'max-content' }}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total) => `총 ${total}개`,
          }}
        />

        {/* 공지사항 등록 모달 */}
        {isRegisterModalVisible && (
          <Modal
            title="공지사항 등록"
            open={isRegisterModalVisible}
            onCancel={handleRegisterModalClose}
            onOk={handleRegisterSave}
            okText="등록"
            cancelText="취소"
            okButtonProps={{
              style: { backgroundColor: '#FFC107', borderColor: '#FFC107', color: '#343A40', fontWeight: 600 }
            }}
            width={900}
            destroyOnClose={true}
          >
            <Form
              form={noticeForm}
              layout="vertical"
              initialValues={{
                is_pinned: false,
                is_displayed: true
              }}
            >
              <Form.Item
                label="제목"
                name="title"
                rules={[
                  { required: true, message: '제목을 입력하세요' },
                  { max: 200, message: '제목은 최대 200자까지 입력 가능합니다.' }
                ]}
              >
                <Input placeholder="공지사항 제목을 입력하세요" maxLength={200} />
              </Form.Item>

              <Form.Item
                label="내용"
                name="content"
                rules={[{ required: true, message: '내용을 입력하세요' }]}
              >
                <ReactQuill
                  ref={quillRef}
                  theme="snow"
                  modules={modules}
                  formats={formats}
                  placeholder="공지사항 내용을 입력하세요"
                  style={{ height: '300px', marginBottom: '50px' }}
                />
              </Form.Item>

              <Form.Item
                label="고정 공지사항"
                name="is_pinned"
                valuePropName="checked"
              >
                <Switch checkedChildren="고정" unCheckedChildren="일반" />
              </Form.Item>

              <Form.Item
                label="전시 여부"
                name="is_displayed"
                valuePropName="checked"
              >
                <Switch checkedChildren="전시" unCheckedChildren="비전시" />
              </Form.Item>
            </Form>
          </Modal>
        )}

        {/* 공지사항 상세 조회 및 수정 모달 */}
        <Modal
          title={`공지사항 상세 - ${selectedNotice?.title}`}
          open={isDetailModalVisible}
          onCancel={handleDetailModalClose}
          footer={[
            <Button key="cancel" onClick={handleDetailModalClose}>
              취소
            </Button>,
            <Button 
              key="save" 
              type="primary" 
              onClick={handleDetailSave}
              style={{ backgroundColor: '#FFC107', borderColor: '#FFC107', color: '#343A40', fontWeight: 600 }}
            >
              저장
            </Button>
          ]}
          width={900}
        >
          {selectedNotice && (
            <Form
              form={detailForm}
              layout="vertical"
            >
              <Form.Item
                label="제목"
                name="title"
                rules={[
                  { required: true, message: '제목을 입력하세요' },
                  { max: 200, message: '제목은 최대 200자까지 입력 가능합니다.' }
                ]}
              >
                <Input placeholder="공지사항 제목을 입력하세요" maxLength={200} />
              </Form.Item>

              <Form.Item
                label="내용"
                name="content"
                rules={[{ required: true, message: '내용을 입력하세요' }]}
              >
                <ReactQuill
                  ref={detailQuillRef}
                  theme="snow"
                  modules={detailModules}
                  formats={formats}
                  placeholder="공지사항 내용을 입력하세요"
                  style={{ height: '300px', marginBottom: '50px' }}
                />
              </Form.Item>

              <Form.Item
                label="고정 공지사항"
                name="is_pinned"
                valuePropName="checked"
              >
                <Switch checkedChildren="고정" unCheckedChildren="일반" />
              </Form.Item>

              <Form.Item
                label="전시 여부"
                name="is_displayed"
                valuePropName="checked"
              >
                <Switch checkedChildren="전시" unCheckedChildren="비전시" />
              </Form.Item>
            </Form>
          )}
        </Modal>
      </div>
    </div>
  )
}

export default AdminNoticeManage

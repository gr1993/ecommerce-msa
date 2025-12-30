import { useState, useEffect } from 'react'
import { Table, Space, Input, Button, Select, Tag, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import './AdminUserManage.css'

const { Option } = Select

interface User {
  user_id: string
  email: string
  name: string
  phone?: string
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED'
  grade: string
  points: number
  created_at: string
  updated_at: string
}

function AdminUserManage() {
  const [users, setUsers] = useState<User[]>([])
  const [filteredUsers, setFilteredUsers] = useState<User[]>([])
  const [searchText, setSearchText] = useState('')
  const [searchStatus, setSearchStatus] = useState<string | undefined>(undefined)
  const [searchGrade, setSearchGrade] = useState<string | undefined>(undefined)

  const statusMap: Record<string, { label: string; color: string }> = {
    ACTIVE: { label: '활성', color: 'green' },
    INACTIVE: { label: '비활성', color: 'default' },
    SUSPENDED: { label: '정지', color: 'red' }
  }

  const gradeMap: Record<string, string> = {
    NORMAL: '일반',
    VIP: 'VIP',
    GOLD: '골드',
    SILVER: '실버'
  }

  // 사용자 데이터 로드 (샘플 데이터)
  useEffect(() => {
    // TODO: API 호출로 사용자 데이터 로드
    const sampleUsers: User[] = [
      {
        user_id: '1',
        email: 'user1@example.com',
        name: '홍길동',
        phone: '010-1234-5678',
        status: 'ACTIVE',
        grade: 'VIP',
        points: 5000,
        created_at: '2024-01-01 10:00:00',
        updated_at: '2024-01-15 10:00:00'
      },
      {
        user_id: '2',
        email: 'user2@example.com',
        name: '김철수',
        phone: '010-2345-6789',
        status: 'ACTIVE',
        grade: 'NORMAL',
        points: 1200,
        created_at: '2024-01-05 14:30:00',
        updated_at: '2024-01-10 09:20:00'
      },
      {
        user_id: '3',
        email: 'user3@example.com',
        name: '이영희',
        phone: '010-3456-7890',
        status: 'INACTIVE',
        grade: 'GOLD',
        points: 3000,
        created_at: '2023-12-20 11:00:00',
        updated_at: '2024-01-01 08:00:00'
      },
      {
        user_id: '4',
        email: 'user4@example.com',
        name: '박민수',
        phone: '010-4567-8901',
        status: 'SUSPENDED',
        grade: 'NORMAL',
        points: 0,
        created_at: '2023-11-15 16:00:00',
        updated_at: '2024-01-10 15:30:00'
      },
      {
        user_id: '5',
        email: 'user5@example.com',
        name: '최지영',
        phone: '010-5678-9012',
        status: 'ACTIVE',
        grade: 'SILVER',
        points: 2500,
        created_at: '2024-01-10 09:00:00',
        updated_at: '2024-01-12 10:15:00'
      }
    ]
    setUsers(sampleUsers)
  }, [])

  // 필터링된 데이터
  useEffect(() => {
    const filtered = users.filter((user) => {
      const textMatch = !searchText || 
        user.email.toLowerCase().includes(searchText.toLowerCase()) ||
        user.name.toLowerCase().includes(searchText.toLowerCase())
      const statusMatch = !searchStatus || user.status === searchStatus
      const gradeMatch = !searchGrade || user.grade === searchGrade
      return textMatch && statusMatch && gradeMatch
    })
    setFilteredUsers(filtered)
  }, [searchText, searchStatus, searchGrade, users])

  const handleSearch = () => {
    // 검색 버튼 클릭 시 필터 적용 (현재는 실시간 필터링이므로 별도 로직 불필요)
  }

  const handleReset = () => {
    setSearchText('')
    setSearchStatus(undefined)
    setSearchGrade(undefined)
  }

  const handleStatusChange = (userId: string, newStatus: string) => {
    // TODO: API 호출로 사용자 상태 업데이트
    setUsers(prev =>
      prev.map(user =>
        user.user_id === userId
          ? { ...user, status: newStatus as User['status'], updated_at: new Date().toISOString() }
          : user
      )
    )
    message.success('사용자 상태가 변경되었습니다.')
  }

  const handleGradeChange = (userId: string, newGrade: string) => {
    // TODO: API 호출로 사용자 등급 업데이트
    setUsers(prev =>
      prev.map(user =>
        user.user_id === userId
          ? { ...user, grade: newGrade, updated_at: new Date().toISOString() }
          : user
      )
    )
    message.success('사용자 등급이 변경되었습니다.')
  }

  const columns: ColumnsType<User> = [
    {
      title: '사용자 ID',
      dataIndex: 'user_id',
      key: 'user_id',
      width: 100,
    },
    {
      title: '이메일',
      dataIndex: 'email',
      key: 'email',
      sorter: (a, b) => a.email.localeCompare(b.email),
      width: 200,
    },
    {
      title: '이름',
      dataIndex: 'name',
      key: 'name',
      sorter: (a, b) => a.name.localeCompare(b.name),
      width: 120,
    },
    {
      title: '연락처',
      dataIndex: 'phone',
      key: 'phone',
      render: (phone: string) => phone || '-',
      width: 150,
    },
    {
      title: '상태',
      dataIndex: 'status',
      key: 'status',
      filters: [
        { text: '활성', value: 'ACTIVE' },
        { text: '비활성', value: 'INACTIVE' },
        { text: '정지', value: 'SUSPENDED' },
      ],
      onFilter: (value, record) => record.status === value,
      render: (status: string, record: User) => {
        const statusInfo = statusMap[status]
        return (
          <Select
            value={status}
            onChange={(value) => handleStatusChange(record.user_id, value)}
            style={{ width: 100 }}
            size="small"
          >
            <Option value="ACTIVE">활성</Option>
            <Option value="INACTIVE">비활성</Option>
            <Option value="SUSPENDED">정지</Option>
          </Select>
        )
      },
      width: 120,
    },
    {
      title: '회원 등급',
      dataIndex: 'grade',
      key: 'grade',
      filters: [
        { text: '일반', value: 'NORMAL' },
        { text: 'VIP', value: 'VIP' },
        { text: '골드', value: 'GOLD' },
        { text: '실버', value: 'SILVER' },
      ],
      onFilter: (value, record) => record.grade === value,
      render: (grade: string, record: User) => {
        return (
          <Select
            value={grade}
            onChange={(value) => handleGradeChange(record.user_id, value)}
            style={{ width: 100 }}
            size="small"
          >
            <Option value="NORMAL">일반</Option>
            <Option value="VIP">VIP</Option>
            <Option value="GOLD">골드</Option>
            <Option value="SILVER">실버</Option>
          </Select>
        )
      },
      width: 120,
    },
    {
      title: '포인트',
      dataIndex: 'points',
      key: 'points',
      sorter: (a, b) => a.points - b.points,
      render: (points: number) => points.toLocaleString(),
      align: 'right',
      width: 120,
    },
    {
      title: '가입 일시',
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
    <div className="admin-user-manage">
      <div className="user-manage-container">
        <div className="user-list-header">
          <h2>회원 관리</h2>
        </div>

        <div className="user-list-filters">
          <div className="filter-inputs">
            <Space size="middle">
              <Input
                placeholder="이메일 또는 이름 검색"
                allowClear
                style={{ width: 250 }}
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
                onPressEnter={handleSearch}
              />
              <Select
                placeholder="상태 선택"
                allowClear
                style={{ width: 150 }}
                value={searchStatus}
                onChange={(value) => setSearchStatus(value)}
              >
                <Option value="ACTIVE">활성</Option>
                <Option value="INACTIVE">비활성</Option>
                <Option value="SUSPENDED">정지</Option>
              </Select>
              <Select
                placeholder="등급 선택"
                allowClear
                style={{ width: 150 }}
                value={searchGrade}
                onChange={(value) => setSearchGrade(value)}
              >
                <Option value="NORMAL">일반</Option>
                <Option value="VIP">VIP</Option>
                <Option value="GOLD">골드</Option>
                <Option value="SILVER">실버</Option>
              </Select>
            </Space>
          </div>
          <div className="filter-actions">
            <Space>
              <Button onClick={handleReset}>초기화</Button>
              <Button type="primary" onClick={handleSearch}>
                검색
              </Button>
            </Space>
          </div>
        </div>

        <Table
          columns={columns}
          dataSource={filteredUsers}
          rowKey="user_id"
          scroll={{ x: 'max-content' }}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total) => `총 ${total}명`,
          }}
        />
      </div>
    </div>
  )
}

export default AdminUserManage


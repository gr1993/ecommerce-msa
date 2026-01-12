import { useState, useEffect } from 'react'
import { Table, Space, Input, Button, Select, Tag, message, Spin } from 'antd'
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table'
import { searchUsers } from '../../../api/userApi'
import type { UserResponse } from '../../../api/userApi'
import './AdminUserManage.css'

const { Option } = Select

function AdminUserManage() {
  const [users, setUsers] = useState<UserResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [searchText, setSearchText] = useState('')
  const [searchStatus, setSearchStatus] = useState<'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | undefined>(undefined)
  const [searchGrade, setSearchGrade] = useState<'NORMAL' | 'VIP' | 'GOLD' | 'SILVER' | undefined>(undefined)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })

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

  // 사용자 데이터 로드
  const fetchUsers = async () => {
    setLoading(true)
    try {
      const response = await searchUsers({
        searchText: searchText || undefined,
        status: searchStatus,
        grade: searchGrade,
        page: pagination.current - 1, // API uses 0-based page numbering
        size: pagination.pageSize,
        sortBy: 'createdAt',
        sortDirection: 'DESC',
      })

      setUsers(response.content)
      setPagination(prev => ({
        ...prev,
        total: response.totalElements,
      }))
    } catch (error) {
      console.error('Failed to fetch users:', error)
      message.error(error instanceof Error ? error.message : '회원 목록을 불러오는데 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchUsers()
  }, [])

  const handleSearch = async () => {
    setPagination(prev => ({ ...prev, current: 1 }))
    setLoading(true)
    try {
      const response = await searchUsers({
        searchText: searchText || undefined,
        status: searchStatus,
        grade: searchGrade,
        page: 0, // Reset to first page
        size: pagination.pageSize,
        sortBy: 'createdAt',
        sortDirection: 'DESC',
      })

      setUsers(response.content)
      setPagination(prev => ({
        ...prev,
        current: 1,
        total: response.totalElements,
      }))
    } catch (error) {
      console.error('Failed to fetch users:', error)
      message.error(error instanceof Error ? error.message : '회원 목록을 불러오는데 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const handleReset = async () => {
    setSearchText('')
    setSearchStatus(undefined)
    setSearchGrade(undefined)
    setPagination(prev => ({ ...prev, current: 1 }))

    setLoading(true)
    try {
      const response = await searchUsers({
        page: 0,
        size: pagination.pageSize,
        sortBy: 'createdAt',
        sortDirection: 'DESC',
      })

      setUsers(response.content)
      setPagination(prev => ({
        ...prev,
        current: 1,
        total: response.totalElements,
      }))
    } catch (error) {
      console.error('Failed to fetch users:', error)
      message.error(error instanceof Error ? error.message : '회원 목록을 불러오는데 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const handleTableChange = async (newPagination: TablePaginationConfig) => {
    const newPage = newPagination.current || 1
    const newSize = newPagination.pageSize || 10

    setPagination({
      current: newPage,
      pageSize: newSize,
      total: pagination.total,
    })

    setLoading(true)
    try {
      const response = await searchUsers({
        searchText: searchText || undefined,
        status: searchStatus,
        grade: searchGrade,
        page: newPage - 1, // API uses 0-based page numbering
        size: newSize,
        sortBy: 'createdAt',
        sortDirection: 'DESC',
      })

      setUsers(response.content)
      setPagination(prev => ({
        ...prev,
        total: response.totalElements,
      }))
    } catch (error) {
      console.error('Failed to fetch users:', error)
      message.error(error instanceof Error ? error.message : '회원 목록을 불러오는데 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const handleStatusChange = (userId: number, newStatus: string) => {
    // TODO: API 호출로 사용자 상태 업데이트
    setUsers(prev =>
      prev.map(user =>
        user.userId === userId
          ? { ...user, status: newStatus as UserResponse['status'], updatedAt: new Date().toISOString() }
          : user
      )
    )
    message.success('사용자 상태가 변경되었습니다.')
  }

  const handleGradeChange = (userId: number, newGrade: string) => {
    // TODO: API 호출로 사용자 등급 업데이트
    setUsers(prev =>
      prev.map(user =>
        user.userId === userId
          ? { ...user, grade: newGrade as UserResponse['grade'], updatedAt: new Date().toISOString() }
          : user
      )
    )
    message.success('사용자 등급이 변경되었습니다.')
  }

  const columns: ColumnsType<UserResponse> = [
    {
      title: '사용자 ID',
      dataIndex: 'userId',
      key: 'userId',
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
      render: (status: string, record: UserResponse) => {
        return (
          <Select
            value={status}
            onChange={(value) => handleStatusChange(record.userId, value)}
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
      render: (grade: string, record: UserResponse) => {
        return (
          <Select
            value={grade}
            onChange={(value) => handleGradeChange(record.userId, value)}
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
      dataIndex: 'createdAt',
      key: 'createdAt',
      sorter: (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime(),
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
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      sorter: (a, b) => new Date(a.updatedAt).getTime() - new Date(b.updatedAt).getTime(),
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

        <Spin spinning={loading}>
          <Table
            columns={columns}
            dataSource={users}
            rowKey="userId"
            scroll={{ x: 'max-content' }}
            pagination={{
              current: pagination.current,
              pageSize: pagination.pageSize,
              total: pagination.total,
              showSizeChanger: true,
              showTotal: (total) => `총 ${total}명`,
            }}
            onChange={handleTableChange}
          />
        </Spin>
      </div>
    </div>
  )
}

export default AdminUserManage


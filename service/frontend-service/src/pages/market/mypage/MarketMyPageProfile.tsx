import { useState, useEffect } from 'react'
import { Card, Form, Input, Button, Space, message, Descriptions, Divider, Modal } from 'antd'
import { UserOutlined, LockOutlined, PhoneOutlined, MailOutlined, EditOutlined } from '@ant-design/icons'
import { getUser } from '../../../utils/authUtils'
import './MarketMyPageProfile.css'

interface UserProfile {
  user_id: string
  email: string
  name: string
  phone?: string
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED'
  grade: 'NORMAL' | 'VIP' | 'GOLD' | 'SILVER'
  points: number
  created_at: string
  updated_at: string
}

function MarketMyPageProfile() {
  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [loading, setLoading] = useState(false)
  const [isPasswordModalVisible, setIsPasswordModalVisible] = useState(false)
  const [profileForm] = Form.useForm()
  const [passwordForm] = Form.useForm()

  useEffect(() => {
    loadProfile()
  }, [])

  const loadProfile = async () => {
    setLoading(true)
    try {
      // TODO: API 호출로 회원 정보 가져오기
      await new Promise(resolve => setTimeout(resolve, 500))
      
      const user = getUser()
      
      // 임시 샘플 데이터
      const sampleProfile: UserProfile = {
        user_id: user?.userId || '1',
        email: user?.email || 'user@example.com',
        name: user?.name || '홍길동',
        phone: '010-1234-5678',
        status: 'ACTIVE',
        grade: 'NORMAL',
        points: 12500,
        created_at: '2024-01-01 10:00:00',
        updated_at: '2024-01-15 14:30:00'
      }
      
      setProfile(sampleProfile)
      profileForm.setFieldsValue({
        name: sampleProfile.name,
        phone: sampleProfile.phone,
        email: sampleProfile.email
      })
    } catch (error) {
      message.error('회원 정보를 불러오는데 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const handleProfileUpdate = async (values: any) => {
    try {
      // TODO: API 호출로 회원 정보 수정
      await new Promise(resolve => setTimeout(resolve, 1000))
      
      setProfile(prev => prev ? {
        ...prev,
        name: values.name,
        phone: values.phone,
        updated_at: new Date().toISOString().replace('T', ' ').slice(0, 19)
      } : null)
      
      message.success('회원 정보가 수정되었습니다.')
    } catch (error) {
      message.error('회원 정보 수정에 실패했습니다.')
    }
  }

  const handlePasswordChange = async (values: any) => {
    if (values.new_password !== values.confirm_password) {
      message.error('새 비밀번호와 확인 비밀번호가 일치하지 않습니다.')
      return
    }

    try {
      // TODO: API 호출로 비밀번호 변경
      await new Promise(resolve => setTimeout(resolve, 1000))
      
      message.success('비밀번호가 변경되었습니다.')
      setIsPasswordModalVisible(false)
      passwordForm.resetFields()
    } catch (error) {
      message.error('비밀번호 변경에 실패했습니다.')
    }
  }

  const getGradeLabel = (grade: string) => {
    const gradeMap: Record<string, string> = {
      NORMAL: '일반',
      VIP: 'VIP',
      GOLD: '골드',
      SILVER: '실버'
    }
    return gradeMap[grade] || grade
  }

  const getStatusLabel = (status: string) => {
    const statusMap: Record<string, { label: string; color: string }> = {
      ACTIVE: { label: '활성', color: 'green' },
      INACTIVE: { label: '비활성', color: 'default' },
      SUSPENDED: { label: '정지', color: 'red' }
    }
    return statusMap[status] || { label: status, color: 'default' }
  }

  if (!profile) {
    return (
      <div className="market-mypage-profile">
        <Card>
          <p>회원 정보를 불러오는 중...</p>
        </Card>
      </div>
    )
  }

  return (
    <div className="market-mypage-profile">
      <Card title="회원 정보 수정" className="profile-card">
        <Form
          form={profileForm}
          layout="vertical"
          onFinish={handleProfileUpdate}
          className="profile-form"
        >
          <Descriptions column={1} bordered className="profile-info-section">
            <Descriptions.Item label="이메일">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <MailOutlined />
                <span>{profile.email}</span>
                <span style={{ color: '#999', fontSize: '0.9rem', marginLeft: '0.5rem' }}>
                  (변경 불가)
                </span>
              </div>
            </Descriptions.Item>
            <Descriptions.Item label="회원 등급">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <UserOutlined />
                <strong>{getGradeLabel(profile.grade)}</strong>
              </div>
            </Descriptions.Item>
            <Descriptions.Item label="상태">
              <span style={{ 
                color: getStatusLabel(profile.status).color === 'green' ? '#52c41a' : 
                       getStatusLabel(profile.status).color === 'red' ? '#ff4d4f' : '#999'
              }}>
                {getStatusLabel(profile.status).label}
              </span>
            </Descriptions.Item>
            <Descriptions.Item label="보유 포인트">
              <strong style={{ color: '#667eea', fontSize: '1.1rem' }}>
                {profile.points.toLocaleString()}P
              </strong>
            </Descriptions.Item>
            <Descriptions.Item label="가입일">
              {profile.created_at.split(' ')[0]}
            </Descriptions.Item>
          </Descriptions>

          <Divider />

          <div className="profile-edit-section">
            <h3>기본 정보 수정</h3>
            
            <Form.Item
              name="name"
              label="이름"
              rules={[
                { required: true, message: '이름을 입력해주세요.' },
                { min: 2, message: '이름은 최소 2자 이상이어야 합니다.' }
              ]}
            >
              <Input
                prefix={<UserOutlined />}
                placeholder="이름을 입력해주세요"
                size="large"
              />
            </Form.Item>

            <Form.Item
              name="phone"
              label="연락처"
              rules={[
                { pattern: /^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$/, message: '올바른 전화번호 형식이 아닙니다.' }
              ]}
            >
              <Input
                prefix={<PhoneOutlined />}
                placeholder="010-1234-5678"
                size="large"
              />
            </Form.Item>

            <Form.Item>
              <Space>
                <Button type="primary" htmlType="submit" size="large" loading={loading}>
                  정보 수정
                </Button>
                <Button 
                  size="large"
                  icon={<LockOutlined />}
                  onClick={() => setIsPasswordModalVisible(true)}
                >
                  비밀번호 변경
                </Button>
              </Space>
            </Form.Item>
          </div>
        </Form>
      </Card>

      {/* 비밀번호 변경 모달 */}
      <Modal
        title="비밀번호 변경"
        open={isPasswordModalVisible}
        onCancel={() => {
          setIsPasswordModalVisible(false)
          passwordForm.resetFields()
        }}
        footer={null}
        width={500}
      >
        <Form
          form={passwordForm}
          layout="vertical"
          onFinish={handlePasswordChange}
        >
          <Form.Item
            name="current_password"
            label="현재 비밀번호"
            rules={[{ required: true, message: '현재 비밀번호를 입력해주세요.' }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="현재 비밀번호"
              size="large"
            />
          </Form.Item>

          <Form.Item
            name="new_password"
            label="새 비밀번호"
            rules={[
              { required: true, message: '새 비밀번호를 입력해주세요.' },
              { min: 8, message: '비밀번호는 최소 8자 이상이어야 합니다.' },
              { 
                pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/, 
                message: '비밀번호는 영문 대소문자와 숫자를 포함해야 합니다.' 
              }
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="새 비밀번호"
              size="large"
            />
          </Form.Item>

          <Form.Item
            name="confirm_password"
            label="새 비밀번호 확인"
            dependencies={['new_password']}
            rules={[
              { required: true, message: '비밀번호 확인을 입력해주세요.' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('new_password') === value) {
                    return Promise.resolve()
                  }
                  return Promise.reject(new Error('비밀번호가 일치하지 않습니다.'))
                }
              })
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="새 비밀번호 확인"
              size="large"
            />
          </Form.Item>

          <Form.Item>
            <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
              <Button onClick={() => {
                setIsPasswordModalVisible(false)
                passwordForm.resetFields()
              }}>
                취소
              </Button>
              <Button type="primary" htmlType="submit">
                비밀번호 변경
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default MarketMyPageProfile


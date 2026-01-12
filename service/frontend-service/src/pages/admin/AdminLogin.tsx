import { useState } from 'react'
import { Form, Input, Button, message } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { adminLogin as adminLoginApi } from '../../api/authApi'
import { useAuthStore } from '../../stores/authStore'
import './AdminLogin.css'

function AdminLogin() {
  const navigate = useNavigate()
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const adminLogin = useAuthStore((state) => state.adminLogin)

  const onFinish = async (values: { email: string; password: string }) => {
    setLoading(true)
    try {
      // API 호출로 관리자 로그인 처리
      const response = await adminLoginApi({
        email: values.email,
        password: values.password,
      })

      // Zustand store에 로그인 정보 저장 (자동으로 localStorage에 저장됨)
      adminLogin(
        {
          adminId: response.adminId,
          email: response.email,
          name: response.name,
          role: response.role,
        },
        response.accessToken
      )

      message.success('관리자 로그인되었습니다.')
      navigate('/admin/dashboard')
    } catch (error) {
      if (error instanceof Error) {
        message.error(error.message)
      } else {
        message.error('로그인에 실패했습니다. 이메일과 비밀번호를 확인해주세요.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="admin-login">
      <div className="admin-login-container">
        <div className="admin-login-box">
          <div className="admin-login-header">
            <h1>관리자 로그인</h1>
            <p>박신사 관리자 시스템</p>
          </div>

          <Form
            form={form}
            name="admin-login"
            onFinish={onFinish}
            autoComplete="off"
            size="large"
            className="admin-login-form"
          >
            <Form.Item
              name="email"
              rules={[
                { required: true, message: '이메일을 입력해주세요.' },
                { type: 'email', message: '올바른 이메일 형식이 아닙니다.' }
              ]}
            >
              <Input
                prefix={<UserOutlined />}
                placeholder="관리자 이메일"
                className="admin-login-input"
              />
            </Form.Item>

            <Form.Item
              name="password"
              rules={[
                { required: true, message: '비밀번호를 입력해주세요.' },
                { min: 6, message: '비밀번호는 최소 6자 이상이어야 합니다.' }
              ]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder="비밀번호"
                className="admin-login-input"
              />
            </Form.Item>

            <Form.Item>
              <Button
                type="primary"
                htmlType="submit"
                className="admin-login-button"
                loading={loading}
                block
              >
                로그인
              </Button>
            </Form.Item>
          </Form>
        </div>
      </div>
    </div>
  )
}

export default AdminLogin


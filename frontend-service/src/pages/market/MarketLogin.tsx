import { useState } from 'react'
import { Form, Input, Button, Checkbox, message } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import { useNavigate, Link } from 'react-router-dom'
import MarketHeader from '../../components/market/MarketHeader'
import MarketFooter from '../../components/market/MarketFooter'
import './MarketLogin.css'

function MarketLogin() {
  const navigate = useNavigate()
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)

  const onFinish = async (values: any) => {
    setLoading(true)
    try {
      // TODO: API 호출로 로그인 처리
      console.log('Login values:', values)
      
      // 임시 처리
      await new Promise(resolve => setTimeout(resolve, 1000))
      
      message.success('로그인되었습니다.')
      navigate('/market')
    } catch (error) {
      message.error('로그인에 실패했습니다. 이메일과 비밀번호를 확인해주세요.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="market-login">
      <MarketHeader />
      
      <div className="login-container">
        <div className="login-box">
          <div className="login-header">
            <h1>로그인</h1>
            <p>박신사몰에 오신 것을 환영합니다</p>
          </div>

          <Form
            form={form}
            name="login"
            onFinish={onFinish}
            autoComplete="off"
            size="large"
            className="login-form"
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
                placeholder="이메일"
                className="login-input"
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
                className="login-input"
              />
            </Form.Item>

            <Form.Item>
              <div className="login-options">
                <Form.Item name="remember" valuePropName="checked" noStyle>
                  <Checkbox>로그인 상태 유지</Checkbox>
                </Form.Item>
                <Link to="/market/find-password" className="find-link">
                  비밀번호 찾기
                </Link>
              </div>
            </Form.Item>

            <Form.Item>
              <Button
                type="primary"
                htmlType="submit"
                className="login-button"
                loading={loading}
                block
              >
                로그인
              </Button>
            </Form.Item>
          </Form>

          <div className="login-footer">
            <p className="signup-text">
              아직 회원이 아니신가요?{' '}
              <Link to="/market/signup" className="signup-link">
                회원가입
              </Link>
            </p>
          </div>
        </div>
      </div>

      <MarketFooter />
    </div>
  )
}

export default MarketLogin


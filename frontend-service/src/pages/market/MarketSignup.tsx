import { useState } from 'react'
import { Form, Input, Button, message } from 'antd'
import { UserOutlined, LockOutlined, MailOutlined, PhoneOutlined } from '@ant-design/icons'
import { useNavigate, Link } from 'react-router-dom'
import MarketHeader from '../../components/market/MarketHeader'
import MarketFooter from '../../components/market/MarketFooter'
import './MarketSignup.css'

function MarketSignup() {
  const navigate = useNavigate()
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)

  const onFinish = async (values: any) => {
    setLoading(true)
    try {
      // TODO: API 호출로 회원가입 처리
      console.log('Signup values:', values)
      
      // 비밀번호 확인
      if (values.password !== values.passwordConfirm) {
        message.error('비밀번호가 일치하지 않습니다.')
        setLoading(false)
        return
      }
      
      // 임시 처리
      await new Promise(resolve => setTimeout(resolve, 1000))
      
      message.success('회원가입이 완료되었습니다. 로그인해주세요.')
      navigate('/market/login')
    } catch (error) {
      message.error('회원가입에 실패했습니다. 다시 시도해주세요.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="market-signup">
      <MarketHeader />
      
      <div className="signup-container">
        <div className="signup-box">
          <div className="signup-header">
            <h1>회원가입</h1>
            <p>박신사몰에 가입하고 다양한 혜택을 받아보세요</p>
          </div>

          <Form
            form={form}
            name="signup"
            onFinish={onFinish}
            autoComplete="off"
            size="large"
            className="signup-form"
            layout="vertical"
          >
            <Form.Item
              label="이메일"
              name="email"
              rules={[
                { required: true, message: '이메일을 입력해주세요.' },
                { type: 'email', message: '올바른 이메일 형식이 아닙니다.' }
              ]}
            >
              <Input
                prefix={<MailOutlined />}
                placeholder="이메일을 입력해주세요"
                className="signup-input"
              />
            </Form.Item>

            <Form.Item
              label="비밀번호"
              name="password"
              rules={[
                { required: true, message: '비밀번호를 입력해주세요.' },
                { min: 8, message: '비밀번호는 최소 8자 이상이어야 합니다.' },
                { 
                  pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/,
                  message: '비밀번호는 영문 대소문자와 숫자를 포함해야 합니다.'
                }
              ]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder="비밀번호를 입력해주세요"
                className="signup-input"
              />
            </Form.Item>

            <Form.Item
              label="비밀번호 확인"
              name="passwordConfirm"
              dependencies={['password']}
              rules={[
                { required: true, message: '비밀번호를 다시 입력해주세요.' },
                ({ getFieldValue }) => ({
                  validator(_, value) {
                    if (!value || getFieldValue('password') === value) {
                      return Promise.resolve()
                    }
                    return Promise.reject(new Error('비밀번호가 일치하지 않습니다.'))
                  },
                }),
              ]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder="비밀번호를 다시 입력해주세요"
                className="signup-input"
              />
            </Form.Item>

            <Form.Item
              label="이름"
              name="name"
              rules={[
                { required: true, message: '이름을 입력해주세요.' },
                { min: 2, message: '이름은 최소 2자 이상이어야 합니다.' }
              ]}
            >
              <Input
                prefix={<UserOutlined />}
                placeholder="이름을 입력해주세요"
                className="signup-input"
              />
            </Form.Item>

            <Form.Item
              label="연락처"
              name="phone"
              rules={[
                {
                  pattern: /^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$/,
                  message: '올바른 연락처 형식이 아닙니다. (예: 010-1234-5678)'
                }
              ]}
            >
              <Input
                prefix={<PhoneOutlined />}
                placeholder="연락처를 입력해주세요 (선택사항)"
                className="signup-input"
              />
            </Form.Item>

            <Form.Item>
              <Button
                type="primary"
                htmlType="submit"
                className="signup-button"
                loading={loading}
                block
              >
                회원가입
              </Button>
            </Form.Item>
          </Form>

          <div className="signup-footer">
            <p className="login-text">
              이미 회원이신가요?{' '}
              <Link to="/market/login" className="login-link">
                로그인
              </Link>
            </p>
          </div>
        </div>
      </div>

      <MarketFooter />
    </div>
  )
}

export default MarketSignup


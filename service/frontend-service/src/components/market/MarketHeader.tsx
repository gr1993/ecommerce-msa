import { Button } from 'antd'
import { ShoppingCartOutlined, UserOutlined, CustomerServiceOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { isLoggedIn } from '../../utils/authUtils'
import './MarketHeader.css'

function MarketHeader() {
  const navigate = useNavigate()
  const loggedIn = isLoggedIn()

  return (
    <header className="market-header">
      <div className="header-container">
        <div className="logo" onClick={() => navigate('/market')}>
          <h1>박신사</h1>
        </div>
        <div className="header-actions">
          <Button 
            type="text" 
            icon={<ShoppingCartOutlined />}
            onClick={() => navigate('/market/cart')}
          >
            장바구니
          </Button>
          <Button 
            type="text" 
            icon={<CustomerServiceOutlined />}
            onClick={() => navigate('/market/support/notices')}
          >
            고객센터
          </Button>
          {loggedIn ? (
            <Button 
              type="primary" 
              icon={<UserOutlined />}
              onClick={() => navigate('/market/mypage')}
            >
              마이페이지
            </Button>
          ) : (
            <Button type="primary" onClick={() => navigate('/market/login')}>
              로그인
            </Button>
          )}
        </div>
      </div>
    </header>
  )
}

export default MarketHeader


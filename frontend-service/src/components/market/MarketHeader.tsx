import { Button } from 'antd'
import { ShoppingCartOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import './MarketHeader.css'

function MarketHeader() {
  const navigate = useNavigate()

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
          <Button type="primary" onClick={() => navigate('/market/login')}>로그인</Button>
        </div>
      </div>
    </header>
  )
}

export default MarketHeader


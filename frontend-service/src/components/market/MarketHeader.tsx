import { Button } from 'antd'
import { ShoppingCartOutlined, HeartOutlined } from '@ant-design/icons'
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
        <nav className="main-nav">
          <a href="#main">메인</a>
          <a href="#recommend">추천</a>
          <a href="#event">이벤트</a>
          <a href="#categories">카테고리</a>
        </nav>
        <div className="header-actions">
          <Button type="text" icon={<HeartOutlined />}>
            찜
          </Button>
          <Button type="text" icon={<ShoppingCartOutlined />}>
            장바구니
          </Button>
          <Button type="primary">로그인</Button>
        </div>
      </div>
    </header>
  )
}

export default MarketHeader


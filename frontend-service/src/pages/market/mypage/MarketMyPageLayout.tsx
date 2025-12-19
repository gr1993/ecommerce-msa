import { useState } from 'react'
import { Outlet, Link, useLocation, useNavigate } from 'react-router-dom'
import { Layout, Menu } from 'antd'
import {
  ShoppingOutlined,
  TruckOutlined,
  UndoOutlined,
  GiftOutlined,
  DollarOutlined,
  UserOutlined,
  LogoutOutlined
} from '@ant-design/icons'
import MarketHeader from '../../../components/market/MarketHeader'
import MarketFooter from '../../../components/market/MarketFooter'
import { logout } from '../../../utils/authUtils'
import { message } from 'antd'
import './MarketMyPageLayout.css'

const { Sider, Content } = Layout

function MarketMyPageLayout() {
  const location = useLocation()
  const navigate = useNavigate()
  const [collapsed, setCollapsed] = useState(false)

  const handleLogout = () => {
    logout()
    message.success('로그아웃되었습니다.')
    navigate('/market')
  }

  const menuItems = [
    {
      key: '/market/mypage/orders',
      icon: <ShoppingOutlined />,
      label: <Link to="/market/mypage/orders">주문 내역</Link>
    },
    {
      key: '/market/mypage/shipping',
      icon: <TruckOutlined />,
      label: <Link to="/market/mypage/shipping">배송 조회</Link>
    },
    {
      key: '/market/mypage/returns',
      icon: <UndoOutlined />,
      label: <Link to="/market/mypage/returns">반품/환불</Link>
    },
    {
      key: '/market/mypage/coupons',
      icon: <GiftOutlined />,
      label: <Link to="/market/mypage/coupons">쿠폰</Link>
    },
    {
      key: '/market/mypage/points',
      icon: <DollarOutlined />,
      label: <Link to="/market/mypage/points">포인트</Link>
    },
    {
      key: '/market/mypage/profile',
      icon: <UserOutlined />,
      label: <Link to="/market/mypage/profile">회원 정보 수정</Link>
    }
  ]

  return (
    <div className="market-mypage-layout">
      <MarketHeader />
      
      <Layout className="mypage-layout-container">
        <Sider
          collapsible
          collapsed={collapsed}
          onCollapse={setCollapsed}
          width={250}
          className="mypage-sider"
        >
          <div className="mypage-sidebar-header">
            <h2>마이페이지</h2>
          </div>
          <Menu
            mode="inline"
            selectedKeys={[location.pathname]}
            items={menuItems}
            className="mypage-menu"
          />
          <div className="mypage-sidebar-footer">
            <Menu
              mode="inline"
              items={[
                {
                  key: 'logout',
                  icon: <LogoutOutlined />,
                  label: '로그아웃',
                  onClick: handleLogout
                }
              ]}
              className="mypage-menu"
            />
          </div>
        </Sider>
        
        <Layout className="mypage-content-layout">
          <Content className="mypage-content">
            <Outlet />
          </Content>
        </Layout>
      </Layout>

      <MarketFooter />
    </div>
  )
}

export default MarketMyPageLayout


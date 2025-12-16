import { useState } from 'react'
import { Outlet, Link, useLocation } from 'react-router-dom'
import './AdminLayout.css'

function AdminLayout() {
  const location = useLocation()
  const [openMenus, setOpenMenus] = useState<string[]>([])

  const toggleMenu = (menuKey: string) => {
    setOpenMenus(prev => 
      prev.includes(menuKey) 
        ? prev.filter(key => key !== menuKey)
        : [...prev, menuKey]
    )
  }

  const isMenuOpen = (menuKey: string) => openMenus.includes(menuKey)
  const isActive = (path: string) => location.pathname === path
  const isActiveParent = (paths: string[]) => paths.some(path => location.pathname.startsWith(path))

  return (
    <div className="admin-layout">
      <header className="admin-header">
        <Link to="/admin" className="admin-logo">
          박신사 관리자
        </Link>
      </header>
      <div className="admin-content-wrapper">
        <aside className="admin-sidebar">
          <nav className="admin-nav">
            {/* 대시보드 */}
            <div className="nav-menu-item">
              <Link 
                to="/admin/dashboard" 
                className={`nav-link ${isActive('/admin/dashboard') ? 'active' : ''}`}
              >
                대시보드
              </Link>
            </div>

            {/* 상품 관리 */}
            <div className="nav-menu-item">
              <div 
                className={`nav-parent ${isActiveParent(['/admin/product']) ? 'active-parent' : ''}`}
                onClick={() => toggleMenu('product')}
              >
                <span>상품 관리</span>
                <span className={`nav-arrow ${isMenuOpen('product') ? 'open' : ''}`}>▼</span>
              </div>
              {isMenuOpen('product') && (
                <div className="nav-submenu">
                  <Link 
                    to="/admin/product/list" 
                    className={`nav-link submenu-link ${isActive('/admin/product/list') || isActive('/admin/product/register') ? 'active' : ''}`}
                  >
                    상품 관리
                  </Link>
                </div>
              )}
            </div>

            {/* 카탈로그 관리 */}
            <div className="nav-menu-item">
              <div 
                className={`nav-parent ${isActiveParent(['/admin/catalog']) ? 'active-parent' : ''}`}
                onClick={() => toggleMenu('catalog')}
              >
                <span>카탈로그 관리</span>
                <span className={`nav-arrow ${isMenuOpen('catalog') ? 'open' : ''}`}>▼</span>
              </div>
              {isMenuOpen('catalog') && (
                <div className="nav-submenu">
                  <Link 
                    to="/admin/catalog/category" 
                    className={`nav-link submenu-link ${isActive('/admin/catalog/category') ? 'active' : ''}`}
                  >
                    카테고리 관리
                  </Link>
                  <Link 
                    to="/admin/catalog/display" 
                    className={`nav-link submenu-link ${isActive('/admin/catalog/display') ? 'active' : ''}`}
                  >
                    전시 상품 관리
                  </Link>
                  <Link 
                    to="/admin/catalog/search" 
                    className={`nav-link submenu-link ${isActive('/admin/catalog/search') ? 'active' : ''}`}
                  >
                    검색 키워드 관리
                  </Link>
                </div>
              )}
            </div>

            {/* 주문 관리 */}
            <div className="nav-menu-item">
              <div 
                className={`nav-parent ${isActiveParent(['/admin/order']) ? 'active-parent' : ''}`}
                onClick={() => toggleMenu('order')}
              >
                <span>주문 관리</span>
                <span className={`nav-arrow ${isMenuOpen('order') ? 'open' : ''}`}>▼</span>
              </div>
              {isMenuOpen('order') && (
                <div className="nav-submenu">
                  <Link 
                    to="/admin/order/list" 
                    className={`nav-link submenu-link ${isActive('/admin/order/list') ? 'active' : ''}`}
                  >
                    주문 관리
                  </Link>
                  <Link 
                    to="/admin/order/payment" 
                    className={`nav-link submenu-link ${isActive('/admin/order/payment') ? 'active' : ''}`}
                  >
                    결제 관리
                  </Link>
                </div>
              )}
            </div>

            {/* 배송 관리 */}
            <div className="nav-menu-item">
              <div 
                className={`nav-parent ${isActiveParent(['/admin/shipping']) ? 'active-parent' : ''}`}
                onClick={() => toggleMenu('shipping')}
              >
                <span>배송 관리</span>
                <span className={`nav-arrow ${isMenuOpen('shipping') ? 'open' : ''}`}>▼</span>
              </div>
              {isMenuOpen('shipping') && (
                <div className="nav-submenu">
                  <Link 
                    to="/admin/shipping/list" 
                    className={`nav-link submenu-link ${isActive('/admin/shipping/list') ? 'active' : ''}`}
                  >
                    배송 관리
                  </Link>
                  <Link 
                    to="/admin/shipping/return" 
                    className={`nav-link submenu-link ${isActive('/admin/shipping/return') ? 'active' : ''}`}
                  >
                    반품 관리
                  </Link>
                  <Link 
                    to="/admin/shipping/exchange" 
                    className={`nav-link submenu-link ${isActive('/admin/shipping/exchange') ? 'active' : ''}`}
                  >
                    교환 관리
                  </Link>
                  <Link 
                    to="/admin/shipping/refund" 
                    className={`nav-link submenu-link ${isActive('/admin/shipping/refund') ? 'active' : ''}`}
                  >
                    환불 관리
                  </Link>
                </div>
              )}
            </div>
          </nav>
        </aside>
        <main className="admin-main">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

export default AdminLayout


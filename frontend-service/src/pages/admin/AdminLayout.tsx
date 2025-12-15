import { Outlet, Link, useLocation } from 'react-router-dom'
import './AdminLayout.css'

function AdminLayout() {
  const location = useLocation()

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
            <Link 
              to="/admin/dashboard" 
              className={location.pathname === '/admin/dashboard' ? 'active' : ''}
            >
              대시보드
            </Link>
            <Link 
              to="/admin/product/list" 
              className={location.pathname.startsWith('/admin/product') ? 'active' : ''}
            >
              상품 관리
            </Link>
            <Link 
              to="/admin/settings" 
              className={location.pathname === '/admin/settings' ? 'active' : ''}
            >
              설정
            </Link>
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


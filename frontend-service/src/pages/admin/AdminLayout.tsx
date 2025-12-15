import { Outlet, Link } from 'react-router-dom'

function AdminLayout() {
  return (
    <div>
      <header>
        <h1>관리자 페이지</h1>
      </header>
      <nav>
        <Link to="/admin/dashboard">대시보드</Link> | <Link to="/admin/settings">설정</Link>
      </nav>
      <main>
        <Outlet />
      </main>
    </div>
  )
}

export default AdminLayout


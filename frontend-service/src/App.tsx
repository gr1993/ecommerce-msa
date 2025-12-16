import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Main from './pages/Main'
import AdminLayout from './pages/admin/AdminLayout'
import AdminDashboard from './pages/admin/AdminDashboard'
import AdminSettings from './pages/admin/AdminSettings'
import AdminProductList from './pages/admin/product/AdminProductList'
import AdminProductRegister from './pages/admin/product/AdminProductRegister'
import AdminProductEdit from './pages/admin/product/AdminProductEdit'
import AdminCategoryManage from './pages/admin/catalog/AdminCategoryManage'
import AdminDisplayProductManage from './pages/admin/catalog/AdminDisplayProductManage'
import AdminSearchKeywordManage from './pages/admin/catalog/AdminSearchKeywordManage'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Main />} />
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<Navigate to="/admin/dashboard" replace />} />
          <Route path="dashboard" element={<AdminDashboard />} />
          <Route path="product/list" element={<AdminProductList />} />
          <Route path="product/register" element={<AdminProductRegister />} />
          <Route path="product/edit/:id" element={<AdminProductEdit />} />
          <Route path="catalog/category" element={<AdminCategoryManage />} />
          <Route path="catalog/display" element={<AdminDisplayProductManage />} />
          <Route path="catalog/search" element={<AdminSearchKeywordManage />} />
          <Route path="settings" element={<AdminSettings />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App

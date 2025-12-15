import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Main from './pages/Main'
import AdminLayout from './pages/admin/AdminLayout'
import AdminDashboard from './pages/admin/AdminDashboard'
import AdminSettings from './pages/admin/AdminSettings'
import AdminProductRegister from './pages/admin/AdminProductRegister'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Main />} />
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<Navigate to="/admin/dashboard" replace />} />
          <Route path="dashboard" element={<AdminDashboard />} />
          <Route path="product/register" element={<AdminProductRegister />} />
          <Route path="settings" element={<AdminSettings />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App

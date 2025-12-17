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
import AdminOrderList from './pages/admin/order/AdminOrderList'
import AdminPaymentManage from './pages/admin/order/AdminPaymentManage'
import AdminShippingList from './pages/admin/shipping/AdminShippingList'
import AdminReturnManage from './pages/admin/shipping/AdminReturnManage'
import AdminExchangeManage from './pages/admin/shipping/AdminExchangeManage'
import AdminCouponManage from './pages/admin/operation/AdminCouponManage'
import AdminDiscountPolicyManage from './pages/admin/operation/AdminDiscountPolicyManage'
import AdminNoticeManage from './pages/admin/operation/AdminNoticeManage'
import AdminUserManage from './pages/admin/user/AdminUserManage'
import AdminSettlementManage from './pages/admin/settlement/AdminSettlementManage'
import AdminRevenueStatistics from './pages/admin/settlement/AdminRevenueStatistics'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Main />} />
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<Navigate to="/admin/dashboard" replace />} />
          <Route path="dashboard" element={<AdminDashboard />} />
          <Route path="user/manage" element={<AdminUserManage />} />
          <Route path="product/list" element={<AdminProductList />} />
          <Route path="product/register" element={<AdminProductRegister />} />
          <Route path="product/edit/:id" element={<AdminProductEdit />} />
          <Route path="catalog/category" element={<AdminCategoryManage />} />
          <Route path="catalog/display" element={<AdminDisplayProductManage />} />
          <Route path="catalog/search" element={<AdminSearchKeywordManage />} />
          <Route path="order/list" element={<AdminOrderList />} />
          <Route path="order/payment" element={<AdminPaymentManage />} />
          <Route path="shipping/list" element={<AdminShippingList />} />
          <Route path="shipping/return" element={<AdminReturnManage />} />
          <Route path="shipping/exchange" element={<AdminExchangeManage />} />
          <Route path="operation/coupon" element={<AdminCouponManage />} />
          <Route path="operation/discount" element={<AdminDiscountPolicyManage />} />
          <Route path="operation/notice" element={<AdminNoticeManage />} />
          <Route path="settlement/manage" element={<AdminSettlementManage />} />
          <Route path="settlement/statistics" element={<AdminRevenueStatistics />} />
          <Route path="settings" element={<AdminSettings />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App

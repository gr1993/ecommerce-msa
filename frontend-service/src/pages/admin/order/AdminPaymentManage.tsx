import { useState } from 'react'
import { Card } from 'antd'
import './AdminPaymentManage.css'

function AdminPaymentManage() {
  return (
    <div className="admin-payment-manage">
      <div className="payment-manage-container">
        <h2>결제 관리</h2>
        <Card>
          <p>결제 관리 페이지입니다.</p>
        </Card>
      </div>
    </div>
  )
}

export default AdminPaymentManage


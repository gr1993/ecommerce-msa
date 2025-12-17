import { Card } from 'antd'
import './AdminNoticeManage.css'

function AdminNoticeManage() {
  return (
    <div className="admin-notice-manage">
      <div className="notice-manage-container">
        <h2>공지사항</h2>
        <Card>
          <p>공지사항 관리 페이지입니다.</p>
        </Card>
      </div>
    </div>
  )
}

export default AdminNoticeManage


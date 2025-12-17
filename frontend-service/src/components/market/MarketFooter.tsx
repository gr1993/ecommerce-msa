import './MarketFooter.css'

function MarketFooter() {
  return (
    <footer className="market-footer">
      <div className="footer-container">
        <div className="footer-section">
          <h3>고객센터</h3>
          <p>1588-0000</p>
          <p>평일 09:00 ~ 18:00</p>
        </div>
        <div className="footer-section">
          <h3>회사정보</h3>
          <p>상호: 박신사</p>
          <p>대표: 박신사</p>
          <p>사업자등록번호: 000-00-00000</p>
        </div>
        <div className="footer-section">
          <h3>이용안내</h3>
          <p>배송안내</p>
          <p>교환/반품 안내</p>
          <p>이용약관</p>
        </div>
      </div>
      <div className="footer-bottom">
        <p>© 2024 박신사. All rights reserved.</p>
      </div>
    </footer>
  )
}

export default MarketFooter


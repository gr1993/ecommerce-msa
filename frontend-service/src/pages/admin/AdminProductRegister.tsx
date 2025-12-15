import { useState } from 'react'
import './AdminProductRegister.css'

function AdminProductRegister() {
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    price: '',
    stock: '',
    category: '',
    image: null as File | null
  })

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = e.target
    setFormData(prev => ({
      ...prev,
      [name]: value
    }))
  }

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setFormData(prev => ({
        ...prev,
        image: e.target.files![0]
      }))
    }
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    console.log('상품 등록:', formData)
    // TODO: API 호출로 상품 등록 처리
  }

  return (
    <div className="admin-product-register">
      <div className="admin-product-register-container">
        <h2>상품 등록</h2>
        <form onSubmit={handleSubmit} className="product-form">
          <div className="form-group">
            <label htmlFor="name">상품명 *</label>
            <input
              type="text"
              id="name"
              name="name"
              value={formData.name}
              onChange={handleChange}
              required
              placeholder="상품명을 입력하세요"
            />
          </div>

          <div className="form-group">
            <label htmlFor="description">상품 설명 *</label>
            <textarea
              id="description"
              name="description"
              value={formData.description}
              onChange={handleChange}
              required
              rows={5}
              placeholder="상품에 대한 상세 설명을 입력하세요"
            />
          </div>

          <div className="form-row">
            <div className="form-group">
              <label htmlFor="price">가격 *</label>
              <input
                type="number"
                id="price"
                name="price"
                value={formData.price}
                onChange={handleChange}
                required
                min="0"
                step="0.01"
                placeholder="0"
              />
            </div>

            <div className="form-group">
              <label htmlFor="stock">재고 수량 *</label>
              <input
                type="number"
                id="stock"
                name="stock"
                value={formData.stock}
                onChange={handleChange}
                required
                min="0"
                placeholder="0"
              />
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="category">카테고리 *</label>
            <select
              id="category"
              name="category"
              value={formData.category}
              onChange={handleChange}
              required
            >
              <option value="">카테고리를 선택하세요</option>
              <option value="electronics">전자제품</option>
              <option value="clothing">의류</option>
              <option value="food">식품</option>
              <option value="books">도서</option>
              <option value="sports">스포츠</option>
              <option value="beauty">뷰티</option>
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="image">상품 이미지</label>
            <input
              type="file"
              id="image"
              name="image"
              accept="image/*"
              onChange={handleImageChange}
            />
            {formData.image && (
              <div className="image-preview">
                <p>선택된 파일: {formData.image.name}</p>
              </div>
            )}
          </div>

          <div className="form-actions">
            <button type="button" className="btn-cancel">
              취소
            </button>
            <button type="submit" className="btn-submit">
              등록하기
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default AdminProductRegister


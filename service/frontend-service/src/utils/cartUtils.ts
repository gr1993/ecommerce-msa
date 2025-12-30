import { message } from 'antd'

export interface CartItem {
  product_id: string
  product_name: string
  product_code: string
  base_price: number
  image_url?: string
  quantity: number
  stock: number
}

// 장바구니에 아이템 추가
export const addToCart = (item: Omit<CartItem, 'quantity'>, quantity: number = 1) => {
  try {
    const cartData = localStorage.getItem('cart')
    let cartItems: CartItem[] = cartData ? JSON.parse(cartData) : []

    // 이미 장바구니에 있는 상품인지 확인
    const existingItemIndex = cartItems.findIndex(
      (cartItem) => cartItem.product_id === item.product_id
    )

    if (existingItemIndex >= 0) {
      // 이미 있는 상품이면 수량만 증가
      const newQuantity = cartItems[existingItemIndex].quantity + quantity
      if (newQuantity > item.stock) {
        message.warning(`재고가 부족합니다. (최대 ${item.stock}개)`)
        return false
      }
      cartItems[existingItemIndex].quantity = newQuantity
    } else {
      // 새로운 상품이면 추가
      cartItems.push({
        ...item,
        quantity: Math.min(quantity, item.stock)
      })
    }

    localStorage.setItem('cart', JSON.stringify(cartItems))
    message.success('장바구니에 추가되었습니다.')
    return true
  } catch (error) {
    console.error('장바구니 추가 실패:', error)
    message.error('장바구니에 추가하는데 실패했습니다.')
    return false
  }
}

// 장바구니 아이템 가져오기
export const getCartItems = (): CartItem[] => {
  try {
    const cartData = localStorage.getItem('cart')
    return cartData ? JSON.parse(cartData) : []
  } catch (error) {
    console.error('장바구니 데이터 로드 실패:', error)
    return []
  }
}

// 장바구니 아이템 개수 가져오기
export const getCartItemCount = (): number => {
  const items = getCartItems()
  return items.reduce((sum, item) => sum + item.quantity, 0)
}


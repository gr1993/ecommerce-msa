/**
 * Shopping Cart Store (Zustand)
 *
 * 장바구니 상태를 관리하는 전역 상태 저장소
 * localStorage에 자동으로 지속성 저장
 */

import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { message } from 'antd'

// 장바구니 아이템 인터페이스
export interface CartItem {
  product_id: string
  product_name: string
  product_code: string
  base_price: number
  image_url?: string
  quantity: number
  stock: number
}

// Cart Store 상태 인터페이스
interface CartState {
  items: CartItem[]

  // 액션
  addToCart: (item: Omit<CartItem, 'quantity'>, quantity?: number) => boolean
  removeFromCart: (productId: string) => void
  updateQuantity: (productId: string, quantity: number) => void
  clearCart: () => void
  getCartItemCount: () => number
  getTotalPrice: () => number
}

/**
 * Shopping Cart Store
 *
 * 사용 예시:
 * ```tsx
 * const { items, addToCart, removeFromCart, getCartItemCount } = useCartStore()
 * ```
 */
export const useCartStore = create<CartState>()(
  persist(
    (set, get) => ({
      // 초기 상태
      items: [],

      // 장바구니에 아이템 추가
      addToCart: (item, quantity = 1) => {
        try {
          const currentItems = get().items
          const existingItemIndex = currentItems.findIndex(
            (cartItem) => cartItem.product_id === item.product_id
          )

          if (existingItemIndex >= 0) {
            // 이미 있는 상품이면 수량만 증가
            const existingItem = currentItems[existingItemIndex]
            const newQuantity = existingItem.quantity + quantity

            if (newQuantity > item.stock) {
              message.warning(`재고가 부족합니다. (최대 ${item.stock}개)`)
              return false
            }

            const updatedItems = [...currentItems]
            updatedItems[existingItemIndex] = {
              ...existingItem,
              quantity: newQuantity,
            }
            set({ items: updatedItems })
          } else {
            // 새로운 상품이면 추가
            if (quantity > item.stock) {
              message.warning(`재고가 부족합니다. (최대 ${item.stock}개)`)
              return false
            }

            set({
              items: [
                ...currentItems,
                {
                  ...item,
                  quantity: Math.min(quantity, item.stock),
                },
              ],
            })
          }

          message.success('장바구니에 추가되었습니다.')
          return true
        } catch (error) {
          console.error('장바구니 추가 실패:', error)
          message.error('장바구니에 추가하는데 실패했습니다.')
          return false
        }
      },

      // 장바구니에서 아이템 제거
      removeFromCart: (productId) => {
        set({
          items: get().items.filter((item) => item.product_id !== productId),
        })
        message.success('장바구니에서 제거되었습니다.')
      },

      // 수량 업데이트
      updateQuantity: (productId, quantity) => {
        const currentItems = get().items
        const itemIndex = currentItems.findIndex(
          (item) => item.product_id === productId
        )

        if (itemIndex >= 0) {
          const item = currentItems[itemIndex]

          if (quantity <= 0) {
            get().removeFromCart(productId)
            return
          }

          if (quantity > item.stock) {
            message.warning(`재고가 부족합니다. (최대 ${item.stock}개)`)
            return
          }

          const updatedItems = [...currentItems]
          updatedItems[itemIndex] = { ...item, quantity }
          set({ items: updatedItems })
        }
      },

      // 장바구니 비우기
      clearCart: () => {
        set({ items: [] })
      },

      // 장바구니 아이템 개수 가져오기
      getCartItemCount: () => {
        return get().items.reduce((sum, item) => sum + item.quantity, 0)
      },

      // 총 가격 계산
      getTotalPrice: () => {
        return get().items.reduce(
          (sum, item) => sum + item.base_price * item.quantity,
          0
        )
      },
    }),
    {
      name: 'cart-storage', // localStorage key
    }
  )
)

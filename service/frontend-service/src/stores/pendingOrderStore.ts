/**
 * Pending Order Store (Zustand)
 *
 * 결제 대기 중인 주문 정보를 관리하는 전역 상태 저장소
 * 토스페이먼츠 리다이렉트 후에도 주문 정보를 복원하기 위해
 * localStorage에 자동으로 지속성 저장
 */

import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export interface PendingOrderInfo {
  orderId: number
  orderNumber: string
  cartItemIds: Array<{ productId: string; skuId: string }>
  totalAmount: number
  fromCart: boolean
}

interface PendingOrderState {
  pendingOrder: PendingOrderInfo | null

  // 액션
  setPendingOrder: (order: PendingOrderInfo) => void
  clearPendingOrder: () => void
}

export const usePendingOrderStore = create<PendingOrderState>()(
  persist(
    (set) => ({
      pendingOrder: null,

      setPendingOrder: (order) => {
        set({ pendingOrder: order })
      },

      clearPendingOrder: () => {
        set({ pendingOrder: null })
      },
    }),
    {
      name: 'pending-order-storage',
    }
  )
)

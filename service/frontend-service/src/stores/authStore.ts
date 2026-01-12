/**
 * Authentication Store (Zustand)
 *
 * 사용자 및 관리자 인증 상태를 관리하는 전역 상태 저장소
 * localStorage에 자동으로 지속성 저장
 */

import { create } from 'zustand'
import { persist } from 'zustand/middleware'

// 사용자 인터페이스
export interface User {
  userId: string
  email: string
  name?: string
  phone?: string
}

// 관리자 인터페이스
export interface AdminUser {
  adminId?: string
  email: string
  name?: string
  role?: string
}

// Auth Store 상태 인터페이스
interface AuthState {
  // 일반 사용자 상태
  userToken: string | null
  user: User | null
  refreshToken: string | null

  // 관리자 상태
  adminToken: string | null
  adminUser: AdminUser | null

  // 일반 사용자 액션
  login: (user: User, token: string, refreshToken?: string) => void
  logout: () => void
  isLoggedIn: () => boolean

  // 관리자 액션
  adminLogin: (adminUser: AdminUser, token: string) => void
  adminLogout: () => void
  isAdminLoggedIn: () => boolean
}

/**
 * Authentication Store
 *
 * 사용 예시:
 * ```tsx
 * const { user, login, logout, isLoggedIn } = useAuthStore()
 * const { adminUser, adminLogin, adminLogout, isAdminLoggedIn } = useAuthStore()
 * ```
 */
export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      // 초기 상태
      userToken: null,
      user: null,
      refreshToken: null,
      adminToken: null,
      adminUser: null,

      // 일반 사용자 액션
      login: (user, token, refreshToken) => {
        set({ user, userToken: token, refreshToken: refreshToken || null })
      },

      logout: () => {
        set({ user: null, userToken: null, refreshToken: null })
      },

      isLoggedIn: () => {
        return !!get().userToken
      },

      // 관리자 액션
      adminLogin: (adminUser, token) => {
        set({ adminUser, adminToken: token })
      },

      adminLogout: () => {
        set({ adminUser: null, adminToken: null })
      },

      isAdminLoggedIn: () => {
        return !!get().adminToken
      },
    }),
    {
      name: 'auth-storage', // localStorage key
      partialize: (state) => ({
        // localStorage에 저장할 필드만 선택
        userToken: state.userToken,
        user: state.user,
        refreshToken: state.refreshToken,
        adminToken: state.adminToken,
        adminUser: state.adminUser,
      }),
    }
  )
)

/**
 * 인증 유틸리티 함수
 */

export interface User {
  userId: string
  email: string
  name?: string
  phone?: string
}

/**
 * 로그인 상태 확인
 */
export const isLoggedIn = (): boolean => {
  const token = localStorage.getItem('userToken')
  return !!token
}

/**
 * 사용자 정보 가져오기
 */
export const getUser = (): User | null => {
  try {
    const userData = localStorage.getItem('user')
    return userData ? JSON.parse(userData) : null
  } catch (error) {
    console.error('Failed to get user:', error)
    return null
  }
}

/**
 * 로그인 처리
 */
export const login = (user: User, token: string): void => {
  localStorage.setItem('userToken', token)
  localStorage.setItem('user', JSON.stringify(user))
}

/**
 * 로그아웃 처리
 */
export const logout = (): void => {
  localStorage.removeItem('userToken')
  localStorage.removeItem('user')
}

/**
 * 관리자 인터페이스
 */
export interface AdminUser {
  adminId?: string
  email: string
  name?: string
  role?: string
}

/**
 * 관리자 로그인 상태 확인
 */
export const isAdminLoggedIn = (): boolean => {
  const token = localStorage.getItem('adminToken')
  return !!token
}

/**
 * 관리자 정보 가져오기
 */
export const getAdminUser = (): AdminUser | null => {
  try {
    const adminData = localStorage.getItem('adminUser')
    return adminData ? JSON.parse(adminData) : null
  } catch (error) {
    console.error('Failed to get admin user:', error)
    return null
  }
}

/**
 * 관리자 로그인 처리
 */
export const adminLogin = (adminUser: AdminUser, token: string): void => {
  localStorage.setItem('adminToken', token)
  localStorage.setItem('adminUser', JSON.stringify(adminUser))
}

/**
 * 관리자 로그아웃 처리
 */
export const adminLogout = (): void => {
  localStorage.removeItem('adminToken')
  localStorage.removeItem('adminUser')
}


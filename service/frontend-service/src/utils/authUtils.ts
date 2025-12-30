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


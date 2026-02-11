/**
 * Authenticated Fetch Utility
 *
 * 인증이 필요한 API 요청 시 자동으로 토큰 갱신을 처리하는 유틸리티
 *
 * 정책:
 * - Access Token 만료 30분 설정 (서버 측)
 * - API 요청 전 토큰 만료 시간 확인
 * - 만료되었거나 만료까지 5분 이내인 경우 자동 갱신
 * - Refresh Token Rotation 미사용 (기존 Refresh Token 유지)
 * - 토큰 갱신 실패 시 로그아웃 처리
 */

import { API_BASE_URL } from '../config/env'
import { useAuthStore } from '../stores/authStore'
import { shouldRefreshToken } from './tokenUtils'
import type { TokenResponse } from '../api/authApi'

/**
 * 토큰 갱신 진행 중인지 추적하는 Promise
 * 동시에 여러 요청이 토큰 갱신을 시도하는 것을 방지
 */
let refreshPromise: Promise<string | null> | null = null

/**
 * 토큰 갱신 에러 클래스
 */
export class TokenRefreshError extends Error {
  constructor(message: string = '토큰 갱신에 실패했습니다. 다시 로그인해주세요.') {
    super(message)
    this.name = 'TokenRefreshError'
  }
}

/**
 * 인증 필요 에러 클래스
 */
export class AuthRequiredError extends Error {
  constructor(message: string = '로그인이 필요합니다.') {
    super(message)
    this.name = 'AuthRequiredError'
  }
}

/**
 * Refresh Token을 사용하여 Access Token 갱신
 *
 * @returns 새로운 Access Token 또는 null (실패 시)
 */
const refreshAccessToken = async (): Promise<string | null> => {
  const { refreshToken: storedRefreshToken, updateTokens, logout } = useAuthStore.getState()

  if (!storedRefreshToken) {
    console.warn('Refresh token not found')
    logout()
    return null
  }

  try {
    const response = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ refreshToken: storedRefreshToken }),
    })

    if (!response.ok) {
      console.error('Token refresh failed:', response.status)
      // 갱신 실패 시 로그아웃
      logout()
      return null
    }

    const data: TokenResponse = await response.json()

    // 새 토큰 저장 (Refresh Token Rotation 미사용이므로 refreshToken은 optional)
    updateTokens(data.accessToken, data.refreshToken)

    console.log('Token refreshed successfully')
    return data.accessToken
  } catch (error) {
    console.error('Token refresh error:', error)
    logout()
    return null
  }
}

/**
 * 유효한 Access Token 획득
 *
 * 현재 토큰이 유효하면 그대로 반환하고,
 * 만료되었거나 5분 이내 만료 예정이면 갱신 후 반환
 *
 * @returns 유효한 Access Token
 * @throws TokenRefreshError - 토큰 갱신 실패 시
 * @throws AuthRequiredError - 로그인되어 있지 않은 경우
 */
export const getValidAccessToken = async (): Promise<string> => {
  const { userToken, refreshToken: storedRefreshToken } = useAuthStore.getState()

  // 로그인되어 있지 않은 경우
  if (!userToken) {
    throw new AuthRequiredError()
  }

  // 토큰이 아직 유효하고 5분 이상 남았으면 그대로 사용
  if (!shouldRefreshToken(userToken)) {
    return userToken
  }

  // Refresh Token이 없으면 갱신 불가
  if (!storedRefreshToken) {
    useAuthStore.getState().logout()
    throw new TokenRefreshError('세션이 만료되었습니다. 다시 로그인해주세요.')
  }

  // 이미 갱신 중이면 해당 Promise 재사용 (중복 갱신 방지)
  if (refreshPromise) {
    const newToken = await refreshPromise
    if (!newToken) {
      throw new TokenRefreshError()
    }
    return newToken
  }

  // 토큰 갱신 시작
  refreshPromise = refreshAccessToken()

  try {
    const newToken = await refreshPromise
    if (!newToken) {
      throw new TokenRefreshError()
    }
    return newToken
  } finally {
    refreshPromise = null
  }
}

/**
 * 인증된 Fetch 요청
 *
 * API 요청 전에 자동으로 토큰 유효성을 검사하고 필요시 갱신합니다.
 *
 * @param url - 요청 URL
 * @param options - Fetch 옵션 (headers에 Authorization 자동 추가)
 * @returns Fetch Response
 * @throws TokenRefreshError - 토큰 갱신 실패 시
 * @throws AuthRequiredError - 로그인되어 있지 않은 경우
 *
 * @example
 * ```typescript
 * const response = await authenticatedFetch('/api/orders', {
 *   method: 'POST',
 *   headers: { 'Content-Type': 'application/json' },
 *   body: JSON.stringify(orderData),
 * })
 * ```
 */
export const authenticatedFetch = async (
  url: string,
  options: RequestInit = {}
): Promise<Response> => {
  // 유효한 토큰 획득 (필요시 자동 갱신)
  const validToken = await getValidAccessToken()

  // Authorization 헤더 추가
  const headers = new Headers(options.headers)
  headers.set('Authorization', `Bearer ${validToken}`)

  // API 요청 수행
  const response = await fetch(url, {
    ...options,
    headers,
  })

  // 401 응답 시 토큰이 서버에서 무효화되었을 수 있음
  if (response.status === 401) {
    // 한 번 더 갱신 시도
    const { refreshToken: storedRefreshToken } = useAuthStore.getState()
    if (storedRefreshToken && !refreshPromise) {
      refreshPromise = refreshAccessToken()
      const newToken = await refreshPromise
      refreshPromise = null

      if (newToken) {
        // 새 토큰으로 재시도
        headers.set('Authorization', `Bearer ${newToken}`)
        return fetch(url, {
          ...options,
          headers,
        })
      }
    }

    // 갱신 실패 시 로그아웃
    useAuthStore.getState().logout()
    throw new TokenRefreshError('인증이 만료되었습니다. 다시 로그인해주세요.')
  }

  return response
}

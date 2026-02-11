/**
 * Token Utilities
 *
 * JWT 토큰 관련 유틸리티 함수
 * - 토큰 디코딩
 * - 만료 시간 확인
 * - 갱신 필요 여부 확인
 */

/**
 * JWT 페이로드 인터페이스
 */
export interface JwtPayload {
  /** Subject (사용자 ID) */
  sub?: string
  /** 사용자 ID */
  userId?: string
  /** 이메일 */
  email?: string
  /** 사용자 이름 */
  name?: string
  /** 사용자명 */
  username?: string
  /** 만료 시간 (Unix timestamp, 초 단위) */
  exp?: number
  /** 발급 시간 (Unix timestamp, 초 단위) */
  iat?: number
  /** 역할 */
  role?: string
}

/**
 * JWT 토큰 디코딩
 *
 * Base64로 인코딩된 JWT 페이로드를 디코딩하여 반환합니다.
 *
 * @param token - JWT 토큰 문자열
 * @returns 디코딩된 페이로드 또는 null (디코딩 실패 시)
 *
 * @example
 * ```typescript
 * const payload = decodeToken(accessToken)
 * if (payload) {
 *   console.log('User ID:', payload.userId)
 *   console.log('Expires at:', new Date(payload.exp * 1000))
 * }
 * ```
 */
export const decodeToken = (token: string): JwtPayload | null => {
  try {
    const payload = token.split('.')[1]
    if (!payload) {
      return null
    }
    const decoded = JSON.parse(atob(payload))
    return decoded
  } catch (error) {
    console.error('Failed to decode token:', error)
    return null
  }
}

/**
 * 토큰 만료 여부 확인
 *
 * 토큰이 이미 만료되었는지 확인합니다.
 *
 * @param token - JWT 토큰 문자열
 * @returns 만료되었으면 true, 유효하면 false
 *
 * @example
 * ```typescript
 * if (isTokenExpired(accessToken)) {
 *   // 토큰 갱신 필요
 * }
 * ```
 */
export const isTokenExpired = (token: string): boolean => {
  const payload = decodeToken(token)
  if (!payload || !payload.exp) {
    // 토큰을 디코딩할 수 없거나 만료 시간이 없으면 만료된 것으로 간주
    return true
  }

  // exp는 초 단위이므로 1000을 곱해서 밀리초로 변환
  const expirationTime = payload.exp * 1000
  const currentTime = Date.now()

  return currentTime >= expirationTime
}

/**
 * 토큰 갱신 필요 여부 확인
 *
 * 토큰이 만료되었거나 만료까지 지정된 시간 이내인 경우 true를 반환합니다.
 * 기본값은 5분(300초) 이내입니다.
 *
 * @param token - JWT 토큰 문자열
 * @param thresholdSeconds - 갱신 임계값 (초), 기본값 300초 (5분)
 * @returns 갱신이 필요하면 true
 *
 * @example
 * ```typescript
 * // 만료 5분 전부터 갱신 필요
 * if (shouldRefreshToken(accessToken)) {
 *   const newTokens = await refreshToken({ refreshToken })
 * }
 *
 * // 만료 10분 전부터 갱신 필요
 * if (shouldRefreshToken(accessToken, 600)) {
 *   const newTokens = await refreshToken({ refreshToken })
 * }
 * ```
 */
export const shouldRefreshToken = (token: string, thresholdSeconds: number = 300): boolean => {
  const payload = decodeToken(token)
  if (!payload || !payload.exp) {
    // 토큰을 디코딩할 수 없거나 만료 시간이 없으면 갱신 필요
    return true
  }

  // exp는 초 단위이므로 1000을 곱해서 밀리초로 변환
  const expirationTime = payload.exp * 1000
  const currentTime = Date.now()
  const thresholdMs = thresholdSeconds * 1000

  // 현재 시간 + 임계값이 만료 시간보다 크거나 같으면 갱신 필요
  return currentTime + thresholdMs >= expirationTime
}

/**
 * 토큰의 남은 유효 시간 계산 (초 단위)
 *
 * @param token - JWT 토큰 문자열
 * @returns 남은 시간(초), 이미 만료되었으면 0
 *
 * @example
 * ```typescript
 * const remainingTime = getTokenRemainingTime(accessToken)
 * console.log(`토큰 만료까지 ${remainingTime}초 남음`)
 * ```
 */
export const getTokenRemainingTime = (token: string): number => {
  const payload = decodeToken(token)
  if (!payload || !payload.exp) {
    return 0
  }

  const expirationTime = payload.exp * 1000
  const currentTime = Date.now()
  const remainingMs = expirationTime - currentTime

  return remainingMs > 0 ? Math.floor(remainingMs / 1000) : 0
}

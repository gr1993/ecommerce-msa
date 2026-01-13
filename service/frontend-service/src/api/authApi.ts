/**
 * Auth Service API Integration
 *
 * Type-safe API functions for auth-service endpoints
 * All requests go through API Gateway (http://localhost:8080)
 */

// ==================== Interfaces ====================

/**
 * 로그인 요청 DTO
 */
export interface LoginRequest {
  /** 이메일 주소 */
  email: string
  /** 비밀번호 */
  password: string
}

/**
 * 토큰 응답 DTO
 */
export interface TokenResponse {
  /** 액세스 토큰 */
  accessToken: string
  /** 리프레시 토큰 */
  refreshToken: string
  /** 토큰 타입 (예: Bearer) */
  tokenType: string
  /** 만료 시간 (초) */
  expiresIn: number
}

/**
 * 리프레시 토큰 요청 DTO
 */
export interface RefreshTokenRequest {
  /** 리프레시 토큰 */
  refreshToken: string
}

/**
 * 관리자 로그인 요청 DTO
 */
export interface AdminLoginRequest {
  /** 이메일 주소 */
  email: string
  /** 비밀번호 */
  password: string
}

/**
 * 관리자 토큰 응답 DTO
 */
export interface AdminTokenResponse {
  /** 액세스 토큰 */
  accessToken: string
  /** 리프레시 토큰 */
  refreshToken?: string
  /** 토큰 타입 (예: Bearer) */
  tokenType: string
  /** 만료 시간 (초) */
  expiresIn: number
  /** 관리자 ID */
  adminId?: string
  /** 이메일 주소 */
  email: string
  /** 관리자 이름 */
  name?: string
  /** 관리자 역할 */
  role?: string
}

// ==================== API Functions ====================

/**
 * 사용자 로그인
 *
 * 이메일과 비밀번호로 사용자를 인증하고 JWT 토큰을 발급받습니다.
 *
 * @param credentials - 로그인 정보 (이메일, 비밀번호)
 * @returns 액세스 토큰, 리프레시 토큰 및 만료 정보
 * @throws Error - 로그인 실패 시 (인증 실패, 계정 정지 등)
 *
 * @example
 * ```typescript
 * const result = await login({
 *   email: 'user@example.com',
 *   password: 'password123'
 * })
 * console.log('Access Token:', result.accessToken)
 * console.log('Expires In:', result.expiresIn)
 * ```
 */
export const login = async (credentials: LoginRequest): Promise<TokenResponse> => {
  try {
    const response = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(credentials),
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Unknown error' }))

      // Handle specific error cases
      if (response.status === 401) {
        throw new Error(error.message || '이메일 또는 비밀번호가 올바르지 않습니다.')
      }
      if (response.status === 403) {
        throw new Error(error.message || '계정이 정지되었거나 비활성화되었습니다.')
      }

      throw new Error(error.message || `로그인 실패 (HTTP ${response.status})`)
    }

    const data: TokenResponse = await response.json()
    return data
  } catch (error) {
    console.error('Login error:', error)

    // Re-throw if it's already our custom error
    if (error instanceof Error) {
      throw error
    }

    // Network or unexpected errors
    throw new Error('로그인 중 오류가 발생했습니다. 네트워크 연결을 확인해주세요.')
  }
}

/**
 * 토큰 갱신
 *
 * 리프레시 토큰을 사용하여 새로운 액세스 토큰과 리프레시 토큰을 발급받습니다.
 *
 * @param request - 리프레시 토큰 요청 데이터
 * @returns 새로운 액세스 토큰 및 리프레시 토큰
 * @throws Error - 토큰 갱신 실패 시 (토큰 만료, 유효하지 않은 토큰 등)
 *
 * @example
 * ```typescript
 * const result = await refreshToken({
 *   refreshToken: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'
 * })
 * console.log('New Access Token:', result.accessToken)
 * ```
 */
export const refreshToken = async (request: RefreshTokenRequest): Promise<TokenResponse> => {
  try {
    const response = await fetch('http://localhost:8080/api/auth/refresh', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Unknown error' }))

      // Handle specific error cases
      if (response.status === 401) {
        throw new Error(error.message || '리프레시 토큰이 유효하지 않거나 만료되었습니다. 다시 로그인해주세요.')
      }

      throw new Error(error.message || `토큰 갱신 실패 (HTTP ${response.status})`)
    }

    const data: TokenResponse = await response.json()
    return data
  } catch (error) {
    console.error('Refresh token error:', error)

    // Re-throw if it's already our custom error
    if (error instanceof Error) {
      throw error
    }

    // Network or unexpected errors
    throw new Error('토큰 갱신 중 오류가 발생했습니다. 네트워크 연결을 확인해주세요.')
  }
}

/**
 * 관리자 로그인
 *
 * 이메일과 비밀번호로 관리자를 인증하고 JWT 토큰과 관리자 정보를 발급받습니다.
 *
 * @param credentials - 관리자 로그인 정보 (이메일, 비밀번호)
 * @returns 액세스 토큰 및 관리자 정보
 * @throws Error - 로그인 실패 시 (인증 실패, 권한 없음 등)
 *
 * @example
 * ```typescript
 * const result = await adminLogin({
 *   email: 'admin@example.com',
 *   password: 'adminPassword123'
 * })
 * console.log('Admin Access Token:', result.accessToken)
 * console.log('Admin Name:', result.name)
 * console.log('Admin Role:', result.role)
 * ```
 */
export const adminLogin = async (credentials: AdminLoginRequest): Promise<AdminTokenResponse> => {
  try {
    const response = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(credentials),
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Unknown error' }))

      // Handle specific error cases
      if (response.status === 401) {
        throw new Error(error.message || '이메일 또는 비밀번호가 올바르지 않습니다.')
      }
      if (response.status === 403) {
        throw new Error(error.message || '관리자 권한이 없습니다.')
      }

      throw new Error(error.message || `관리자 로그인 실패 (HTTP ${response.status})`)
    }

    const data: AdminTokenResponse = await response.json()
    return data
  } catch (error) {
    console.error('Admin login error:', error)

    // Re-throw if it's already our custom error
    if (error instanceof Error) {
      throw error
    }

    // Network or unexpected errors
    throw new Error('관리자 로그인 중 오류가 발생했습니다. 네트워크 연결을 확인해주세요.')
  }
}

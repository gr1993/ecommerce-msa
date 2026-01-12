/**
 * Auth Service API
 * Authentication and token management APIs
 */

// TypeScript interfaces from Swagger schemas

export interface LoginRequest {
  email: string
  password: string
}

export interface TokenResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

export interface RefreshTokenRequest {
  refreshToken: string
}

/**
 * Login API
 * POST /api/auth/login
 *
 * Authenticates user with email and password and returns JWT tokens.
 *
 * @param credentials - Login credentials (email and password)
 * @returns TokenResponse with access token, refresh token, and expiration info
 * @throws Error if login fails (401: invalid credentials, 403: account suspended)
 */
export const login = async (credentials: LoginRequest): Promise<TokenResponse> => {
  try {
    const response = await fetch(`http://localhost:8080/api/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(credentials),
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Login failed' }))

      // Provide user-friendly error messages based on status code
      if (response.status === 401) {
        throw new Error('이메일 또는 비밀번호가 올바르지 않습니다.')
      } else if (response.status === 403) {
        throw new Error('계정이 정지되었거나 비활성화되었습니다.')
      }

      throw new Error(error.message || `로그인에 실패했습니다. (HTTP ${response.status})`)
    }

    const data: TokenResponse = await response.json()
    return data
  } catch (error) {
    console.error('Login error:', error)
    throw error
  }
}

/**
 * Refresh Token API
 * POST /api/auth/refresh
 *
 * Uses refresh token to get new access token and refresh token.
 *
 * @param request - Refresh token request
 * @returns TokenResponse with new access token and refresh token
 * @throws Error if refresh fails (401: invalid/expired refresh token)
 */
export const refreshToken = async (request: RefreshTokenRequest): Promise<TokenResponse> => {
  try {
    const response = await fetch(`http://localhost:8080/api/auth/refresh`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Token refresh failed' }))

      if (response.status === 401) {
        throw new Error('리프레시 토큰이 유효하지 않거나 만료되었습니다. 다시 로그인해주세요.')
      }

      throw new Error(error.message || `토큰 갱신에 실패했습니다. (HTTP ${response.status})`)
    }

    const data: TokenResponse = await response.json()
    return data
  } catch (error) {
    console.error('Refresh token error:', error)
    throw error
  }
}

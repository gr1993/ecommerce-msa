/**
 * User Service API Integration
 *
 * Type-safe API functions for user-service endpoints
 * All requests go through API Gateway (http://localhost:8080)
 */

// ==================== Interfaces ====================

/**
 * 회원가입 요청 DTO
 */
export interface SignUpRequest {
  /** 이메일 주소 */
  email: string
  /** 비밀번호 (최소 8자, 영문/숫자/특수문자 포함) */
  password: string
  /** 비밀번호 확인 */
  passwordConfirm: string
  /** 사용자 이름 */
  name: string
  /** 연락처 (예: 010-1234-5678) */
  phone?: string
}

/**
 * 회원가입 응답 DTO
 */
export interface SignUpResponse {
  /** 사용자 ID */
  userId: number
  /** 이메일 주소 */
  email: string
  /** 사용자 이름 */
  name: string
  /** 연락처 */
  phone?: string
  /** 생성 일시 */
  createdAt: string
}

// ==================== API Functions ====================

/**
 * 회원가입
 *
 * 새로운 사용자를 등록합니다. 이메일, 비밀번호, 이름, 연락처 정보가 필요합니다.
 *
 * @param signUpData - 회원가입 요청 데이터
 * @returns 생성된 사용자 정보
 * @throws Error - 회원가입 실패 시 (유효성 검증 실패, 이메일 중복 등)
 *
 * @example
 * ```typescript
 * const result = await signUp({
 *   email: 'user@example.com',
 *   password: 'Password123!',
 *   passwordConfirm: 'Password123!',
 *   name: '홍길동',
 *   phone: '010-1234-5678'
 * })
 * console.log('Created user ID:', result.userId)
 * ```
 */
export const signUp = async (signUpData: SignUpRequest): Promise<SignUpResponse> => {
  try {
    const response = await fetch('http://localhost:8080/api/users/signup', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(signUpData),
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Unknown error' }))

      // Handle specific error cases
      if (response.status === 400) {
        throw new Error(error.message || '입력 정보를 확인해주세요.')
      }
      if (response.status === 409) {
        throw new Error(error.message || '이미 사용 중인 이메일입니다.')
      }

      throw new Error(error.message || `회원가입 실패 (HTTP ${response.status})`)
    }

    const data: SignUpResponse = await response.json()
    return data
  } catch (error) {
    console.error('Sign up error:', error)

    // Re-throw if it's already our custom error
    if (error instanceof Error) {
      throw error
    }

    // Network or unexpected errors
    throw new Error('회원가입 중 오류가 발생했습니다. 네트워크 연결을 확인해주세요.')
  }
}

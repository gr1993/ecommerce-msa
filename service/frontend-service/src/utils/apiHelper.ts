/**
 * API Helper Utilities
 *
 * 인증 헤더 생성 및 인증 fetch 래핑을 공통으로 처리
 * AUTH_DISABLED=true (npm run local) 일 때 인증을 스킵
 */

import { AUTH_DISABLED } from '../config/env'
import { useAuthStore } from '../stores/authStore'
import { authenticatedFetch } from './authFetch'

/**
 * 관리자용 인증 헤더 생성
 *
 * AUTH_DISABLED=true 면 Content-Type만 포함
 * AUTH_DISABLED=false 면 adminToken을 Authorization 헤더에 추가
 *
 * @returns 요청 헤더 객체
 * @throws Error - AUTH_DISABLED=false이고 adminToken이 없을 때
 */
export function getAdminHeaders(): Record<string, string> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  if (!AUTH_DISABLED) {
    const adminToken = useAuthStore.getState().adminToken
    if (!adminToken) {
      throw new Error('관리자 인증이 필요합니다. 다시 로그인해주세요.')
    }
    headers['Authorization'] = `Bearer ${adminToken}`
  }
  return headers
}

/**
 * 관리자용 파일 업로드 인증 헤더 생성 (Content-Type 제외)
 *
 * FormData 전송 시 사용. Content-Type은 브라우저가 자동 설정.
 *
 * @returns 요청 헤더 객체
 * @throws Error - AUTH_DISABLED=false이고 adminToken이 없을 때
 */
export function getAdminUploadHeaders(): Record<string, string> {
  const headers: Record<string, string> = {}
  if (!AUTH_DISABLED) {
    const adminToken = useAuthStore.getState().adminToken
    if (!adminToken) {
      throw new Error('관리자 인증이 필요합니다. 다시 로그인해주세요.')
    }
    headers['Authorization'] = `Bearer ${adminToken}`
  }
  return headers
}

/**
 * 사용자(쇼핑몰)용 인증 fetch
 *
 * AUTH_DISABLED=true 면 일반 fetch로 요청 (토큰 없이)
 * AUTH_DISABLED=false 면 authenticatedFetch로 토큰 자동 갱신 처리
 *
 * @param url - 요청 URL
 * @param options - Fetch 옵션
 * @returns Fetch Response
 */
export async function userFetch(url: string, options: RequestInit = {}): Promise<Response> {
  if (AUTH_DISABLED) {
    return fetch(url, options)
  }
  return authenticatedFetch(url, options)
}

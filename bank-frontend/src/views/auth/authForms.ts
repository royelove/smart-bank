/**
 * Auth-page helpers.
 *
 * The generic rules (patterns, field validators, masking, error extraction) live
 * in `@/utils/validation` so settings and the support desk share exactly the same
 * constraints. This module only re-exports them and adds what is specific to the
 * login / register / forgot-password screens.
 */
import { maskPhone } from '@/utils/validation'
import type { User } from '@/types'

export * from '@/utils/validation'

export interface UserRegisterForm {
  userName: string
  userPhone: string
  userIdCard: string
  password: string
  checkPassword: string
  agreeTerms: boolean
}

/**
 * Minimal profile used when `/user/info` fails right after a successful login:
 * the token is still valid, so the user must not be bounced back to the form.
 */
export function fallbackUser(phone: string): User {
  return {
    id: '',
    userPhone: phone,
    userName: maskPhone(phone),
    userIdCard: '',
    faceBound: 0,
    realnameBound: 0,
    idCardMask: '',
  }
}

/**
 * Shared validation + error helpers.
 *
 * Every rule here mirrors a backend constraint (see `bank-finance`), so a form
 * can never promise something the API rejects:
 *   - new password / reset code   6 digits (`ResetPasswordDTO`, `UpdatePsdDTO` `^\d{6}$`)
 *   - userPhone                   11-digit mainland mobile number
 *   - userName / userIdCard       required by `UserRegisterDTO`
 *
 * The auth pages re-export these through `views/auth/authForms.ts`; settings and
 * the support desk import them straight from here.
 */

/** Field -> message map. `''` means "valid", so results assign straight into it. */
export type FieldErrors = Record<string, string>

export const PHONE_PATTERN = /^1[3-9]\d{9}$/
/** Payment / login password: exactly six digits. */
export const PASSWORD_PATTERN = /^\d{6}$/
export const SMS_CODE_PATTERN = /^\d{6}$/
export const ID_CARD_PATTERN = /^(\d{15}|\d{17}[\dXx])$/

export function validateUserName(value: string): string {
  const name = value.trim()
  if (!name) return '请输入姓名'
  if (name.length < 2) return '姓名至少 2 个字符'
  if (name.length > 20) return '姓名不能超过 20 个字符'
  return ''
}

export function validatePhone(value: string): string {
  const phone = value.trim()
  if (!phone) return '请输入手机号'
  if (!PHONE_PATTERN.test(phone)) return '请输入 11 位有效手机号'
  return ''
}

export function validateIdCard(value: string): string {
  const idCard = value.trim()
  if (!idCard) return '请输入身份证号'
  if (!ID_CARD_PATTERN.test(idCard)) return '请输入 15 位或 18 位有效身份证号'
  return ''
}

export function validatePassword(value: string, label = '密码'): string {
  if (!value) return `请输入${label}`
  if (!PASSWORD_PATTERN.test(value)) return `${label}为 6 位数字`
  return ''
}

export function validateConfirmPassword(password: string, confirmPassword: string): string {
  if (!confirmPassword) return '请再次输入密码'
  if (confirmPassword !== password) return '两次输入的密码不一致'
  return ''
}

export function validateSmsCode(value: string): string {
  const code = value.trim()
  if (!code) return '请输入验证码'
  if (!SMS_CODE_PATTERN.test(code)) return '验证码为 6 位数字'
  return ''
}

/** `13812345678` -> `138****5678` */
export function maskPhone(phone: string): string {
  const value = (phone ?? '').trim()
  if (PHONE_PATTERN.test(value)) return value.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
  return value
}

/** Only same-origin, absolute-path redirects are honoured. */
export function safeRedirect(raw: unknown, fallback: string): string {
  const value = typeof raw === 'string' ? raw : ''
  return value.startsWith('/') && !value.startsWith('//') ? value : fallback
}

/** `request.js` rejects with `new Error(msg)`, so `message` is the backend text. */
export function errorMessage(error: unknown, fallback: string): string {
  if (error instanceof Error && error.message) return error.message
  return fallback
}

/**
 * Read a File as a base64 string.
 *
 * `withPrefix` keeps the `data:image/...;base64,` header for local previews;
 * the face microservice decodes the payload with `base64.b64decode`, so network
 * payloads must drop it.
 */
export function readImageAsBase64(file: File, withPrefix = false): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const result = String(reader.result ?? '')
      resolve(withPrefix ? result : result.slice(result.indexOf(',') + 1))
    }
    reader.onerror = () => reject(new Error('图片读取失败，请重试'))
    reader.readAsDataURL(file)
  })
}

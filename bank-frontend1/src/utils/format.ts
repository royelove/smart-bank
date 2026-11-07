/**
 * Shared formatting helpers.
 *
 * The backend returns money as BigDecimal, which arrives as a number or a
 * numeric string depending on the serializer. Everything here tolerates both
 * and never renders `NaN` / `undefined` into the UI.
 */

export function toNumber(value: unknown): number {
  if (value === null || value === undefined || value === '') return 0
  const num = typeof value === 'number' ? value : Number(value)
  return Number.isFinite(num) ? num : 0
}

/** `12345.6` -> `12,345.60` */
export function formatAmount(value: unknown, fractionDigits = 2): string {
  return toNumber(value).toLocaleString('zh-CN', {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  })
}

/** `12345.6` -> `¥12,345.60` */
export function formatMoney(value: unknown, fractionDigits = 2): string {
  return `¥${formatAmount(value, fractionDigits)}`
}

/** Signed, for transaction rows: `+¥100.00` / `-¥100.00` */
export function formatSignedMoney(value: unknown, direction: 1 | -1 = 1, fractionDigits = 2): string {
  const sign = direction >= 0 ? '+' : '-'
  return `${sign}¥${formatAmount(Math.abs(toNumber(value)), fractionDigits)}`
}

/** `1234.5678` -> `1,234.57%` style; pass `withSign` for change columns. */
export function formatPercent(value: unknown, fractionDigits = 2, withSign = false): string {
  const num = toNumber(value)
  const sign = withSign && num > 0 ? '+' : ''
  return `${sign}${num.toFixed(fractionDigits)}%`
}

/** Mask all but the last four characters: `6225880000001234` -> `**** **** **** 1234` */
export function maskAccountNumber(accountNumber?: string | null): string {
  if (!accountNumber) return '--'
  const digits = String(accountNumber).replace(/\s/g, '')
  if (digits.length <= 4) return digits
  return `**** **** **** ${digits.slice(-4)}`
}

/**
 * `2024-05-01T10:20:30` / `2024-05-01 10:20:30` / epoch -> `2024-05-01 10:20`
 * Returns `--` for empty or unparseable input.
 */
export function formatDateTime(value?: string | number | null): string {
  if (!value) return '--'
  const date = parseDate(value)
  if (!date) return String(value)
  return `${formatDate(date)} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** `2024-05-01` */
export function formatDate(value?: string | number | Date | null): string {
  if (!value) return '--'
  const date = value instanceof Date ? value : parseDate(value)
  if (!date) return String(value)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function parseDate(value: string | number): Date | null {
  if (typeof value === 'number') {
    const date = new Date(value)
    return Number.isNaN(date.getTime()) ? null : date
  }
  // Safari/older engines reject "2024-05-01 10:20:30"; normalise it first.
  const normalised = value.includes('T') ? value : value.replace(' ', 'T')
  const date = new Date(normalised)
  return Number.isNaN(date.getTime()) ? null : date
}

function pad(value: number): string {
  return String(value).padStart(2, '0')
}

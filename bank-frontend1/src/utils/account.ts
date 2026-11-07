/**
 * Account presentation helpers.
 *
 * Verified against the backend: `AccountServiceImpl` writes new accounts with
 * `accountStatus = 1` and rejects every money movement when the status is `0`
 * (BusinessException TRANSACTION_INVALID_FREEZE), so 1 = 正常 / 0 = 冻结.
 * `accountType` is a free-form VARCHAR with no enum in the codebase, so
 * unknown values are rendered verbatim instead of being mapped to a wrong label.
 */
import type { Account } from '@/types'

export type AccountTone = 'success' | 'danger' | 'warning' | 'neutral'

const KNOWN_TYPES: Record<string, string> = {
  savings: '储蓄卡',
  储蓄卡: '储蓄卡',
  debit: '借记卡',
  借记卡: '借记卡',
  credit: '信用卡',
  信用卡: '信用卡',
  current: '活期账户',
  活期: '活期账户',
  定期: '定期账户',
  fixed: '定期账户',
}

export function accountTypeLabel(accountType?: string | null): string {
  if (!accountType) return '银行卡'
  return KNOWN_TYPES[accountType] ?? accountType
}

export function accountStatusLabel(status?: number | null): string {
  return status === 1 ? '正常' : '冻结'
}

export function accountStatusTone(status?: number | null): AccountTone {
  return status === 1 ? 'success' : 'danger'
}

export function isAccountActive(account?: Partial<Account> | null): boolean {
  return account?.accountStatus === 1
}

/** Total balance across a set of accounts; frozen accounts are still real money. */
export function sumBalance(accounts: Partial<Account>[]): number {
  return accounts.reduce((sum, account) => sum + Number(account.accountBalance ?? 0), 0)
}

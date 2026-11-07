/**
 * Trade presentation helpers.
 *
 * The backend stores `amount` as a positive BigDecimal and encodes direction
 * implicitly: a trade is money-in when the account is the destination and
 * money-out when it is the source. `tradeType` is a Chinese label straight
 * from the database, so it is used for display and as a fallback only.
 */
import type { Transaction } from '@/types'
import { toNumber } from './format'

export type TradeDirection = 'in' | 'out' | 'neutral'

export const TRADE_TYPES = [
  '存款',
  '取款',
  '转账',
  '理财购买',
  '理财赎回',
  '理财提前赎回',
] as const

const MONEY_OUT_TYPES = new Set(['取款', '理财购买'])
const MONEY_IN_TYPES = new Set(['存款', '理财赎回', '理财提前赎回'])

/**
 * @param trade the trade row
 * @param accountNumber the account the row is being displayed for
 */
export function tradeDirection(trade: Partial<Transaction>, accountNumber?: string): TradeDirection {
  const from = trade.fromAccount ?? ''
  const to = trade.toAccount ?? ''
  const account = accountNumber ?? ''

  if (account) {
    if (to === account && from !== account) return 'in'
    if (from === account && to !== account) return 'out'
  }

  const type = trade.tradeType ?? ''
  if (MONEY_IN_TYPES.has(type)) return 'in'
  if (MONEY_OUT_TYPES.has(type)) return 'out'
  return 'neutral'
}

/** Signed amount as a number, for summing and charting. */
export function signedAmount(trade: Partial<Transaction>, accountNumber?: string): number {
  const direction = tradeDirection(trade, accountNumber)
  const amount = Math.abs(toNumber(trade.amount))
  return direction === 'out' ? -amount : amount
}

/** The counterparty account for a trade row, when there is one. */
export function tradeCounterparty(trade: Partial<Transaction>, accountNumber?: string): string {
  const direction = tradeDirection(trade, accountNumber)
  if (direction === 'out') return trade.toAccount ?? ''
  if (direction === 'in') return trade.fromAccount ?? ''
  return trade.toAccount || trade.fromAccount || ''
}

export type TradeTone = 'success' | 'danger' | 'info' | 'warning' | 'neutral'

export function tradeTone(tradeType?: string): TradeTone {
  switch (tradeType) {
    case '存款':
      return 'success'
    case '取款':
      return 'danger'
    case '转账':
      return 'info'
    case '理财购买':
      return 'warning'
    case '理财赎回':
    case '理财提前赎回':
      return 'success'
    default:
      return 'neutral'
  }
}

/**
 * Wealth-management & stock presentation helpers.
 *
 * Verified against the backend:
 *  - FinancialProduct.status: 1 = 在售 (FinancialServiceImpl rejects a purchase
 *    when status != 1), anything else = 停售.
 *  - FinancialHolding.status: 0 = 持有中 (redeem rejects status != 0 and the
 *    scheduled settlement only scans status = 0), 1 = 已赎回.
 *  - StockOrder.direction is stored upper-case: BUY / SELL
 *    (DIRECTIONS = Set.of("BUY","SELL")); priceType is MARKET / LIMIT.
 *  - StockOrder.status: orders are inserted as 1 (fully filled in the same
 *    transaction: filledQuantity = orderQuantity), 0 = 待成交, 2 = 已撤销.
 *    cancelOrder only accepts status 0, so 撤单 is offered for 0 only.
 */

export type MarketTone = 'success' | 'danger' | 'info' | 'warning' | 'neutral'

export function productStatusLabel(status?: number | null): string {
  return status === 1 ? '在售' : '停售'
}

export function productStatusTone(status?: number | null): MarketTone {
  return status === 1 ? 'success' : 'neutral'
}

export function holdingStatusLabel(status?: number | null): string {
  return status === 0 ? '持有中' : '已赎回'
}

export function holdingStatusTone(status?: number | null): MarketTone {
  return status === 0 ? 'info' : 'neutral'
}

export function orderDirectionLabel(direction?: string | null): string {
  const value = (direction ?? '').toUpperCase()
  if (value === 'BUY') return '买入'
  if (value === 'SELL') return '卖出'
  return direction || '—'
}

export function orderDirectionTone(direction?: string | null): MarketTone {
  return (direction ?? '').toUpperCase() === 'SELL' ? 'danger' : 'success'
}

export function orderPriceTypeLabel(priceType?: string | null): string {
  const value = (priceType ?? '').toUpperCase()
  if (value === 'MARKET') return '市价'
  if (value === 'LIMIT') return '限价'
  return priceType || '—'
}

export function orderStatusLabel(status?: number | null): string {
  switch (status) {
    case 0:
      return '待成交'
    case 1:
      return '已成交'
    case 2:
      return '已撤销'
    default:
      return '未知'
  }
}

export function orderStatusTone(status?: number | null): MarketTone {
  switch (status) {
    case 0:
      return 'warning'
    case 1:
      return 'success'
    case 2:
      return 'neutral'
    default:
      return 'neutral'
  }
}

/**
 * Chinese market convention: rising is red, falling is green.
 */
export function changeClass(value?: number | string | null): string {
  const num = Number(value ?? 0)
  if (num > 0) return 'text-danger-600 dark:text-danger-400'
  if (num < 0) return 'text-success-600 dark:text-success-400'
  return 'text-navy-500 dark:text-navy-400'
}

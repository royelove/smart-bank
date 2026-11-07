/**
 * Types mirroring the backend VOs/DTOs exactly.
 *
 * Field names here must match the Java VO getters (Jackson serialises the
 * property names), otherwise a page silently renders `undefined`.
 */

export interface User {
  id: string
  userPhone: string
  userName: string
  userIdCard: string
  faceBound: number
  realnameBound: number
  idCardMask: string
}

export interface Staff {
  id: string
  name: string
  email: string
  role: string
  avatar?: string
}

/** AccountVO */
export interface Account {
  accountNumber: string
  accountHolderName: string
  accountType: string
  accountBalance: number
  /** 1 = 正常, 0 = 冻结 (see utils/account.ts) */
  accountStatus: number
  userPhoneNumber: string
  userIdCard: string
  userId: string
}

/** Trade entity, as returned inside the MyBatis-Plus IPage */
export interface Transaction {
  id: string
  tradeNo: string
  fromAccount: string
  toAccount: string
  /** Always positive; direction is derived from from/to (see utils/trade.ts) */
  amount: number
  /** Chinese label: 存款 / 取款 / 转账 / 理财购买 / 理财赎回 / 理财提前赎回 */
  tradeType: string
  tradeDate: string
  updateDate: string
}

/** MyBatis-Plus page envelope */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface BalancePoint {
  date: string
  balance: number
}

/** BalanceForecastVO */
export interface BalanceForecast {
  accountNumber: string
  currentBalance: number
  points: BalancePoint[]
  minForecastBalance: number
  lowRiskWarning: boolean
}

/** FinancialProductVO */
export interface FinancialProduct {
  id: number
  productCode: string
  productName: string
  annualReturnRate: number
  termDays: number
  minAmount: number
  totalAmount: number
  remainingAmount: number
  status: number
  description: string
  creationDate: string
}

/** FinancialHoldingVO */
export interface FinancialHolding {
  id: number
  productId: number
  productName: string
  annualReturnRate: number
  termDays: number
  investAmount: number
  expectedReturn: number
  status: number
  purchaseDate: string
  redeemDate: string
}

/** FinancialRecommendVO */
export interface FinancialRecommend {
  product: FinancialProduct
  matchScore: number
  reason: string
  userRiskLevel: string
}

/** StockVO */
export interface Stock {
  id: number
  stockCode: string
  stockName: string
  market: string
  status: number
  prevClose: number
  currentPrice: number
  changePercent: number
}

/** StockQuoteVO */
export interface StockQuote {
  stockCode: string
  stockName: string
  currentPrice: number
  prevClose: number
  changePercent: number
  quoteTime: string
}

/** StockOrderVO */
export interface StockOrder {
  id: number
  orderNo: string
  accountNumber: string
  stockCode: string
  stockName: string
  /** 买入 / 卖出 */
  direction: string
  /** 市价 / 限价 */
  priceType: string
  orderPrice: number
  orderQuantity: number
  filledAmount: number
  filledQuantity: number
  status: number
  creationDate: string
}

/** PositionVO */
export interface Position {
  id: number
  accountNumber: string
  stockCode: string
  stockName: string
  quantity: number
  avgCost: number
  currentPrice: number
  marketValue: number
  profitLoss: number
  profitLossPercent: number
}

/** SupportTicketVO */
export interface SupportTicket {
  id: number
  ticketNo: string
  accountNumber: string
  userName: string
  userPhone: string
  category: string
  categoryText: string
  title: string
  description: string
  /** 0 待处理 / 1 处理中 / 2 已回复 / 3 已关闭 */
  status: number
  statusText: string
  reply: string
  handlerName: string
  /** 积极 / 消极 / 中性 / 恶意 */
  sentiment: string
  aiScore: number
  /** 1 = 有效工单, 0 = 无效工单 */
  aiValid: number
  aiEval: string
  creationDate: string
  closeDate: string
}

/** IdCardLibVO */
export interface IdCardLibEntry {
  id: number
  idCard: string
  name: string
  creationDate: string
}

export interface ApiResponse<T> {
  code: string
  msg: string
  data: T
}

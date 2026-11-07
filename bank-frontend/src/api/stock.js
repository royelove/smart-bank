import request from '@/utils/request'

// ==================== 股票相关 ====================

// 股票列表
export function listStocks() {
    return request({ url: '/stock/list', method: 'get' })
}

// 实时行情
export function listQuotes() {
    return request({ url: '/stock/quote', method: 'get' })
}

// 行情源
export function getProvider() {
    return request({ url: '/stock/provider', method: 'get' })
}

// 委托下单（买入/卖出）
export function placeOrder(data) {
    return request({ url: '/stock/order', method: 'post', data })
}

// 我的委托
export function myOrders(accountNumber) {
    return request({ url: '/stock/orders', method: 'get', params: { accountNumber } })
}

// 我的持仓
export function myPositions(accountNumber) {
    return request({ url: '/stock/positions', method: 'get', params: { accountNumber } })
}

// 撤单
export function cancelOrder(id, accountNumber) {
    return request({ url: `/stock/cancel/${id}`, method: 'post', params: { accountNumber } })
}

// ==================== 排行榜相关 ====================

// 获取某类型排行榜 TOP N
export function getRankTopN(type, n) {
    return request({ url: `/rank/top/${type}/${n}`, method: 'get' })
}

// 获取某股票在某个榜的详情
export function getRankDetail(code, type) {
    return request({ url: `/rank/detail/${code}/${type}`, method: 'get' })
}

// 获取所有类型排行榜 TOP N
export function getAllRankTopN(n) {
    return request({ url: `/rank/all/top/${n}`, method: 'get' })
}

// 获取某股票的排名
export function getStockRank(code, type) {
    return request({ url: `/rank/rank/${code}/${type}`, method: 'get' })
}

// 获取某股票的分数
export function getStockRankScore(code, type) {
    return request({ url: `/rank/score/${code}/${type}`, method: 'get' })
}

// 手动刷新排行
export function refreshRank() {
    return request({ url: '/rank/refresh', method: 'post' })
}

// ==================== K线相关 ====================

// 获取K线数据（后端映射在 StockController：GET /stock/kline/{stockCode}）
export function getKline(stockCode, period) {
    return request({ url: `/stock/kline/${stockCode}`, method: 'get', params: { period } })
}
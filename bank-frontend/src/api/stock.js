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
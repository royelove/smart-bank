import request from '@/utils/request'

export function listProducts() {
    return request({ url: '/financial/products', method: 'get' })
}

export function getRecommendations(accountNumber, topN = 3) {
    return request({
        url: '/financial/recommend',
        method: 'get',
        params: { accountNumber, topN }
    })
}

export function getProductDetail(id) {
    return request({ url: `/financial/product/${id}`, method: 'get' })
}

export function purchaseProduct(data) {
    return request({ url: '/financial/purchase', method: 'post', data })
}

export function listHoldings(accountNumber) {
    return request({ url: '/financial/holdings', method: 'get', params: { accountNumber } })
}

export function redeemProduct(data) {
    return request({ url: '/financial/redeem', method: 'post', data })
}

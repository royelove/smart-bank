import request from '@/utils/request'

export function staffLogin(data) {
    return request({
        url: '/support/login',
        method: 'post',
        data
    })
}

export function staffRegister(data) {
    return request({
        url: '/support/register',
        method: 'post',
        data
    })
}

export function submitTicket(data) {
    return request({
        url: '/support/ticket',
        method: 'post',
        data
    })
}

export function getMyTickets(accountNumber) {
    return request({
        url: '/support/tickets',
        method: 'get',
        params: { accountNumber }
    })
}

export function getAllTickets(status, valid) {
    return request({
        url: '/support/admin/tickets',
        method: 'get',
        params: { status, valid }
    })
}

export function acceptTicket(id) {
    return request({
        url: `/support/admin/accept/${id}`,
        method: 'post'
    })
}

export function replyTicket(data) {
    return request({
        url: '/support/admin/reply',
        method: 'post',
        data
    })
}

export function closeTicket(id) {
    return request({
        url: `/support/admin/close/${id}`,
        method: 'post'
    })
}

export function enrollIdCard(data) {
    return request({
        url: '/support/admin/idcard/enroll',
        method: 'post',
        data
    })
}

export function getIdCardList() {
    return request({
        url: '/support/admin/idcard/list',
        method: 'get'
    })
}

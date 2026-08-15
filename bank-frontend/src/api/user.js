import request from '@/utils/request'

export function userLogin(data) {
    return request({
        url: '/user/login',
        method: 'post',
        data
    })
}

export function userRegister(data) {
    return request({
        url: '/user/register',
        method: 'post',
        data
    })
}

export function getUserAccounts() {
    return request({
        url: '/user/accounts',
        method: 'get'
    })
}

export function addAccount(data) {
    return request({
        url: '/user/addAccount',
        method: 'post',
        data
    })
}

export function getUserInfo() {
    return request({
        url: '/user/info',
        method: 'get'
    })
}

export function updateUserPassword(data) {
    return request({
        url: '/user/updatePsw',
        method: 'post',
        data
    })
}

export function bindFace(data) {
    return request({
        url: '/user/face/bind',
        method: 'post',
        data
    })
}

export function verifyFace(data) {
    return request({
        url: '/user/face/verify',
        method: 'post',
        data
    })
}

export function verifyRealName(image) {
    return request({
        url: '/user/face/realname/verify',
        method: 'post',
        data: { image }
    })
}

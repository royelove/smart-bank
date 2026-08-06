import request from '@/utils/request'

// ==================== 账户相关 ====================

// 登录
export function login(data) {
    return request({
        url: '/account/login',
        method: 'post',
        data
    })
}

// 注册
export function register(data) {
    return request({
        url: '/account/register',
        method: 'post',
        data
    })
}

// 查询账户信息
export function getAccount(accountNumber) {
    return request({
        url: `/account/getAccountEntity?accountNumber=${accountNumber}`,
        method: 'post'
    })
}

// 修改密码
export function updatePassword(data) {
    return request({
        url: '/account/updatePsw',
        method: 'post',
        data
    })
}

// 发送验证码（忘记密码）
export function sendCode(data) {
    return request({
        url: '/account/sendCode',
        method: 'post',
        data
    })
}

// 重置密码（忘记密码）
export function resetPassword(data) {
    return request({
        url: '/account/resetPassword',
        method: 'post',
        data
    })
}



// 存款
export function deposit(data) {
    return request({
        url: '/account/deposit',
        method: 'post',
        data
    })
}

// 取款
export function withdraw(data) {
    return request({
        url: '/account/withdraw',
        method: 'post',
        data
    })
}

// 转账
export function transfer(data) {
    return request({
        url: '/account/transfer',
        method: 'post',
        data
    })
}
// 修改个人信息
export function updateProfile(data) {
    return request({
        url: '/account/updateProfile',
        method: 'post',
        data
    })
}
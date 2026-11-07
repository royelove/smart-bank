import axios from 'axios'
import router from '@/router'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || ''

const request = axios.create({
    baseURL: apiBaseUrl,
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json',
    },
})

// 请求拦截器
request.interceptors.request.use(
    (config) => {
        const fullUrl = `${config.baseURL || ''}${config.url}`
        console.log('[Request]', config.method?.toUpperCase(), fullUrl, 'params:', config.params, 'data:', config.data);
        // 登录/注册接口不需要携带 token
        const isAuthEndpoint = config.url?.includes('/login') || config.url?.includes('/register')
        const isSupportAdmin = config.url?.startsWith('/support/admin/')
        const key = isSupportAdmin ? 'staff_token' : 'token'
        if (!isAuthEndpoint) {
            const token = localStorage.getItem(key)
            if (token) {
                config.headers.Authorization = `Bearer ${token}`
            }
        }
        return config
    },
    (error) => {
        console.error('[Request Error]', error);
        return Promise.reject(error)
    }
)

// 响应拦截器
request.interceptors.response.use(
    (response) => {
        const res = response.data
        console.log('[Response]', response.config.method?.toUpperCase(), response.config.url, 'status:', response.status, 'data:', res);
        // 后端约定：code === '000000' 表示成功，其他为失败
        if (res.code !== '000000') {
            const msg = res.msg || res.message || '操作失败'
            return Promise.reject(new Error(msg))
        }
        // 登录接口可能直接返回 token 字符串，或在 data 字段中
        // 如果有 data 字段返回 data，否则返回完整响应（可能包含 token）
        return res.data !== undefined ? res.data : res
    },
    (error) => {
        if (error.response) {
            console.error('[Response Error]', error.config?.method?.toUpperCase(), error.config?.url, 'status:', error.response.status, 'data:', error.response.data);
            const status = error.response.status
            if (status === 401) {
                const isSupportAdmin = error.config?.url?.startsWith('/support/admin/')
                if (isSupportAdmin) {
                    localStorage.removeItem('staff_token')
                    router.push('/support/login')
                } else {
                    localStorage.removeItem('token')
                    localStorage.removeItem('loginType')
                    router.push('/user/login')
                }
            }
        } else {
            console.error('[Network Error]', error.message);
        }
        const msg = error.response?.data?.msg || error.response?.data?.message || error.message || '网络异常'
        return Promise.reject(new Error(msg))
    }
)

export default request
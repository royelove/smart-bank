import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
    baseURL: 'http://localhost:8080',
    timeout: 10000
})

// 请求拦截器：自动携带 Token
request.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    (error) => Promise.reject(error)
)

// 响应拦截器：统一处理错误
request.interceptors.response.use(
    (response) => {
        const res = response.data
        if (res.code !== '000000') {
            ElMessage.error(res.msg || '操作失败')
            return Promise.reject(new Error(res.msg || 'Error'))
        }
        return res
    },
    (error) => {
        if (error.response) {
            const status = error.response.status
            if (status === 401) {
                ElMessage.error('登录已过期，请重新登录')
                localStorage.removeItem('token')
                window.location.href = '/login'
            } else {
                ElMessage.error(error.response.data?.msg || '请求失败')
            }
        } else {
            ElMessage.error('网络异常，请检查连接')
        }
        return Promise.reject(error)
    }
)

export default request
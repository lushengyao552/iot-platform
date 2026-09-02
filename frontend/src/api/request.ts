// ============================================================
// Axios 封装：请求拦截器、响应拦截器、统一错误处理
// ============================================================

import axios, { AxiosInstance, AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios'
import { message } from 'antd'
import { getToken, removeToken } from '@/utils/auth'
import type { ApiResponse } from '@/types'

// 创建 Axios 实例
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
})

// ============================================================
// 请求拦截器：自动携带 Token
// ============================================================
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// ============================================================
// 响应拦截器：统一处理响应和错误
// ============================================================
service.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse

    // 业务成功：返回原始 response，由封装方法提取 data
    if (res.code === 20000) {
      return response
    }

    // 未登录或 Token 过期
    if (res.code === 40100) {
      message.error('登录已过期，请重新登录')
      removeToken()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.message || '未登录'))
    }

    // 其他业务错误
    message.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    console.error('响应错误:', error)

    // HTTP 状态码处理
    if (error.response) {
      const status = error.response.status
      switch (status) {
        case 401:
          message.error('登录已过期，请重新登录')
          removeToken()
          if (window.location.pathname !== '/login') {
            window.location.href = '/login'
          }
          break
        case 403:
          message.error('没有权限访问')
          break
        case 404:
          message.error('请求的资源不存在')
          break
        case 500:
          message.error('服务器内部错误')
          break
        default:
          message.error(error.response.data?.message || `请求失败 (${status})`)
      }
    } else if (error.request) {
      message.error('网络异常，请检查网络连接')
    } else {
      message.error('请求配置错误')
    }

    return Promise.reject(error)
  }
)

// ============================================================
// 封装通用请求方法
// ============================================================

/** GET 请求 */
export function get<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.get(url, { params, ...config }).then((res) => res.data as ApiResponse<T>)
}

/** POST 请求 */
export function post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.post(url, data, config).then((res) => res.data as ApiResponse<T>)
}

/** PUT 请求 */
export function put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.put(url, data, config).then((res) => res.data as ApiResponse<T>)
}

/** DELETE 请求 */
export function del<T = any>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.delete(url, config).then((res) => res.data as ApiResponse<T>)
}

export default service

import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// ========== 请求拦截器 ==========
request.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

// ========== Token 刷新相关 ==========
let isRefreshing = false
let refreshQueue = []

/** 执行等待队列中的所有请求 */
function executeQueue(newToken) {
  refreshQueue.forEach(({ resolve }) => resolve(newToken))
  refreshQueue = []
}

/** 拒绝等待队列中的所有请求 */
function rejectQueue(error) {
  refreshQueue.forEach(({ reject }) => reject(error))
  refreshQueue = []
}

/** 发送刷新请求 */
async function tryRefreshToken() {
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) return null

  try {
    const res = await axios.post('/api/auth/refresh', { refreshToken })
    if (res.data?.code === 200 && res.data?.data) {
      const { accessToken: newAccess, refreshToken: newRefresh } = res.data.data
      localStorage.setItem('accessToken', newAccess)
      localStorage.setItem('refreshToken', newRefresh)
      return newAccess
    }
  } catch (e) {
    // 刷新失败
  }
  return null
}

/** 清理登录状态 */
function clearAuthAndRedirect() {
  localStorage.clear()
  router.push('/login')
}

// ========== 响应拦截器 ==========
request.interceptors.response.use(
  response => {
    // blob/arraybuffer 响应直接返回，不做 code 校验
    const responseType = response.config?.responseType
    if (responseType === 'blob' || responseType === 'arraybuffer') {
      return response.data
    }
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        clearAuthAndRedirect()
      }
      return Promise.reject(res)
    }
    return res
  },
  async error => {
    const originalRequest = error.config

    // 如果是 401 且不是刷新请求自身
    if (error.response?.status === 401 && !originalRequest._isRefresh) {
      // 如果已经在刷新中，把请求放入队列等待
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          refreshQueue.push({ resolve, reject, originalRequest })
        }).then(newToken => {
          originalRequest.headers['Authorization'] = `Bearer ${newToken}`
          return request(originalRequest)
        })
      }

      // 开始刷新
      isRefreshing = true
      originalRequest._isRefresh = true

      try {
        const newToken = await tryRefreshToken()
        if (newToken) {
          // 刷新成功：执行队列中的请求，重试当前请求
          executeQueue(newToken)
          originalRequest.headers['Authorization'] = `Bearer ${newToken}`
          return request(originalRequest)
        } else {
          // 刷新失败：拒绝队列，跳转登录
          rejectQueue(new Error('Token 刷新失败'))
          clearAuthAndRedirect()
          return Promise.reject(error)
        }
      } catch (e) {
        rejectQueue(e)
        clearAuthAndRedirect()
        return Promise.reject(error)
      } finally {
        isRefreshing = false
      }
    }

    ElMessage.error(error.response?.data?.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request

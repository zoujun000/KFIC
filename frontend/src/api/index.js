import request from '@/utils/request'

// 认证
export const authApi = {
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data)
}

// 客户
export const customerApi = {
  page: (params) => request.get('/customers', { params }),
  getById: (id) => request.get(`/customers/${id}`),
  save: (data) => request.post('/customers', data),
  update: (data) => request.put('/customers', data),
  delete: (id) => request.delete(`/customers/${id}`)
}

// 订单
export const orderApi = {
  page: (params) => request.get('/orders', { params }),
  getById: (id) => request.get(`/orders/${id}`),
  create: (data) => request.post('/orders', data),
  update: (data) => request.put('/orders', data),
  updateStatus: (id, status) => request.put(`/orders/${id}/status`, null, { params: { status } }),
  delete: (id) => request.delete(`/orders/${id}`),
  etaAlerts: () => request.get('/orders/eta-alerts')
}
export const quoteApi = {
  upload: (formData) => request.post('/quotes/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  query: (params) => request.get('/quotes', { params }),
  all: () => request.get('/quotes/all'),
  countries: () => request.get('/quotes/countries'),
  destinations: (country) => request.get('/quotes/destinations', { params: { country } }),
  byDestination: (destination) => request.get('/quotes/by-destination', { params: { destination } }),
  byPortCode: (portCode) => request.get('/quotes/by-port-code', { params: { portCode } }),
  update: (id, data) => request.put(`/quotes/${id}`, data),
  delete: (id) => request.delete(`/quotes/${id}`),
  logs: () => request.get('/quotes/logs')
}
export const destChargeApi = {
  upload: (formData) => request.post('/dest-charges/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  calc: (destination, volume) => request.get('/dest-charges/calc', { params: { destination, volume } }),
  ports: () => request.get('/dest-charges/ports'),
  logs: () => request.get('/dest-charges/logs')
}
// 文件上传
export const fileApi = {
  uploadBusinessLicense: (formData) => request.post('/files/upload/business-license', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  getPhotoUrl: (filename) => `/api/files/photo/${filename}`
}

export const portChargeApi = {
  upload: (formData) => request.post('/port-charges/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  calc: (destination, volume, clientType) => request.get('/port-charges/calc', { params: { destination, volume, clientType: clientType || 'direct' } }),
  all: () => request.get('/port-charges/all'),
  destinations: () => request.get('/port-charges/destinations'),
  logs: () => request.get('/port-charges/logs'),
  list: (destination) => request.get('/port-charges', { params: { destination } }),
  update: (id, data) => request.put(`/port-charges/${id}`, data),
  delete: (id) => request.delete(`/port-charges/${id}`)
}

// 日志
export const logApi = {
  list: (type) => request.get('/logs', { params: type ? { type } : {} })
}
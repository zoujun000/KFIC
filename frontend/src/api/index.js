import request from '@/utils/request'

// 认证
export const authApi = {
  login: (data) => request.post('/auth/login', data),
  refresh: (data) => request.post('/auth/refresh', data),
  register: (data) => request.post('/auth/register', data)
}

// 客户
export const customerApi = {
  page: (params) => request.get('/customers', { params }),
  getById: (id) => request.get(`/customers/${id}`),
  save: (data) => request.post('/customers', data),
  update: (data) => request.put('/customers', data),
  delete: (id) => request.delete(`/customers/${id}`),
  stats: () => request.get('/customers/stats')
}

// 订单
// 工作台
export const dashboardApi = {
  stats: () => request.get('/dashboard/stats')
}

export const orderApi = {
  page: (params) => request.get('/orders', { params }),
  getById: (id) => request.get(`/orders/${id}`),
  create: (data) => request.post('/orders', data),
  update: (data) => request.put('/orders', data),
  updateStatus: (id, status) => request.put(`/orders/${id}/status`, null, { params: { status } }),
  delete: (id) => request.delete(`/orders/${id}`),
  etaAlerts: () => request.get('/orders/eta-alerts'),
  uploadAttachments: (id, formData) => request.post(`/orders/${id}/attachments`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  getAttachments: (id) => request.get(`/orders/${id}/attachments`),
  downloadAttachment: (id, filename) => request.get(`/orders/${id}/attachments/${encodeURIComponent(filename)}`, { responseType: 'blob' }),
  previewDocAttachment: (id, filename) => request.get(`/orders/${id}/attachments/${encodeURIComponent(filename)}/word-preview`, { responseType: 'blob' })
}
export const quoteApi = {
  upload: (formData) => request.post('/quotes/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  query: (params) => request.get('/quotes', { params }),
  countries: () => request.get('/quotes/countries'),
  destinations: (country) => request.get('/quotes/destinations', { params: { country } }),
  byDestination: (destination) => request.get('/quotes/by-destination', { params: { destination } }),
  byPortCode: (portCode) => request.get('/quotes/by-port-code', { params: { portCode } }),
  create: (data) => request.post('/quotes', data),
  update: (id, data) => request.put(`/quotes/${id}`, data),
  delete: (id) => request.delete(`/quotes/${id}`),
  logs: () => request.get('/quotes/logs')
}
export const vesselScheduleApi = {
  upload: (formData) => request.post('/vessel-schedules/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  logs: () => request.get('/vessel-schedules/logs')
}
export const quoteTemplateApi = {
  get: () => request.get('/quote-template'),
  save: (template) => request.put('/quote-template', { template })
}
export const destChargeApi = {
  upload: (formData) => request.post('/dest-charges/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  calc: (destination, volume) => request.get('/dest-charges/calc', { params: { destination, volume } }),
  ports: () => request.get('/dest-charges/ports'),
  logs: () => request.get('/dest-charges/logs')
}

// 船舶定位（ShipXY）
export const shipLocateApi = {
  search: (keyword) => request.get('/ship-locate/search', { params: { keyword } }),
  locate: (mmsi) => request.get(`/ship-locate/${mmsi}`),
  portCalls: (mmsi, days = 7) => request.get(`/ship-locate/${mmsi}/port-calls`, { params: { days } }),
  status: () => request.get('/ship-locate/status')
}

// 文件上传
export const fileApi = {
  uploadBusinessLicense: (formData) => request.post('/files/upload/business-license', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  getPhotoUrl: (filename) => {
    const safePath = filename.split('/').map(encodeURIComponent).join('/')
    return `/api/files/photo/${safePath}?token=${localStorage.getItem('accessToken') || ''}`
  }
}

export const portChargeApi = {
  upload: (formData) => request.post('/port-charges/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  calc: (destination, volume, clientType) => request.get('/port-charges/calc', { params: { destination, volume, clientType: clientType || 'direct' } }),
  countries: () => request.get('/port-charges/countries'),
  destinations: (country) => request.get('/port-charges/destinations', { params: country ? { country } : {} }),
  logs: () => request.get('/port-charges/logs'),
  list: (destination) => request.get('/port-charges', { params: { destination } }),
  create: (data) => request.post('/port-charges', data),
  update: (id, data) => request.put(`/port-charges/${id}`, data),
  delete: (id) => request.delete(`/port-charges/${id}`),
  addDestination: (country, destination) => request.post('/port-charges/add-destination', null, { params: { country, destination } })
}

// 日志
export const logApi = {
  list: (type) => request.get('/logs', { params: type ? { type } : {} })
}

// 公告栏
export const announcementApi = {
  page: (params) => request.get('/announcements', { params }),
  create: (data) => request.post('/announcements', data),
  update: (id, data) => request.put(`/announcements/${id}`, data),
  delete: (id) => request.delete(`/announcements/${id}`),
  uploadAttachments: (id, formData) => request.post(`/announcements/${id}/attachments`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  downloadAttachment: (id, filename) => request.get(`/announcements/${id}/attachments/${encodeURIComponent(filename)}`, { responseType: 'blob' }),
  deleteAttachment: (id, filename) => request.delete(`/announcements/${id}/attachments/${encodeURIComponent(filename)}`),
  previewDocAttachment: (id, filename) => request.get(`/announcements/${id}/attachments/${encodeURIComponent(filename)}/word-preview`, { responseType: 'blob' })
}

import axios from 'axios'

// 维运网货物跟踪接口（逆向自 www.weiyun001.com/track，无需登录，CORS 已放开）
const BASE = 'https://wywapi.weiyun001.com/api'

const normalize = (number) => String(number || '').trim().toUpperCase()

export const weiyunTrackApi = {
  // 船司列表（含官网地址）
  carrierSource: () => axios.get(`${BASE}/cargoTracking/getCarrierSource`).then((res) => res.data),
  // 航空公司列表（含 3 位数字前缀 digitalCode 与官网地址）
  airlineSource: () => axios.get(`${BASE}/cargoTracking/getAirlineSource`).then((res) => res.data),
  // 快递公司列表（含带 {0} 占位符的查询链接模板）
  expressSource: () => axios.get(`${BASE}/cargoTracking/getExpressSupplierSource`).then((res) => res.data),
  // 按单号识别船司
  recognize: (number) =>
    axios.get(`${BASE}/cargoTracking/recognitionCarrierNumber`, { params: { number: normalize(number) } }).then((res) => res.data),
  // 按运单号识别航空公司
  recognizeAirline: (number) =>
    axios.get(`${BASE}/cargoTracking/recognitionAirlineNumber`, { params: { number: normalize(number) } }).then((res) => res.data),
  // 按单号识别快递公司（返回候选列表）
  recognizeExpress: (number) =>
    axios.get(`${BASE}/cargoTracking/recognizeExpressNo`, { params: { number: normalize(number) } }).then((res) => res.data),
  // 获取带单号的官网跳转链接
  searchLink: (number, code) =>
    axios
      .post(`${BASE}/cargoTracking/getCarrierSearchLink`, { number: normalize(number), code })
      .then((res) => res.data),
  // 获取带运单号的航空公司官网跳转链接
  airSearchLink: (number, code) =>
    axios
      .post(`${BASE}/cargoTracking/getAirSearchLink`, { number: normalize(number), code })
      .then((res) => res.data)
}

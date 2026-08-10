// 船公司前缀识别与官网货物跟踪链接配置
// 新增船公司：在 carriers 中追加一条配置即可（prefixes 支持多个前缀，trackUrl 返回官网查询链接）

const encode = encodeURIComponent

export const carriers = [
  {
    code: 'ZIM',
    wyCode: 'ZIM',
    name: '以星轮船',
    prefixes: ['ZIMU', 'ZIM'],
    trackUrl: (number) => `https://www.zimchina.com/tools/track-a-shipment?consnumber=${encode(number)}`
  },
  {
    code: 'MAERSK',
    wyCode: 'MSK',
    name: '马士基',
    prefixes: ['MAEU'],
    trackUrl: (number) => `https://www.maersk.com/tracking/${encode(number)}`
  },
  {
    code: 'MSC',
    wyCode: 'MSC',
    name: '地中海航运',
    prefixes: ['MSCU'],
    trackUrl: (number) => {
      const params = btoa(`trackingNumber=${number}&trackingMode=0`)
      return `https://www.msc.com/en/track-a-shipment?params=${params}`
    }
  },
  {
    code: 'CMA',
    wyCode: 'CMA',
    name: '达飞轮船',
    prefixes: ['CMAU', 'CMDU', 'CMCU', 'CMFU', 'CMNU', 'CGMU'],
    trackUrl: () => 'https://www.cma-cgm.com/ebusiness/tracking'
  },
  {
    code: 'COSCO',
    wyCode: 'COSU',
    name: '中远海运',
    prefixes: ['COSU', 'CSLU', 'CSNU', 'TGHU'],
    trackUrl: () => 'https://elines.coscoshipping.com/ebusiness/cargotracking'
  },
  {
    code: 'EMC',
    wyCode: 'EMC',
    name: '长荣海运',
    prefixes: ['EISU', 'EMCU', 'EVGU', 'EGHU', 'EGSU', 'EGLV'],
    trackUrl: () => 'https://www.shipmentlink.com/servlet/TDB1_CargoTracking.do'
  },
  {
    code: 'ONE',
    wyCode: 'ONE',
    name: '海洋网联',
    prefixes: ['ONEU', 'ONEY', 'NYKU', 'MOLU', 'KKLU'],
    trackUrl: () => 'https://www.one-line.com/en'
  },
  {
    code: 'HLC',
    wyCode: 'HLC',
    name: '赫伯罗特',
    prefixes: ['HLCU'],
    trackUrl: () => 'https://www.hapag-lloyd.com/en/online-business/track/track-by-container-solution.html'
  },
  {
    code: 'OOCL',
    wyCode: 'OOLU',
    name: '东方海外',
    prefixes: ['OOLU'],
    trackUrl: () => 'https://www.oocl.com/eng/ourservices/eservices/cargotracking/Pages/cargotracking.aspx'
  },
  {
    code: 'YML',
    wyCode: 'YML',
    name: '阳明海运',
    prefixes: ['YMLU'],
    trackUrl: () => 'https://www.yangming.com/e-service/Track_Trace/tt_ov.aspx'
  },
  {
    code: 'WHL',
    wyCode: 'WHL',
    name: '万海航运',
    prefixes: ['WHLU', 'WHLC'],
    trackUrl: () => 'https://www.wanhai.com/views/cargo_track_v2/tracking_query.xhtml'
  },
  {
    code: 'PIL',
    wyCode: 'PIL',
    name: '太平船务',
    prefixes: ['PCIU', 'PILU'],
    trackUrl: () => 'https://www.pilship.com/en-our-track-and-trace-pil-pacific-international-lines/120.html'
  },
  {
    code: 'HMM',
    wyCode: 'HMM',
    name: '韩新海运',
    prefixes: ['HDMU', 'HMMU'],
    trackUrl: () => 'https://www.hmm21.com/cms/business/ebiz/trackTrace/trackTrace/index.jsp'
  },
  {
    code: 'SITC',
    wyCode: 'SITC',
    name: '海丰国际',
    prefixes: ['SITC', 'SITU'],
    trackUrl: () => 'https://www.sitc.com'
  },
  {
    code: 'KMTC',
    wyCode: 'KMTC',
    name: '高丽海运',
    prefixes: ['KMTU', 'KMTCU', 'KMTC'],
    trackUrl: () => 'https://www.ekmtc.com'
  },
  {
    code: 'TSL',
    wyCode: 'TSL',
    name: '德翔海运',
    prefixes: ['TSLU', 'TCLU'],
    trackUrl: () => 'https://www.tslines.com/en/tracking'
  },
  {
    code: 'RCL',
    wyCode: 'RCL',
    name: '宏海箱运',
    prefixes: ['RCLU', 'RCLX'],
    trackUrl: () => 'https://www.rclgroup.com/Home#cargo'
  },
  {
    code: 'MATSON',
    wyCode: 'MATSON',
    name: '美森轮船',
    prefixes: ['MATU', 'MATS'],
    trackUrl: () => 'https://www.matson.com/matnav/tracking.html'
  }
]

// 前缀索引：按前缀长度倒序，保证最长匹配优先（如 ZIMU 优先于 ZIM）
const prefixIndex = carriers
  .flatMap((carrier) => carrier.prefixes.map((prefix) => ({ prefix: prefix.toUpperCase(), carrier })))
  .sort((a, b) => b.prefix.length - a.prefix.length)

// 数字订舱号格式规则（参考维运网识别逻辑：大公司订舱号位数/起始数字稳定）
const numericRules = [
  { digits: 8, match: () => true, wyCode: 'HLC' }, // 赫伯罗特订舱号
  { digits: 9, match: () => true, wyCode: 'MSK' }, // 马士基订舱号
  { digits: 10, match: (digits) => digits[0] === '2' || digits[0] === '4', wyCode: 'OOLU' }, // 东方海外订舱号
  { digits: 10, match: (digits) => ['5', '6', '7', '9'].includes(digits[0]), wyCode: 'COSU' }, // 中远海运订舱号
  { digits: 12, match: (digits) => ['2', '4', '5'].includes(digits[0]), wyCode: 'EMC' }, // 长荣订舱号
  { digits: 12, match: (digits) => digits[0] === '9', wyCode: 'TSL' } // 德翔订舱号
]

// 按单号识别船公司：前缀优先，纯数字订舱号按位数规则，忽略大小写与空格
export function detectCarrierByNumber(number) {
  const normalized = String(number || '').replace(/\s+/g, '').toUpperCase()
  if (!normalized) return null
  const prefixMatch = prefixIndex.find((item) => normalized.startsWith(item.prefix))
  if (prefixMatch) return prefixMatch.carrier
  if (/^\d+$/.test(normalized)) {
    const rule = numericRules.find((item) => normalized.length === item.digits && item.match(normalized))
    if (rule) return carriers.find((carrier) => (carrier.wyCode || carrier.code) === rule.wyCode) || null
  }
  return null
}

// 兼容旧引用
export const detectCarrier = detectCarrierByNumber

export function carrierByCode(code) {
  return carriers.find((carrier) => carrier.code === code) || null
}

// 合并维运网船司列表与本地配置（远程列表优先，本地补充官网链接兜底）
export function mergeCarrierOptions(remoteList) {
  if (!Array.isArray(remoteList) || remoteList.length === 0) {
    return carriers.map((carrier) => ({
      code: carrier.wyCode || carrier.code,
      wyCode: carrier.wyCode || carrier.code,
      name: carrier.name,
      website: '',
      trackUrl: carrier.trackUrl
    }))
  }

  const localByWyCode = new Map(carriers.map((carrier) => [carrier.wyCode || carrier.code, carrier]))
  const merged = remoteList.map((item) => {
    const local = localByWyCode.get(item.code)
    return {
      code: item.code,
      wyCode: item.code,
      name: item.cnName || item.enName || item.code,
      website: item.cnWebsite || item.enWebsite || '',
      trackUrl: local?.trackUrl || null,
      isSupport: !!item.isSupport
    }
  })

  for (const carrier of carriers) {
    const wyCode = carrier.wyCode || carrier.code
    if (!merged.some((item) => item.wyCode === wyCode)) {
      merged.push({
        code: carrier.code,
        wyCode,
        name: carrier.name,
        website: '',
        trackUrl: carrier.trackUrl,
        isSupport: true
      })
    }
  }
  return merged
}

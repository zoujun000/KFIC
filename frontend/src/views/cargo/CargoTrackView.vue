<template>
  <div class="cargo-track-page">
    <header class="page-hero">
      <div class="hero-mark">
        <el-icon :size="20"><Ship /></el-icon>
      </div>
      <div>
        <h1>货物跟踪</h1>
        <p>海运、空运、快递，一个入口查询全程</p>
      </div>
    </header>

    <section class="glass-card">
      <div class="segmented" role="tablist" aria-label="跟踪类型">
        <div
          class="segmented-indicator"
          :style="{ transform: `translateX(${activeIndex * 100}%)` }"
          aria-hidden="true"
        />
        <button
          v-for="tab in tabs"
          :key="tab.name"
          type="button"
          role="tab"
          class="segment"
          :class="{ active: activeTab === tab.name }"
          :aria-selected="activeTab === tab.name"
          @click="switchTab(tab.name)"
        >
          <el-icon :size="16"><component :is="tab.icon" /></el-icon>
          <span>{{ tab.label }}</span>
        </button>
      </div>

      <form class="track-form" @submit.prevent="query">
        <div class="field">
          <label class="field-label" :for="`track-number-${activeTab}`">{{ numberLabel }}</label>
          <el-input
            :id="`track-number-${activeTab}`"
            v-model="number"
            class="track-input"
            size="large"
            :placeholder="numberPlaceholder"
            clearable
            @input="handleInput"
          />
        </div>

        <div class="field">
          <label class="field-label">{{ companyLabel }}</label>
          <el-select
            v-model="supplierCode"
            class="track-select"
            size="large"
            filterable
            clearable
            :placeholder="`输入单号自动识别，也可手动选择${companyLabel}`"
          >
            <el-option
              v-for="option in currentOptions"
              :key="option.code"
              :label="`${option.code} · ${option.name}`"
              :value="option.code"
            />
          </el-select>
        </div>

        <div class="actions">
          <el-button type="primary" native-type="submit" class="query-btn" :loading="queryLoading">
            <el-icon v-if="!queryLoading" :size="16"><Search /></el-icon>
            <span>查询</span>
          </el-button>
          <el-button class="reset-btn" :icon="RefreshLeft" @click="reset">重置</el-button>
        </div>
      </form>

      <transition name="status">
        <div v-if="activeNumber && matchedSupplier" class="status-card success">
          <div class="status-avatar">{{ matchedSupplier.code.slice(0, 2).toUpperCase() }}</div>
          <div class="status-body">
            <div class="status-title">已识别 · {{ matchedSupplier.code }} {{ matchedSupplier.name }}</div>
            <div class="status-text">单号「{{ activeNumber }}」查询链接已就绪</div>
          </div>
          <el-link
            v-if="previewUrl"
            class="status-link"
            :href="previewUrl"
            target="_blank"
            rel="noopener"
          >
            预览链接
          </el-link>
        </div>
        <div v-else class="status-card hint">
          <div class="status-avatar ghost">
            <el-icon :size="16"><Search /></el-icon>
          </div>
          <div class="status-body">
            <div class="status-title">{{ warningText }}</div>
            <div class="status-text">输入单号后会自动识别公司，点击「查询」跳转官网</div>
          </div>
        </div>
      </transition>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Ship, Promotion, Van, Search, RefreshLeft } from '@element-plus/icons-vue'
import { detectCarrierByNumber, mergeCarrierOptions } from '@/utils/carriers'
import { weiyunTrackApi } from '@/api/weiyunTrack'

defineOptions({ name: 'CargoTrack' })

const tabs = [
  { name: 'shipping', label: '海运跟踪', icon: Ship },
  { name: 'air', label: '空运跟踪', icon: Promotion },
  { name: 'express', label: '快递跟踪', icon: Van }
]

const activeTab = ref('shipping')
const number = ref('')
const supplierCode = ref('')
const queryLoading = ref(false)

const shippingOptions = ref(mergeCarrierOptions(null))
const airOptions = ref([])
const expressOptions = ref([])
const airlineByPrefix = ref(new Map())

const tabMeta = {
  shipping: {
    numberLabel: '订舱单号',
    numberPlaceholder: '请输入订舱单号、提单号或箱号',
    companyLabel: '船公司'
  },
  air: {
    numberLabel: '运单号',
    numberPlaceholder: '请输入航空运单号（如 99912345678）',
    companyLabel: '航空公司'
  },
  express: {
    numberLabel: '快递单号',
    numberPlaceholder: '请输入快递单号',
    companyLabel: '快递公司'
  }
}

const currentMeta = computed(() => tabMeta[activeTab.value])
const activeIndex = computed(() => tabs.findIndex((tab) => tab.name === activeTab.value))
const numberLabel = computed(() => currentMeta.value.numberLabel)
const numberPlaceholder = computed(() => currentMeta.value.numberPlaceholder)
const companyLabel = computed(() => currentMeta.value.companyLabel)

const currentOptions = computed(() => {
  if (activeTab.value === 'air') return airOptions.value
  if (activeTab.value === 'express') return expressOptions.value
  return shippingOptions.value
})

const matchedSupplier = computed(() =>
  supplierCode.value ? currentOptions.value.find((option) => option.code === supplierCode.value) || null : null
)

const activeNumber = computed(() => number.value.trim())

const warningText = computed(() => {
  if (!activeNumber.value) return `请输入${numberLabel.value}`
  return `未识别到${companyLabel.value}，请手动选择`
})

const previewUrl = computed(() => {
  if (!activeNumber.value || !matchedSupplier.value) return ''
  if (activeTab.value === 'shipping') {
    if (matchedSupplier.value.trackUrl) return matchedSupplier.value.trackUrl(activeNumber.value)
    return matchedSupplier.value.website || ''
  }
  if (activeTab.value === 'express') return buildExpressUrl(matchedSupplier.value, activeNumber.value)
  return matchedSupplier.value.website || ''
})

// 本地快递单号前缀兜底（维运网快递识别较弱）
const expressPatterns = [
  { test: (value) => /^SF/i.test(value), code: 'sfb2c' },
  { test: (value) => /^EMS/i.test(value), code: 'china-ems' },
  { test: (value) => /^DHL/i.test(value), code: 'dhl' },
  { test: (value) => /^TNT/i.test(value), code: 'tnt' },
  { test: (value) => /^1Z/i.test(value), code: 'ups' },
  { test: (value) => /^4PX/i.test(value), code: '4px' },
  { test: (value) => /^YUN/i.test(value), code: 'yunexpress' },
  { test: (value) => /^[A-Z]{2}\d{9}CN$/i.test(value), code: 'china-post' }
]

const buildExpressUrl = (supplier, numberValue) => {
  const template = supplier.urlTemplate || ''
  return template ? template.replace(/\{0\}/g, encodeURIComponent(numberValue)) : supplier.website || ''
}

// 航空运单号取前 3 位数字匹配航空公司
const detectAirline = (value) => {
  const digits = value.replace(/\D/g, '')
  if (digits.length < 3) return null
  return airlineByPrefix.value.get(digits.slice(0, 3)) || null
}

const detectExpress = (value) => {
  const pattern = expressPatterns.find((item) => item.test(value))
  if (!pattern) return null
  return expressOptions.value.find((option) => option.code === pattern.code) || null
}

let recognizeTimer = null

// 输入时优先本地规则识别，识别不到再调维运网接口
const handleInput = () => {
  const value = number.value.trim()
  if (!value) {
    clearTimeout(recognizeTimer)
    return
  }
  clearTimeout(recognizeTimer)

  if (activeTab.value === 'shipping') {
    const localCarrier = detectCarrierByNumber(value)
    if (localCarrier) {
      const option = shippingOptions.value.find(
        (carrier) => carrier.wyCode === (localCarrier.wyCode || localCarrier.code)
      )
      supplierCode.value = option?.code || localCarrier.code
      return
    }
    recognizeTimer = setTimeout(async () => {
      try {
        const res = await weiyunTrackApi.recognize(value)
        if (res?.result?.code) supplierCode.value = res.result.code
      } catch {
        // 保持当前选择
      }
    }, 400)
    return
  }

  if (activeTab.value === 'air') {
    const airline = detectAirline(value)
    if (airline) {
      supplierCode.value = airline.code
      return
    }
    recognizeTimer = setTimeout(async () => {
      try {
        const res = await weiyunTrackApi.recognizeAirline(value)
        const result = res?.result
        const code = result?.code || (Array.isArray(result) ? result[0]?.code : '')
        if (code) supplierCode.value = code
      } catch {
        // 保持当前选择
      }
    }, 400)
    return
  }

  const express = detectExpress(value)
  if (express) {
    supplierCode.value = express.code
    return
  }
  recognizeTimer = setTimeout(async () => {
    try {
      const res = await weiyunTrackApi.recognizeExpress(value)
      const list = res?.result
      if (Array.isArray(list) && list.length === 1) supplierCode.value = list[0].code
    } catch {
      // 保持当前选择
    }
  }, 400)
}

const query = async () => {
  const numberValue = activeNumber.value
  if (!numberValue) {
    ElMessage.warning(`请输入${numberLabel.value}`)
    return
  }
  const supplier = matchedSupplier.value
  if (!supplier) {
    ElMessage.warning(`请选择${companyLabel.value}`)
    return
  }

  queryLoading.value = true
  try {
    if (activeTab.value === 'express') {
      const url = buildExpressUrl(supplier, numberValue)
      if (url) {
        window.open(url, '_blank', 'noopener,noreferrer')
        return
      }
    } else {
      let url = ''
      let formHtml = ''
      try {
        const api = activeTab.value === 'shipping' ? weiyunTrackApi.searchLink : weiyunTrackApi.airSearchLink
        const res = await api(numberValue, supplier.code)
        if (res?.success && res.result) {
          url = res.result.url || ''
          formHtml = res.result.form || ''
        }
      } catch {
        // 维运网接口异常时走本地兜底
      }
      if (formHtml && submitWeiyunForm(formHtml)) return
      if (url) {
        window.open(url, '_blank', 'noopener,noreferrer')
        return
      }
    }

    if (supplier.trackUrl) {
      window.open(supplier.trackUrl(numberValue), '_blank', 'noopener,noreferrer')
      return
    }
    if (supplier.website) {
      window.open(supplier.website, '_blank', 'noopener,noreferrer')
      return
    }
    ElMessage.warning('未获取到查询链接，请手动选择后重试')
  } finally {
    queryLoading.value = false
  }
}

// 达飞、长荣等官网需要 POST 表单，这里用隐藏表单提交实现
const submitWeiyunForm = (formHtml) => {
  const doc = new DOMParser().parseFromString(formHtml, 'text/html')
  const sourceForm = doc.querySelector('form')
  if (!sourceForm) return false
  const tempForm = document.createElement('form')
  tempForm.action = sourceForm.action
  tempForm.method = sourceForm.method || 'post'
  tempForm.target = '_blank'
  tempForm.style.display = 'none'
  sourceForm.querySelectorAll('input').forEach((input) => {
    const copy = document.createElement('input')
    copy.type = input.type || 'hidden'
    copy.name = input.name
    copy.value = input.value
    tempForm.appendChild(copy)
  })
  document.body.appendChild(tempForm)
  tempForm.submit()
  setTimeout(() => tempForm.remove(), 1000)
  return true
}

// 并行加载维运网三类公司列表，接口异常时保留本地兜底
const loadOptions = async () => {
  const [carrierRes, airlineRes, expressRes] = await Promise.allSettled([
    weiyunTrackApi.carrierSource(),
    weiyunTrackApi.airlineSource(),
    weiyunTrackApi.expressSource()
  ])

  if (carrierRes.status === 'fulfilled' && carrierRes.value?.success && Array.isArray(carrierRes.value.result)) {
    shippingOptions.value = mergeCarrierOptions(carrierRes.value.result)
  }

  if (airlineRes.status === 'fulfilled' && airlineRes.value?.success && Array.isArray(airlineRes.value.result)) {
    airOptions.value = airlineRes.value.result.map((item) => ({
      code: item.code,
      name: item.cnName || item.enName || item.code,
      website: item.cnWebsite || item.enWebsite || '',
      digitalCode: item.digitalCode || ''
    }))
    airlineByPrefix.value = new Map(
      airOptions.value
        .filter((item) => /^\d{3}$/.test(item.digitalCode))
        .map((item) => [item.digitalCode, item])
    )
  }

  if (expressRes.status === 'fulfilled' && expressRes.value?.success && Array.isArray(expressRes.value.result)) {
    expressOptions.value = expressRes.value.result.map((item) => ({
      code: item.code,
      name: item.cnName || item.enName || item.code,
      website: item.siteUrl || '',
      urlTemplate: item.cnUrl || item.enUrl || ''
    }))
  }
}

const switchTab = (name) => {
  if (name === activeTab.value) return
  activeTab.value = name
  reset()
}

const reset = () => {
  number.value = ''
  supplierCode.value = ''
  clearTimeout(recognizeTimer)
}

onMounted(loadOptions)
onUnmounted(() => clearTimeout(recognizeTimer))
</script>

<style scoped>
.cargo-track-page {
  width: 100%;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  padding: 8px;
  box-sizing: border-box;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'PingFang SC',
    'Hiragino Sans GB', 'Microsoft YaHei', system-ui, sans-serif;
}

/* ── 页头 ── */
.page-hero {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 4px 2px 18px;
  flex-shrink: 0;
}

.hero-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 15px;
  color: #fff;
  background: linear-gradient(135deg, #3b82f6, #0ea5e9);
  box-shadow: 0 10px 24px -8px rgba(37, 99, 235, 0.55);
  flex: none;
}

.page-hero h1 {
  margin: 0;
  font-size: clamp(1.5rem, 4vw, 1.9rem);
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: -0.02em;
  color: #0f172a;
}

.page-hero p {
  margin: 5px 0 0;
  font-size: 14px;
  color: #64748b;
}

/* ── 玻璃卡片 ── */
.glass-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 26px 24px 22px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.72);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  backdrop-filter: blur(24px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.65);
  box-shadow:
    0 1px 2px rgba(15, 23, 42, 0.04),
    0 18px 44px -14px rgba(15, 23, 42, 0.14);
  animation: card-in 520ms cubic-bezier(0.22, 1, 0.36, 1) both;
}

@keyframes card-in {
  from {
    opacity: 0;
    transform: translateY(14px) scale(0.99);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

/* ── 分段控制器 ── */
.segmented {
  position: relative;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  padding: 4px;
  margin-bottom: 26px;
  background: rgba(15, 23, 42, 0.06);
  border-radius: 14px;
}

.segmented-indicator {
  position: absolute;
  top: 4px;
  bottom: 4px;
  left: 4px;
  width: calc((100% - 8px) / 3);
  border-radius: 10px;
  background: #fff;
  box-shadow:
    0 2px 8px rgba(15, 23, 42, 0.1),
    0 0 0 1px rgba(15, 23, 42, 0.04);
  transition: transform 320ms cubic-bezier(0.22, 1, 0.36, 1);
  will-change: transform;
  pointer-events: none;
}

.segment {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 40px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  font: inherit;
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
  cursor: pointer;
  transition: color 220ms ease, transform 100ms ease-out;
}

.segment:active {
  transform: scale(0.97);
}

.segment.active {
  color: #0f172a;
  font-weight: 600;
}

.segment:focus-visible {
  outline: 2px solid rgba(37, 99, 235, 0.6);
  outline-offset: 2px;
}

/* ── 表单 ── */
.track-form {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.field {
  margin-bottom: 18px;
}

.field-label {
  display: block;
  margin: 0 0 8px 2px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.01em;
  color: #334155;
}

.track-input :deep(.el-input__wrapper) {
  min-height: 46px;
  padding: 4px 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 0 0 1px rgba(148, 163, 184, 0.35) inset;
  transition: box-shadow 180ms ease, background 180ms ease;
}

.track-input :deep(.el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow:
    0 0 0 1px var(--color-primary) inset,
    0 0 0 4px rgba(59, 130, 246, 0.14);
}

.track-select {
  width: 100%;
}

.track-select :deep(.el-select__wrapper) {
  min-height: 46px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 0 0 1px rgba(148, 163, 184, 0.35) inset;
  transition: box-shadow 180ms ease, background 180ms ease;
}

.track-select :deep(.el-select__wrapper.is-focused) {
  background: #fff;
  box-shadow:
    0 0 0 1px var(--color-primary) inset,
    0 0 0 4px rgba(59, 130, 246, 0.14);
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: auto;
  padding-top: 18px;
}

.query-btn {
  flex: 1;
  height: 48px;
  border-radius: 14px;
  font-weight: 600;
  letter-spacing: 0.02em;
  transition: transform 100ms ease-out;
}

.query-btn:active {
  transform: scale(0.97);
}

.query-btn :deep(span) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.reset-btn {
  height: 48px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.7);
  border-color: rgba(148, 163, 184, 0.4);
  transition: transform 100ms ease-out;
}

.reset-btn:active {
  transform: scale(0.97);
}

.query-btn:focus-visible,
.reset-btn:focus-visible {
  outline: 2px solid rgba(37, 99, 235, 0.6);
  outline-offset: 2px;
}

/* ── 状态卡片 ── */
.status-card {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 18px;
  padding: 14px 16px;
  border-radius: 16px;
}

.status-card.success {
  background: rgba(240, 253, 250, 0.92);
  border: 1px solid rgba(16, 185, 129, 0.22);
}

.status-card.hint {
  background: rgba(248, 250, 252, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.status-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 700;
  color: #047857;
  background: rgba(16, 185, 129, 0.14);
  flex: none;
}

.status-avatar.ghost {
  color: #64748b;
  background: rgba(148, 163, 184, 0.14);
}

.status-body {
  flex: 1;
  min-width: 0;
}

.status-title {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.status-text {
  margin-top: 2px;
  font-size: 13px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-link {
  font-size: 13px;
  flex: none;
}

.status-enter-active {
  transition: opacity 220ms ease, transform 320ms cubic-bezier(0.22, 1, 0.36, 1);
}

.status-leave-active {
  transition: opacity 160ms ease;
}

.status-enter-from,
.status-leave-to {
  opacity: 0;
}

.status-enter-from {
  transform: translateY(6px);
}

/* ── 无障碍 / 减弱动态 ── */
@media (prefers-reduced-motion: reduce) {
  .glass-card {
    animation: none;
  }

  .segmented-indicator,
  .segment,
  .query-btn,
  .reset-btn {
    transition: none;
  }

  .segment:active,
  .query-btn:active,
  .reset-btn:active {
    transform: none;
  }

  .status-enter-active {
    transition: opacity 160ms ease;
  }

  .status-enter-from {
    transform: none;
  }
}
</style>

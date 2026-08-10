<template>
  <div class="ship-locate-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <div class="search-box">
          <el-select
            v-model="selectedMmsi"
            filterable
            remote
            clearable
            reserve-keyword
            :remote-method="searchShips"
            :loading="searchLoading"
            placeholder="输入船名 / 船号 / 9 位 MMSI"
            class="ship-select"
            @change="onSelectShip"
            @clear="clearShip"
          >
            <el-option
              v-for="item in searchResults"
              :key="item.mmsi"
              :label="optionLabel(item)"
              :value="item.mmsi"
            >
              <div class="option-item">
                <span class="option-name">{{ item.name }}</span>
                <span class="option-mmsi">MMSI {{ item.mmsi }}</span>
                <span v-if="item.shipType" class="option-type">{{ item.shipType }}</span>
              </div>
            </el-option>
            <template #empty>
              <div class="search-empty">
                未匹配到船舶
                <br />
                <small>船名索引随 AIS 实时数据持续积累，可稍后重试或直接输入 9 位 MMSI</small>
              </div>
            </template>
          </el-select>
          <el-button type="primary" :icon="Position" @click="locateInput">定位</el-button>
          <el-button v-if="selectedMmsi" :icon="Aim" @click="fitTrack">居中</el-button>
        </div>
        <div class="status-box">
          <el-tag :type="streamConnected ? 'success' : 'danger'" size="small" effect="light">
            {{ streamConnected ? 'AIS 已连接' : 'AIS 未连接' }}
          </el-tag>
          <span class="status-text">已收录船舶 {{ trackedShips }}</span>
          <span v-if="lastMessageText" class="status-text">最后消息 {{ lastMessageText }}</span>
        </div>
      </div>
    </el-card>

    <div class="content">
      <!-- 船舶信息 -->
      <el-card shadow="never" class="info-card">
        <template #header>
          <div class="card-header">
            <span>船舶信息</span>
            <el-tag v-if="shipInfo" size="small" type="info" effect="plain">
              {{ trackPoints }} 个轨迹点
            </el-tag>
          </div>
        </template>
        <template v-if="shipInfo">
          <div class="ship-name">
            <span class="ship-name-text">{{ shipInfo.name || '—' }}</span>
          </div>
          <el-descriptions :column="1" size="small" class="ship-desc">
            <el-descriptions-item label="MMSI">{{ shipInfo.mmsi || '—' }}</el-descriptions-item>
            <el-descriptions-item label="IMO">{{ shipInfo.imo || '—' }}</el-descriptions-item>
            <el-descriptions-item label="呼号">{{ shipInfo.callsign || '—' }}</el-descriptions-item>
            <el-descriptions-item label="类型">{{ shipInfo.shipType || '—' }}</el-descriptions-item>
            <el-descriptions-item label="船长 × 船宽">{{ formatSize(shipInfo.length, shipInfo.breadth) }}</el-descriptions-item>
            <el-descriptions-item label="吃水">{{ shipInfo.draught ? shipInfo.draught + ' m' : '—' }}</el-descriptions-item>
            <el-descriptions-item label="目的地">{{ shipInfo.destination || '—' }}</el-descriptions-item>
            <el-descriptions-item label="ETA">{{ shipInfo.eta || '—' }}</el-descriptions-item>
          </el-descriptions>
          <el-divider />
          <div v-if="latest" class="latest">
            <div class="latest-row">
              <span class="label">纬度</span>
              <span>{{ formatCoord(latest.lat, 'N', 'S') }}</span>
            </div>
            <div class="latest-row">
              <span class="label">经度</span>
              <span>{{ formatCoord(latest.lon, 'E', 'W') }}</span>
            </div>
            <div class="latest-row">
              <span class="label">航速</span>
              <span>{{ latest.sog != null ? latest.sog.toFixed(1) + ' 节' : '—' }}</span>
            </div>
            <div class="latest-row">
              <span class="label">航向</span>
              <span>{{ latest.cog != null ? latest.cog.toFixed(0) + '°' : '—' }}</span>
            </div>
            <div class="latest-row">
              <span class="label">状态</span>
              <span>{{ navStatusText(latest.navStatus) }}</span>
            </div>
            <div class="latest-row">
              <span class="label">数据时间</span>
              <span>{{ formatTime(latest.time) }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无实时位置" :image-size="60" />
        </template>
        <el-empty v-else description="请先搜索并选择船舶" :image-size="80" />
      </el-card>

      <!-- 地图 -->
      <el-card shadow="never" class="map-card">
        <div ref="mapEl" class="map-container"></div>
        <div v-if="!selectedMmsi" class="map-placeholder">
          <el-icon :size="42"><Ship /></el-icon>
          <p>输入船名或 MMSI，点击「定位」查看实时位置与累计轨迹</p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { Aim, Position } from '@element-plus/icons-vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { shipLocateApi } from '@/api'

defineOptions({ name: 'ShipLocate' })

const mapEl = ref(null)
const selectedMmsi = ref('')
const searchKeyword = ref('')
const searchResults = ref([])
const searchLoading = ref(false)
const shipInfo = ref(null)
const latest = ref(null)
const track = ref([])
const streamConnected = ref(false)
const trackedShips = ref(0)
const lastMessageAt = ref(0)

let map = null
let trackLayer = null
let marker = null
let fitDone = false
let pollTimer = null
let statusTimer = null

const trackPoints = computed(() => track.value.length)
const lastMessageText = computed(() => {
  if (!lastMessageAt.value) return ''
  const diff = Math.max(0, Math.floor((Date.now() - lastMessageAt.value) / 1000))
  if (diff < 60) return diff + ' 秒前'
  if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前'
  return Math.floor(diff / 3600) + ' 小时前'
})

const optionLabel = (item) => {
  let label = `${item.name || '未知船名'}（MMSI ${item.mmsi}`
  if (item.imo) label += ` · IMO ${item.imo}`
  return label + '）'
}

const formatSize = (length, breadth) => {
  if (!length && !breadth) return '—'
  const l = length ? length.toFixed(0) + ' m' : '?'
  const b = breadth ? breadth.toFixed(0) + ' m' : '?'
  return `${l} × ${b}`
}

const formatCoord = (value, positive, negative) => {
  if (value == null || Number.isNaN(Number(value))) return '—'
  const num = Number(value)
  const suffix = num >= 0 ? positive : negative
  return `${Math.abs(num).toFixed(5)}° ${suffix}`
}

const formatTime = (time) => {
  if (!time) return '—'
  const d = new Date(time)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const navStatusText = (status) => {
  const map = {
    0: '航行中', 1: '锚泊', 2: '失控', 3: '操纵受限', 4: '吃水受限',
    5: '靠泊', 6: '搁浅', 7: '捕捞作业', 8: '帆船航行',
    11: '拖曳航行', 12: '拖曳/顶推操作', 14: 'AIS-SART', 15: '未定义'
  }
  return status == null ? '—' : (map[status] || `状态 ${status}`)
}

const searchShips = async (keyword) => {
  searchKeyword.value = keyword || ''
  if (!keyword || !keyword.trim()) {
    searchResults.value = []
    return
  }
  searchLoading.value = true
  try {
    const res = await shipLocateApi.search(keyword.trim())
    searchResults.value = res.data || []
  } catch {
    searchResults.value = []
  } finally {
    searchLoading.value = false
  }
}

const onSelectShip = (mmsi) => {
  if (mmsi) selectShip(mmsi)
}

const locateInput = () => {
  const kw = (searchKeyword.value || '').trim()
  const mmsi = selectedMmsi.value || (kw && /^\d{9}$/.test(kw) ? kw : '')
  if (!mmsi) return
  selectedMmsi.value = mmsi
  selectShip(mmsi)
}

const selectShip = async (mmsi) => {
  fitDone = false
  try {
    const res = await shipLocateApi.locate(mmsi)
    const data = res.data
    if (!data) return
    shipInfo.value = data.ship || null
    latest.value = data.latest || null
    track.value = data.track || []
    drawShip()
  } catch {
    // 错误提示已由请求拦截器统一处理
  }
}

const clearShip = () => {
  selectedMmsi.value = ''
  shipInfo.value = null
  latest.value = null
  track.value = []
  clearMap()
}

const clearMap = () => {
  if (trackLayer) {
    trackLayer.remove()
    trackLayer = null
  }
  if (marker) {
    marker.remove()
    marker = null
  }
}

const initMap = () => {
  map = L.map(mapEl.value, { zoomControl: true }).setView([22, 120], 4)
  L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    subdomains: ['1', '2', '3', '4'],
    maxZoom: 18,
    attribution: '© 高德地图'
  }).addTo(map)
}

const drawShip = () => {
  if (!map) return
  const points = track.value
    .filter(p => p.lat != null && p.lon != null)
    .map(p => [p.lat, p.lon])

  if (trackLayer) {
    trackLayer.remove()
    trackLayer = null
  }
  if (points.length >= 2) {
    trackLayer = L.polyline(points, { color: '#1677ff', weight: 3, opacity: 0.8 }).addTo(map)
  }

  if (latest.value && latest.value.lat != null && latest.value.lon != null) {
    const latlng = [latest.value.lat, latest.value.lon]
    if (!marker) {
      const icon = L.divIcon({
        className: 'ship-marker',
        html: '<div class="ship-marker-dot"></div>',
        iconSize: [14, 14],
        iconAnchor: [7, 7]
      })
      marker = L.marker(latlng, { icon, zIndexOffset: 1000 }).addTo(map)
    } else {
      marker.setLatLng(latlng)
    }
    marker.setPopupContent(popupHtml())
  }

  if (!fitDone && points.length) {
    map.fitBounds(L.latLngBounds(points), { padding: [40, 40], maxZoom: 11 })
    fitDone = true
  }
}

const popupHtml = () => {
  const ship = shipInfo.value || {}
  const pos = latest.value || {}
  return `
    <div style="min-width:160px">
      <div style="font-weight:600;font-size:14px">${ship.name || '未知船名'}</div>
      <div style="font-size:12px;color:#666">MMSI ${ship.mmsi || '—'}${ship.imo ? ' · IMO ' + ship.imo : ''}</div>
      <div style="font-size:12px;color:#666;margin-top:4px">
        ${formatCoord(pos.lat, 'N', 'S')}，${formatCoord(pos.lon, 'E', 'W')}
      </div>
      <div style="font-size:12px;color:#666">
        ${pos.sog != null ? pos.sog.toFixed(1) + ' 节' : '—'} / ${pos.cog != null ? pos.cog.toFixed(0) + '°' : '—'} / ${navStatusText(pos.navStatus)}
      </div>
      <div style="font-size:12px;color:#999">${formatTime(pos.time)}</div>
    </div>
  `
}

const fitTrack = () => {
  fitDone = false
  drawShip()
}

const loadShip = async () => {
  if (!selectedMmsi.value || document.hidden) return
  try {
    const res = await shipLocateApi.locate(selectedMmsi.value)
    const data = res.data
    if (!data) return
    shipInfo.value = data.ship || shipInfo.value
    latest.value = data.latest || null
    track.value = data.track || []
    drawShip()
  } catch {
    // 轮询失败静默，等待下次
  }
}

const loadStatus = async () => {
  try {
    const res = await shipLocateApi.status()
    streamConnected.value = !!res.data?.connected
    trackedShips.value = res.data?.trackedShips || 0
    lastMessageAt.value = res.data?.lastMessageAt || 0
  } catch {
    // 静默
  }
}

onMounted(async () => {
  await nextTick()
  initMap()
  loadStatus()
  pollTimer = setInterval(loadShip, 5000)
  statusTimer = setInterval(loadStatus, 10000)
})

onUnmounted(() => {
  clearInterval(pollTimer)
  clearInterval(statusTimer)
  if (map) {
    map.remove()
    map = null
  }
})
</script>

<style scoped>
.ship-locate-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
}

.toolbar-card {
  flex: none;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.ship-select {
  width: 420px;
}

.status-box {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.status-text {
  font-size: 12px;
  color: #909399;
}

.content {
  flex: 1;
  display: flex;
  gap: 12px;
  min-height: 0;
}

.info-card {
  width: 320px;
  flex: none;
  overflow: auto;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ship-name {
  margin-bottom: 12px;
  font-size: 18px;
  font-weight: 600;
  color: #1d2129;
}

.ship-desc :deep(.el-descriptions__label) {
  width: 84px;
  color: #86909c;
}

.latest-row {
  display: flex;
  justify-content: space-between;
  padding: 5px 0;
  font-size: 13px;
  color: #1d2129;
}

.latest-row .label {
  color: #86909c;
}

.map-card {
  flex: 1;
  min-width: 0;
  position: relative;
  overflow: hidden;
}

.map-container {
  height: 100%;
  min-height: 440px;
}

.map-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
  pointer-events: none;
  z-index: 500;
  background: rgba(255, 255, 255, 0.7);
}

.option-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.option-name {
  font-weight: 600;
}

.option-mmsi {
  font-size: 12px;
  color: #86909c;
}

.option-type {
  font-size: 12px;
  color: #409eff;
  background: #ecf5ff;
  border-radius: 4px;
  padding: 0 6px;
}

.search-empty {
  padding: 8px 4px;
  font-size: 13px;
  color: #909399;
  text-align: center;
}

.search-empty small {
  color: #c0c4cc;
}

.map-card :deep(.ship-marker) {
  background: transparent;
  border: none;
}

.map-card :deep(.ship-marker-dot) {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #f56c6c;
  border: 3px solid #fff;
  box-shadow: 0 0 8px rgba(245, 108, 108, 0.9);
}

@media (max-width: 900px) {
  .content {
    flex-direction: column;
  }

  .info-card {
    width: 100%;
  }

  .ship-select {
    width: 100%;
  }
}
</style>

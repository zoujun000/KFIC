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
          <el-button v-if="latest" :icon="Aim" @click="fitTrack">居中</el-button>
        </div>
        <div class="status-box">
          <el-tag type="info" size="small" effect="light">ShipXY 船位查询</el-tag>
          <span class="status-text">选择船舶后显示最近一次 AIS 位置</span>
        </div>
      </div>
      <div v-if="recentShips.length" class="recent-searches">
        <span class="recent-label">最近定位</span>
        <el-tag
          v-for="item in recentShips"
          :key="item.mmsi"
          class="recent-ship"
          effect="plain"
          closable
          @click="locateRecentShip(item)"
          @close="removeRecentShip(item.mmsi)"
        >{{ item.name }} · {{ item.mmsi }}</el-tag>
      </div>
    </el-card>

    <div class="content">
      <!-- 船舶信息 -->
      <el-card shadow="never" class="info-card">
        <template #header>
          <div class="card-header">
            <span>船舶信息</span>
            <el-tag v-if="shipInfo" size="small" type="info" effect="plain">
              查询结果
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
          <el-divider />
          <div class="port-calls-header">
            <span>近 7 天挂靠记录</span>
            <el-button
              size="small"
              :loading="portCallsLoading"
              @click="loadPortCalls"
            >加载</el-button>
          </div>
          <template v-if="portCallsLoaded">
            <el-timeline v-if="portCalls.length" class="port-calls">
              <el-timeline-item
                v-for="(call, index) in portCalls"
                :key="`${call.portCode}-${call.ata}-${index}`"
                :timestamp="call.ata || call.arrivalAnchorage || '时间未知'"
                placement="top"
              >
                <div class="port-name">{{ call.portName || '未知港口' }}</div>
                <div class="port-meta">{{ [call.countryName, call.portCode].filter(Boolean).join(' · ') || '—' }}</div>
                <div v-if="call.terminalName || call.berthName" class="port-meta">
                  {{ [call.terminalName, call.berthName].filter(Boolean).join(' · ') }}
                </div>
                <div class="port-meta">离港 {{ call.atd || '—' }}{{ formatStayTime(call.stayTime) }}</div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="近 7 天无挂靠记录" :image-size="52" />
          </template>
        </template>
        <el-empty v-else description="请先搜索并选择船舶" :image-size="80" />
      </el-card>

      <!-- 地图 -->
      <el-card shadow="never" class="map-card">
        <div ref="mapEl" class="map-container"></div>
        <div v-if="!selectedMmsi" class="map-placeholder">
          <el-icon :size="42"><Ship /></el-icon>
          <p>输入船名或 MMSI，选择船舶后查看当前位置</p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
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
const portCalls = ref([])
const portCallsLoading = ref(false)
const portCallsLoaded = ref(false)
const RECENT_SHIPS_KEY = 'ship-locate-recent-ships'
const recentShips = ref(loadRecentShips())

let map = null
let marker = null

const optionLabel = (item) => {
  let label = `${item.name || '未知船名'}（MMSI ${item.mmsi}`
  if (item.imo) label += ` · IMO ${item.imo}`
  return label + '）'
}

function loadRecentShips() {
  try {
    const value = JSON.parse(localStorage.getItem(RECENT_SHIPS_KEY) || '[]')
    return Array.isArray(value) ? value.filter(item => /^\d{9}$/.test(item?.mmsi)).slice(0, 8) : []
  } catch {
    return []
  }
}

const saveRecentShips = () => {
  localStorage.setItem(RECENT_SHIPS_KEY, JSON.stringify(recentShips.value))
}

const addRecentShip = (ship, mmsi) => {
  const item = {
    mmsi: ship?.mmsi || mmsi,
    name: ship?.name || `MMSI ${mmsi}`
  }
  recentShips.value = [item, ...recentShips.value.filter(record => record.mmsi !== item.mmsi)].slice(0, 8)
  saveRecentShips()
}

const locateRecentShip = (item) => {
  selectedMmsi.value = item.mmsi
  searchKeyword.value = item.mmsi
  selectShip(item.mmsi)
}

const removeRecentShip = (mmsi) => {
  recentShips.value = recentShips.value.filter(item => item.mmsi !== mmsi)
  saveRecentShips()
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

const formatStayTime = (hours) => {
  if (hours == null) return ''
  return ` · 停留 ${Number(hours).toFixed(1)} 小时`
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
  try {
    const res = await shipLocateApi.locate(mmsi)
    const data = res.data
    if (!data) return
    shipInfo.value = data.ship || null
    latest.value = data.latest || null
    portCalls.value = []
    portCallsLoaded.value = false
    addRecentShip(data.ship, mmsi)
    drawShip()
  } catch {
    // 错误提示已由请求拦截器统一处理
  }
}

const loadPortCalls = async () => {
  if (!selectedMmsi.value) return
  portCallsLoading.value = true
  try {
    const res = await shipLocateApi.portCalls(selectedMmsi.value)
    portCalls.value = res.data || []
    portCallsLoaded.value = true
  } catch {
    portCalls.value = []
    portCallsLoaded.value = false
  } finally {
    portCallsLoading.value = false
  }
}

const clearShip = () => {
  selectedMmsi.value = ''
  shipInfo.value = null
  latest.value = null
  portCalls.value = []
  portCallsLoaded.value = false
  clearMap()
}

const clearMap = () => {
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
  if (latest.value && latest.value.lat != null && latest.value.lon != null) {
    const latlng = [latest.value.lat, latest.value.lon]
    const heading = Number(latest.value.heading ?? latest.value.cog)
    const rotation = Number.isFinite(heading) ? heading : 0
    if (!marker) {
      const icon = L.divIcon({
        className: 'ship-marker',
        html: `<span class="ship-marker-icon" style="transform:rotate(${rotation}deg)">&#9650;</span>`,
        iconSize: [28, 28],
        iconAnchor: [14, 14]
      })
      marker = L.marker(latlng, { icon, zIndexOffset: 1000 }).addTo(map)
    } else {
      marker.setLatLng(latlng)
    }
    marker.setPopupContent(popupHtml())
    map.setView(latlng, 8)
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
  if (latest.value?.lat != null && latest.value?.lon != null && map) {
    map.setView([latest.value.lat, latest.value.lon], Math.max(map.getZoom(), 8))
  }
}

onMounted(async () => {
  await nextTick()
  initMap()
})

onUnmounted(() => {
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

.recent-searches {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f2f5;
  flex-wrap: wrap;
}

.recent-label {
  font-size: 12px;
  color: #86909c;
}

.recent-ship {
  cursor: pointer;
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

.port-calls-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  font-weight: 600;
}

.port-calls {
  margin: 14px 0 0;
  padding-left: 4px;
}

.port-name {
  font-size: 13px;
  font-weight: 600;
  color: #1d2129;
}

.port-meta {
  margin-top: 3px;
  font-size: 12px;
  color: #86909c;
  line-height: 18px;
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

.map-card :deep(.ship-marker-icon) {
  display: block;
  color: #2563eb;
  font-size: 26px;
  line-height: 28px;
  text-align: center;
  text-shadow: 0 1px 0 #fff, 1px 0 0 #fff, 0 -1px 0 #fff, -1px 0 0 #fff;
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

<template>
  <div>
    <el-card style="margin-bottom:20px">
      <el-form :model="query" :inline="true">
        <el-form-item label="国家/地区">
          <el-select v-model="query.country" placeholder="选择国家" clearable filterable
            style="width:160px" @change="onCountryChange">
            <el-option v-for="c in countries" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="目的港">
          <el-select v-model="query.destination" placeholder="选择目的港" clearable filterable
            style="width:200px">
            <el-option v-for="d in quoteDestinations" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item label="货物体积">
          <el-input v-model="query.volume" placeholder="CBM"
            style="width:120px" @input="onVolumeInput" />
          <span style="margin-left:4px;color:#909399">CBM</span>
        </el-form-item>
        <el-form-item label="货物重量">
          <el-input v-model="query.weight" placeholder="KG"
            style="width:120px" @input="onWeightInput" />
          <span style="margin-left:4px;color:#909399">KG</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="searching" @click="doSearch">查询报价</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="results.length > 0 || searched">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>查询结果 <el-tag type="info" size="small">共 {{ total }} 条</el-tag></span>
          <span v-if="portSummary && portSummary.totalDisplay !== '—'" style="font-size:13px">
            目的港费用（{{ query.volume }}CBM<template v-if="query.weight"> · {{ query.weight }}KG</template>）：
            <b style="color:#e6a23c">{{ portSummary.totalDisplay }}</b>
          </span>
        </div>
      </template>

      <el-collapse v-if="portSummary && portSummary.items && portSummary.items.length" style="margin-bottom:16px">
        <el-collapse-item title="📋 目的港费用明细（点击展开）" name="1">
          <el-table :data="portSummary.items" size="small" stripe border>
            <el-table-column prop="feeNameCn" label="费项(中文)" width="110" />
            <el-table-column prop="feeNameEn" label="费项(英文)" min-width="140" />
            <el-table-column label="直客费用" width="150">
              <template #default="{ row }">
                <span v-if="row.amountDirectRaw">
                  {{ row.currency }} {{ row.amountDirectRaw }}
                  <el-tag size="small" type="info">{{ row.unitDirect }}</el-tag>
                </span>
                <span v-else style="color:#ccc">—</span>
              </template>
            </el-table-column>
            <el-table-column prop="remarks" label="备注" show-overflow-tooltip />
          </el-table>
          <div style="margin-top:10px;text-align:right;font-weight:600">
            合计（{{ query.volume }}CBM<template v-if="query.weight"> · {{ query.weight }}KG</template>）：
            <template v-for="(val, cur) in portSummary.totalByCurrency" :key="cur">
              <span style="color:#e6a23c;margin-left:12px">{{ cur }} {{ Number(val).toFixed(2) }}</span>
            </template>
          </div>
        </el-collapse-item>
      </el-collapse>

      <el-table :data="results" v-loading="searching" stripe border>
        <el-table-column prop="country" label="国家" width="100" />
        <el-table-column prop="destination" label="目的港" min-width="160" show-overflow-tooltip />
        <el-table-column prop="volumeRange" label="体积区间" width="120" />
        <el-table-column prop="via" label="中转" width="80" />
        <el-table-column label="乌冲 OF" width="100">
          <template #default="{ row }"><price-cell :val="row.ofWuchong" /></template>
        </el-table-column>
        <el-table-column label="北沙 OF" width="100">
          <template #default="{ row }"><price-cell :val="row.ofBeisha" /></template>
        </el-table-column>
        <el-table-column label="滘心/南沙" width="110">
          <template #default="{ row }"><price-cell :val="row.ofJiaoxin" /></template>
        </el-table-column>
        <el-table-column prop="transitTime" label="时效" width="80" />
        <el-table-column prop="carrier" label="船公司" width="110" show-overflow-tooltip />
        <el-table-column label="有效期" width="150">
          <template #default="{ row }">
            <span style="font-size:11px;color:#909399">{{ row.validFrom }} ~ {{ row.validTo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" :icon="CopyDocument"
              :loading="copyingRow === row.id" @click="copyQuote(row)">
              复制
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pagination"
        v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
        :total="total" :page-sizes="[20,50,100]"
        layout="total, sizes, prev, pager, next" @change="doSearch" />
    </el-card>

    <el-card v-else>
      <el-empty description="请选择目的港并输入体积后点击查询" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, defineComponent, h } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument } from '@element-plus/icons-vue'
import { quoteApi, portChargeApi } from '@/api'

const PriceCell = defineComponent({
  props: ['val'],
  setup(props) {
    return () => {
      if (!props.val) return h('span', { style: 'color:#ccc' }, '—')
      const n = parseFloat(props.val)
      const color = isNaN(n) ? '#606266' : n >= 0 ? '#67c23a' : '#f56c6c'
      return h('b', { style: `color:${color}` }, props.val)
    }
  }
})

const searching = ref(false)
const searched = ref(false)
const results = ref([])
const total = ref(0)
const portSummary = ref(null)
const countries = ref([])
const quoteDestinations = ref([])

const query = reactive({
  country: '', destination: '', volume: '1', weight: '', pageNum: 1, pageSize: 20
})

// 体积输入过滤：只允许数字和小数点
const onVolumeInput = (val) => {
  query.volume = val.replace(/[^\d.]/g, '').replace(/(\..*?)\..*/g, '$1')
}
// 重量输入过滤：只允许数字和小数点
const onWeightInput = (val) => {
  query.weight = val.replace(/[^\d.]/g, '').replace(/(\..*?)\..*/g, '$1')
}

const onCountryChange = async (val) => {
  query.destination = ''
  const res = await quoteApi.destinations(val || '')
  quoteDestinations.value = res.data
}

const doSearch = async () => {
  searching.value = true; searched.value = true; portSummary.value = null
  const vol = parseFloat(query.volume) || 1
  try {
    const [qRes, pRes] = await Promise.all([
      quoteApi.query({ ...query, volume: vol }),
      query.destination
        ? portChargeApi.calc(query.destination, vol).catch(() => null)
        : Promise.resolve(null)
    ])
    results.value = qRes.data.records
    total.value = qRes.data.total
    if (pRes) portSummary.value = pRes.data
  } finally { searching.value = false }
}

const resetQuery = () => {
  Object.assign(query, { country: '', destination: '', volume: '1', weight: '', pageNum: 1 })
  results.value = []; searched.value = false; portSummary.value = null
}

const copyingRow = ref(null)

const copyQuote = async (row) => {
  copyingRow.value = row.id
  const volume = parseFloat(query.volume) || 1

  // 从三个仓库OF中挑最便宜的价格，同时记下是哪个仓库
  const parseOf = (v) => {
    if (!v) return null
    const n = parseFloat(String(v).replace(/[^-\d.]/g, ''))
    return isNaN(n) ? null : n
  }
  const warehouses = [
    { name: '乌冲', of: parseOf(row.ofWuchong), firstLeg: row.wuchongFirstLeg, mother: row.wuchongMotherVessel },
    { name: '北沙', of: parseOf(row.ofBeisha), firstLeg: row.beishaFirstLeg, mother: row.beishaMotherVessel },
    { name: '滘心', of: parseOf(row.ofJiaoxin), firstLeg: row.jiaoxinFirstLeg, mother: row.jiaoxinMotherVessel }
  ].filter(w => w.of !== null)
  let cheapest = warehouses.length ? warehouses.reduce((a, b) => a.of < b.of ? a : b) : null
  const cheapestOf = cheapest?.of ?? null
  const of = cheapestOf !== null ? String(cheapestOf) : '—'

  // 船期 = 头程 + 大船
  const firstLeg = cheapest?.firstLeg || ''
  const mother = cheapest?.mother || ''
  const parts = []
  if (firstLeg) parts.push('头程' + firstLeg)
  if (mother) parts.push('大船' + mother)
  const scheduleText = parts.length ? parts.join('') : '—'

  // 提取港口名+仓库，匹配 dest_port_charge
  const parseDestForCharge = (dest) => {
    const lines = (dest || '').split('\n').map(l => l.trim()).filter(Boolean)
    let portParts = []
    let warehouse = ''
    for (const line of lines) {
      const whMatch = line.match(/[（(]([^，,)）]*?(?:直拼|仓))/)  // 懒惰匹配，防止吞掉"直拼"
      if (whMatch) {
        warehouse = whMatch[1].trim()
        // 仓库在同一行时，提取前面的港口名
        const before = line.substring(0, whMatch.index).replace(/[（(].*/, '').trim()
        if (before) portParts.push(before)
        break
      }
      portParts.push(line.replace(/[（(].*/, '').trim())
    }
    const port = portParts.join(' ').replace(/[/,]\s*$/, '').trim()
    return { port, warehouse, full: warehouse ? port + '(' + warehouse + ')' : port }
  }
  const destInfo = parseDestForCharge(row.destination)
  console.log('[copyQuote] destInfo:', destInfo, 'volume:', volume)
  let portSummaryData = null
  try {
    let pRes = await portChargeApi.calc(destInfo.full, volume)
    console.log('[copyQuote] full match result:', pRes?.data?.items?.length, 'items')
    portSummaryData = pRes.data
    // 尝试无括号格式（DB里可能存为 "BUSAN直拼" 而非 "BUSAN(乌冲直拼)"）
    if ((!portSummaryData?.items?.length) && destInfo.warehouse) {
      const noParen = destInfo.port + destInfo.warehouse
      console.log('[copyQuote] retry no-paren:', noParen)
      const pRes2 = await portChargeApi.calc(noParen, volume).catch(() => null)
      if (pRes2?.data?.items?.length) {
        portSummaryData = pRes2.data
      }
    }
    // 兜底：仓库匹配没结果，用纯港口名（只取第一部分）再查
    if ((!portSummaryData?.items?.length) && destInfo.warehouse) {
      const portFirst = destInfo.port.replace(/[,/].*/, '').replace(/\s+/g, ' ').trim()
      console.log('[copyQuote] fallback to port:', portFirst)
      pRes = await portChargeApi.calc(portFirst, volume)
      let items = pRes?.data?.items || []
      // 多条结果时，仓库含"直拼"则优先取直拼条目，排除 VIA/中转
      if (items.length > 1 && destInfo.warehouse.includes('直拼')) {
        const directItems = items.filter(i => i.destination && i.destination.includes('直拼'))
        if (directItems.length > 0) items = directItems
      }
      console.log('[copyQuote] fallback result:', items.length, 'items, dests:', [...new Set(items.map(i => i.destination))])
      // 安全检查：如果结果来自多个不同 destination（仓库），说明数据有冲突，不合并
      const destSet = new Set(items.map(i => i.destination))
      if (destSet.size <= 1) {
        portSummaryData = { ...pRes.data, items }
      } else {
        console.warn('[copyQuote] 多个仓库冲突，丢弃结果:', [...destSet])
      }
    }
  } catch (e) {
    console.error('[copyQuote] API error:', e)
    ElMessage.warning('目的港费用获取失败，请先上传费用表')
  }

  // 构建目的港费用明细
  let portDetailText = ''
  let portTotal = '—'
  if (portSummaryData?.items?.length) {
    portTotal = portSummaryData.totalDisplay || '—'
    const fmtNum = (n) => {
      if (n == null) return '—'
      return Number(n).toFixed(2)
    }
    const items = portSummaryData.items
      .filter(i => i.amountDirectRaw)
      .map(i => {
        const amt = i.amountDirect != null ? Number(i.amountDirect).toFixed(2) : '—'
        const unit = i.unitDirect ? `/${i.unitDirect}` : ''
        return `  ${i.feeNameCn || i.feeNameEn}: ${i.currency || ''} ${amt}${unit}`
      })
    if (items.length) {
      portDetailText = items.join('\n') + '\n'
    }
  }

  // CIF总价：最便宜OF×体积 + 文件费300 + 报关费300
  let cifTotal = '—'
  if (cheapestOf !== null) {
    cifTotal = `USD ${(cheapestOf * volume).toFixed(2)} + RMB 600(文件费+报关费)`
  } else {
    cifTotal = 'RMB 600(文件费+报关费)'
  }

  const text =
`广州    to   ${row.destination}
O/F 海运费: ${of}/CBM
DOC 文件费:RMB300/BL
CDF  单证报关:RMB 300/BL(六个品名一份报关费) 进仓费:RMB 100(办单司机现场给)
船期: ${scheduleText}
时效:开大船起 ${row.transitTime || '—'} 天到港
${volume}个方CIF总价: ${cifTotal}
目的港费用明细(${volume}CBM):
${portDetailText}目的港费用总价: ${portTotal}`

  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch (_) {
    const el = document.createElement('textarea')
    el.value = text; document.body.appendChild(el); el.select()
    document.execCommand('copy'); document.body.removeChild(el)
    ElMessage.success('已复制到剪贴板')
  } finally {
    copyingRow.value = null
  }
}

const loadDropdowns = async () => {
  const [cRes, dRes] = await Promise.all([quoteApi.countries(), quoteApi.destinations('')])
  countries.value = cRes.data
  quoteDestinations.value = dRes.data
}

onMounted(loadDropdowns)
</script>

<style scoped>
.pagination { margin-top:16px; justify-content:flex-end; }

/* ── 移动端适配 ── */
@media (max-width: 768px) {
  :deep(.el-form--inline) {
    display: flex;
    flex-direction: column;
  }
  :deep(.el-form--inline .el-form-item) {
    margin-right: 0;
    width: 100%;
  }
  :deep(.el-select), :deep(.el-input-number) {
    width: 100% !important;
  }
  :deep(.el-select .el-input) {
    width: 100%;
  }
  :deep(.el-button) {
    width: 100%;
    margin-left: 0 !important;
  }
  /* 两个并排按钮 */
  :deep(.el-form-item:has(.el-button + .el-button) .el-button) {
    width: calc(50% - 4px);
  }
  /* 表格横向滚动 */
  :deep(.el-table) {
    font-size: 12px;
  }
  :deep(.el-table th),
  :deep(.el-table td) {
    padding: 8px 4px;
  }
  /* 折叠面板 */
  :deep(.el-collapse-item__header) {
    font-size: 13px;
    padding: 10px 12px;
  }
  .pagination {
    justify-content: center;
    flex-wrap: wrap;
  }
}
</style>

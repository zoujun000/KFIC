<template>
  <div>
    <!-- 查询条件 -->
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
        <el-form-item label="仓库">
          <el-select v-model="query.warehouse" placeholder="全部仓库" clearable style="width:130px">
            <el-option label="全部" value="" />
            <el-option label="乌冲" value="乌冲" />
            <el-option label="北沙" value="北沙" />
            <el-option label="滘心/南沙" value="滘心" />
          </el-select>
        </el-form-item>
        <el-form-item label="客户类型">
          <el-radio-group v-model="query.clientType" size="small">
            <el-radio-button value="direct">直客</el-radio-button>
            <el-radio-button value="coload">同行</el-radio-button>
          </el-radio-group>
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
          <el-button type="success" :icon="Download" @click="downloadExcel" :disabled="results.length === 0">
            下载Excel
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 查询结果 -->
    <el-card v-if="results.length > 0 || searched">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px">
          <span>查询结果 <el-tag type="info" size="small">共 {{ total }} 条</el-tag></span>
          <span v-if="portSummary && portSummary.totalDisplay !== '—'" style="font-size:13px">
            目的港费用（{{ query.volume }}CBM<template v-if="query.weight"> · {{ query.weight }}KG</template>）
            · {{ query.clientType === 'direct' ? '直客' : '同行' }}：
            <b style="color:#e6a23c">{{ portSummary.totalDisplay }}</b>
          </span>
        </div>
      </template>

      <!-- 目的港费用明细 -->
      <el-collapse v-if="portSummary && portSummary.items && portSummary.items.length" style="margin-bottom:16px">
        <el-collapse-item title="📋 目的港费用明细（点击展开）" name="1">
          <el-table :data="portSummary.items" size="small" stripe border>
            <el-table-column prop="feeNameCn" label="费项(中文)" width="110" />
            <el-table-column prop="feeNameEn" label="费项(英文)" min-width="140" />
            <el-table-column :label="query.clientType === 'direct' ? '直客费用' : '同行费用'" width="150">
              <template #default="{ row }">
                <span v-if="query.clientType === 'direct' ? row.amountDirectRaw : row.amountColoadRaw">
                  {{ row.currency }}
                  {{ query.clientType === 'direct' ? row.amountDirectRaw : row.amountColoadRaw }}
                  <el-tag size="small" type="info">
                    {{ query.clientType === 'direct' ? row.unitDirect : row.unitCoload }}
                  </el-tag>
                </span>
                <span v-else style="color:#ccc">—</span>
              </template>
            </el-table-column>
            <el-table-column prop="remarks" label="备注" show-overflow-tooltip />
          </el-table>
          <div style="margin-top:10px;text-align:right;font-weight:600">
            合计（{{ query.volume }}CBM<template v-if="query.weight"> · {{ query.weight }}KG</template>）
            · {{ query.clientType === 'direct' ? '直客' : '同行' }}：
            <template v-for="(val, cur) in portSummary.totalByCurrency" :key="cur">
              <span style="color:#e6a23c;margin-left:12px">{{ cur }} {{ Number(val).toFixed(2) }}</span>
            </template>
          </div>
        </el-collapse-item>
      </el-collapse>

      <!-- 报价表格 -->
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
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" :icon="CopyDocument"
              :loading="copyingRow === row.id" @click="copyQuote(row)">
              复制
            </el-button>
            <el-button size="small" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-popconfirm title="确认删除？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button size="small" type="danger" :icon="Delete" />
              </template>
            </el-popconfirm>
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

    <!-- 编辑对话框 -->
    <el-dialog v-model="editVisible" title="编辑报价" width="600px" destroy-on-close>
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="国家">
          <el-input v-model="editForm.country" />
        </el-form-item>
        <el-form-item label="目的港">
          <el-input v-model="editForm.destination" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="体积区间">
          <el-input v-model="editForm.volumeRange" />
        </el-form-item>
        <el-form-item label="中转">
          <el-input v-model="editForm.via" />
        </el-form-item>
        <el-form-item label="乌冲 OF">
          <el-input v-model="editForm.ofWuchong" />
        </el-form-item>
        <el-form-item label="乌冲头程">
          <el-input v-model="editForm.wuchongFirstLeg" />
        </el-form-item>
        <el-form-item label="乌冲大船">
          <el-input v-model="editForm.wuchongMotherVessel" />
        </el-form-item>
        <el-form-item label="北沙 OF">
          <el-input v-model="editForm.ofBeisha" />
        </el-form-item>
        <el-form-item label="北沙头程">
          <el-input v-model="editForm.beishaFirstLeg" />
        </el-form-item>
        <el-form-item label="北沙大船">
          <el-input v-model="editForm.beishaMotherVessel" />
        </el-form-item>
        <el-form-item label="滘心 OF">
          <el-input v-model="editForm.ofJiaoxin" />
        </el-form-item>
        <el-form-item label="滘心头程">
          <el-input v-model="editForm.jiaoxinFirstLeg" />
        </el-form-item>
        <el-form-item label="滘心大船">
          <el-input v-model="editForm.jiaoxinMotherVessel" />
        </el-form-item>
        <el-form-item label="时效">
          <el-input v-model="editForm.transitTime" />
        </el-form-item>
        <el-form-item label="船公司">
          <el-input v-model="editForm.carrier" />
        </el-form-item>
        <el-form-item label="有效期从">
          <el-date-picker v-model="editForm.validFrom" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="有效期至">
          <el-date-picker v-model="editForm.validTo" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remarks" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, defineComponent, h } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument, Edit, Delete, Download } from '@element-plus/icons-vue'
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
const saving = ref(false)
const editVisible = ref(false)
const editingId = ref(null)
const editForm = reactive({
  country: '', destination: '', volumeRange: '', via: '',
  ofWuchong: '', wuchongFirstLeg: '', wuchongMotherVessel: '',
  ofBeisha: '', beishaFirstLeg: '', beishaMotherVessel: '',
  ofJiaoxin: '', jiaoxinFirstLeg: '', jiaoxinMotherVessel: '',
  transitTime: '', carrier: '', remarks: '',
  validFrom: '', validTo: ''
})

const query = reactive({
  country: '', destination: '', warehouse: '', clientType: 'direct',
  volume: '1', weight: '', pageNum: 1, pageSize: 20
})

const onVolumeInput = (val) => {
  query.volume = val.replace(/[^\d.]/g, '').replace(/(\..*?)\..*/g, '$1')
}
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
        ? portChargeApi.calc(query.destination, vol, query.clientType).catch(() => null)
        : Promise.resolve(null)
    ])
    results.value = qRes.data.records
    total.value = qRes.data.total

    if (pRes) portSummary.value = pRes.data
  } finally { searching.value = false }
}

const resetQuery = () => {
  Object.assign(query, {
    country: '', destination: '', warehouse: '', clientType: 'direct',
    volume: '1', weight: '', pageNum: 1
  })
  results.value = []; searched.value = false; portSummary.value = null
}

const copyingRow = ref(null)

// 根据选中的仓库解析 OF 值和船期
const getWarehouseInfo = (row) => {
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

  if (warehouses.length === 0) return null

  // 如果用户选择了仓库，优先用该仓库
  if (query.warehouse) {
    const selected = warehouses.find(w => w.name === query.warehouse)
    if (selected) return selected
  }

  // 否则选最便宜的
  return warehouses.reduce((a, b) => a.of < b.of ? a : b)
}

const copyQuote = async (row) => {
  copyingRow.value = row.id
  const volume = parseFloat(query.volume) || 1

  const wh = getWarehouseInfo(row)
  const cheapestOf = wh?.of ?? null
  const of = cheapestOf !== null ? String(cheapestOf) : '—'

  // 船期
  const firstLeg = wh?.firstLeg || ''
  const mother = wh?.mother || ''
  const parts = []
  if (firstLeg) parts.push('头程' + firstLeg)
  if (mother) parts.push('大船' + mother)
  const scheduleText = parts.length ? parts.join('') : '—'

  // 目的港费用
  const parseDestForCharge = (dest) => {
    const lines = (dest || '').split('\n').map(l => l.trim()).filter(Boolean)
    let portParts = []
    let warehouse = ''
    for (const line of lines) {
      const whMatch = line.match(/[（(]([^，,)）]*?(?:直拼|仓))/)
      if (whMatch) {
        warehouse = whMatch[1].trim()
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
  let portSummaryData = null
  try {
    let pRes = await portChargeApi.calc(destInfo.full, volume, query.clientType)
    portSummaryData = pRes.data
    if ((!portSummaryData?.items?.length) && destInfo.warehouse) {
      const noParen = destInfo.port + destInfo.warehouse
      const pRes2 = await portChargeApi.calc(noParen, volume, query.clientType).catch(() => null)
      if (pRes2?.data?.items?.length) portSummaryData = pRes2.data
    }
    if ((!portSummaryData?.items?.length) && destInfo.warehouse) {
      const portFirst = destInfo.port.replace(/[,/].*/, '').replace(/\s+/g, ' ').trim()
      pRes = await portChargeApi.calc(portFirst, volume, query.clientType)
      let items = pRes?.data?.items || []
      if (items.length > 1 && destInfo.warehouse.includes('直拼')) {
        const directItems = items.filter(i => i.destination && i.destination.includes('直拼'))
        if (directItems.length > 0) items = directItems
      }
      const destSet = new Set(items.map(i => i.destination))
      if (destSet.size <= 1) portSummaryData = { ...pRes.data, items }
    }
  } catch (e) {
    ElMessage.warning('目的港费用获取失败，请先上传费用表')
  }

  // 构建目的港费用明细
  const isDirect = query.clientType === 'direct'
  let portDetailText = ''
  let portTotal = '—'
  if (portSummaryData?.items?.length) {
    portTotal = portSummaryData.totalDisplay || '—'
    const items = portSummaryData.items
      .filter(i => isDirect ? i.amountDirectRaw : i.amountColoadRaw)
      .map(i => {
        const raw = isDirect ? i.amountDirectRaw : i.amountColoadRaw
        const unit = isDirect ? i.unitDirect : i.unitCoload
        const unitStr = unit ? `/${unit}` : ''
        return `${i.feeNameCn || i.feeNameEn}: ${i.currency || ''} ${raw || '—'}${unitStr}`
      })
    if (items.length) {
      portDetailText = items.join('\n') + '\n'
    }
  }

  // CIF总价
  let cifTotal = '—'
  if (cheapestOf !== null) {
    cifTotal = `USD ${(cheapestOf * volume).toFixed(2)} + CNY 600(文件费+报关费)`
  } else {
    cifTotal = 'CNY 600(文件费+报关费)'
  }

  const clientLabel = isDirect ? '直客' : '同行'
  const whName = wh ? wh.name : ''

  const text =
`广州${whName ? ' ' + whName : ''} - ${row.destination}
O/F 海运费: USD ${of}/RT
DOC 文件费:CNY 300/BL
CDF 单证报关:CNY 300/BL(六个品名一份报关费)
进仓费: CNY 100(办单司机现场给)
船期: ${scheduleText}
时效:开大船起 ${row.transitTime || '—'} 天到港
${volume}个方CIF总价: ${cifTotal}
备注 ：${row.remarks || ''}
目的港费用明细(${volume}CBM)[${clientLabel}]:
${portDetailText}目的港费用总价[${clientLabel}]: ${portTotal}`

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

// ===== 编辑功能 =====
const openEditDialog = (row) => {
  editingId.value = row.id
  Object.assign(editForm, {
    country: row.country || '',
    destination: row.destination || '',
    volumeRange: row.volumeRange || '',
    via: row.via || '',
    ofWuchong: row.ofWuchong || '',
    wuchongFirstLeg: row.wuchongFirstLeg || '',
    wuchongMotherVessel: row.wuchongMotherVessel || '',
    ofBeisha: row.ofBeisha || '',
    beishaFirstLeg: row.beishaFirstLeg || '',
    beishaMotherVessel: row.beishaMotherVessel || '',
    ofJiaoxin: row.ofJiaoxin || '',
    jiaoxinFirstLeg: row.jiaoxinFirstLeg || '',
    jiaoxinMotherVessel: row.jiaoxinMotherVessel || '',
    transitTime: row.transitTime || '',
    carrier: row.carrier || '',
    remarks: row.remarks || '',
    validFrom: row.validFrom || '',
    validTo: row.validTo || ''
  })
  editVisible.value = true
}

const handleSave = async () => {
  saving.value = true
  try {
    await quoteApi.update(editingId.value, { ...editForm })
    ElMessage.success('保存成功')
    editVisible.value = false
    doSearch()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleDelete = async (id) => {
  try {
    await quoteApi.delete(id)
    ElMessage.success('删除成功')
    doSearch()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

// ===== 下载 Excel =====
const downloadExcel = () => {
  if (results.value.length === 0) {
    ElMessage.warning('没有数据可下载')
    return
  }
  const rows = results.value
  const headers = [
    '国家', '目的港', '体积区间', '中转',
    '乌冲OF', '乌冲头程', '乌冲大船',
    '北沙OF', '北沙头程', '北沙大船',
    '滘心OF', '滘心头程', '滘心大船',
    '时效', '船公司', '有效期从', '有效期至', '备注'
  ]
  const fields = [
    'country', 'destination', 'volumeRange', 'via',
    'ofWuchong', 'wuchongFirstLeg', 'wuchongMotherVessel',
    'ofBeisha', 'beishaFirstLeg', 'beishaMotherVessel',
    'ofJiaoxin', 'jiaoxinFirstLeg', 'jiaoxinMotherVessel',
    'transitTime', 'carrier', 'validFrom', 'validTo', 'remarks'
  ]

  // 生成 HTML 表格（Excel 可以直接打开）
  let html = '<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel">'
  html += '<head><meta charset="UTF-8"><!--[if gte mso 9]><xml><x:ExcelWorkbook><x:ExcelWorksheets>'
  html += '<x:ExcelWorksheet><x:Name>散货报价</x:Name><x:WorksheetOptions><x:DisplayGridlines/></x:WorksheetOptions></x:ExcelWorksheet>'
  html += '</x:ExcelWorksheets></x:ExcelWorkbook></xml><![endif]--></head><body>'
  html += '<table border="1">'

  // 标题行
  html += '<tr><td colspan="' + headers.length + '" style="font-size:14px;font-weight:bold;text-align:center;background:#4472C4;color:#fff">'
  html += '崴航（广州）国际货运代理有限公司 - 散货报价表</td></tr>'

  // 查询条件行
  const conditions = []
  if (query.country) conditions.push('国家：' + query.country)
  if (query.destination) conditions.push('目的港：' + query.destination)
  if (query.warehouse) conditions.push('仓库：' + query.warehouse)
  conditions.push('客户类型：' + (query.clientType === 'direct' ? '直客' : '同行'))
  conditions.push('体积：' + query.volume + 'CBM')
  if (query.weight) conditions.push('重量：' + query.weight + 'KG')
  html += '<tr><td colspan="' + headers.length + '" style="font-size:11px;background:#D6E4F0">查询条件：' + conditions.join(' | ') + '</td></tr>'

  // 表头
  html += '<tr style="background:#4472C4;color:#fff;font-weight:bold">'
  headers.forEach(h => { html += '<td>' + h + '</td>' })
  html += '</tr>'

  // 数据行
  rows.forEach(row => {
    html += '<tr>'
    fields.forEach(f => {
      let val = row[f]
      if (val == null) val = ''
      html += '<td>' + String(val).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;') + '</td>'
    })
    html += '</tr>'
  })

  html += '</table></body></html>'

  const blob = new Blob(['\ufeff' + html], { type: 'application/vnd.ms-excel;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '散货报价_' + new Date().toISOString().slice(0, 10) + '.xls'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  ElMessage.success('下载成功')
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
  :deep(.el-form-item:has(.el-button + .el-button) .el-button) {
    width: calc(50% - 4px);
  }
  :deep(.el-table) {
    font-size: 12px;
  }
  :deep(.el-table th),
  :deep(.el-table td) {
    padding: 8px 4px;
  }
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

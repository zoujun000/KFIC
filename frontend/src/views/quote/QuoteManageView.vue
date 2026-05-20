<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span style="font-size:16px;font-weight:600">
          <el-icon color="#409EFF"><Goods /></el-icon>
          报价管理
        </span>
      </div>
    </template>

    <!-- 搜索 -->
    <el-form :inline="true" class="search-bar">
      <el-form-item label="目的港">
        <el-select v-model="selectedDest" placeholder="选择或输入目的港" filterable clearable
          style="width:320px" @change="loadData" :loading="destLoading">
          <el-option v-for="d in destinations" :key="d" :label="d" :value="d" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData" :disabled="!selectedDest">查询</el-button>
        <el-button type="success" :icon="Download" @click="downloadExcel" :disabled="tableData.length === 0">
          下载Excel
        </el-button>
        <el-button :icon="Download" @click="downloadAll">
          下载全部
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 表格 -->
    <el-table :data="tableData" v-loading="loading" stripe border style="margin-top:12px"
      :header-cell-style="{ background:'#fafafa', color:'#606266', fontWeight:600 }">
      <el-table-column prop="country" label="国家" width="90" />
      <el-table-column prop="destination" label="目的港" min-width="150" show-overflow-tooltip />
      <el-table-column prop="portCode" label="代码" width="65" />
      <el-table-column prop="volumeRange" label="体积区间" width="110" />
      <el-table-column prop="via" label="中转" width="70" />
      <el-table-column label="乌冲OF" width="90" align="right">
        <template #default="{ row }">{{ row.ofWuchong || '—' }}</template>
      </el-table-column>
      <el-table-column label="北沙OF" width="90" align="right">
        <template #default="{ row }">{{ row.ofBeisha || '—' }}</template>
      </el-table-column>
      <el-table-column label="滘心OF" width="90" align="right">
        <template #default="{ row }">{{ row.ofJiaoxin || '—' }}</template>
      </el-table-column>
      <el-table-column prop="transitTime" label="时效" width="70" />
      <el-table-column prop="carrier" label="船公司" width="100" show-overflow-tooltip />
      <el-table-column label="有效期" width="150">
        <template #default="{ row }">
          <span style="font-size:11px;color:#909399">{{ row.validFrom }} ~ {{ row.validTo }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确认删除？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger" link>删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="!loading && selectedDest && tableData.length === 0" style="text-align:center;padding:60px 0;color:#c0c4cc">
      <el-empty description="该目的港暂无报价数据">
        <template #image>
          <el-icon size="60" color="#c0c4cc"><Goods /></el-icon>
        </template>
      </el-empty>
    </div>

    <!-- 编辑对话框 -->
    <el-dialog v-model="editVisible" title="编辑报价" width="650px" destroy-on-close>
      <el-form :model="editForm" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="国家">
              <el-input v-model="editForm.country" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="体积区间">
              <el-input v-model="editForm.volumeRange" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="目的港">
          <el-input v-model="editForm.destination" type="textarea" :rows="2" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="中转">
              <el-input v-model="editForm.via" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="时效">
              <el-input v-model="editForm.transitTime" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="船公司">
              <el-input v-model="editForm.carrier" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">乌冲仓库</el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="OF">
              <el-input v-model="editForm.ofWuchong" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="头程">
              <el-input v-model="editForm.wuchongFirstLeg" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="大船">
              <el-input v-model="editForm.wuchongMotherVessel" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">北沙仓库</el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="OF">
              <el-input v-model="editForm.ofBeisha" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="头程">
              <el-input v-model="editForm.beishaFirstLeg" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="大船">
              <el-input v-model="editForm.beishaMotherVessel" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">滘心/南沙仓库</el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="OF">
              <el-input v-model="editForm.ofJiaoxin" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="头程">
              <el-input v-model="editForm.jiaoxinFirstLeg" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="大船">
              <el-input v-model="editForm.jiaoxinMotherVessel" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="有效期从">
              <el-date-picker v-model="editForm.validFrom" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="有效期至">
              <el-date-picker v-model="editForm.validTo" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="editForm.remarks" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Goods, Download } from '@element-plus/icons-vue'
import { quoteApi } from '@/api'

const destinations = ref([])
const selectedDest = ref('')
const tableData = ref([])
const loading = ref(false)
const destLoading = ref(false)
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

// 加载目的港列表
const loadDests = async () => {
  destLoading.value = true
  try {
    const res = await quoteApi.destinations('')
    destinations.value = res.data
  } finally { destLoading.value = false }
}

// 查询
const loadData = async () => {
  if (!selectedDest.value) return
  loading.value = true
  try {
    const res = await quoteApi.byDestination(selectedDest.value)
    tableData.value = res.data
  } finally { loading.value = false }
}

// 打开编辑
const openEdit = (row) => {
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

// 保存
const handleSave = async () => {
  saving.value = true
  try {
    await quoteApi.update(editingId.value, { ...editForm })
    ElMessage.success('保存成功')
    editVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 删除
const handleDelete = async (id) => {
  try {
    await quoteApi.delete(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

// 下载 Excel（当前筛选数据）
const downloadExcel = () => {
  if (!tableData.value.length) return ElMessage.warning('没有数据')
  generateExcel(tableData.value, '目的港：' + selectedDest.value)
}

// 下载全部数据库数据（直接请求后端生成Excel，避免cpolar截断大JSON）
const downloadAll = async () => {
  try {
    ElMessage.info('正在导出全部数据...')
    const token = localStorage.getItem('token')
    const resp = await fetch('/api/quotes/export', {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    if (!resp.ok) throw new Error('导出失败')
    const blob = await resp.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '散货报价_全部_' + new Date().toISOString().slice(0, 10) + '.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('下载成功')
  } catch { ElMessage.error('导出失败') }
}

const generateExcel = (rows, subtitle) => {
  const headers = [
    '国家', '目的港', '代码', '体积区间', '中转',
    '乌冲OF', '乌冲头程', '乌冲大船',
    '北沙OF', '北沙头程', '北沙大船',
    '滘心OF', '滘心头程', '滘心大船',
    '时效', '船公司', '有效期从', '有效期至', '备注'
  ]
  const fields = [
    'country', 'destination', 'portCode', 'volumeRange', 'via',
    'ofWuchong', 'wuchongFirstLeg', 'wuchongMotherVessel',
    'ofBeisha', 'beishaFirstLeg', 'beishaMotherVessel',
    'ofJiaoxin', 'jiaoxinFirstLeg', 'jiaoxinMotherVessel',
    'transitTime', 'carrier', 'validFrom', 'validTo', 'remarks'
  ]

  let html = '<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel">'
  html += '<head><meta charset="UTF-8"><!--[if gte mso 9]><xml><x:ExcelWorkbook><x:ExcelWorksheets>'
  html += '<x:ExcelWorksheet><x:Name>散货报价</x:Name><x:WorksheetOptions><x:DisplayGridlines/></x:WorksheetOptions></x:ExcelWorksheet>'
  html += '</x:ExcelWorksheets></x:ExcelWorkbook></xml><![endif]--></head><body>'
  html += '<table border="1">'
  html += '<tr><td colspan="' + headers.length + '" style="font-size:14px;font-weight:bold;text-align:center;background:#4472C4;color:#fff">崴航（广州）国际货运代理有限公司 - 散货报价表</td></tr>'
  html += '<tr><td colspan="' + headers.length + '" style="font-size:12px;background:#D6E4F0;font-weight:bold">' + subtitle + '</td></tr>'
  html += '<tr style="background:#4472C4;color:#fff;font-weight:bold">'
  headers.forEach(h => { html += '<td>' + h + '</td>' })
  html += '</tr>'
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
  const a = document.createElement('a'); a.href = url
  a.download = '散货报价_' + selectedDest.value.replace(/[\\/:*?"<>|]/g, '_') + '_' + new Date().toISOString().slice(0, 10) + '.xls'
  document.body.appendChild(a); a.click(); document.body.removeChild(a)
  URL.revokeObjectURL(url)
  ElMessage.success('下载成功')
}

onMounted(loadDests)
</script>

<style scoped>
.search-bar { background: #fafafa; padding: 16px 16px 0; border-radius: 6px; margin-bottom: 12px; }

@media (max-width: 768px) {
  :deep(.el-form--inline) { flex-direction: column; }
  :deep(.el-form--inline .el-form-item) { margin-right: 0; width: 100%; }
  :deep(.el-select), :deep(.el-input) { width: 100% !important; }
}
</style>

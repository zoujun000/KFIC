<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span style="font-size:16px;font-weight:600">
          <el-icon color="#67c23a"><Ship /></el-icon>
          目的港费用管理
        </span>
      </div>
    </template>

    <!-- 搜索 -->
    <el-form :inline="true" class="search-bar">
      <el-form-item label="国家/地区">
        <el-select v-model="selectedCountry" placeholder="全部国家" clearable filterable
          style="width:160px" @change="onCountryChange" :loading="countryLoading">
          <el-option v-for="c in countries" :key="c" :label="c" :value="c" />
        </el-select>
      </el-form-item>
      <el-form-item label="目的港">
        <el-select v-model="selectedDest" placeholder="选择或输入目的港" filterable clearable
          style="width:280px" @change="loadData" :loading="destLoading">
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

    <!-- 操作按钮栏 -->
    <div v-if="selectedDest" style="margin-bottom:8px;display:flex;gap:8px">
      <el-button size="small" type="success" @click="openAdd">+ 添加费项</el-button>
      <template v-if="tableData.length && editAllMode">
        <el-button size="small" type="primary" :loading="savingAll" @click="saveAll">保存全部</el-button>
        <el-button size="small" @click="cancelEditAll">取消</el-button>
      </template>
      <template v-else-if="tableData.length">
        <el-button size="small" type="warning" @click="enterEditAll">编辑全部</el-button>
      </template>
    </div>
    <el-table :data="tableData" v-loading="loading" stripe border style="margin-top:12px"
      :header-cell-style="{ background:'#fafafa', color:'#606266', fontWeight:600 }">
      <el-table-column prop="feeNameCn" label="中文费项" width="140">
        <template #default="{ row }">
          <el-input v-if="editAllMode || editingId === row.id" v-model="(editAllMode ? getEditData(row.id) : editForm).feeNameCn" size="small" />
          <span v-else>{{ row.feeNameCn || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="feeNameEn" label="英文费项" min-width="160">
        <template #default="{ row }">
          <el-input v-if="editAllMode || editingId === row.id" v-model="(editAllMode ? getEditData(row.id) : editForm).feeNameEn" size="small" />
          <span v-else>{{ row.feeNameEn || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="货币" width="90">
        <template #default="{ row }">
          <el-select v-if="editAllMode || editingId === row.id" v-model="(editAllMode ? getEditData(row.id) : editForm).currency" size="small" style="width:80px"
            filterable allow-create>
            <el-option v-for="c in currencies" :key="c" :label="c" :value="c" />
          </el-select>
          <span v-else>{{ row.currency || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="amountDirect" label="直客金额" width="120" align="right">
        <template #default="{ row }">
          <el-input v-if="editAllMode || editingId === row.id" v-model="(editAllMode ? getEditData(row.id) : editForm).amountDirect" size="small" style="width:100px" />
          <span v-else>{{ row.amountDirect != null ? Number(row.amountDirect).toFixed(2) : '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="unitDirect" label="单位" width="80">
        <template #default="{ row }">
          <el-select v-if="editAllMode || editingId === row.id" v-model="(editAllMode ? getEditData(row.id) : editForm).unitDirect" size="small" style="width:70px" clearable filterable allow-create>
            <el-option v-for="u in units" :key="u" :label="u" :value="u" />
          </el-select>
          <span v-else>{{ row.unitDirect || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="amountCoload" label="同行金额" width="120" align="right">
        <template #default="{ row }">
          <el-input v-if="editAllMode || editingId === row.id" v-model="(editAllMode ? getEditData(row.id) : editForm).amountCoload" size="small" style="width:100px" />
          <span v-else>{{ row.amountCoload != null ? Number(row.amountCoload).toFixed(2) : '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="unitCoload" label="同行单位" width="80">
        <template #default="{ row }">
          <el-select v-if="editAllMode || editingId === row.id" v-model="(editAllMode ? getEditData(row.id) : editForm).unitCoload" size="small" style="width:70px" clearable filterable allow-create>
            <el-option v-for="u in units" :key="u" :label="u" :value="u" />
          </el-select>
          <span v-else>{{ row.unitCoload || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="remarks" label="备注" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          <el-input v-if="editAllMode || editingId === row.id" v-model="(editAllMode ? getEditData(row.id) : editForm).remarks" size="small" />
          <span v-else>{{ row.remarks || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="!editAllMode" label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <template v-if="editingId === row.id">
            <el-button size="small" type="primary" @click="handleSave(row)">保存</el-button>
            <el-button size="small" @click="cancelEdit">取消</el-button>
          </template>
          <template v-else>
            <el-button size="small" type="primary" link @click="startEdit(row)">编辑</el-button>
            <el-popconfirm title="确认删除？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button size="small" type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="!loading && selectedDest && tableData.length === 0" style="text-align:center;padding:60px 0;color:#c0c4cc">
      <el-empty description="该目的港暂无费用数据" />
    </div>

    <!-- 新增费项弹窗 -->
    <el-dialog v-model="addVisible" title="新增费项" width="520px">
      <el-form :model="addForm" label-width="90px">
        <el-form-item label="目的港">
          <el-input v-model="addForm.destination" disabled />
        </el-form-item>
        <el-form-item label="中文费项">
          <el-input v-model="addForm.feeNameCn" placeholder="如：码头操作费" />
        </el-form-item>
        <el-form-item label="英文费项">
          <el-input v-model="addForm.feeNameEn" placeholder="如：THC" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="货币" label-width="70px">
              <el-select v-model="addForm.currency" filterable allow-create default-first-option>
                <el-option v-for="c in currencies" :key="c" :label="c" :value="c" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="直客金额" label-width="80px">
              <el-input v-model="addForm.amountDirect" placeholder="0.00" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="直客单位" label-width="70px">
              <el-select v-model="addForm.unitDirect" filterable allow-create>
                <el-option v-for="u in units" :key="u" :label="u" :value="u" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="同行金额" label-width="80px">
              <el-input v-model="addForm.amountCoload" placeholder="0.00" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="同行单位" label-width="90px">
          <el-select v-model="addForm.unitCoload" filterable allow-create style="width:100%">
            <el-option v-for="u in units" :key="u" :label="u" :value="u" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="addForm.remarks" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="adding" @click="handleAdd">添加</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Ship, Download } from '@element-plus/icons-vue'
import { portChargeApi } from '@/api'

const destinations = ref([])
const selectedDest = ref('')
const countries = ref([])
const selectedCountry = ref('')
const countryLoading = ref(false)
const tableData = ref([])
const loading = ref(false)
const destLoading = ref(false)
const adding = ref(false)
const addVisible = ref(false)
const addForm = reactive({
  destination: '', feeNameCn: '', feeNameEn: '', currency: 'USD',
  amountDirect: '', unitDirect: 'WM',
  amountCoload: '', unitCoload: 'WM', remarks: ''
})
const editingId = ref(null)
const editForm = reactive({
  feeNameCn: '', feeNameEn: '', currency: '',
  amountDirect: '', unitDirect: '',
  amountCoload: '', unitCoload: '', remarks: ''
})

// 编辑全部模式
const editAllMode = ref(false)
const savingAll = ref(false)
const editAllData = ref({})

const getEditData = (id) => {
  if (!editAllData.value[id]) {
    const row = tableData.value.find(r => r.id === id)
    if (row) {
      editAllData.value[id] = reactive({
        feeNameCn: row.feeNameCn || '',
        feeNameEn: row.feeNameEn || '',
        currency: row.currency || '',
        amountDirect: row.amountDirect != null ? String(row.amountDirect) : '',
        unitDirect: row.unitDirect || '',
        amountCoload: row.amountCoload != null ? String(row.amountCoload) : '',
        unitCoload: row.unitCoload || '',
        remarks: row.remarks || ''
      })
    }
  }
  return editAllData.value[id]
}

const enterEditAll = () => {
  editAllData.value = {}
  editAllMode.value = true
  editingId.value = null
}

const cancelEditAll = () => {
  editAllMode.value = false
  editAllData.value = {}
}

const saveAll = async () => {
  savingAll.value = true
  let success = 0, fail = 0
  for (const row of tableData.value) {
    const data = editAllData.value[row.id]
    if (!data) continue
    const payload = {
      id: row.id,
      destination: row.destination,
      feeNameCn: data.feeNameCn || null,
      feeNameEn: data.feeNameEn || null,
      currency: data.currency || null,
      amountDirect: data.amountDirect ? Number(data.amountDirect) : null,
      amountDirectRaw: data.amountDirect || null,
      unitDirect: data.unitDirect || null,
      amountCoload: data.amountCoload ? Number(data.amountCoload) : null,
      amountColoadRaw: data.amountCoload || null,
      unitCoload: data.unitCoload || null,
      remarks: data.remarks || null
    }
    try {
      await portChargeApi.update(row.id, payload)
      success++
    } catch { fail++ }
  }
  savingAll.value = false
  editAllMode.value = false
  editAllData.value = {}
  if (fail === 0) ElMessage.success(`成功更新 ${success} 条`)
  else ElMessage.warning(`成功 ${success} 条，失败 ${fail} 条`)
  loadData()
}

const currencies = ['USD', 'EUR', 'HKD', 'SGD', 'THB', 'AED', 'KRW', 'NTD', 'GBP', 'AUD', 'CAD', 'ZAR', 'BRL', 'MOP', 'TWD', 'RMB', 'JPY']
const units = ['WM', 'BL', 'SET', 'TON', 'BLOCK', '100KG']

const loadCountries = async () => {
  countryLoading.value = true
  try {
    const res = await portChargeApi.countries()
    countries.value = res.data
  } finally { countryLoading.value = false }
}

const loadDests = async (country) => {
  destLoading.value = true; selectedDest.value = ''
  try {
    const res = await portChargeApi.destinations(country || '')
    destinations.value = res.data
  } finally { destLoading.value = false }
}

const onCountryChange = (val) => {
  loadDests(val || '')
}

const loadData = async () => {
  if (!selectedDest.value) return
  loading.value = true; editingId.value = null
  try {
    const res = await portChargeApi.list(selectedDest.value)
    tableData.value = res.data
  } finally { loading.value = false }
}

const startEdit = (row) => {
  editingId.value = row.id
  Object.assign(editForm, {
    feeNameCn: row.feeNameCn || '',
    feeNameEn: row.feeNameEn || '',
    currency: row.currency || '',
    amountDirect: row.amountDirect != null ? String(row.amountDirect) : '',
    unitDirect: row.unitDirect || '',
    amountCoload: row.amountCoload != null ? String(row.amountCoload) : '',
    unitCoload: row.unitCoload || '',
    remarks: row.remarks || ''
  })
}

const cancelEdit = () => { editingId.value = null }

const handleSave = async (row) => {
  const payload = {
    id: row.id,
    destination: row.destination,
    feeNameCn: editForm.feeNameCn || null,
    feeNameEn: editForm.feeNameEn || null,
    currency: editForm.currency || null,
    amountDirect: editForm.amountDirect ? Number(editForm.amountDirect) : null,
    amountDirectRaw: editForm.amountDirect || null,
    unitDirect: editForm.unitDirect || null,
    amountCoload: editForm.amountCoload ? Number(editForm.amountCoload) : null,
    amountColoadRaw: editForm.amountCoload || null,
    unitCoload: editForm.unitCoload || null,
    remarks: editForm.remarks || null
  }
  try {
    await portChargeApi.update(row.id, payload)
    ElMessage.success('保存成功')
    editingId.value = null
    loadData()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const handleDelete = async (id) => {
  try {
    await portChargeApi.delete(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const openAdd = () => {
  addForm.destination = selectedDest.value
  addForm.feeNameCn = ''; addForm.feeNameEn = ''
  addForm.currency = 'USD'; addForm.amountDirect = ''
  addForm.unitDirect = 'WM'; addForm.amountCoload = ''
  addForm.unitCoload = 'WM'; addForm.remarks = ''
  addVisible.value = true
}

const handleAdd = async () => {
  adding.value = true
  try {
    await portChargeApi.create({
      destination: addForm.destination,
      feeNameCn: addForm.feeNameCn,
      feeNameEn: addForm.feeNameEn || null,
      currency: addForm.currency || 'USD',
      amountDirect: addForm.amountDirect ? Number(addForm.amountDirect) : null,
      amountDirectRaw: addForm.amountDirect || null,
      unitDirect: addForm.unitDirect || null,
      amountCoload: addForm.amountCoload ? Number(addForm.amountCoload) : null,
      amountColoadRaw: addForm.amountCoload || null,
      unitCoload: addForm.unitCoload || null,
      remarks: addForm.remarks || null
    })
    ElMessage.success('添加成功')
    addVisible.value = false
    loadData()
  } catch {
    ElMessage.error('添加失败')
  } finally {
    adding.value = false
  }
}

const exportHeaders = ['中文费项', '英文费项', '货币', '直客金额', '直客单位', '同行金额', '同行单位', '备注']
const exportFields = ['feeNameCn', 'feeNameEn', 'currency', 'amountDirect', 'unitDirect', 'amountCoload', 'unitCoload', 'remarks']

// 生成Excel并下载
const doExport = (rows, headers, fields, title, subtitle, filename) => {
  let html = '<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel">'
  html += '<head><meta charset="UTF-8"><!--[if gte mso 9]><xml><x:ExcelWorkbook><x:ExcelWorksheets>'
  html += '<x:ExcelWorksheet><x:Name>目的港费用</x:Name><x:WorksheetOptions><x:DisplayGridlines/></x:WorksheetOptions></x:ExcelWorksheet>'
  html += '</x:ExcelWorksheets></x:ExcelWorkbook></xml><![endif]--></head><body>'
  html += '<table border="1">'
  html += '<tr><td colspan="' + headers.length + '" style="font-size:14px;font-weight:bold;text-align:center;background:#C00000;color:#fff">' + title + '</td></tr>'
  html += '<tr><td colspan="' + headers.length + '" style="font-size:12px;background:#FCE4D6;font-weight:bold">' + subtitle + '</td></tr>'
  html += '<tr style="background:#C00000;color:#fff;font-weight:bold">'
  headers.forEach(h => { html += '<td>' + h + '</td>' })
  html += '</tr>'
  rows.forEach(row => {
    html += '<tr>'
    fields.forEach(f => {
      let val = row[f]
      if (val == null) val = ''
      if ((f === 'amountDirect' || f === 'amountCoload') && val !== '') val = Number(val).toFixed(2)
      html += '<td>' + String(val).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;') + '</td>'
    })
    html += '</tr>'
  })
  html += '</table></body></html>'
  const blob = new Blob(['\ufeff' + html], { type: 'application/vnd.ms-excel;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a'); a.href = url
  a.download = filename + '_' + new Date().toISOString().slice(0, 10) + '.xls'
  document.body.appendChild(a); a.click(); document.body.removeChild(a)
  URL.revokeObjectURL(url)
  ElMessage.success('下载成功')
}

// 下载Excel（当前筛选数据）
const downloadExcel = () => {
  if (!tableData.value.length) return ElMessage.warning('没有数据')
  doExport(tableData.value, exportHeaders, exportFields,
    '崴航（广州）国际货运代理有限公司 - 目的港费用表',
    '目的港：' + selectedDest.value,
    '目的港费用_' + selectedDest.value.replace(/[\\/:*?"<>|]/g, '_'))
}

// 下载全部数据库数据（直接请求后端生成Excel，避免cpolar截断大JSON）
const downloadAll = async () => {
  try {
    ElMessage.info('正在导出全部数据...')
    const token = localStorage.getItem('token')
    const resp = await fetch('/api/port-charges/export', {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    if (!resp.ok) throw new Error('导出失败')
    const blob = await resp.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '目的港费用_全部_' + new Date().toISOString().slice(0, 10) + '.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('下载成功')
  } catch { ElMessage.error('导出失败') }
}

onMounted(() => { loadCountries(); loadDests() })
</script>

<style scoped>
.search-bar { background: #fafafa; padding: 16px 16px 0; border-radius: 6px; margin-bottom: 12px; }

@media (max-width: 768px) {
  :deep(.el-form--inline) { flex-direction: column; }
  :deep(.el-form--inline .el-form-item) { margin-right: 0; width: 100%; }
  :deep(.el-select), :deep(.el-input) { width: 100% !important; }
}
</style>

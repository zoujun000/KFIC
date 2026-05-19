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
      <el-form-item label="目的港">
        <el-select v-model="selectedDest" placeholder="选择或输入目的港" filterable clearable
          style="width:320px" @change="loadData" :loading="destLoading">
          <el-option v-for="d in destinations" :key="d" :label="d" :value="d" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData" :disabled="!selectedDest">查询</el-button>
        <el-button type="success" :icon="Download" @click="downloadExcel">
          下载Excel(全部)
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 表格 -->
    <div v-if="tableData.length" style="margin-bottom:8px">
      <el-button size="small" type="warning" @click="openBatchEdit" :disabled="selectedRows.length === 0">
        批量修改 ({{ selectedRows.length }})
      </el-button>
    </div>
    <el-table :data="tableData" v-loading="loading" stripe border
      @selection-change="onSelectionChange" ref="tableRef">
      <el-table-column type="selection" width="40" /> style="margin-top:12px"
      :header-cell-style="{ background:'#fafafa', color:'#606266', fontWeight:600 }">
      <el-table-column prop="feeNameCn" label="中文费项" width="140">
        <template #default="{ row }">
          <el-input v-if="editingId === row.id" v-model="editForm.feeNameCn" size="small" />
          <span v-else>{{ row.feeNameCn || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="feeNameEn" label="英文费项" min-width="160">
        <template #default="{ row }">
          <el-input v-if="editingId === row.id" v-model="editForm.feeNameEn" size="small" />
          <span v-else>{{ row.feeNameEn || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="货币" width="90">
        <template #default="{ row }">
          <el-select v-if="editingId === row.id" v-model="editForm.currency" size="small" style="width:80px"
            filterable allow-create>
            <el-option v-for="c in currencies" :key="c" :label="c" :value="c" />
          </el-select>
          <span v-else>{{ row.currency || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="amountDirect" label="直客金额" width="120" align="right">
        <template #default="{ row }">
          <el-input v-if="editingId === row.id" v-model="editForm.amountDirect" size="small" style="width:100px" />
          <span v-else>{{ row.amountDirect != null ? Number(row.amountDirect).toFixed(2) : '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="unitDirect" label="单位" width="80">
        <template #default="{ row }">
          <el-select v-if="editingId === row.id" v-model="editForm.unitDirect" size="small" style="width:70px" clearable>
            <el-option v-for="u in units" :key="u" :label="u" :value="u" />
          </el-select>
          <span v-else>{{ row.unitDirect || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="amountCoload" label="同行金额" width="120" align="right">
        <template #default="{ row }">
          <el-input v-if="editingId === row.id" v-model="editForm.amountCoload" size="small" style="width:100px" />
          <span v-else>{{ row.amountCoload != null ? Number(row.amountCoload).toFixed(2) : '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="unitCoload" label="同行单位" width="80">
        <template #default="{ row }">
          <el-select v-if="editingId === row.id" v-model="editForm.unitCoload" size="small" style="width:70px" clearable>
            <el-option v-for="u in units" :key="u" :label="u" :value="u" />
          </el-select>
          <span v-else>{{ row.unitCoload || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="remarks" label="备注" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          <el-input v-if="editingId === row.id" v-model="editForm.remarks" size="small" />
          <span v-else>{{ row.remarks || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
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

    <!-- 批量修改对话框 -->
    <el-dialog v-model="batchVisible" title="批量修改" width="500px">
      <el-form :model="batchForm" label-width="80px">
        <el-form-item label="修改字段">
          <el-checkbox-group v-model="batchForm.fields">
            <el-checkbox label="currency">货币</el-checkbox>
            <el-checkbox label="amountDirect">直客金额</el-checkbox>
            <el-checkbox label="unitDirect">直客单位</el-checkbox>
            <el-checkbox label="amountCoload">同行金额</el-checkbox>
            <el-checkbox label="unitCoload">同行单位</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item v-if="batchForm.fields.includes('currency')" label="货币">
          <el-select v-model="batchForm.currency" filterable allow-create style="width:120px">
            <el-option v-for="c in currencies" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="batchForm.fields.includes('amountDirect')" label="直客金额">
          <el-input v-model="batchForm.amountDirect" style="width:120px" />
        </el-form-item>
        <el-form-item v-if="batchForm.fields.includes('unitDirect')" label="直客单位">
          <el-select v-model="batchForm.unitDirect" clearable style="width:120px">
            <el-option v-for="u in units" :key="u" :label="u" :value="u" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="batchForm.fields.includes('amountCoload')" label="同行金额">
          <el-input v-model="batchForm.amountCoload" style="width:120px" />
        </el-form-item>
        <el-form-item v-if="batchForm.fields.includes('unitCoload')" label="同行单位">
          <el-select v-model="batchForm.unitCoload" clearable style="width:120px">
            <el-option v-for="u in units" :key="u" :label="u" :value="u" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchSaving" @click="doBatchUpdate">
          批量更新 ({{ selectedRows.length }}条)
        </el-button>
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
const tableData = ref([])
const loading = ref(false)
const destLoading = ref(false)
const editingId = ref(null)
const editForm = reactive({
  feeNameCn: '', feeNameEn: '', currency: '',
  amountDirect: '', unitDirect: '',
  amountCoload: '', unitCoload: '', remarks: ''
})

// 批量修改
const tableRef = ref()
const selectedRows = ref([])
const batchVisible = ref(false)
const batchSaving = ref(false)
const batchForm = reactive({
  fields: ['currency'],
  currency: '', amountDirect: '', unitDirect: '',
  amountCoload: '', unitCoload: ''
})

const onSelectionChange = (rows) => { selectedRows.value = rows }

const openBatchEdit = () => {
  if (!selectedRows.value.length) return
  Object.assign(batchForm, {
    fields: ['currency'],
    currency: '', amountDirect: '', unitDirect: '',
    amountCoload: '', unitCoload: ''
  })
  batchVisible.value = true
}

const doBatchUpdate = async () => {
  if (!batchForm.fields.length) {
    ElMessage.warning('请选择要修改的字段')
    return
  }
  batchSaving.value = true
  let success = 0, fail = 0
  for (const row of selectedRows.value) {
    const payload = { id: row.id, destination: row.destination }
    if (batchForm.fields.includes('currency')) payload.currency = batchForm.currency || null
    if (batchForm.fields.includes('amountDirect')) payload.amountDirect = batchForm.amountDirect ? Number(batchForm.amountDirect) : null
    if (batchForm.fields.includes('amountDirect')) payload.amountDirectRaw = batchForm.amountDirect || null
    if (batchForm.fields.includes('unitDirect')) payload.unitDirect = batchForm.unitDirect || null
    if (batchForm.fields.includes('amountCoload')) payload.amountCoload = batchForm.amountCoload ? Number(batchForm.amountCoload) : null
    if (batchForm.fields.includes('amountCoload')) payload.amountColoadRaw = batchForm.amountCoload || null
    if (batchForm.fields.includes('unitCoload')) payload.unitCoload = batchForm.unitCoload || null
    try {
      await portChargeApi.update(row.id, payload)
      success++
    } catch { fail++ }
  }
  batchSaving.value = false
  batchVisible.value = false
  if (fail === 0) ElMessage.success(`成功更新 ${success} 条`)
  else ElMessage.warning(`成功 ${success} 条，失败 ${fail} 条`)
  loadData()
}

const currencies = ['USD', 'EUR', 'HKD', 'SGD', 'THB', 'AED', 'KRW', 'NTD', 'GBP', 'AUD', 'CAD', 'ZAR', 'BRL', 'MOP', 'TWD', 'RMB', 'JPY']
const units = ['WM', 'BL', 'SET', 'TON', 'BLOCK', '100KG']

const loadDests = async () => {
  destLoading.value = true
  try {
    const res = await portChargeApi.destinations()
    destinations.value = res.data
  } finally { destLoading.value = false }
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
    feeNameCn: editForm.feeNameEn || null,
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

// ===== 下载 Excel =====
const downloadExcel = async () => {
  let rows = []
  try {
    ElMessage.info('正在导出全部目的港费用数据...')
    const res = await portChargeApi.all()
    rows = res.data || []
  } catch (e) {
    ElMessage.error('导出失败')
    return
  }
  if (!rows || rows.length === 0) {
    ElMessage.warning('没有数据可下载')
    return
  }
  const headers = ['中文费项', '英文费项', '货币', '直客金额', '直客单位', '同行金额', '同行单位', '备注']
  const fields = ['feeNameCn', 'feeNameEn', 'currency', 'amountDirect', 'unitDirect', 'amountCoload', 'unitCoload', 'remarks']

  let html = '<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel">'
  html += '<head><meta charset="UTF-8"><!--[if gte mso 9]><xml><x:ExcelWorkbook><x:ExcelWorksheets>'
  html += '<x:ExcelWorksheet><x:Name>目的港费用</x:Name><x:WorksheetOptions><x:DisplayGridlines/></x:WorksheetOptions></x:ExcelWorksheet>'
  html += '</x:ExcelWorksheets></x:ExcelWorkbook></xml><![endif]--></head><body>'
  html += '<table border="1">'

  // 标题行
  html += '<tr><td colspan="' + headers.length + '" style="font-size:14px;font-weight:bold;text-align:center;background:#C00000;color:#fff">'
  html += '崴航（广州）国际货运代理有限公司 - 目的港费用表</td></tr>'
  html += '<tr><td colspan="' + headers.length + '" style="font-size:12px;background:#FCE4D6;font-weight:bold">目的港：' + selectedDest.value + '</td></tr>'

  // 表头
  html += '<tr style="background:#C00000;color:#fff;font-weight:bold">'
  headers.forEach(h => { html += '<td>' + h + '</td>' })
  html += '</tr>'

  // 数据行
  rows.forEach(row => {
    html += '<tr>'
    fields.forEach(f => {
      let val = row[f]
      if (val == null) val = ''
      // 金额格式化
      if ((f === 'amountDirect' || f === 'amountCoload') && val !== '') {
        val = Number(val).toFixed(2)
      }
      html += '<td>' + String(val).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;') + '</td>'
    })
    html += '</tr>'
  })

  html += '</table></body></html>'

  const blob = new Blob(['\ufeff' + html], { type: 'application/vnd.ms-excel;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '目的港费用_' + selectedDest.value.replace(/[\\/:*?"<>|]/g, '_') + '_' + new Date().toISOString().slice(0, 10) + '.xls'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
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

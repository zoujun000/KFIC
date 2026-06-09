<template>
  <el-card>
    <!-- 搜索栏 -->
    <el-form :inline="true" :model="query" class="search-form">
      <el-form-item label="SO号">
        <el-input v-model.trim="query.orderSo" placeholder="请输入SO号" clearable />
      </el-form-item>
      <el-form-item label="运输方式">
        <el-select v-model="query.shipType" placeholder="全部" clearable style="width:120px">
          <el-option label="海运" value="SEA" />
          <el-option label="空运" value="AIR" />
          <el-option label="陆运" value="LAND" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width:120px">
          <el-option v-for="(label, key) in statusLabel" :key="key" :label="label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item label="ETD">
        <el-date-picker v-model="etdRange" type="daterange" range-separator="至"
          start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD"
          :shortcuts="dateShortcuts" style="width:260px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
        <el-button :icon="Download" @click="exportOrders" :loading="exporting">导出</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <span></span>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新建订单</el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="tableData" v-loading="loading" stripe style="margin-top:12px">
      <el-table-column prop="orderSo" label="SO号" width="180" />
      <el-table-column prop="shipType" label="运输方式" width="100">
        <template #default="{ row }">
          <el-tag :type="shipTypeTag[row.shipType]" size="small">{{ shipTypeLabel[row.shipType] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="origin" label="起运地" />
      <el-table-column prop="destination" label="目的地" />
      <el-table-column prop="cargoName" label="货物名称" />
      <el-table-column prop="vesselVoyage" label="船名航次" width="130" show-overflow-tooltip />
      <el-table-column prop="etd" label="ETD" width="110" />
      <el-table-column prop="eta" label="ETA" width="110" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag[row.status]" size="small">{{ statusLabel[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDialog(row)">编辑</el-button>
          <el-dropdown @command="(s) => handleStatusChange(row.id, s)" size="small">
            <el-button link type="warning" size="small">状态</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="(label, key) in statusLabel" :key="key" :command="key">
                  {{ label }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-popconfirm title="确认删除？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button link type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pagination" v-model:current-page="query.pageNum"
      v-model:page-size="query.pageSize" :total="total"
      :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
      @change="loadData" />

    <!-- 弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑订单' : '新建订单'" width="800px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" class="order-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="客户" prop="customerId">
              <el-select v-model="form.customerId" placeholder="选择客户" filterable style="width:100%">
                <el-option v-for="c in customers" :key="c.id" :label="c.companyName" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="SO号" prop="orderSo">
              <el-input v-model.trim="form.orderSo" placeholder="请输入SO号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="贸易方式">
              <el-select v-model="form.tradeTerms" placeholder="请选择" clearable style="width:100%">
                <el-option label="FOB" value="FOB" />
                <el-option label="CIF" value="CIF" />
                <el-option label="EXW" value="EXW" />
                <el-option label="DDP" value="DDP" />
                <el-option label="DAP" value="DAP" />
                <el-option label="CFR" value="CFR" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="运输方式" prop="shipType">
              <el-select v-model="form.shipType" style="width:100%">
                <el-option label="海运" value="SEA" />
                <el-option label="空运" value="AIR" />
                <el-option label="陆运" value="LAND" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="起运地" prop="origin">
              <el-input v-model="form.origin" placeholder="请输入起运地" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目的地" prop="destination">
              <el-input v-model="form.destination" placeholder="请输入目的地" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="货物名称">
              <el-input v-model="form.cargoName" placeholder="货物名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="重量(KG)" label-width="85px">
              <el-input v-model="form.cargoWeight" placeholder="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="体积(CBM)" label-width="85px">
              <el-input v-model="form.cargoVolume" placeholder="0.000" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="金额" label-width="60px">
              <el-input v-model="form.totalAmount" placeholder="0.00" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="船名航次">
              <el-input v-model="form.vesselVoyage" placeholder="船名/航次" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="船公司">
              <el-input v-model="form.shippingCompany" placeholder="船公司" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="柜封号">
              <el-input v-model="form.containerSeal" placeholder="柜号/封号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="ETD">
              <el-date-picker v-model="form.etd" type="date" value-format="YYYY-MM-DD" style="width:100%" placeholder="预计离港日" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="ETA">
              <el-date-picker v-model="form.eta" type="date" value-format="YYYY-MM-DD" style="width:100%" placeholder="预计到港日" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注信息" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <!-- 附件上传（仅编辑时显示） -->
      <div v-if="isEdit && form.id" style="margin-top:16px">
        <el-divider content-position="left">📎 附件</el-divider>
        <div v-if="attachments.length > 0" style="margin-bottom:8px">
          <el-tag
            v-for="f in attachments" :key="f"
            size="small"
            style="margin-right:6px;margin-bottom:4px;cursor:pointer"
            @dblclick="previewAttachment(f)"
          >
            {{ f }}
          </el-tag>
        </div>
        <div v-else style="color:#c0c4cc;font-size:13px;margin-bottom:8px">暂无附件</div>
        <el-upload
          ref="uploadRef"
          :action="`/api/orders/${form.id}/attachments`"
          :headers="uploadHeaders"
          :file-list="uploadFiles"
          :auto-upload="false"
          multiple
          @change="onUploadChange"
        >
          <el-button type="primary" size="small">选择文件</el-button>
          <template #tip>
            <span style="font-size:12px;color:#909399;margin-left:8px">可多选，选完后点"上传附件"</span>
          </template>
        </el-upload>
        <el-button
          v-if="uploadFiles.length > 0"
          type="success"
          size="small"
          :loading="uploading"
          @click="doUpload"
          style="margin-top:8px"
        >
          上传附件
        </el-button>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Download } from '@element-plus/icons-vue'
import { orderApi, customerApi } from '@/api'

defineOptions({ name: 'Orders' })

const loading = ref(false)
const saving = ref(false)
const exporting = ref(false)
const tableData = ref([])
const total = ref(0)
const customers = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const uploadRef = ref()
const uploading = ref(false)
const attachments = ref([])
const uploadFiles = ref([])

const statusLabel = { '进仓': '进仓', '走船': '走船', '已到港': '已到港', '已提货': '已提货' }
const statusTag = { '进仓': 'info', '走船': 'warning', '已到港': 'primary', '已提货': 'success' }
const shipTypeLabel = { SEA: '海运', AIR: '空运', LAND: '陆运' }
const shipTypeTag = { SEA: 'primary', AIR: 'success', LAND: 'warning' }

const etdRange = ref([])
const dateShortcuts = [
  { text: '本月', value: () => { const d = new Date(); return [new Date(d.getFullYear(), d.getMonth(), 1), new Date(d.getFullYear(), d.getMonth() + 1, 0)] } },
  { text: '上月', value: () => { const d = new Date(); return [new Date(d.getFullYear(), d.getMonth() - 1, 1), new Date(d.getFullYear(), d.getMonth(), 0)] } },
  { text: '最近3个月', value: () => { const d = new Date(); return [new Date(d.getFullYear(), d.getMonth() - 2, 1), new Date(d.getFullYear(), d.getMonth() + 1, 0)] } },
  { text: '最近6个月', value: () => { const d = new Date(); return [new Date(d.getFullYear(), d.getMonth() - 5, 1), new Date(d.getFullYear(), d.getMonth() + 1, 0)] } },
  { text: '今年', value: () => { const d = new Date(); return [new Date(d.getFullYear(), 0, 1), new Date(d.getFullYear(), 11, 31)] } }
]

const query = reactive({ orderSo: '', shipType: '', status: '', pageNum: 1, pageSize: 10 })

const emptyForm = () => ({
  id: null, customerId: null, orderSo: '', tradeTerms: '', shipType: 'SEA',
  origin: '', destination: '', cargoName: '',
  cargoWeight: '', cargoVolume: '', totalAmount: '', packageCount: '',
  vesselVoyage: '', shippingCompany: '', containerSeal: '',
  etd: '', eta: '', remark: ''
})
const form = reactive(emptyForm())

const rules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  orderSo: [{ required: true, message: '请填写SO号', trigger: 'blur' }],
  shipType: [{ required: true, message: '请选择运输方式', trigger: 'change' }],
  origin: [{ required: true, message: '请填写起运地', trigger: 'blur' }],
  destination: [{ required: true, message: '请填写目的地', trigger: 'blur' }]
}

const formatRow = (row) => ({
  ...row,
  cargoWeight: row.cargoWeight != null ? String(row.cargoWeight) : '',
  cargoVolume: row.cargoVolume != null ? String(row.cargoVolume) : '',
  totalAmount: row.totalAmount != null ? String(row.totalAmount) : '',
  packageCount: row.packageCount != null ? String(row.packageCount) : '',
  vesselVoyage: row.vesselVoyage || '',
  shippingCompany: row.shippingCompany || '',
  containerSeal: row.containerSeal || '',
  etd: row.etd || '',
  eta: row.eta || ''
})

const uploadHeaders = computed(() => ({
  Authorization: 'Bearer ' + localStorage.getItem('token')
}))

const loadData = async () => {
  loading.value = true
  try {
    query.etdStart = etdRange.value?.[0] || null
    query.etdEnd = etdRange.value?.[1] || null
    const res = await orderApi.page(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const exportOrders = async () => {
  exporting.value = true
  try {
    const exportQuery = { ...query, pageNum: 1, pageSize: 10000 }
    exportQuery.etdStart = etdRange.value?.[0] || null
    exportQuery.etdEnd = etdRange.value?.[1] || null
    const res = await orderApi.page(exportQuery)
    const rows = res.data.records
    if (!rows.length) { ElMessage.warning('没有数据可导出'); return }

    const headers = ['SO号', '运输方式', '贸易方式', '起运地', '目的地', '货物名称', '件数', '重量(kg)', '体积(CBM)', '船名航次', 'ETD', 'ETA', '状态', '总金额', '备注', '创建时间']
    const keys = ['orderSo', 'shipType', 'tradeTerms', 'origin', 'destination', 'cargoName', 'packageCount', 'cargoWeight', 'cargoVolume', 'vesselVoyage', 'etd', 'eta', 'status', 'totalAmount', 'remark', 'createTime']
    const shipMap = { SEA: '海运', AIR: '空运', LAND: '陆运' }

    const csvRows = [headers.join(',')]
    for (const row of rows) {
      csvRows.push(keys.map(k => {
        let v = row[k] ?? ''
        if (k === 'shipType') v = shipMap[v] || v
        // CSV 转义
        v = String(v).replace(/"/g, '""')
        if (v.includes(',') || v.includes('"') || v.includes('\n')) v = `"${v}"`
        return v
      }).join(','))
    }

    const bom = '\uFEFF'
    const blob = new Blob([bom + csvRows.join('\n')], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `订单列表_${new Date().toISOString().slice(0, 10)}.csv`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success(`已导出 ${rows.length} 条订单数据`)
  } catch (e) {
    ElMessage.error('导出失败: ' + (e.message || '请重试'))
  } finally { exporting.value = false }
}

const resetQuery = () => {
  etdRange.value = []
  Object.assign(query, { orderSo: '', shipType: '', status: '', pageNum: 1 })
  loadData()
}

const openDialog = async (row = null) => {
  isEdit.value = !!row
  if (!customers.value.length) {
    const res = await customerApi.page({ pageSize: 999 })
    customers.value = res.data.records
  }
  Object.assign(form, row ? formatRow(row) : emptyForm())
  uploadFiles.value = []
  attachments.value = []
  dialogVisible.value = true
  if (row) loadAttachments()
}

const handleSave = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    if (isEdit.value) await orderApi.update(form)
    else await orderApi.create(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

const handleStatusChange = async (id, status) => {
  await orderApi.updateStatus(id, status)
  ElMessage.success('状态已更新')
  loadData()
}

const handleDelete = async (id) => {
  await orderApi.delete(id)
  ElMessage.success('删除成功')
  loadData()
}

const loadAttachments = async () => {
  if (!form.id) return
  try {
    const res = await orderApi.getAttachments(form.id)
    attachments.value = res.data || []
  } catch { attachments.value = [] }
}

const onUploadChange = (file) => {
  if (file.status === 'ready') uploadFiles.value.push(file)
}

const doUpload = async () => {
  if (!uploadFiles.value.length) return
  uploading.value = true
  try {
    const fd = new FormData()
    uploadFiles.value.forEach(f => fd.append('files', f.raw))
    await orderApi.uploadAttachments(form.id, fd)
    ElMessage.success('上传成功')
    uploadFiles.value = []
    uploadRef.value.clearFiles()
    loadAttachments()
  } catch { ElMessage.error('上传失败') }
  finally { uploading.value = false }
}

const previewAttachment = async (filename) => {
  try {
    const token = localStorage.getItem('token')
    const resp = await fetch(`/api/orders/${form.id}/attachments/${encodeURIComponent(filename)}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    if (!resp.ok) throw new Error('加载失败')
    const blob = await resp.blob()
    const url = URL.createObjectURL(blob)
    window.open(url, '_blank')
    setTimeout(() => URL.revokeObjectURL(url), 60000)
  } catch { ElMessage.error('预览失败') }
}

onMounted(loadData)
</script>

<style scoped>
.search-form { background: #fafafa; padding: 16px 16px 0; border-radius: 6px; margin-bottom: 12px; }
.toolbar { display: flex; justify-content: flex-end; }
.pagination { margin-top: 16px; justify-content: flex-end; }
.order-form :deep(.el-input__inner) { font-size: 15px; }
.order-form :deep(.el-select .el-input__inner) { font-size: 15px; }

/* ── 移动端适配 ── */
@media (max-width: 768px) {
  .search-form {
    padding: 10px 10px 0;
  }
  :deep(.el-form--inline) {
    display: flex;
    flex-direction: column;
  }
  :deep(.el-form--inline .el-form-item) {
    margin-right: 0;
    width: 100%;
  }
  :deep(.el-select), :deep(.el-input) {
    width: 100% !important;
  }
  :deep(.el-select .el-input) {
    width: 100%;
  }
  :deep(.el-button) {
    width: 100%;
    margin-left: 0 !important;
  }
  :deep(.el-table) {
    font-size: 12px;
  }
  :deep(.el-table th),
  :deep(.el-table td) {
    padding: 8px 4px;
  }
  .toolbar {
    justify-content: stretch;
  }
  .toolbar .el-button {
    width: 100%;
  }
  .pagination {
    justify-content: center;
    flex-wrap: wrap;
  }
  /* 弹窗全屏 */
  :deep(.el-dialog) {
    width: 94vw !important;
    margin-top: 2vh !important;
    padding: 0 10px;
  }
  :deep(.el-dialog__body) {
    padding: 16px 10px;
  }
}
</style>

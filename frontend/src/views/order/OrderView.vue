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
      <el-form-item>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
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
      <el-table-column prop="totalAmount" label="金额" width="110">
        <template #default="{ row }">{{ row.totalAmount ? '¥' + row.totalAmount : '-' }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag[row.status]" size="small">{{ statusLabel[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="etd" label="ETD" width="110" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-dropdown @command="(s) => handleStatusChange(row.id, s)" size="small">
            <el-button link type="warning">状态</el-button>
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
              <el-button link type="danger">删除</el-button>
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
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { orderApi, customerApi } from '@/api'

defineOptions({ name: 'Orders' })

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const total = ref(0)
const customers = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()

const statusLabel = { '进仓': '进仓', '走船': '走船', '已到港': '已到港', '已提货': '已提货' }
const statusTag = { '进仓': 'info', '走船': 'warning', '已到港': 'primary', '已提货': 'success' }
const shipTypeLabel = { SEA: '海运', AIR: '空运', LAND: '陆运' }
const shipTypeTag = { SEA: 'primary', AIR: 'success', LAND: 'warning' }

const query = reactive({ orderSo: '', shipType: '', status: '', pageNum: 1, pageSize: 10 })

const emptyForm = () => ({
  id: null, customerId: null, orderSo: '', tradeTerms: '', shipType: 'SEA',
  origin: '', destination: '', cargoName: '',
  cargoWeight: '', cargoVolume: '', totalAmount: '',
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
  etd: row.etd || '',
  eta: row.eta || ''
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await orderApi.page(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
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
  dialogVisible.value = true
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

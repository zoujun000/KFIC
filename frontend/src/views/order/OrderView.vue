<template>
  <el-card shadow="never" class="main-card">
    <!-- 搜索栏 -->
    <el-form :inline="true" :model="query" class="search-form">
      <el-form-item label="SO号">
        <el-input v-model.trim="query.orderSo" placeholder="请输入SO号" clearable />
      </el-form-item>
      <el-form-item label="客户">
        <el-select v-model="query.customerId" placeholder="全部" filterable clearable style="width:180px">
          <el-option v-for="customer in customers" :key="customer.id" :label="customer.companyName" :value="customer.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="运输方式">
        <el-select v-model="query.shipType" placeholder="全部" clearable style="width:120px">
          <el-option label="海运" value="SEA" />
          <el-option label="空运" value="AIR" />
          <el-option label="陆运" value="LAND" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.statuses" multiple placeholder="全部" clearable style="width:200px" collapse-tags>
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
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <span></span>
      <div>
        <el-button :icon="Download" @click="exportOrders" :loading="exporting">导出</el-button>
        <el-button type="primary" :icon="Plus" @click="openDialog()">新建订单</el-button>
      </div>
    </div>

    <!-- 表格 -->
    <el-table :data="tableData" v-loading="loading" stripe style="margin-top:12px">
      <el-table-column prop="orderSo" label="SO号" width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="showOrderDetail(row)">{{ row.orderSo }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="客户" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          {{ customerNameOf(row.customerId) }}
        </template>
      </el-table-column>
      <el-table-column prop="shipType" label="运输方式" width="120">
        <template #default="{ row }">
          <el-tag :type="shipTypeTag[row.shipType]" size="small">{{ shipTypeLabel[row.shipType] }}{{ row.tradeTerms ? ' ' + row.tradeTerms : '' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="origin" label="起运港" />
      <el-table-column prop="destination" label="目的港" />
      <el-table-column prop="cargoName" label="货物名称" />
      <el-table-column prop="vesselVoyage" label="船名航次" width="130" show-overflow-tooltip />
      <el-table-column prop="etd" label="ETD" width="110" />
      <el-table-column prop="eta" label="ETA" width="110" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag[row.status]" size="small">{{ statusLabel[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right" align="center">
        <template #default="{ row }">
          <div class="action-group">
            <el-button link type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-dropdown @command="(s) => handleStatusChange(row.id, s)" size="small">
              <el-button link type="warning" size="small">
                状态 <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-for="(label, key) in statusLabel" :key="key" :command="key">
                    {{ label }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-popconfirm title="确认删除？" confirm-button-text="删除" cancel-button-text="取消" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
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
              <el-select v-model="form.customerId" placeholder="选择客户" filterable style="width:100%" @change="onCustomerChange">
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
            <el-form-item label="起运港" prop="origin">
              <el-select
                v-model="form.origin"
                filterable
                allow-create
                default-first-option
                placeholder="请选择或输入起运港"
                style="width: 100%"
              >
                <el-option label="广州乌冲" value="广州乌冲" />
                <el-option label="广州滘心" value="广州滘心" />
                <el-option label="广州南沙" value="广州南沙" />
                <el-option label="深圳金运达" value="深圳金运达" />
                <el-option label="深圳平湖仓" value="深圳平湖仓" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目的港" prop="destination">
              <el-input v-model="form.destination" placeholder="请输入目的港" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="货物名称">
              <el-input v-model="form.cargoName" placeholder="货物名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="金额" prop="totalAmount">
              <el-input v-model="form.totalAmount" placeholder="0.00" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="重量(KG)" prop="cargoWeight">
              <el-input v-model="form.cargoWeight" placeholder="0" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="收费重(KG)" prop="chargeableWeight">
              <el-input v-model="form.chargeableWeight" placeholder="0" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="体积(CBM)" prop="cargoVolume">
              <el-input v-model="form.cargoVolume" placeholder="0.000" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="件数" prop="packageCount">
              <el-input v-model="form.packageCount" placeholder="0" />
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
              <el-input v-model="form.remark" type="textarea" :autosize="{ minRows: 2, maxRows: 8 }" placeholder="备注信息" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <!-- 附件上传（仅编辑时显示） -->
      <div v-if="isEdit && form.id" style="margin-top:16px">
        <el-divider content-position="left">📎 附件（点击文件名可预览/下载）</el-divider>
        <div v-if="attachments.length > 0" style="margin-bottom:8px">
          <el-tag
            v-for="f in attachments" :key="f"
            size="small"
            style="margin-right:6px;margin-bottom:4px;cursor:pointer"
            title="点击预览/下载"
            @click="previewAttachment(f)"
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

    <!-- 点击 SO 号后的订单控制台 -->
    <el-drawer
      v-model="detailVisible"
      :title="null"
      :show-close="false"
      :lock-scroll="false"
      size="760px"
      direction="rtl"
      class="order-detail-drawer"
    >
      <template #header="{ close }">
        <div class="detail-drawer-header">
          <button class="drawer-close" @click="close" aria-label="关闭订单详情">
            <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 6L6 18M6 6l12 12"/>
            </svg>
          </button>
        </div>
      </template>

      <div class="shipment-dossier">
        <template v-if="currentOrder">
          <section class="dossier-hero" :class="`transport-${currentOrder.shipType || 'SEA'}`">
            <div class="hero-grid"></div>
            <div class="hero-topline">
              <span class="dossier-kicker">物流订单 / {{ shipTypeLabel[currentOrder.shipType] || '货运' }}</span>
              <span class="dossier-status"><i></i>{{ statusLabel[currentOrder.status] || currentOrder.status }}</span>
            </div>
            <div class="hero-title-row">
              <div>
                <p class="hero-label">SO NUMBER</p>
                <h2>{{ currentOrder.orderSo || currentOrder.orderNo || '未编号订单' }}</h2>
              </div>
              <button class="copy-so" type="button" @click="copyOrderSo">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                  <rect x="9" y="9" width="11" height="11" rx="2"/><path d="M15 9V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h4"/>
                </svg>
                复制 SO
              </button>
            </div>
            <div class="hero-route" aria-label="运输路线">
              <div><span>ORIGIN</span><strong>{{ currentOrder.origin || '待确认' }}</strong></div>
              <div class="route-line"><span class="route-vehicle">{{ shipTypeIcon[currentOrder.shipType] || '•' }}</span><i></i></div>
              <div class="route-destination"><span>DESTINATION</span><strong>{{ currentOrder.destination || '待确认' }}</strong></div>
            </div>
            <div class="hero-meta">
              <span>客户 <b>{{ customerNameOf(currentOrder.customerId) }}</b></span>
              <span>创建订单时间 <b>{{ formatDateTime(currentOrder.createTime) }}</b></span>
            </div>
          </section>

          <section class="progress-panel">
            <div class="section-heading"><span>运输进度</span><small>当前节点：{{ statusLabel[currentOrder.status] || currentOrder.status }}</small></div>
            <div class="shipment-steps">
              <div v-for="(step, index) in shipmentSteps" :key="step.key" class="shipment-step" :class="{ done: index <= currentStatusIndex, active: index === currentStatusIndex }">
                <span class="step-dot"><i v-if="index < currentStatusIndex">✓</i></span>
                <strong>{{ step.label }}</strong>
                <small>{{ step.note }}</small>
              </div>
            </div>
          </section>

          <div class="dossier-body">
            <section class="detail-block schedule-block">
              <div class="section-heading"><span>关键节点</span><small>计划时间</small></div>
              <div class="schedule-grid">
                <div class="schedule-cell"><span>ETD · 预计离港</span><strong>{{ currentOrder.etd || '待定' }}</strong></div>
                <div class="schedule-cell"><span>ETA · 预计到港</span><strong>{{ currentOrder.eta || '待定' }}</strong></div>
                <div class="schedule-cell"><span>贸易条款</span><strong>{{ currentOrder.tradeTerms || '—' }}</strong></div>
                <div class="schedule-cell"><span>订单金额</span><strong class="amount-value">{{ currentOrder.totalAmount ? `¥ ${currentOrder.totalAmount}` : '—' }}</strong></div>
              </div>
            </section>

            <section class="detail-block cargo-block">
              <div class="section-heading"><span>货物摘要</span><small>{{ currentOrder.cargoName || '未填写货物名称' }}</small></div>
              <div class="cargo-layout">
                <div class="cargo-mark">
                  <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="m4 7 8-4 8 4-8 4-8-4Z"/><path d="m4 7 8 4 8-4M4 7v10l8 4 8-4V7M12 11v10"/></svg>
                </div>
                <div class="cargo-stat"><span>重量</span><strong>{{ currentOrder.cargoWeight || '—' }}<em v-if="currentOrder.cargoWeight">KG</em></strong></div>
                <div class="cargo-stat"><span>收费重</span><strong>{{ currentOrder.chargeableWeight || '—' }}<em v-if="currentOrder.chargeableWeight">KG</em></strong></div>
                <div class="cargo-stat"><span>体积</span><strong>{{ currentOrder.cargoVolume || '—' }}<em v-if="currentOrder.cargoVolume">CBM</em></strong></div>
                <div class="cargo-stat"><span>件数</span><strong>{{ currentOrder.packageCount || '—' }}<em v-if="currentOrder.packageCount">件</em></strong></div>
              </div>
            </section>

            <section class="detail-block detail-facts">
              <div class="section-heading"><span>承运与装箱</span><small>运输信息</small></div>
              <dl>
                <div><dt>运输方式</dt><dd><span class="transport-pill">{{ shipTypeIcon[currentOrder.shipType] }} {{ shipTypeLabel[currentOrder.shipType] || '—' }}</span></dd></div>
                <div><dt>船名航次</dt><dd>{{ currentOrder.vesselVoyage || '待补充' }}</dd></div>
                <div><dt>船公司</dt><dd>{{ currentOrder.shippingCompany || '待补充' }}</dd></div>
                <div><dt>柜号 / 封号</dt><dd>{{ currentOrder.containerSeal || '待补充' }}</dd></div>
              </dl>
            </section>

            <section v-if="currentOrder.remark" class="detail-block remark-block">
              <div class="section-heading"><span>操作备注</span><small>内部信息</small></div>
              <p>{{ currentOrder.remark }}</p>
            </section>

            <section class="detail-block attachment-block">
              <div class="section-heading"><span>随附文件</span><small>{{ detailAttachments.length }} 个文件</small></div>
              <div v-loading="attachLoading" element-loading-background="transparent" class="attachment-zone">
                <div v-if="!attachLoading && detailAttachments.length" class="attachment-list">
                  <button v-for="file in detailAttachments" :key="file" type="button" class="attachment-card" @click="previewDetailAttachment(file)">
                    <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6M8 13h8M8 17h5"/></svg>
                    <span>{{ file }}</span><b>打开 ↗</b>
                  </button>
                </div>
                <div v-else-if="!attachLoading" class="attachment-empty">暂未上传附件</div>
              </div>
            </section>
          </div>
        </template>
      </div>

      <template #footer>
        <div class="dossier-footer">
          <span v-if="currentOrder">更新时间 {{ formatDateTime(currentOrder.updateTime || currentOrder.createTime) }}</span>
          <el-button type="primary" @click="editCurrentOrder">编辑订单</el-button>
        </div>
      </template>
    </el-drawer>
  </el-card>

  <!-- Excel 在线预览弹窗 -->
  <el-dialog v-model="excelPreviewVisible" :title="excelPreviewTitle" width="90vw" top="3vh" destroy-on-close
    class="excel-preview-dialog" @closed="cleanupExcelPreview">
    <div v-loading="excelPreviewLoading" element-loading-text="正在解析表格…" class="excel-preview-body">
      <template v-if="excelPreviewSheets.length">
        <div class="excel-sheet-tabs" role="tablist">
          <button v-for="sheet in excelPreviewSheets" :key="sheet.name" type="button"
            :class="['excel-sheet-tab', { active: sheet.name === excelPreviewActiveSheet }]"
            @click="selectExcelSheet(sheet.name)">
            {{ sheet.name }}
          </button>
        </div>
        <div class="excel-table-wrap">
          <table class="excel-table">
            <tbody>
              <tr v-for="(row, rowIndex) in excelPreviewRows" :key="rowIndex">
                <th>{{ rowIndex + 1 }}</th>
                <td v-for="(cell, cellIndex) in row" :key="cellIndex">{{ cell }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>
      <el-empty v-else-if="!excelPreviewLoading" description="表格中没有可显示的数据" />
    </div>
    <template #footer>
      <div class="excel-preview-footer">
        <el-button @click="downloadExcelOriginal">下载原文件</el-button>
        <el-button type="primary" @click="excelPreviewVisible = false">关闭</el-button>
      </div>
    </template>
  </el-dialog>

  <el-dialog v-model="wordPreviewVisible" :title="wordPreviewTitle" width="90vw" top="3vh" destroy-on-close
    class="word-preview-dialog" @closed="cleanupWordPreview">
    <div v-loading="wordPreviewLoading" element-loading-text="正在解析文档…" class="word-preview-body">
      <VueOfficeDocx v-if="wordPreviewMode === 'docx' && wordPreviewSrc" :src="wordPreviewSrc"
        @rendered="wordPreviewLoading = false" @error="onWordPreviewError" />
      <iframe v-else-if="wordPreviewMode === 'doc' && wordPreviewSrc" :src="wordPreviewSrc"
        :title="wordPreviewTitle" sandbox @load="wordPreviewLoading = false"></iframe>
    </div>
    <template #footer>
      <div class="word-preview-footer">
        <el-button @click="downloadWordOriginal">下载原文件</el-button>
        <el-button type="primary" @click="wordPreviewVisible = false">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Download, ArrowDown, Edit } from '@element-plus/icons-vue'
import { orderApi, customerApi } from '@/api'
import VueOfficeDocx from '@vue-office/docx'
import '@vue-office/docx/lib/index.css'
import * as XLSX from 'xlsx'

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

// 订单详情抽屉
const detailVisible = ref(false)
const currentOrder = ref(null)
const detailAttachments = ref([])

// ── Excel 在线预览 ──
const EXCEL_EXTS = ['.xls', '.xlsx', '.csv']
const isExcelFile = (name) => EXCEL_EXTS.some(ext => name.toLowerCase().endsWith(ext))
const excelPreviewVisible = ref(false)
const excelPreviewLoading = ref(false)
const excelPreviewTitle = ref('')
const excelPreviewFilename = ref('')
const excelPreviewOrderId = ref(null)
const excelPreviewSheets = ref([])
const excelPreviewActiveSheet = ref('')
const excelPreviewRows = computed(() =>
  excelPreviewSheets.value.find(sheet => sheet.name === excelPreviewActiveSheet.value)?.rows || []
)
const WORD_EXTS = ['.doc', '.docx']
const isWordFile = (name) => WORD_EXTS.some(ext => name.toLowerCase().endsWith(ext))
const wordPreviewVisible = ref(false)
const wordPreviewLoading = ref(false)
const wordPreviewSrc = ref(null)
const wordPreviewMode = ref('')
const wordPreviewTitle = ref('')
const wordPreviewFilename = ref('')
const wordPreviewOrderId = ref(null)
const attachLoading = ref(false)

const statusLabel = { '订舱': '订舱', '进仓': '进仓', '走船': '走船', '已到港': '已到港', '已提货': '已提货' }
const statusTag = { '订舱': '', '进仓': 'info', '走船': 'warning', '已到港': 'primary', '已提货': 'success' }
const shipTypeLabel = { SEA: '海运', AIR: '空运', LAND: '陆运' }
const shipTypeTag = { SEA: 'primary', AIR: 'success', LAND: 'warning' }
const shipTypeIcon = { SEA: '🚢', AIR: '✈️', LAND: '🚛' }
const shipmentSteps = [
  { key: '订舱', label: '订舱', note: '舱位确认' },
  { key: '进仓', label: '进仓', note: '货物入仓' },
  { key: '走船', label: '走船', note: '已启运' },
  { key: '已到港', label: '到港', note: '目的港抵达' },
  { key: '已提货', label: '提货', note: '交付完成' }
]
const currentStatusIndex = computed(() => {
  const index = shipmentSteps.findIndex(step => step.key === currentOrder.value?.status)
  return index === -1 ? 0 : index
})

const etdRange = ref([])
const dateShortcuts = [
  { text: '本月', value: () => { const d = new Date(); return [new Date(d.getFullYear(), d.getMonth(), 1), new Date(d.getFullYear(), d.getMonth() + 1, 0)] } },
  { text: '上月', value: () => { const d = new Date(); return [new Date(d.getFullYear(), d.getMonth() - 1, 1), new Date(d.getFullYear(), d.getMonth(), 0)] } },
  { text: '最近3个月', value: () => { const d = new Date(); return [new Date(d.getFullYear(), d.getMonth() - 2, 1), new Date(d.getFullYear(), d.getMonth() + 1, 0)] } },
  { text: '最近6个月', value: () => { const d = new Date(); return [new Date(d.getFullYear(), d.getMonth() - 5, 1), new Date(d.getFullYear(), d.getMonth() + 1, 0)] } },
  { text: '今年', value: () => { const d = new Date(); return [new Date(d.getFullYear(), 0, 1), new Date(d.getFullYear(), 11, 31)] } }
]

const query = reactive({ orderSo: '', customerId: null, shipType: '', statuses: [], pageNum: 1, pageSize: 10 })

const emptyForm = () => ({
  id: null, customerId: null, orderSo: '', tradeTerms: '', shipType: 'SEA',
  origin: '', destination: '', cargoName: '',
  cargoWeight: '', chargeableWeight: '', cargoVolume: '', totalAmount: '', packageCount: '',
  vesselVoyage: '', shippingCompany: '', containerSeal: '',
  etd: '', eta: '', remark: ''
})
const form = reactive(emptyForm())

const rules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  orderSo: [{ required: true, message: '请填写SO号', trigger: 'blur' }],
  shipType: [{ required: true, message: '请选择运输方式', trigger: 'change' }],
  origin: [{ required: true, message: '请填写起运港', trigger: 'blur' }],
  destination: [{ required: true, message: '请填写目的港', trigger: 'blur' }],
  cargoWeight: [{ validator: numberValidator('重量'), trigger: 'blur' }],
  chargeableWeight: [{ validator: numberValidator('收费重'), trigger: 'blur' }],
  cargoVolume: [{ validator: numberValidator('体积'), trigger: 'blur' }],
  totalAmount: [{ validator: numberValidator('金额'), trigger: 'blur' }],
  packageCount: [{ validator: positiveIntegerValidator('件数'), trigger: 'blur' }]
}

// 数字校验：允许留空，但填了就必须是非负数字，避免提交到后端报 400
function numberValidator(label) {
  return (rule, value, callback) => {
    if (value === '' || value === null || value === undefined) return callback()
    const s = String(value).trim()
    if (!/^\d*\.?\d+$/.test(s)) return callback(new Error(`${label}须为数字`))
    callback()
  }
}

// 件数校验：允许留空，填了必须是正整数（后端为 Integer，小数会 400）
function positiveIntegerValidator(label) {
  return (rule, value, callback) => {
    if (value === '' || value === null || value === undefined) return callback()
    const s = String(value).trim()
    if (!/^[1-9]\d*$/.test(s)) return callback(new Error(`${label}须为正整数`))
    callback()
  }
}

const formatRow = (row) => ({
  ...row,
  cargoWeight: row.cargoWeight != null ? String(row.cargoWeight) : '',
  chargeableWeight: row.chargeableWeight != null ? String(row.chargeableWeight) : '',
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
  Authorization: 'Bearer ' + localStorage.getItem('accessToken')
}))

// 列表与导出共用的查询参数拼装（statuses 多选转逗号分隔、ETD 区间转起止）
const buildQueryParams = (overrides = {}) => {
  const params = { ...query, ...overrides }
  params.etdStart = etdRange.value?.[0] || null
  params.etdEnd = etdRange.value?.[1] || null
  params.statuses = Array.isArray(params.statuses) && params.statuses.length > 0 ? params.statuses.join(',') : ''
  return params
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await orderApi.page(buildQueryParams())
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const exportOrders = async () => {
  exporting.value = true
  try {
    const res = await orderApi.page(buildQueryParams({ pageNum: 1, pageSize: 10000 }))
    const rows = res.data.records
    if (!rows.length) { ElMessage.warning('没有数据可导出'); return }

    const headers = ['SO号', '客户', '运输方式', '贸易方式', '起运港', '目的港', '货物名称', '件数', '重量(kg)', '收费重(kg)', '体积(CBM)', '船名航次', 'ETD', 'ETA', '状态', '总金额', '备注', '创建时间']
    const keys = ['orderSo', 'customerName', 'shipType', 'tradeTerms', 'origin', 'destination', 'cargoName', 'packageCount', 'cargoWeight', 'chargeableWeight', 'cargoVolume', 'vesselVoyage', 'etd', 'eta', 'status', 'totalAmount', 'remark', 'createTime']
    const shipMap = { SEA: '海运', AIR: '空运', LAND: '陆运' }

    const csvRows = [headers.join(',')]
    for (const row of rows) {
      csvRows.push(keys.map(k => {
        let v = row[k] ?? ''
        if (k === 'customerName') v = customerNameOf(row.customerId)
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
  Object.assign(query, { orderSo: '', customerId: null, shipType: '', statuses: [], pageNum: 1 })
  loadData()
}

const customerNameOf = (id) => customers.value.find(c => c.id === id)?.companyName || '—'

// 新建订单时，选择客户后自动回填客户管理中的备注
const onCustomerChange = (customerId) => {
  if (isEdit.value || !customerId) return
  const customer = customers.value.find(c => c.id === customerId)
  form.remark = customer?.remark || ''
}

let customersLoadedAt = 0
// 客户列表缓存 60 秒，避免每次打开弹窗都全量拉取；新建订单时强制刷新，保证刚建的客户可选
const loadCustomers = async (force = false) => {
  if (!force && customers.value.length && Date.now() - customersLoadedAt < 60_000) return
  try {
    const res = await customerApi.page({ pageSize: 999 })
    customers.value = res.data.records
    customersLoadedAt = Date.now()
  } catch { /* 加载失败保留旧列表 */ }
}

// 客户数超过下拉分页上限时，确保当前订单的客户在选项里（否则 el-select 会直接显示 ID）
const ensureCustomerLoaded = async (customerId) => {
  if (!customerId || customers.value.some(c => c.id === customerId)) return
  try {
    const res = await customerApi.getById(customerId)
    if (res?.data) customers.value.unshift(res.data)
  } catch { /* 客户不存在或已删除时保持现状 */ }
}

const openDialog = async (row = null) => {
  isEdit.value = !!row
  await loadCustomers(!isEdit.value)
  await ensureCustomerLoaded(row?.customerId)
  Object.assign(form, row ? formatRow(row) : emptyForm())
  uploadFiles.value = []
  attachments.value = []
  dialogVisible.value = true
  // 表单实例被复用，清除上一次残留的校验提示
  nextTick(() => formRef.value?.clearValidate())
  if (row) loadAttachments()
}

// 仅提交后端 DTO 定义的字段，避免携带表格行展开的 status/createTime 等冗余字段
const buildOrderPayload = () => ({
  id: form.id,
  customerId: form.customerId,
  orderSo: form.orderSo,
  tradeTerms: form.tradeTerms,
  shipType: form.shipType,
  origin: form.origin,
  destination: form.destination,
  cargoName: form.cargoName,
  cargoWeight: form.cargoWeight,
  chargeableWeight: form.chargeableWeight,
  cargoVolume: form.cargoVolume,
  packageCount: form.packageCount,
  vesselVoyage: form.vesselVoyage,
  shippingCompany: form.shippingCompany,
  containerSeal: form.containerSeal,
  etd: form.etd,
  eta: form.eta,
  totalAmount: form.totalAmount,
  remark: form.remark
})

const handleSave = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    if (isEdit.value) {
      await orderApi.update(buildOrderPayload())
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadData()
    } else {
      const res = await orderApi.create(buildOrderPayload())
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadData()
      // 新建成功后直接打开详情抽屉，便于立即上传附件
      if (res?.data?.id) showOrderDetail(res.data)
    }
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

// 订单详情
const showOrderDetail = async (row) => {
  if (detailVisible.value && currentOrder.value?.id === row.id) return

  const orderId = row.id
  currentOrder.value = { ...row }
  detailAttachments.value = []
  attachLoading.value = Boolean(orderId)

  await nextTick()
  detailVisible.value = true
  if (!orderId) return

  try {
    const attachmentRes = await orderApi.getAttachments(orderId)
    if (currentOrder.value?.id === orderId) {
      detailAttachments.value = attachmentRes.data || []
    }
  } catch {
    if (currentOrder.value?.id === orderId) detailAttachments.value = []
  } finally {
    if (currentOrder.value?.id === orderId) attachLoading.value = false
  }
}

// 时间格式化：后端 LocalDateTime 默认序列化为 ISO 带 T（如 2026-07-28T09:33:07），
// 统一转成 "yyyy-MM-dd HH:mm:ss" 显示。用字符串处理而非 new Date()，避免时区解析差异。
const formatDateTime = (val) => {
  if (val == null || val === '') return '—'
  return String(val)
    .replace('T', ' ')
    .replace(/\.\d+/, '')          // 去掉小数秒（毫秒/微秒/纳秒）
    .replace(/Z$/, '')            // 去掉 UTC 标记
    .trim()
}

const copyOrderSo = async () => {
  const so = currentOrder.value?.orderSo || currentOrder.value?.orderNo
  if (!so) return
  try {
    await navigator.clipboard.writeText(so)
    ElMessage.success('SO 号已复制')
  } catch {
    ElMessage.info(`SO 号：${so}`)
  }
}

const editCurrentOrder = () => {
  if (currentOrder.value) {
    detailVisible.value = false
    openDialog(currentOrder.value)
  }
}

// 编辑弹窗与详情抽屉共用的附件预览入口：Excel/Word 走页内预览，其余文件下载后新窗口打开
const previewOrderAttachment = async (orderId, filename) => {
  if (!orderId) return
  if (isExcelFile(filename)) {
    openExcelPreview(orderId, filename)
  } else if (isWordFile(filename)) {
    openWordPreview(orderId, filename)
  } else {
    try {
      const blob = await orderApi.downloadAttachment(orderId, filename)
      const url = URL.createObjectURL(blob)
      window.open(url, '_blank')
      setTimeout(() => URL.revokeObjectURL(url), 60000)
    } catch { ElMessage.error('预览失败') }
  }
}

const previewDetailAttachment = (filename) => previewOrderAttachment(currentOrder.value?.id, filename)

const loadAttachments = async () => {
  if (!form.id) return
  try {
    const res = await orderApi.getAttachments(form.id)
    attachments.value = res.data || []
  } catch { attachments.value = [] }
}

const openExcelPreview = async (orderId, filename) => {
  excelPreviewTitle.value = filename
  excelPreviewFilename.value = filename
  excelPreviewOrderId.value = orderId
  excelPreviewSheets.value = []
  excelPreviewActiveSheet.value = ''
  excelPreviewLoading.value = true
  excelPreviewVisible.value = true
  try {
    const blob = await orderApi.downloadAttachment(orderId, filename)
    const workbook = XLSX.read(await blob.arrayBuffer(), { type: 'array', cellDates: true })
    excelPreviewSheets.value = workbook.SheetNames.map((name) => ({
      name,
      rows: XLSX.utils.sheet_to_json(workbook.Sheets[name], { header: 1, defval: '', raw: false })
    }))
    excelPreviewActiveSheet.value = workbook.SheetNames[0] || ''
    excelPreviewLoading.value = false
  } catch {
    excelPreviewLoading.value = false
    ElMessage.warning('表格解析失败，可尝试下载后用 Excel 打开')
  }
}
const selectExcelSheet = (name) => { excelPreviewActiveSheet.value = name }
const cleanupExcelPreview = () => {
  excelPreviewSheets.value = []
  excelPreviewActiveSheet.value = ''
}
const downloadExcelOriginal = async () => {
  if (!excelPreviewOrderId.value || !excelPreviewFilename.value) return
  try {
    const blob = await orderApi.downloadAttachment(excelPreviewOrderId.value, excelPreviewFilename.value)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = excelPreviewFilename.value; a.click()
    setTimeout(() => URL.revokeObjectURL(url), 5000)
  } catch { ElMessage.error('下载失败') }
}

const cleanupWordPreview = () => {
  if (wordPreviewMode.value === 'doc' && typeof wordPreviewSrc.value === 'string') {
    URL.revokeObjectURL(wordPreviewSrc.value)
  }
  wordPreviewSrc.value = null
  wordPreviewMode.value = ''
}
const openWordPreview = async (orderId, filename) => {
  cleanupWordPreview()
  wordPreviewTitle.value = filename
  wordPreviewFilename.value = filename
  wordPreviewOrderId.value = orderId
  wordPreviewLoading.value = true
  wordPreviewVisible.value = true
  try {
    if (filename.toLowerCase().endsWith('.docx')) {
      const blob = await orderApi.downloadAttachment(orderId, filename)
      wordPreviewMode.value = 'docx'
      wordPreviewSrc.value = await blob.arrayBuffer()
    } else {
      const blob = await orderApi.previewDocAttachment(orderId, filename)
      wordPreviewMode.value = 'doc'
      wordPreviewSrc.value = URL.createObjectURL(blob)
    }
  } catch {
    wordPreviewLoading.value = false
    ElMessage.error('Word 文档加载失败')
  }
}
const onWordPreviewError = () => {
  wordPreviewLoading.value = false
  ElMessage.warning('Word 文档解析失败，可尝试下载原文件')
}
const downloadWordOriginal = async () => {
  if (!wordPreviewOrderId.value || !wordPreviewFilename.value) return
  try {
    const blob = await orderApi.downloadAttachment(wordPreviewOrderId.value, wordPreviewFilename.value)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = wordPreviewFilename.value; a.click()
    setTimeout(() => URL.revokeObjectURL(url), 5000)
  } catch { ElMessage.error('下载失败') }
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

const previewAttachment = (filename) => previewOrderAttachment(form.id, filename)

onMounted(() => { loadData(); loadCustomers() })
</script>

<style scoped>
.main-card { margin-bottom: 0; }
.search-form :deep(.el-form-item) { margin-bottom: 10px; }
.toolbar { display: flex; justify-content: flex-end; margin-top: 12px; }
.pagination { margin-top: 16px; justify-content: flex-end; }
.order-form :deep(.el-input__inner) { font-size: 15px; }
.order-form :deep(.el-select .el-input__inner) { font-size: 15px; }

/* ── 订单详情：运输档案 ── */
.order-detail-drawer :deep(.el-drawer__header) { margin: 0; padding: 0; }
.order-detail-drawer :deep(.el-drawer__body) { padding: 0; background: #f3f6f7; }
.order-detail-drawer :deep(.el-drawer__footer) { padding: 12px 22px; border-top: 1px solid #dce5e6; background: #fff; }
.detail-drawer-header { display: flex; align-items: center; justify-content: flex-end; width: 100%; height: 48px; padding: 0 16px; }
.drawer-close { display: grid; place-items: center; width: 32px; height: 32px; padding: 0; border: 1px solid #d3dee0; border-radius: 50%; background: #fff; color: #456168; cursor: pointer; transition: transform 160ms ease-out, background-color 160ms ease, border-color 160ms ease, color 160ms ease; }
.drawer-close:hover { background: #164d58; border-color: #164d58; color: #fff; transform: rotate(90deg); }
.drawer-close:active, .copy-so:active, .attachment-card:active { transform: scale(.97); }
.shipment-dossier { min-height: 100%; color: #193b42; font-family: "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif; }
.dossier-hero { position: relative; overflow: hidden; padding: 28px 30px 24px; color: #f8fcfc; background: #164d58; }
.dossier-hero.transport-AIR { background: #245e81; }
.dossier-hero.transport-LAND { background: #6a542d; }
.hero-grid { position: absolute; inset: 0; opacity: .3; background-image: repeating-linear-gradient(90deg, transparent 0 29px, rgba(255,255,255,.1) 29px 30px), repeating-linear-gradient(0deg, transparent 0 29px, rgba(255,255,255,.1) 29px 30px); background-size: 30px 30px; }
.hero-topline, .hero-title-row, .hero-route, .hero-meta { position: relative; }
.hero-topline, .hero-title-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.dossier-kicker, .hero-label, .hero-route span, .hero-meta { font: 700 10px/1.3 "SFMono-Regular", Consolas, monospace; letter-spacing: .1em; }
.dossier-kicker { color: #b9d3d6; }
.dossier-status { display: inline-flex; align-items: center; gap: 7px; padding: 6px 10px; border: 1px solid rgba(255,255,255,.42); border-radius: 5px; background: rgba(255,255,255,.1); color: #fff; font-size: 12px; font-weight: 700; }
.dossier-status i { width: 6px; height: 6px; border-radius: 50%; background: #b7df67; box-shadow: 0 0 0 4px rgba(183,223,103,.15); }
.hero-title-row { margin-top: 25px; }
.hero-label { margin: 0 0 7px; color: #b9d3d6; }
.hero-title-row h2 { margin: 0; color: #fff; font: 700 clamp(25px, 3vw, 34px)/1.15 "SFMono-Regular", Consolas, monospace; letter-spacing: 0; overflow-wrap: anywhere; }
.copy-so { display: inline-flex; align-items: center; gap: 6px; flex: none; padding: 9px 11px; border: 1px solid rgba(255,255,255,.36); border-radius: 6px; background: rgba(255,255,255,.08); color: #fff; cursor: pointer; font: 700 11px/1 "PingFang SC", sans-serif; transition: transform 160ms ease-out, background-color 160ms ease, border-color 160ms ease, color 160ms ease; }
.copy-so:hover { border-color: #b7df67; background: #b7df67; color: #164d58; }
.hero-route { display: grid; grid-template-columns: minmax(0,1fr) 94px minmax(0,1fr); align-items: center; gap: 12px; margin-top: 28px; padding: 15px 16px; border: 1px solid rgba(255,255,255,.2); border-radius: 8px; background: rgba(6,40,47,.2); }
.hero-route span { display: block; margin-bottom: 7px; color: #b9d3d6; }
.hero-route strong { display: block; overflow-wrap: anywhere; color: #fff; font-size: 17px; line-height: 1.32; }
.route-destination { text-align: right; }
.route-line { position: relative; height: 1px; background: repeating-linear-gradient(90deg, #b7df67 0 7px, transparent 7px 12px); }
.route-line::after { content: ''; position: absolute; right: -1px; top: -4px; border-width: 5px 0 5px 7px; border-style: solid; border-color: transparent transparent transparent #b7df67; }
.route-vehicle { position: absolute; left: 50%; top: 50%; z-index: 1; display: grid !important; place-items: center; width: 30px; height: 30px; margin: 0 !important; border: 1px solid #b7df67; border-radius: 50%; background: #164d58; color: #fff !important; font-size: 14px !important; letter-spacing: 0 !important; transform: translate(-50%, -50%); }
.hero-meta { display: flex; flex-wrap: wrap; gap: 14px 26px; margin-top: 18px; color: #b9d3d6; letter-spacing: .04em; font-size: 11px; }
.hero-meta b { margin-left: 6px; color: #fff; font-weight: 700; letter-spacing: 0; font-size: 13px; }
.progress-panel { margin: 16px 22px 0; padding: 18px 20px 17px; border: 1px solid #dce5e6; border-radius: 8px; background: #fff; box-shadow: 0 4px 12px rgba(24,65,72,.05); }
.section-heading { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; margin-bottom: 14px; }
.section-heading > span { display: inline-flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 800; letter-spacing: 0; }
.section-heading > span::before { width: 3px; height: 14px; border-radius: 2px; background: #247989; content: ''; }
.section-heading > small { color: #71868b; font-size: 11px; }
.shipment-steps { display: grid; grid-template-columns: repeat(5, 1fr); }
.shipment-step { position: relative; min-width: 0; padding-top: 28px; color: #95a6aa; }
.shipment-step::before { content: ''; position: absolute; top: 9px; left: 0; width: 100%; height: 2px; background: #e5edef; }
.shipment-step:first-child::before { left: 50%; width: 50%; }
.shipment-step:last-child::before { width: 50%; }
.shipment-step.done::before { background: #65a075; }
.step-dot { position: absolute; top: 2px; left: 50%; z-index: 1; display: grid; place-items: center; width: 16px; height: 16px; border: 2px solid #dce7e9; border-radius: 50%; background: #fff; font-size: 9px; transform: translateX(-50%); }
.shipment-step.done .step-dot { border-color: #65a075; background: #65a075; color: #fff; }
.shipment-step.active .step-dot { border-color: #247989; box-shadow: 0 0 0 4px #d8edf0; }
.shipment-step strong, .shipment-step small { display: block; overflow: hidden; padding: 0 3px; text-align: center; text-overflow: ellipsis; white-space: nowrap; }
.shipment-step strong { color: inherit; font-size: 12px; }
.shipment-step small { margin-top: 3px; font-size: 10px; opacity: .78; }
.shipment-step.active { color: #195a66; }
.dossier-body { padding: 16px 22px 30px; }
.detail-block { margin-bottom: 12px; padding: 18px 20px; border: 1px solid #dce5e6; border-radius: 8px; background: #fff; box-shadow: 0 2px 8px rgba(24,65,72,.03); }
.detail-block .section-heading { margin-bottom: 16px; }
.schedule-grid { display: grid; grid-template-columns: repeat(4, 1fr); overflow: hidden; border: 1px solid #e2eaeb; border-radius: 6px; }
.schedule-cell { min-width: 0; padding: 13px 12px; border-right: 1px solid #e2eaeb; background: #f9fbfb; }
.schedule-cell:last-child { border-right: 0; }
.schedule-cell span, .cargo-stat span { display: block; margin-bottom: 6px; color: #71868b; font-size: 10px; }
.schedule-cell strong { display: block; overflow: hidden; color: #1e424a; font: 700 13px/1.3 "SFMono-Regular", Consolas, monospace; text-overflow: ellipsis; white-space: nowrap; }
.schedule-cell .amount-value { color: #b36f26; }
.cargo-layout { display: grid; grid-template-columns: 54px repeat(4, 1fr); align-items: stretch; overflow: hidden; border: 1px solid #d9e8e9; border-radius: 6px; background: #f4f9f9; }
.cargo-mark { display: grid; place-items: center; color: #247989; background: #dbeff0; }
.cargo-stat { padding: 13px 14px; border-right: 1px solid #d9e8e9; }
.cargo-stat:last-child { border: 0; }
.cargo-stat strong { display: block; color: #164d58; font: 800 18px/1.1 "SFMono-Regular", Consolas, monospace; letter-spacing: 0; }
.cargo-stat em { margin-left: 4px; color: #60777c; font: 700 10px/1 "PingFang SC", sans-serif; letter-spacing: 0; }
.detail-facts dl { display: grid; grid-template-columns: 1fr 1fr; gap: 0 26px; margin: 0; }
.detail-facts dl > div { display: grid; grid-template-columns: 79px minmax(0,1fr); align-items: center; min-height: 42px; border-bottom: 1px solid #edf2f2; }
.detail-facts dt { color: #71868b; font-size: 12px; }
.detail-facts dd { margin: 0; overflow-wrap: anywhere; color: #1e424a; font-size: 13px; font-weight: 700; }
.transport-pill { display: inline-block; padding: 4px 8px; border-radius: 4px; background: #e5f2f3; color: #19606d; font-size: 12px; }
.remark-block { border-color: #ead8b8; background: #fffdf8; }
.remark-block .section-heading > span::before { background: #bd7b23; }
.remark-block p { margin: 0; white-space: pre-wrap; color: #69542e; font-size: 13px; line-height: 1.8; }
.attachment-list { display: grid; gap: 8px; }
.attachment-card { display: grid; grid-template-columns: 20px minmax(0, 1fr) auto; align-items: center; gap: 10px; width: 100%; padding: 11px; border: 1px solid #dce7e8; border-radius: 6px; background: #fbfdfd; color: #285a64; cursor: pointer; text-align: left; transition: transform 160ms ease-out, border-color 160ms ease, background-color 160ms ease; }
.attachment-card:hover { border-color: #76aeb7; background: #f0f8f9; transform: translateX(2px); }
.attachment-card span { overflow: hidden; color: #234951; font-size: 12px; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
.attachment-card b { color: #51777e; font-size: 10px; font-weight: 700; }
.attachment-empty { padding: 13px; border: 1px dashed #b9cdd0; border-radius: 6px; color: #72878c; text-align: center; font-size: 12px; }
.dossier-footer { display: flex; align-items: center; justify-content: space-between; gap: 14px; }
.dossier-footer > span { color: #71868b; font-size: 11px; }
.dossier-footer :deep(.el-button) { min-width: 110px; background: #1a6170; border-color: #1a6170; transition: transform 160ms ease-out, background-color 160ms ease, border-color 160ms ease; }
.dossier-footer :deep(.el-button:hover) { background: #164d58; border-color: #164d58; }
.dossier-footer :deep(.el-button:active) { transform: scale(.97); }

/* ── 操作栏 ── */
.action-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
.action-group .el-button + .el-button {
  margin-left: 0;
}
.dropdown-arrow {
  font-size: 10px;
  margin-left: 2px;
}

/* ── 移动端适配 ── */
@media (max-width: 768px) {
  .main-card :deep(.el-card__body) { padding: 12px; }
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

  .dossier-hero { padding: 21px 18px 18px; }
  .hero-title-row { align-items: flex-start; }
  .hero-title-row h2 { font-size: 25px; }
  .copy-so { padding: 8px; font-size: 0; }
  .copy-so svg { width: 17px; height: 17px; }
  .hero-route { grid-template-columns: minmax(0, 1fr) 55px minmax(0, 1fr); margin-top: 24px; }
  .hero-route strong { font-size: 14px; }
  .hero-meta { gap: 7px 14px; font-size: 10px; }
  .hero-meta b { font-size: 12px; }
  .progress-panel, .dossier-body { margin-left: 12px; margin-right: 12px; }
  .progress-panel { padding: 17px 10px 14px; }
  .dossier-body { padding: 14px 0 22px; }
  .detail-block { padding: 16px 14px; }
  .shipment-step strong { font-size: 11px; }
  .shipment-step small { display: none; }
  .schedule-grid { grid-template-columns: 1fr 1fr; }
  .schedule-cell:nth-child(2) { border-right: 0; }
  .schedule-cell:nth-child(-n+2) { border-bottom: 1px solid #e5e9e4; }
  .cargo-layout { grid-template-columns: 45px repeat(4, 1fr); }
  .cargo-stat { padding: 11px 8px; }
  .cargo-stat strong { font-size: 14px; }
  .detail-facts dl { grid-template-columns: 1fr; }
  .detail-facts dl > div { grid-template-columns: 84px minmax(0, 1fr); }
  .dossier-footer { padding: 0 2px; }
}
</style>

<style>
/* el-drawer 通过 teleport 渲染到 body，scoped 的 :deep 可能匹配不到，
   这里用全局选择器兜底，确保删除标题栏后顶部不留默认间距 */
.order-detail-drawer .el-drawer__header { margin: 0 !important; padding: 0 !important; }
.order-detail-drawer .el-drawer__body {
  padding: 0 !important;
  scrollbar-gutter: stable;
}
.order-detail-drawer.el-drawer { transition-property: transform !important; }

@media (max-width: 768px) {
  .order-detail-drawer.el-drawer { width: 100vw !important; }
}

/* ── Excel 在线预览弹窗 ── */
.excel-preview-dialog {
  display: flex;
  flex-direction: column;
  height: 94vh;
  margin-bottom: 0;
}
.excel-preview-dialog .el-dialog__header { flex: none; padding: 20px 24px 16px; }
.excel-preview-dialog .el-dialog__body {
  flex: 1;
  min-height: 0;
  padding: 0 24px;
  overflow: hidden;
}
.excel-preview-dialog .el-dialog__footer { flex: none; padding: 16px 24px 20px; }
.excel-preview-body {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  border: 1px solid #dcdfe6;
  background: #fff;
}
.excel-sheet-tabs { display: flex; flex: none; gap: 2px; padding: 8px 10px 0; border-bottom: 1px solid #dcdfe6; overflow-x: auto; }
.excel-sheet-tab { flex: none; max-width: 200px; overflow: hidden; padding: 7px 12px; border: 0; border-radius: 4px 4px 0 0; background: transparent; color: #606266; cursor: pointer; text-overflow: ellipsis; white-space: nowrap; }
.excel-sheet-tab:hover { background: #f5f7fa; color: #409eff; }
.excel-sheet-tab.active { background: #ecf5ff; color: #409eff; font-weight: 600; }
.excel-table-wrap { flex: 1; min-height: 0; overflow: auto; }
.excel-table { min-width: 100%; border-spacing: 0; border-collapse: separate; font-size: 13px; }
.excel-table th, .excel-table td { min-width: 96px; max-width: 360px; padding: 7px 10px; border-right: 1px solid #ebeef5; border-bottom: 1px solid #ebeef5; color: #303133; text-align: left; vertical-align: top; white-space: pre-wrap; word-break: break-word; }
.excel-table th { position: sticky; left: 0; z-index: 1; min-width: 44px; width: 44px; padding: 7px 0; background: #f5f7fa; color: #909399; text-align: center; font-weight: 500; }
.excel-table tr:nth-child(even) td { background: #fafafa; }
.excel-preview-footer { display: flex; justify-content: flex-end; gap: 10px; }

.word-preview-dialog {
  display: flex;
  flex-direction: column;
  height: 94vh;
  margin-bottom: 0;
}
.word-preview-dialog .el-dialog__header { flex: none; padding: 20px 24px 16px; }
.word-preview-dialog .el-dialog__body {
  flex: 1;
  min-height: 0;
  padding: 0 24px;
  overflow: hidden;
}
.word-preview-dialog .el-dialog__footer { flex: none; padding: 16px 24px 20px; }
.word-preview-body {
  height: 100%;
  min-height: 0;
  overflow: auto;
  border: 1px solid #dcdfe6;
  background: #eef0f3;
}
.word-preview-body iframe { display: block; width: 100%; height: 100%; border: 0; background: #fff; }
.word-preview-footer { display: flex; justify-content: flex-end; gap: 10px; }

@media (max-width: 768px) {
  .excel-preview-dialog,
  .word-preview-dialog { width: 96vw !important; height: 96vh; margin-top: 2vh !important; }
  .excel-preview-dialog .el-dialog__header,
  .word-preview-dialog .el-dialog__header { padding: 14px 16px 12px; }
  .excel-preview-dialog .el-dialog__body,
  .word-preview-dialog .el-dialog__body { padding: 0 12px; }
  .excel-preview-dialog .el-dialog__footer,
  .word-preview-dialog .el-dialog__footer { padding: 12px 16px 14px; }
}
</style>

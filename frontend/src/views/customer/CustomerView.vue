<template>
  <div class="customer-page">
    <!-- 统计卡片 -->
    <div class="stat-row">
      <div class="stat-card" style="background: linear-gradient(135deg, #667eea, #764ba2)">
        <div class="stat-icon"><el-icon :size="28"><User /></el-icon></div>
        <div class="stat-info">
          <span class="stat-label">客户总数</span>
          <span class="stat-value">{{ total }}</span>
        </div>
      </div>
      <div class="stat-card" style="background: linear-gradient(135deg, #f093fb, #f5576c)">
        <div class="stat-icon"><el-icon :size="28"><ChatDotRound /></el-icon></div>
        <div class="stat-info">
          <span class="stat-label">活跃客户</span>
          <span class="stat-value">{{ activeCount }}</span>
        </div>
      </div>
    </div>

    <!-- 搜索 + 操作 -->
    <el-card class="content-card" shadow="never">
      <div class="table-toolbar">
        <el-input v-model="keyword" placeholder="搜索公司名 / 联系人 / 微信 / WhatsApp" clearable
          :prefix-icon="Search" class="search-input" @change="loadData" />
        <el-button type="primary" :icon="Plus" @click="openDialog()" round>新增客户</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="tableData" v-loading="loading" stripe
        :header-cell-style="{ background:'#f8f9fc', color:'#606266', fontWeight:600, fontSize:'13px' }"
        :cell-style="{ padding:'12px 0' }"
        highlight-current-row>
        <el-table-column label="客户信息" min-width="220">
          <template #default="{ row }">
            <div class="customer-cell">
              <div class="customer-avatar" :style="{ background: avatarColor(row.companyName) }">
                {{ (row.companyName || '?')[0] }}
              </div>
              <div class="customer-info">
                <span class="customer-name">{{ row.companyName }}</span>
                <span class="customer-contact">{{ row.contactName || '—' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="联系方式" min-width="220">
          <template #default="{ row }">
            <div class="contact-row">
              <template v-if="row.wechat">
                <el-icon size="14" color="#67c23a"><ChatDotRound /></el-icon>
                <span class="contact-text">{{ row.wechat }}</span>
              </template>
              <template v-if="row.whatsapp">
                <el-icon size="14" color="#25d366"><ChatLineSquare /></el-icon>
                <span class="contact-text">{{ row.whatsapp }}</span>
              </template>
              <template v-if="row.phone">
                <el-icon size="14" color="#909399"><Phone /></el-icon>
                <span class="contact-text">{{ row.phone }}</span>
              </template>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="邮箱" min-width="180">
          <template #default="{ row }">
            <a v-if="row.email" :href="'mailto:' + row.email" class="email-link">
              <el-icon size="14"><Message /></el-icon>
              {{ row.email }}
            </a>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="执照" width="100" align="center">
          <template #default="{ row }">
            <template v-if="row.photoUrl">
              <el-button v-if="isPdf(row.photoUrl)" link type="warning" size="small"
                @click="openPdf(row.photoUrl)">
                <el-icon><Document /></el-icon> PDF
              </el-button>
              <el-button v-else link type="primary" size="small"
                @click="previewPhoto(row.photoUrl)">
                <el-icon><Picture /></el-icon> 查看
              </el-button>
            </template>
            <span v-else class="text-muted" style="font-size:12px">—</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.remark" class="remark-text">{{ row.remark }}</span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDialog(row)">
              <el-icon><Edit /></el-icon>编辑
            </el-button>
            <el-divider direction="vertical" />
            <el-popconfirm title="确认删除？" @confirm="handleDelete(row.id)" width="180">
              <template #reference>
                <el-button link type="danger" size="small">
                  <el-icon><Delete /></el-icon>删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize"
          :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
          @change="loadData" background />
      </div>
    </el-card>

    <!-- 弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑客户' : '新增客户'" width="680px"
      :close-on-click-modal="false" destroy-on-close class="customer-dialog">
      <div class="dialog-body">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" label-position="left">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="公司名称" prop="companyName">
                <el-input v-model="form.companyName" placeholder="请输入公司名称" size="large" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="联系人" prop="contactName">
                <el-input v-model="form.contactName" placeholder="请输入联系人" size="large" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="微信" prop="wechat">
                <el-input v-model="form.wechat" placeholder="微信号" size="large">
                  <template #prefix><el-icon color="#67c23a"><ChatDotRound /></el-icon></template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="WhatsApp">
                <el-input v-model="form.whatsapp" placeholder="WhatsApp号码" size="large">
                  <template #prefix><el-icon color="#25d366"><ChatLineSquare /></el-icon></template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="电话">
                <el-input v-model="form.phone" placeholder="联系电话" size="large">
                  <template #prefix><el-icon><Phone /></el-icon></template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="邮箱">
                <el-input v-model="form.email" placeholder="邮箱地址" size="large">
                  <template #prefix><el-icon><Message /></el-icon></template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="地址">
            <el-input v-model="form.address" type="textarea" :rows="2" placeholder="公司地址" />
          </el-form-item>
          <el-form-item label="营业执照">
            <el-upload
              ref="uploadRef"
              :action="uploadAction"
              :headers="uploadHeaders"
              :on-success="onUploadSuccess"
              :on-error="onUploadError"
              :before-upload="beforeUpload"
              :show-file-list="false"
              accept="image/*,.pdf"
            >
              <div v-if="form.photoUrl" class="photo-preview">
                <template v-if="isPdf(form.photoUrl)">
                  <div class="pdf-badge">
                    <el-icon size="24" color="#e6a23c"><Document /></el-icon>
                    <span>PDF 已上传</span>
                  </div>
                </template>
                <template v-else>
                  <el-image
                    :src="getPhotoSrc(form.photoUrl)"
                    fit="cover"
                    class="photo-thumb"
                    :preview-src-list="[getPhotoSrc(form.photoUrl)]"
                    preview-teleported />
                </template>
                <el-button type="danger" size="small" circle :icon="Close"
                  class="photo-remove" @click.stop="removePhoto" />
              </div>
              <div v-else class="upload-trigger">
                <el-icon size="24" color="#c0c4cc"><Plus /></el-icon>
                <span>上传营业执照</span>
              </div>
            </el-upload>
          </el-form-item>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="状态">
                <el-switch v-model="form.status" :active-value="1" :inactive-value="0"
                  active-text="启用" inactive-text="禁用" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注信息" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false" size="large">取 消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave" size="large">保 存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 图片预览 -->
    <el-dialog v-model="photoPreviewVisible" title="营业执照" width="600px" append-to-body>
      <div class="preview-wrap">
        <el-image :src="previewPhotoUrl" fit="contain" style="max-width:100%;max-height:70vh" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search, Plus, Upload, Delete, Document, User, Edit, Phone,
  ChatDotRound, ChatLineSquare, Message, Picture, Close
} from '@element-plus/icons-vue'
import { customerApi, fileApi } from '@/api'

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const uploadRef = ref()
const photoPreviewVisible = ref(false)
const previewPhotoUrl = ref('')

const activeCount = computed(() => tableData.value.filter(c => c.status === 1).length)

const avatarColors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#764ba2', '#f093fb', '#00d2ff']
const avatarColor = (name) => {
  if (!name) return '#409eff'
  return avatarColors[name.charCodeAt(0) % avatarColors.length]
}

const uploadAction = import.meta.env.DEV ? '/api/files/upload/business-license' : '/api/files/upload/business-license'
const uploadHeaders = { Authorization: `Bearer ${localStorage.getItem('token')}` }

const form = reactive({
  id: null, companyName: '', contactName: '', wechat: '', whatsapp: '',
  phone: '', email: '', address: '', remark: '', status: 1, photoUrl: ''
})
const rules = {
  companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  wechat: [{ required: true, message: '请输入微信号', trigger: 'blur' }]
}

const getPhotoSrc = (filename) => filename ? fileApi.getPhotoUrl(filename) : ''

const beforeUpload = (file) => {
  const allowed = file.type.startsWith('image/') || file.type === 'application/pdf'
  if (!allowed) { ElMessage.error('仅支持图片或 PDF'); return false }
  if (file.size / 1024 / 1024 > 5) { ElMessage.error('文件不能超过 5MB'); return false }
  return true
}

const isPdf = (filename) => filename?.toLowerCase().endsWith('.pdf')

const onUploadSuccess = (res) => {
  if (res.code === 200) { form.photoUrl = res.data; ElMessage.success('上传成功') }
  else ElMessage.error(res.message || '上传失败')
}
const onUploadError = () => ElMessage.error('上传失败')

const removePhoto = () => { form.photoUrl = '' }

const previewPhoto = (filename) => {
  previewPhotoUrl.value = fileApi.getPhotoUrl(filename)
  photoPreviewVisible.value = true
}
const openPdf = (filename) => window.open(fileApi.getPhotoUrl(filename), '_blank')

const loadData = async () => {
  loading.value = true
  try {
    const res = await customerApi.page({ keyword: keyword.value, pageNum: pageNum.value, pageSize: pageSize.value })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

const openDialog = (row = null) => {
  isEdit.value = !!row
  Object.assign(form, row ? { ...row } : {
    id: null, companyName: '', contactName: '', wechat: '', whatsapp: '',
    phone: '', email: '', address: '', remark: '', status: 1, photoUrl: ''
  })
  dialogVisible.value = true
}

const handleSave = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    const { id, companyName, contactName, wechat, whatsapp, phone, email, address, remark, status, photoUrl } = form
    if (isEdit.value) await customerApi.update({ id, companyName, contactName, wechat, whatsapp, phone, email, address, remark, status, photoUrl })
    else await customerApi.save({ companyName, contactName, wechat, whatsapp, phone, email, address, remark, status, photoUrl })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally { saving.value = false }
}

const handleDelete = async (id) => {
  await customerApi.delete(id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.customer-page { max-width: 1400px; }

/* 统计卡片 */
.stat-row { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; margin-bottom: 20px; }
.stat-card {
  padding: 20px 24px; border-radius: 12px; color: #fff;
  display: flex; align-items: center; gap: 16px;
  transition: transform 0.2s; cursor: default;
}
.stat-card:hover { transform: translateY(-2px); }
.stat-icon {
  width: 52px; height: 52px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  background: rgba(255,255,255,0.2);
}
.stat-label { font-size: 13px; opacity: 0.9; }
.stat-value { font-size: 28px; font-weight: 700; display: block; margin-top: 2px; }

/* 表格容器 */
.content-card { border-radius: 10px; border: none; box-shadow: 0 2px 12px rgba(0,0,0,0.04); }
.content-card :deep(.el-card__body) { padding: 0 20px 20px; }

.table-toolbar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20px 0 16px;
}
.search-input { width: 360px; }

/* 客户单元格 */
.customer-cell { display: flex; align-items: center; gap: 12px; }
.customer-avatar {
  width: 40px; height: 40px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-weight: 600; font-size: 16px; flex-shrink: 0;
}
.customer-info { display: flex; flex-direction: column; }
.customer-name { font-weight: 600; font-size: 14px; color: #303133; }
.customer-contact { font-size: 12px; color: #909399; margin-top: 2px; }

/* 联系方式 */
.contact-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.contact-text { font-size: 13px; color: #606266; }

/* 邮箱 */
.email-link {
  display: inline-flex; align-items: center; gap: 6px;
  color: #409eff; text-decoration: none; font-size: 13px;
}
.email-link:hover { color: #66b1ff; }

/* 通用 */
.text-muted { color: #c0c4cc; font-size: 13px; }
.remark-text { font-size: 13px; color: #909399; }
.pagination-wrap { display: flex; justify-content: flex-end; padding: 16px 0 4px; }

/* 弹窗 */
.customer-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid #ebeef5; padding: 20px 24px;
}
.dialog-body { padding: 8px 0; }
.dialog-footer { text-align: right; }

/* 上传 */
.upload-trigger {
  width: 120px; height: 80px; border: 2px dashed #dcdfe6; border-radius: 8px;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 6px; cursor: pointer; color: #909399; font-size: 12px;
  transition: all 0.2s;
}
.upload-trigger:hover { border-color: #409eff; color: #409eff; }
.photo-preview { position: relative; display: inline-block; }
.photo-thumb {
  width: 120px; height: 80px; border-radius: 8px;
  border: 1px solid #ebeef5; object-fit: cover;
}
.pdf-badge {
  display: flex; align-items: center; gap: 8px;
  background: #fdf6ec; border: 1px solid #faecd8; border-radius: 8px;
  padding: 10px 16px; color: #e6a23c; font-size: 13px;
}
.photo-remove { position: absolute; top: -10px; right: -10px; }
.preview-wrap { text-align: center; }

/* ── 移动端 ── */
@media (max-width: 768px) {
  .stat-row { grid-template-columns: 1fr; gap: 10px; }
  .stat-value { font-size: 24px; }
  .table-toolbar { flex-direction: column; gap: 10px; }
  .search-input { width: 100% !important; }
  .table-toolbar .el-button { width: 100%; }
  .customer-cell .customer-avatar { width: 32px; height: 32px; font-size: 14px; }
  .customer-name { font-size: 13px; }
  :deep(.el-dialog) { width: 94vw !important; }
  :deep(.el-table) { font-size: 12px; }
  :deep(.el-table th), :deep(.el-table td) { padding: 8px 4px !important; }
  .pagination-wrap { justify-content: center; }
}
</style>

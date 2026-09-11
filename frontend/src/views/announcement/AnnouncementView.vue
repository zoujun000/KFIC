<template>
  <section class="announcement-page">
    <div class="page-heading">
      <div>
        <h2>公告栏</h2>
        <p>查看系统最新通知与相关文件</p>
      </div>
      <el-button v-if="userStore.isManager" type="primary" :icon="Plus" @click="openCreate">发布公告</el-button>
    </div>

    <el-card v-loading="loading" class="announcement-list" shadow="never">
      <div class="announcement-toolbar">
        <el-input v-model="titleKeyword" clearable :prefix-icon="Search" placeholder="搜索公告标题" />
      </div>
      <template v-if="announcements.length">
        <article v-for="item in announcements" :key="item.id" class="announcement-item">
          <div class="announcement-main">
            <div class="announcement-title-row">
              <el-icon class="notice-icon"><Bell /></el-icon>
              <h3>{{ item.title }}</h3>
            </div>
            <p class="announcement-content">{{ item.content }}</p>
            <div v-if="item.attachments && item.attachments.length" class="attachment-list">
              <div v-for="name in item.attachments" :key="name" class="attachment-row">
                <button class="attachment" type="button" @click="previewAttachment(item.id, name)">
                  <el-icon><Paperclip /></el-icon>
                  <span>{{ name }}</span>
                </button>
                <el-button link type="primary" :icon="View" @click="previewAttachment(item.id, name)">预览</el-button>
                <el-button link type="primary" :icon="Download" @click="downloadAttachment(item.id, name)">下载</el-button>
              </div>
            </div>
            <p class="announcement-time">
              发布时间：{{ formatDate(item.createTime) }}
              <span class="announcement-publisher">发布人：{{ item.publisherName || '未知用户' }}</span>
            </p>
          </div>
          <div v-if="userStore.isManager" class="announcement-actions">
            <el-button link type="primary" :icon="EditPen" @click="openEdit(item)">编辑</el-button>
            <el-popconfirm title="删除后无法恢复，确认删除该公告？" @confirm="removeAnnouncement(item.id)">
              <template #reference>
                <el-button link type="danger" :icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </article>
      </template>
      <el-empty v-else-if="!loading" description="暂无公告" :image-size="90" />

      <div v-if="total > pageSize" class="pagination">
        <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total"
          layout="prev, pager, next" @current-change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑公告' : '发布公告'" width="620px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="74px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="8" maxlength="5000" show-word-limit
            placeholder="请输入公告内容" />
        </el-form-item>
        <el-form-item label="附件">
          <div class="file-field">
            <div v-if="editingId && existingAttachments.length" class="existing-attachments">
              <div v-for="name in existingAttachments" :key="name" class="attachment-row">
                <button class="attachment" type="button" @click="downloadAttachment(editingId, name)">
                  <el-icon><Paperclip /></el-icon>
                  <span>{{ name }}</span>
                </button>
                <el-popconfirm :title="'确认删除附件「' + name + '」？'" @confirm="removeExistingAttachment(name)">
                  <template #reference>
                    <el-button link type="danger" :icon="Delete">删除</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </div>
            <el-upload :auto-upload="false" multiple :file-list="fileList"
              :on-change="handleFileChange" :on-remove="handleFileRemove">
              <el-button :icon="Upload">选择文件</el-button>
              <template #tip><div class="upload-tip">可选，支持多选；保存后上传，新文件追加，不会覆盖已有附件。</div></template>
            </el-upload>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAnnouncement">保存并发布</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" :title="previewTitle" width="90vw" top="3vh" destroy-on-close
      class="announcement-preview-dialog" @closed="cleanupPreview">
      <div v-loading="previewLoading" element-loading-text="正在加载预览…" class="preview-body">
        <el-image v-if="previewMode === 'image' && previewSrc" :src="previewSrc" fit="contain" class="image-preview" />
        <iframe v-else-if="previewMode === 'pdf' && previewSrc" :src="previewSrc" :title="previewTitle" />
        <iframe v-else-if="previewMode === 'doc' && previewSrc" :src="previewSrc" :title="previewTitle" sandbox />
        <VueOfficeDocx v-else-if="previewMode === 'docx' && previewSrc" :src="previewSrc"
          @rendered="previewLoading = false" @error="handlePreviewError" />
        <template v-else-if="previewMode === 'excel'">
          <div v-if="excelSheets.length" class="excel-preview">
            <div class="excel-tabs">
              <button v-for="sheet in excelSheets" :key="sheet.name" type="button"
                :class="['excel-tab', { active: sheet.name === activeSheet }]" @click="activeSheet = sheet.name">
                {{ sheet.name }}
              </button>
            </div>
            <div class="excel-table-wrap">
              <table class="excel-table"><tbody>
                <tr v-for="(row, rowIndex) in excelRows" :key="rowIndex">
                  <th>{{ rowIndex + 1 }}</th>
                  <td v-for="(cell, cellIndex) in row" :key="cellIndex">{{ cell }}</td>
                </tr>
              </tbody></table>
            </div>
          </div>
          <el-empty v-else-if="!previewLoading" description="表格中没有可显示的数据" />
        </template>
        <pre v-else-if="previewMode === 'text'" class="text-preview">{{ previewText }}</pre>
      </div>
      <template #footer>
        <el-button @click="downloadAttachment(previewId, previewTitle)">下载原文件</el-button>
        <el-button type="primary" @click="previewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, reactive, ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell, Delete, Download, EditPen, Paperclip, Plus, Search, Upload, View } from '@element-plus/icons-vue'
import { announcementApi } from '@/api'
import { useUserStore } from '@/store/user'
import { useDebounce } from '@/composables/useDebounce'
import VueOfficeDocx from '@vue-office/docx'
import '@vue-office/docx/lib/index.css'
import * as XLSX from 'xlsx'

defineOptions({ name: 'Announcements' })

const userStore = useUserStore()
const announcements = ref([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const titleKeyword = ref('')
const debouncedTitleKeyword = useDebounce(titleKeyword, 300)
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)
const existingAttachments = ref([])
const fileList = ref([])
const formRef = ref()
const form = reactive({ title: '', content: '' })
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewTitle = ref('')
const previewId = ref(null)
const previewMode = ref('')
const previewSrc = ref(null)
const previewText = ref('')
const excelSheets = ref([])
const activeSheet = ref('')
const excelRows = computed(() => excelSheets.value.find(sheet => sheet.name === activeSheet.value)?.rows || [])
const EXCEL_EXTS = ['.xls', '.xlsx', '.csv']
const TEXT_EXTS = ['.txt', '.md', '.json', '.xml', '.csv', '.log']
const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await announcementApi.page({ pageNum: pageNum.value, pageSize, title: debouncedTitleKeyword.value.trim() })
    announcements.value = res.data.records || []
    total.value = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}

watch(debouncedTitleKeyword, () => {
  pageNum.value = 1
  loadData()
})

const openCreate = () => {
  dialogVisible.value = true
}

const openEdit = (item) => {
  editingId.value = item.id
  form.title = item.title
  form.content = item.content
  existingAttachments.value = [...(item.attachments || [])]
  dialogVisible.value = true
}

const resetForm = () => {
  editingId.value = null
  existingAttachments.value = []
  fileList.value = []
  form.title = ''
  form.content = ''
  formRef.value?.clearValidate()
}

const handleFileChange = (file, files) => {
  fileList.value = files
}

const handleFileRemove = (file, files) => {
  fileList.value = files
}

const removeExistingAttachment = async (name) => {
  const res = await announcementApi.deleteAttachment(editingId.value, name)
  existingAttachments.value = res.data || []
  ElMessage.success('附件已删除')
}

const saveAnnouncement = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    let id = editingId.value
    if (id) {
      await announcementApi.update(id, form)
    } else {
      const res = await announcementApi.create(form)
      id = res.data.id
    }
    if (!id) throw new Error('公告创建后未找到记录')
    const files = fileList.value.map(item => item.raw).filter(Boolean)
    if (files.length) {
      const data = new FormData()
      files.forEach(file => data.append('files', file))
      await announcementApi.uploadAttachments(id, data)
    }
    ElMessage.success('公告已发布')
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

const removeAnnouncement = async (id) => {
  await announcementApi.delete(id)
  ElMessage.success('公告已删除')
  if (announcements.value.length === 1 && pageNum.value > 1) pageNum.value--
  await loadData()
}

const downloadAttachment = async (id, name) => {
  const blob = await announcementApi.downloadAttachment(id, name)
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = name
  link.click()
  URL.revokeObjectURL(url)
}

const getExtension = (name) => name.slice(name.lastIndexOf('.')).toLowerCase()

const previewAttachment = async (id, name) => {
  const extension = getExtension(name)
  previewTitle.value = name
  previewId.value = id
  previewMode.value = ''
  previewSrc.value = null
  previewText.value = ''
  excelSheets.value = []
  activeSheet.value = ''
  previewLoading.value = true
  previewVisible.value = true
  try {
    if (extension === '.doc') {
      const blob = await announcementApi.previewDocAttachment(id, name)
      previewMode.value = 'doc'
      previewSrc.value = URL.createObjectURL(blob)
    } else {
      const blob = await announcementApi.downloadAttachment(id, name)
      if (blob.type.startsWith('image/')) {
        previewMode.value = 'image'
        previewSrc.value = URL.createObjectURL(blob)
      } else if (extension === '.pdf') {
        previewMode.value = 'pdf'
        previewSrc.value = URL.createObjectURL(blob)
      } else if (extension === '.docx') {
        previewMode.value = 'docx'
        previewSrc.value = await blob.arrayBuffer()
      } else if (EXCEL_EXTS.includes(extension)) {
        const workbook = XLSX.read(await blob.arrayBuffer(), { type: 'array', cellDates: true })
        excelSheets.value = workbook.SheetNames.map(name => ({
          name,
          rows: XLSX.utils.sheet_to_json(workbook.Sheets[name], { header: 1, defval: '', raw: false })
        }))
        activeSheet.value = workbook.SheetNames[0] || ''
        previewMode.value = 'excel'
      } else if (TEXT_EXTS.includes(extension)) {
        previewMode.value = 'text'
        previewText.value = await blob.text()
      } else {
        previewVisible.value = false
        ElMessage.info('该文件格式暂不支持在线预览，请下载后查看')
      }
    }
  } catch {
    previewVisible.value = false
    ElMessage.error('文件预览失败')
  } finally {
    if (previewMode.value !== 'docx') previewLoading.value = false
  }
}

const handlePreviewError = () => {
  previewLoading.value = false
  ElMessage.warning('文档解析失败，可下载原文件后查看')
}

const cleanupPreview = () => {
  if (typeof previewSrc.value === 'string') URL.revokeObjectURL(previewSrc.value)
  previewMode.value = ''
  previewSrc.value = null
  previewText.value = ''
  excelSheets.value = []
}

const formatDate = (value) => value ? value.replace('T', ' ') : '—'

onMounted(loadData)
</script>

<style scoped>
.announcement-page { max-width: 1000px; margin: 0 auto; }
.page-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin: 4px 0 16px; }
.page-heading h2 { margin: 0; font-size: 20px; color: var(--text-primary); font-weight: 600; }
.page-heading p { margin: 6px 0 0; font-size: 13px; color: var(--text-muted); }
.announcement-list { border-radius: 8px; }
.announcement-toolbar { max-width: 320px; margin-bottom: 4px; }
.announcement-item { display: flex; gap: 18px; padding: 20px 4px; border-bottom: 1px solid var(--border-light); }
.announcement-item:last-of-type { border-bottom: 0; }
.announcement-main { min-width: 0; flex: 1; }
.announcement-title-row { display: flex; align-items: center; gap: 8px; }
.notice-icon { color: var(--color-primary); font-size: 17px; }
.announcement-title-row h3 { margin: 0; color: var(--text-primary); font-size: 16px; font-weight: 600; }
.announcement-content { margin: 12px 0; color: var(--text-regular); font-size: 14px; line-height: 1.75; white-space: pre-wrap; word-break: break-word; }
.announcement-time { margin: 12px 0 0; font-size: 12px; color: var(--text-muted); }
.announcement-publisher { margin-left: 16px; }
.attachment-list { display: flex; flex-direction: column; gap: 6px; }
.attachment-row { display: flex; align-items: center; gap: 4px; min-width: 0; }
.attachment { display: inline-flex; align-items: center; gap: 6px; min-width: 0; max-width: 100%; padding: 5px 8px; color: var(--color-primary); background: #ecf5ff; border: 0; border-radius: 4px; cursor: pointer; font-size: 13px; }
.attachment span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.attachment:hover { background: #d9ecff; }
.announcement-actions { display: flex; flex-shrink: 0; align-items: flex-start; gap: 4px; }
.file-field { display: flex; flex-direction: column; gap: 8px; width: 100%; }
.existing-attachments { display: flex; flex-direction: column; gap: 6px; }
.existing-attachments .attachment-row { justify-content: space-between; }
.upload-tip { color: var(--text-muted); line-height: 1.5; }
.pagination { display: flex; justify-content: flex-end; padding-top: 16px; }
@media (max-width: 768px) {
  .page-heading { align-items: center; }
  .announcement-toolbar { max-width: none; }
  .announcement-item { padding: 16px 0; gap: 8px; }
  .announcement-actions { flex-direction: column; gap: 0; }
  .file-field { display: block; }
}
</style>

<style>
.announcement-preview-dialog { display: flex; flex-direction: column; height: 94vh; margin-bottom: 0; }
.announcement-preview-dialog .el-dialog__header { flex: none; }
.announcement-preview-dialog .el-dialog__body { flex: 1; min-height: 0; padding-top: 0; overflow: hidden; }
.announcement-preview-dialog .el-dialog__footer { flex: none; }
.preview-body { height: 100%; min-height: 0; overflow: auto; border: 1px solid #dcdfe6; background: #fff; }
.preview-body > iframe { width: 100%; height: 100%; border: 0; }
.image-preview { display: flex; align-items: center; justify-content: center; width: 100%; height: 100%; background: #f5f7fa; }
.image-preview .el-image__inner { object-fit: contain; }
.text-preview { box-sizing: border-box; min-height: 100%; margin: 0; padding: 20px; color: #303133; font: 13px/1.7 monospace; white-space: pre-wrap; word-break: break-word; }
.excel-preview { display: flex; flex-direction: column; height: 100%; min-height: 0; }
.excel-tabs { display: flex; gap: 2px; padding: 8px 10px 0; border-bottom: 1px solid #dcdfe6; overflow-x: auto; }
.excel-tab { flex: none; max-width: 200px; overflow: hidden; padding: 7px 12px; border: 0; border-radius: 4px 4px 0 0; background: transparent; color: #606266; cursor: pointer; text-overflow: ellipsis; white-space: nowrap; }
.excel-tab:hover, .excel-tab.active { background: #ecf5ff; color: #409eff; }
.excel-tab.active { font-weight: 600; }
.excel-table-wrap { flex: 1; min-height: 0; overflow: auto; }
.excel-table { min-width: 100%; border-spacing: 0; border-collapse: separate; font-size: 13px; }
.excel-table th, .excel-table td { min-width: 96px; max-width: 360px; padding: 7px 10px; border-right: 1px solid #ebeef5; border-bottom: 1px solid #ebeef5; color: #303133; text-align: left; vertical-align: top; white-space: pre-wrap; word-break: break-word; }
.excel-table th { position: sticky; left: 0; z-index: 1; min-width: 44px; width: 44px; padding: 7px 0; background: #f5f7fa; color: #909399; text-align: center; font-weight: 500; }
@media (max-width: 768px) {
  .announcement-preview-dialog { width: 96vw !important; height: 96vh; margin-top: 2vh !important; }
  .announcement-preview-dialog .el-dialog__body { padding: 0 12px; }
}
</style>

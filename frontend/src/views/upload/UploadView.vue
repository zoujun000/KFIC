<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div style="display:flex;align-items:center;gap:8px">
              <el-icon size="20" color="#409eff"><Upload /></el-icon>
              <span style="font-weight:600">上传散货报价表</span>
            </div>
          </template>
          <el-upload ref="uploadQuoteRef" drag :auto-upload="false" accept=".xlsx,.xls"
            :limit="1" :on-change="f => quoteFile = f.raw"
            :on-exceed="() => ElMessage.warning('每次只能上传1个文件')">
            <el-icon size="36" color="#c0c4cc"><Upload /></el-icon>
            <div style="color:#606266;margin-top:6px">拖拽或点击上传 <em>.xlsx</em></div>
          </el-upload>
          <div v-if="quoteLog" class="log-badge">
            <el-tag type="success">新增 {{ quoteLog.inserted }}</el-tag>
            <el-tag type="warning">更新 {{ quoteLog.updated }}</el-tag>
            <el-tag>未变 {{ quoteLog.unchanged }}</el-tag>
            <span class="log-period">{{ quoteLog.validFrom }} ~ {{ quoteLog.validTo }}</span>
          </div>
          <el-button type="primary" :loading="uploadingQuote" :disabled="!quoteFile"
            style="margin-top:12px;width:100%" @click="doUploadQuote">
            解析散货报价入库
          </el-button>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div style="display:flex;align-items:center;gap:8px">
              <el-icon size="20" color="#67c23a"><Ship /></el-icon>
              <span style="font-weight:600">上传目的港收费标准</span>
            </div>
          </template>
          <el-upload ref="uploadPortRef" drag :auto-upload="false" accept=".xlsx,.xls"
            :limit="1" :on-change="f => portFile = f.raw"
            :on-exceed="() => ElMessage.warning('每次只能上传1个文件')">
            <el-icon size="36" color="#c0c4cc"><Upload /></el-icon>
            <div style="color:#606266;margin-top:6px">拖拽或点击上传 <em>.xlsx</em></div>
          </el-upload>
          <div v-if="portLog" class="log-badge">
            <el-tag type="success">新增 {{ portLog.inserted }}</el-tag>
            <el-tag type="warning">更新 {{ portLog.updated }}</el-tag>
            <el-tag>未变 {{ portLog.unchanged }}</el-tag>
          </div>
          <el-button type="primary" :loading="uploadingPort" :disabled="!portFile"
            style="margin-top:12px;width:100%" @click="doUploadPort">
            解析目的港费用入库
          </el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload, Ship } from '@element-plus/icons-vue'
import { quoteApi, portChargeApi } from '@/api'

defineOptions({ name: 'Upload' })

const uploadQuoteRef = ref()
const uploadPortRef = ref()
const quoteFile = ref(null)
const portFile = ref(null)
const uploadingQuote = ref(false)
const uploadingPort = ref(false)
const quoteLog = ref(null)
const portLog = ref(null)

const doUploadQuote = async () => {
  if (!quoteFile.value) return
  uploadingQuote.value = true
  const fd = new FormData(); fd.append('file', quoteFile.value)
  try {
    const res = await quoteApi.upload(fd)
    quoteLog.value = res.data
    ElMessage.success(`散货报价：新增${res.data.inserted} 更新${res.data.updated}`)
    uploadQuoteRef.value?.clearFiles(); quoteFile.value = null
  } finally { uploadingQuote.value = false }
}

const doUploadPort = async () => {
  if (!portFile.value) return
  uploadingPort.value = true
  const fd = new FormData(); fd.append('file', portFile.value)
  try {
    const res = await portChargeApi.upload(fd)
    portLog.value = res.data
    ElMessage.success(`目的港费用：新增${res.data.inserted} 更新${res.data.updated}`)
    uploadPortRef.value?.clearFiles(); portFile.value = null
  } finally { uploadingPort.value = false }
}
</script>

<style scoped>
.log-badge { display:flex; gap:8px; align-items:center; margin-top:10px; flex-wrap:wrap; }
.log-period { font-size:12px; color:#909399; }

/* ── 移动端适配 ── */
@media (max-width: 768px) {
  :deep(.el-row) {
    flex-direction: column;
  }
  :deep(.el-col) {
    max-width: 100% !important;
    flex: 0 0 100% !important;
    margin-bottom: 12px;
  }
}
</style>

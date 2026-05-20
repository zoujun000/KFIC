<template>
  <el-card>
    <template #header>
      <div class="page-header">
        <span class="page-title">
          <el-icon color="#764ba2"><Setting /></el-icon>
          角色管理
        </span>
        <el-tag type="info" size="small">仅管理员可见</el-tag>
      </div>
    </template>

    <el-table :data="tableData" v-loading="loading" stripe border
      :header-cell-style="{ background:'#f8f9fc', color:'#606266', fontWeight:600 }">
      <el-table-column label="用户" min-width="160">
        <template #default="{ row }">
          <div class="user-cell">
            <div class="user-avatar" :style="{ background: colorMap[row.id % colorMap.length] }">
              {{ (row.realName || row.username || '?')[0] }}
            </div>
            <div class="user-info">
              <span class="user-name">{{ row.realName || row.username }}</span>
              <span class="user-account">@{{ row.username }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="邮箱" min-width="160">
        <template #default="{ row }">{{ row.email || '—' }}</template>
      </el-table-column>
      <el-table-column label="电话" width="130">
        <template #default="{ row }">{{ row.phone || '—' }}</template>
      </el-table-column>
      <el-table-column label="角色" width="160">
        <template #default="{ row }">
          <el-select v-model="roleMap[row.id]" size="small" @change="(v) => handleRoleChange(row, v)"
            :disabled="row.id === currentUserId" style="width:130px">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="系统维护员" value="MAINTAINER" />
            <el-option label="普通用户" value="USER" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" @change="(v) => handleStatusChange(row, v)"
            :disabled="row.id === currentUserId"
            active-color="#67c23a" inactive-color="#f56c6c" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ row.createTime || '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center">
        <template #default="{ row }">
          <el-popconfirm title="确认重置密码为 123456 ？" @confirm="handleResetPwd(row.id)">
            <template #reference>
              <el-button link type="warning" size="small">重置密码</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Setting } from '@element-plus/icons-vue'
import request from '@/utils/request'

defineOptions({ name: 'UserManage' })

const loading = ref(false)
const tableData = ref([])
const roleMap = reactive({})
const currentUserId = ref(null)
const colorMap = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#764ba2', '#00b4d8']

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/users')
    tableData.value = res.data
    tableData.value.forEach(u => { roleMap[u.id] = u.role })
    currentUserId.value = Number(localStorage.getItem('userId') || 0)
  } finally { loading.value = false }
}

const handleRoleChange = async (row, newRole) => {
  try {
    await request.put(`/users/${row.id}/role`, { role: newRole })
    row.role = newRole
    ElMessage.success(`角色已更新为 ${newRole === 'ADMIN' ? '管理员' : newRole === 'MAINTAINER' ? '系统维护员' : '普通用户'}`)
  } catch {
    roleMap[row.id] = row.role
    ElMessage.error('更新失败')
  }
}

const handleStatusChange = async (row, enabled) => {
  try {
    await request.put(`/users/${row.id}/status`, { status: enabled ? 1 : 0 })
    row.status = enabled ? 1 : 0
    ElMessage.success(enabled ? '已启用' : '已禁用')
  } catch { ElMessage.error('操作失败') }
}

const handleResetPwd = async (id) => {
  try {
    await request.put(`/users/${id}/reset-pwd`)
    ElMessage.success('密码已重置为 123456')
  } catch { ElMessage.error('重置失败') }
}

onMounted(loadData)
</script>

<style scoped>
.page-header { display: flex; align-items: center; gap: 12px; }
.page-title { font-size: 16px; font-weight: 600; display: flex; align-items: center; gap: 8px; }
.user-cell { display: flex; align-items: center; gap: 10px; }
.user-avatar {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-weight: 600; font-size: 15px; flex-shrink: 0;
}
.user-info { display: flex; flex-direction: column; }
.user-name { font-weight: 600; font-size: 14px; color: #303133; }
.user-account { font-size: 12px; color: #909399; }

@media (max-width: 768px) {
  :deep(.el-table) { font-size: 12px; }
  :deep(.el-table th), :deep(.el-table td) { padding: 8px 4px; }
}
</style>

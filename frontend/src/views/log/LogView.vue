<template>
  <el-card>
    <div class="toolbar">
      <el-radio-group v-model="filterType" @change="loadData">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="QUOTE_UPLOAD">散货报价上传</el-radio-button>
        <el-radio-button value="PORT_CHARGE_UPLOAD">目的港费用上传</el-radio-button>
      </el-radio-group>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border style="margin-top:16px"
      :header-cell-style="{ background: '#fafafa', color: '#606266', fontWeight: 600 }">
      <el-table-column label="类型" width="140">
        <template #default="{ row }">
          <el-tag :type="row.type === 'QUOTE_UPLOAD' ? 'primary' : 'success'" size="small" effect="plain">
            <el-icon style="margin-right:4px"><component :is="row.type === 'QUOTE_UPLOAD' ? 'Goods' : 'Ship'" /></el-icon>
            {{ row.typeName }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="fileName" label="文件名" min-width="260" show-overflow-tooltip />
      <el-table-column label="有效期" width="160">
        <template #default="{ row }">
          <template v-if="row.validFrom">
            <span style="font-size:12px;color:#909399">{{ row.validFrom }} ~ {{ row.validTo }}</span>
          </template>
          <span v-else style="color:#ccc">—</span>
        </template>
      </el-table-column>
      <el-table-column label="解析结果" width="240">
        <template #default="{ row }">
          <div class="stats">
            <span class="stat-tag total">总数 {{ row.total }}</span>
            <span class="stat-tag inserted">新增 {{ row.inserted }}</span>
            <span class="stat-tag updated">更新 {{ row.updated }}</span>
            <span class="stat-tag unchanged">未变 {{ row.unchanged }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作时间" width="170">
        <template #default="{ row }">{{ row.createTime }}</template>
      </el-table-column>
    </el-table>

    <div v-if="!loading && tableData.length === 0" style="text-align:center;padding:60px 0;color:#c0c4cc">
      <el-empty description="暂无日志记录" />
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Goods, Ship } from '@element-plus/icons-vue'
import { logApi } from '@/api'

const loading = ref(false)
const tableData = ref([])
const filterType = ref('')

const loadData = async () => {
  loading.value = true
  try {
    const res = await logApi.list(filterType.value || undefined)
    tableData.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; }
.stats {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.stat-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}
.stat-tag.total { background: #f0f2f5; color: #606266; }
.stat-tag.inserted { background: #e1f3d8; color: #67c23a; }
.stat-tag.updated { background: #faecd8; color: #e6a23c; }
.stat-tag.unchanged { background: #f0f2f5; color: #909399; }
</style>

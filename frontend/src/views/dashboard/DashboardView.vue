<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <div class="stat-row">
      <div
        v-for="item in stats"
        :key="item.label"
        class="stat-card"
        :style="{ background: item.bg }"
      >
        <div class="stat-icon">
          <el-icon :size="36" :color="item.color"><component :is="item.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-label">{{ item.label }}</span>
          <span class="stat-value">{{ item.value }}</span>
        </div>
      </div>
    </div>

    <!-- 订单概览 -->
    <el-row :gutter="20" style="margin-top:20px">
      <!-- 进行中的订单 -->
      <el-col :span="12">
        <el-card class="order-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-title">
                <el-icon color="#E6A23C"><Loading /></el-icon>
                进行中
              </span>
              <el-tag type="warning" round>{{ inProgressOrders.length }}</el-tag>
            </div>
          </template>
          <div v-if="inProgressOrders.length === 0" class="empty-hint">暂无进行中的订单</div>
          <div
            v-for="order in inProgressOrders"
            :key="order.id"
            class="order-item"
            @click="$router.push('/orders')"
          >
            <div class="order-top">
              <div class="order-route">
                <span class="origin">{{ order.origin || '—' }}</span>
                <el-icon class="arrow-icon"><Right /></el-icon>
                <span class="dest">{{ order.destination || '—' }}</span>
              </div>
              <el-tag :type="statusTag[order.status]" size="small" effect="dark">
                {{ statusLabel[order.status] }}
              </el-tag>
            </div>
            <div class="order-meta">
              <span class="order-no">{{ order.orderSo || order.orderNo }}</span>
              <span class="order-customer">{{ customerNameMap[order.customerId] || '—' }}</span>
              <span class="order-ship">
                <el-tag :type="shipTag[order.shipType]" size="small">{{ shipLabel[order.shipType] }}</el-tag>
                <el-tag v-if="order.tradeTerms" size="small" effect="plain">{{ order.tradeTerms }}</el-tag>
              </span>
            </div>
            <div v-if="order.etd" class="order-date">
              <el-icon size="14"><Calendar /></el-icon>
              ETD {{ order.etd }}
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 最近创建的订单 -->
      <el-col :span="12">
        <el-card class="order-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-title">
                <el-icon color="#409EFF"><Clock /></el-icon>
                最近创建
              </span>
              <el-button link type="primary" @click="$router.push('/orders')">查看全部</el-button>
            </div>
          </template>
          <div v-if="recentOrders.length === 0" class="empty-hint">暂无订单</div>
          <div
            v-for="order in recentOrders"
            :key="order.id"
            class="order-item"
            @click="$router.push('/orders')"
          >
            <div class="order-top">
              <div class="order-route">
                <span class="origin">{{ order.origin || '—' }}</span>
                <el-icon class="arrow-icon"><Right /></el-icon>
                <span class="dest">{{ order.destination || '—' }}</span>
              </div>
              <el-tag :type="statusTag[order.status]" size="small" effect="dark">
                {{ statusLabel[order.status] }}
              </el-tag>
            </div>
            <div class="order-meta">
              <span class="order-no">{{ order.orderSo || order.orderNo }}</span>
              <span class="order-customer">{{ customerNameMap[order.customerId] || '—' }}</span>
              <span class="order-ship">
                <el-tag :type="shipTag[order.shipType]" size="small">{{ shipLabel[order.shipType] }}</el-tag>
              </span>
            </div>
            <div v-if="order.cargoName" class="order-cargo">
              <el-icon size="14"><Box /></el-icon>
              {{ order.cargoName }}
              <template v-if="order.cargoWeight"> · {{ order.cargoWeight }}KG</template>
              <template v-if="order.cargoVolume"> · {{ order.cargoVolume }}CBM</template>
            </div>
            <div class="order-bottom">
              <span v-if="order.totalAmount" class="order-amount">¥{{ order.totalAmount }}</span>
              <span class="order-time">{{ formatTime(order.createTime) }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 底部：最近完成 -->
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="24">
        <el-card class="order-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-title">
                <el-icon color="#67C23A"><CircleCheck /></el-icon>
                已完成订单
              </span>
              <el-tag type="success" round>{{ completedOrders.length }}</el-tag>
            </div>
          </template>
          <el-table v-if="completedOrders.length > 0" :data="completedOrders" size="small" stripe
            :header-cell-style="{ background: '#fafafa', color: '#606266', fontWeight: 600 }">
            <el-table-column label="订单号" width="170">
              <template #default="{ row }">{{ row.orderSo || row.orderNo }}</template>
            </el-table-column>
            <el-table-column label="客户" width="160">
              <template #default="{ row }">{{ customerNameMap[row.customerId] || '—' }}</template>
            </el-table-column>
            <el-table-column label="路线" min-width="200">
              <template #default="{ row }">
                <span style="color:#409eff">{{ row.origin || '—' }}</span>
                <span style="margin:0 6px;color:#c0c4cc">→</span>
                <span style="color:#67c23a">{{ row.destination || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="运输方式" width="100">
              <template #default="{ row }">
                <el-tag :type="shipTag[row.shipType]" size="small">{{ shipLabel[row.shipType] }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="110">
              <template #default="{ row }">
                {{ row.totalAmount ? '¥' + row.totalAmount : '—' }}
              </template>
            </el-table-column>
            <el-table-column label="完成时间" width="120">
              <template #default="{ row }">{{ formatTime(row.updateTime) }}</template>
            </el-table-column>
          </el-table>
          <div v-else class="empty-hint">暂无已完成的订单</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷链接 -->
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="24">
        <el-card class="link-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-title">
                <el-icon color="#909399"><Link /></el-icon>
                常用链接
              </span>
            </div>
          </template>
          <div class="link-list">
            <a href="https://online.sinotrans.com/" target="_blank" class="link-item">
              <el-icon size="18"><Box /></el-icon>
              <span>中外运仓库</span>
            </a>
            <a href="https://www.weiyun001.com" target="_blank" class="link-item">
              <el-icon size="18"><Ship /></el-icon>
              <span>维运网</span>
            </a>
            <a href="http://www.customs.gov.cn/customs/302427/302442/jckszcx/index.html" target="_blank" class="link-item">
              <el-icon size="18"><Search /></el-icon>
              <span>海关出口税查询</span>
            </a>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  User, Document, Loading, CircleCheck, Clock, Calendar, Right, Box,
  Link, Search
} from '@element-plus/icons-vue'
import { orderApi, customerApi } from '@/api'

const loading = ref(false)
const recentOrders = ref([])
const customerNameMap = ref({})

const shipLabel = { SEA: '海运', AIR: '空运', LAND: '陆运' }
const shipTag = { SEA: 'primary', AIR: 'success', LAND: 'warning' }
const statusLabel = { '进仓': '进仓', '走船': '走船', '已到港': '已到港', '已提货': '已提货' }
const statusTag = { '进仓': 'info', '走船': 'warning', '已到港': 'primary', '已提货': 'success' }

const stats = ref([
  { label: '客户总数', value: 0, icon: 'User', color: '#fff', bg: 'linear-gradient(135deg, #409eff, #337ecc)' },
  { label: '订单总数', value: 0, icon: 'Document', color: '#fff', bg: 'linear-gradient(135deg, #67c23a, #529b2e)' },
  { label: '进行中', value: 0, icon: 'Loading', color: '#fff', bg: 'linear-gradient(135deg, #e6a23c, #cc8a2e)' },
  { label: '已完成', value: 0, icon: 'CircleCheck', color: '#fff', bg: 'linear-gradient(135deg, #909399, #707378)' }
])

const inProgressOrders = computed(() =>
  recentOrders.value.filter(o => o.status === '进仓' || o.status === '走船' || o.status === '已到港')
)
const completedOrders = computed(() =>
  recentOrders.value.filter(o => o.status === '已提货')
)

const formatTime = (t) => {
  if (!t) return '—'
  return t.replace('T', ' ').substring(0, 16)
}

onMounted(async () => {
  loading.value = true
  try {
    // 并行请求：统计 + 最近订单 + 客户列表
    const [allRes, processingRes, shippedRes, completedRes, recentRes, customerRes] = await Promise.all([
      orderApi.page({ pageNum: 1, pageSize: 1 }),
      orderApi.page({ status: '走船', pageNum: 1, pageSize: 1 }),
      orderApi.page({ status: '已到港', pageNum: 1, pageSize: 1 }),
      orderApi.page({ status: '已提货', pageNum: 1, pageSize: 1 }),
      orderApi.page({ pageNum: 1, pageSize: 20 }),
      customerApi.page({ pageNum: 1, pageSize: 999 })
    ])

    stats.value[0].value = customerRes.data.total || 0
    stats.value[1].value = allRes.data.total || 0
    stats.value[2].value = (processingRes.data.total || 0) + (shippedRes.data.total || 0)
    stats.value[3].value = completedRes.data.total || 0

    recentOrders.value = recentRes.data.records || []

    // 建立客户 ID → 公司名 映射
    const nameMap = {}
    ;(customerRes.data.records || []).forEach(c => {
      nameMap[c.id] = c.companyName
    })
    customerNameMap.value = nameMap
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.dashboard { max-width: 1400px; }

/* 统计卡片 */
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.stat-card {
  padding: 20px;
  border-radius: 12px;
  color: #fff;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: transform 0.2s;
  cursor: default;
}
.stat-card:hover { transform: translateY(-2px); }
.stat-icon {
  width: 56px; height: 56px;
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  background: rgba(255, 255, 255, 0.2);
}
.stat-info { flex: 1; }
.stat-label { font-size: 13px; opacity: 0.85; display: block; }
.stat-value { font-size: 32px; font-weight: 700; display: block; margin-top: 2px; }

/* 订单卡片 */
.order-card { border-radius: 10px; }
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 订单项 */
.order-item {
  padding: 14px 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fafafa;
}
.order-item:last-child { margin-bottom: 0; }
.order-item:hover {
  border-color: #409eff;
  background: #ecf5ff;
  transform: translateX(4px);
}
.order-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.order-route {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
}
.origin { color: #409eff; }
.dest { color: #67c23a; }
.arrow-icon { color: #c0c4cc; font-size: 14px; }
.order-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}
.order-no { font-size: 12px; color: #909399; font-family: monospace; }
.order-customer { font-size: 13px; color: #606266; font-weight: 500; }
.order-ship { display: flex; gap: 4px; }
.order-cargo {
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 4px;
}
.order-date {
  font-size: 12px;
  color: #e6a23c;
  display: flex;
  align-items: center;
  gap: 4px;
}
.order-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4px;
}
.order-amount {
  font-size: 14px;
  color: #f56c6c;
  font-weight: 600;
}
.order-time {
  font-size: 12px;
  color: #c0c4cc;
}
.empty-hint {
  text-align: center;
  color: #c0c4cc;
  padding: 40px 0;
  font-size: 14px;
}

/* ── 移动端适配 ── */
@media (max-width: 768px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }
  .stat-card {
    padding: 14px;
    gap: 10px;
  }
  .stat-icon {
    width: 44px;
    height: 44px;
    border-radius: 10px;
  }
  .stat-icon :deep(.el-icon) {
    font-size: 24px !important;
  }
  .stat-value {
    font-size: 24px;
  }
  .stat-label {
    font-size: 11px;
  }
  :deep(.el-row) {
    flex-direction: column;
  }
  :deep(.el-col) {
    max-width: 100% !important;
    flex: 0 0 100% !important;
    margin-bottom: 12px;
  }
  .order-item {
    padding: 10px 12px;
  }
  .order-route {
    font-size: 13px;
  }
  /* 表格横向滚动 */
  :deep(.el-table) {
    font-size: 12px;
  }
  :deep(.el-table th),
  :deep(.el-table td) {
    padding: 8px 4px;
  }
  .pagination {
    justify-content: center;
  }
  .link-item {
    font-size: 13px;
  }
}

/* 快捷链接 */
.link-card { border-radius: 10px; }
.link-list {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}
.link-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: #f5f7fa;
  border-radius: 8px;
  text-decoration: none;
  color: #409eff;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
}
.link-item:hover {
  background: #ecf5ff;
  color: #337ecc;
  transform: translateY(-2px);
}
</style>

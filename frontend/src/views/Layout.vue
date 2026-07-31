<template>
  <el-container class="layout-container">
    <!-- 侧边栏（桌面端可见，手机端隐藏） -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="aside">
      <div class="logo">
        <el-icon size="22" color="#fff"><Ship /></el-icon>
        <span v-show="!isCollapse" class="logo-text">货代管理系统</span>
      </div>
      <el-menu
        :default-active="$route.path"
        background-color="#1e293b"
        text-color="#ffffffa6"
        active-text-color="#ffffff"
        :collapse="isCollapse"
        router
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 移动端抽屉菜单 -->
    <el-drawer v-model="drawerVisible" direction="ltr" size="240px" :with-header="false">
      <div class="drawer-logo">
        <el-icon size="22" color="#fff"><Ship /></el-icon>
        <span class="drawer-logo-text">货代管理系统</span>
      </div>
      <el-menu
        :default-active="$route.path"
        background-color="#1e293b"
        text-color="#ffffffa6"
        active-text-color="#ffffff"
        @select="drawerVisible = false"
        router
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-drawer>

    <el-container>
      <!-- 顶部栏 -->
      <el-header class="header">
        <div class="header-left">
          <el-icon class="menu-btn" @click="drawerVisible = true">
            <Menu />
          </el-icon>
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" /><Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <!-- 到港提醒小喇叭 -->
          <el-popover placement="bottom" :width="380" trigger="click" @show="loadEtaAlerts">
            <template #reference>
              <el-badge :value="etaAlertCount" :hidden="etaAlertCount === 0" class="bell-badge">
                <el-icon class="bell-icon" :size="20"><Bell /></el-icon>
              </el-badge>
            </template>
            <div class="alert-list">
              <h4 style="margin:0 0 12px;color:#303133;">📢 到港提醒（ETA+1天）</h4>
              <div v-if="etaAlerts.length === 0" style="color:#909399;text-align:center;padding:20px;">✅ 暂无到港提醒</div>
              <div v-for="alert in etaAlerts" :key="alert.id" class="alert-item" @click="$router.push('/orders')">
                <div class="alert-so">🚢 SO: {{ alert.orderSo }}</div>
                <div class="alert-detail">ETA: {{ alert.eta }} — 已到港，请及时提醒客户提货</div>
              </div>
            </div>
          </el-popover>
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar size="small" :style="{ background: 'var(--color-primary)' }">
                {{ userStore.realName?.[0] || 'U' }}
              </el-avatar>
              <span class="username">{{ userStore.realName || userStore.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout" :icon="SwitchButton">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容 -->
      <el-main class="main">
        <!-- 标签栏 -->
        <div class="tab-bar" v-if="tabsStore.openedTabs.length">
          <div class="tab-list">
            <div
              v-for="tab in tabsStore.openedTabs" :key="tab.path"
              class="tab-item"
              :class="{ active: tabsStore.activeTab === tab.path }"
              @click="switchTab(tab.path)"
            >
              <span class="tab-title">{{ tab.title }}</span>
              <el-icon class="tab-close" @click.stop="tabsStore.closeTab(tab.path)"><Close /></el-icon>
            </div>
          </div>
          <el-dropdown trigger="click" v-if="tabsStore.openedTabs.length > 0">
            <el-icon class="tab-more" :size="16"><ArrowDown /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="closeOtherTabs">关闭其他</el-dropdown-item>
                <el-dropdown-item @click="tabsStore.closeAll();$router.push('/dashboard')">关闭全部</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div class="tab-content">
          <router-view v-slot="{ Component }">
            <keep-alive :include="cachedViews">
              <component :is="Component" :key="$route.fullPath" />
            </keep-alive>
          </router-view>
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useTabsStore } from '@/store/tabs'
import { ElMessageBox } from 'element-plus'
import { SwitchButton, Menu, Close, Bell } from '@element-plus/icons-vue'
import { orderApi } from '@/api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const tabsStore = useTabsStore()
const isCollapse = ref(false)
const drawerVisible = ref(false)
const etaAlerts = ref([])
const etaAlertCount = computed(() => etaAlerts.value.length)

// 缓存视图名称列表（keep-alive include）
const cachedViews = computed(() => tabsStore.openedTabs.map(t => t.name).filter(Boolean))

// 菜单项（侧边栏和抽屉共用，避免重复代码）
const menuItems = computed(() => {
  const items = [
    { path: '/dashboard', title: '工作台', icon: 'Odometer' },
    { path: '/customers', title: '客户管理', icon: 'User' },
    { path: '/orders', title: '订单管理', icon: 'Document' },
    { path: '/quotes', title: '费用报价', icon: 'Goods' },
    { path: '/quote-manage', title: '报价费用管理', icon: 'EditPen', require: 'manager' },
    { path: '/port-charge-manage', title: '目的港费用管理', icon: 'EditPen', require: 'manager' },
    { path: '/upload', title: '上传模块', icon: 'Upload', require: 'manager' },
    { path: '/logs', title: '日志管理', icon: 'Notebook', require: 'admin' },
    { path: '/user-manage', title: '角色管理', icon: 'Setting', require: 'admin' },
  ]
  return items.filter(item => {
    if (item.require === 'admin') return userStore.isAdmin
    if (item.require === 'manager') return userStore.isManager
    return true
  })
})

// 切换标签
const switchTab = (path) => {
  tabsStore.setActive(path)
  router.push(path)
}

// 关闭其他标签
const closeOtherTabs = () => {
  tabsStore.closeOther(tabsStore.activeTab)
  const tab = tabsStore.openedTabs[0]
  if (tab) router.push(tab.path)
  else router.push('/dashboard')
}

// 路由变化时自动打开标签
watch(() => route.path, (path) => {
  if (path !== '/login' && path !== '/register' && path !== '/') {
    tabsStore.openTab(route)
  }
}, { immediate: true })

// 仪表盘默认打开
onMounted(() => {
  if (route.path === '/dashboard' || route.path === '/') {
    tabsStore.openTab(route)
  }
})

// 加载到港提醒
const loadEtaAlerts = async () => {
  try {
    const res = await orderApi.etaAlerts()
    etaAlerts.value = res.data || []
  } catch { /* 静默失败 */ }
}

let alertTimer = null
onMounted(() => {
  loadEtaAlerts()
  alertTimer = setInterval(loadEtaAlerts, 5 * 60 * 1000)
})
onUnmounted(() => {
  if (alertTimer) clearInterval(alertTimer)
})

const handleCommand = async (cmd) => {
  if (cmd === 'logout') {
    await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout-container { height: 100vh; }
.layout-container :deep(.el-container) { gap: 0; }

/* ── 侧边栏 ── */
.aside {
  background: var(--bg-sidebar);
  transition: width 0.3s;
  overflow: hidden;
  border-right: none;
}

/* Logo 区域 */
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
  position: relative;
}
.logo::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 20px;
  right: 20px;
  height: 2px;
  background: var(--color-primary);
  border-radius: 1px;
  opacity: 0;
  transition: opacity 0.3s;
}
.logo-text { color: #fff; font-weight: 600; font-size: 15px; white-space: nowrap; }

/* 菜单 */
.el-menu { border-right: none; }

/* 菜单项 */
.aside :deep(.el-menu-item) {
  margin: 2px 8px;
  border-radius: 8px;
  height: 44px;
  line-height: 44px;
  transition: all 0.2s ease;
}
.aside :deep(.el-menu-item:hover) {
  background: rgba(255,255,255,0.06) !important;
}
.aside :deep(.el-menu-item.is-active) {
  background: rgba(64,158,255,0.15) !important;
  color: #fff !important;
  position: relative;
}
.aside :deep(.el-menu-item.is-active::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 10px;
  bottom: 10px;
  width: 3px;
  background: var(--color-primary);
  border-radius: 2px;
}

/* 折叠态 */
.aside :deep(.el-menu--collapse) {
  width: 64px;
}
.aside :deep(.el-menu--collapse .el-menu-item) {
  margin: 2px 10px;
  padding: 0 !important;
  justify-content: center;
}

/* 菜单项图标 */
.aside :deep(.el-menu-item .el-icon) {
  font-size: 18px;
  transition: color 0.2s;
}
.aside :deep(.el-menu-item.is-active .el-icon) {
  color: var(--color-primary);
}
.header {
  background: var(--bg-white);
  border-bottom: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}
.header-left { display: flex; align-items: center; gap: 16px; }
.collapse-btn { font-size: 20px; cursor: pointer; color: var(--text-regular); }
.menu-btn { display: none; }
@media (max-width: 768px) {
  .menu-btn { display: block; font-size: 22px; cursor: pointer; color: var(--text-regular); }
}
.header-right .user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--text-regular);
}
.username { font-size: 14px; }

/* ── 小喇叭通知 ── */
.bell-badge { margin-right: 16px; cursor: pointer; }
.bell-icon { color: var(--text-regular); transition: color 0.2s; font-size: 20px; }
.bell-icon:hover { color: var(--color-primary); }
.alert-list { max-height: 320px; overflow-y: auto; }
.alert-item {
  padding: 10px 12px;
  border-bottom: 1px solid var(--border-light);
  cursor: pointer;
  transition: background 0.2s;
}
.alert-item:last-child { border-bottom: none; }
.alert-item:hover { background: var(--bg-page); }
.alert-so { font-weight: 600; color: var(--text-primary); margin-bottom: 4px; }
.alert-detail { font-size: 13px; color: var(--color-warning); }

/* ── 主内容区（无负 margin hack）── */
.main {
  background: var(--bg-page);
  padding: 0;
  display: flex;
  flex-direction: column;
}

/* ── 标签栏 ── */
.tab-bar {
  display: flex;
  align-items: center;
  background: var(--bg-white);
  border-bottom: 1px solid var(--border-light);
  padding: 0 12px;
  height: 38px;
  flex-shrink: 0;
}
.tab-list {
  display: flex;
  flex: 1;
  overflow-x: auto;
  overflow-y: hidden;
  white-space: nowrap;
  scrollbar-width: none;
}
.tab-list::-webkit-scrollbar { display: none; }
.tab-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 14px;
  height: 36px;
  line-height: 36px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  border-right: 1px solid #f0f0f0;
  white-space: nowrap;
  transition: all 0.2s;
  position: relative;
}
.tab-item:hover { color: var(--color-primary); background: var(--bg-page); }
.tab-item.active {
  color: var(--color-primary);
  background: var(--bg-page);
}
.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0; left: 0; right: 0;
  height: 2px;
  background: var(--color-primary);
}
.tab-close {
  font-size: 12px;
  border-radius: 50%;
  padding: 2px;
  transition: all 0.2s;
}
.tab-close:hover { background: #ddd; color: var(--color-danger); }
.tab-more {
  flex-shrink: 0;
  cursor: pointer;
  color: #999;
  padding: 4px 8px;
  border-left: 1px solid var(--border-light);
}
.tab-more:hover { color: var(--color-primary); }

/* ── 内容区 ── */
.tab-content {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
}

@media (max-width: 768px) {
  .tab-bar { padding: 0 6px; }
  .tab-item { padding: 0 10px; font-size: 12px; }
  .tab-content { padding: 10px; }
}

/* ── 移动端抽屉 ── */
.drawer-logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-bottom: 1px solid #ffffff15;
  background: var(--bg-sidebar);
}
.drawer-logo-text { color: #fff; font-weight: 600; font-size: 15px; white-space: nowrap; }

/* 抽屉菜单样式（与侧边栏一致） */
:deep(.el-drawer__body) { padding: 0; background: var(--bg-sidebar); }
:deep(.el-drawer__body .el-menu-item) {
  margin: 2px 8px;
  border-radius: 8px;
  height: 44px;
  line-height: 44px;
}
:deep(.el-drawer__body .el-menu-item:hover) {
  background: rgba(255,255,255,0.06) !important;
}
:deep(.el-drawer__body .el-menu-item.is-active) {
  background: rgba(64,158,255,0.15) !important;
  color: #fff !important;
}
:deep(.el-drawer__body .el-menu-item.is-active::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 10px;
  bottom: 10px;
  width: 3px;
  background: var(--color-primary);
  border-radius: 2px;
}

/* ── 移动端适配 ── */
@media (max-width: 768px) {
  .aside { display: none; }
  .header {
    padding: 0 12px;
  }
  .header-left {
    gap: 10px;
  }
  .collapse-btn {
    display: none;
  }
  .tab-content {
    padding: 10px;
  }
}
</style>

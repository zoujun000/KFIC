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
        background-color="#001529"
        text-color="#ffffffa6"
        active-text-color="#ffffff"
        :collapse="isCollapse"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>工作台</template>
        </el-menu-item>
        <el-menu-item index="/customers">
          <el-icon><User /></el-icon>
          <template #title>客户管理</template>
        </el-menu-item>
        <el-menu-item index="/orders">
          <el-icon><Document /></el-icon>
          <template #title>订单管理</template>
        </el-menu-item>
        <el-menu-item index="/quotes">
          <el-icon><Goods /></el-icon>
          <template #title>费用报价</template>
        </el-menu-item>
        <el-menu-item v-if="userStore.isManager" index="/quote-manage">
          <el-icon><EditPen /></el-icon>
          <template #title>报价费用管理</template>
        </el-menu-item>
        <el-menu-item v-if="userStore.isManager" index="/upload">
          <el-icon><Upload /></el-icon>
          <template #title>上传模块</template>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/logs">
          <el-icon><Notebook /></el-icon>
          <template #title>日志管理</template>
        </el-menu-item>
        <el-menu-item v-if="userStore.isManager" index="/port-charge-manage">
          <el-icon><EditPen /></el-icon>
          <template #title>目的港费用管理</template>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/user-manage">
          <el-icon><Setting /></el-icon>
          <template #title>角色管理</template>
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
        background-color="#001529"
        text-color="#ffffffa6"
        active-text-color="#ffffff"
        @select="drawerVisible = false"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>工作台</template>
        </el-menu-item>
        <el-menu-item index="/customers">
          <el-icon><User /></el-icon>
          <template #title>客户管理</template>
        </el-menu-item>
        <el-menu-item index="/orders">
          <el-icon><Document /></el-icon>
          <template #title>订单管理</template>
        </el-menu-item>
        <el-menu-item index="/quotes">
          <el-icon><Goods /></el-icon>
          <template #title>费用报价</template>
        </el-menu-item>
        <el-menu-item v-if="userStore.isManager" index="/quote-manage">
          <el-icon><EditPen /></el-icon>
          <template #title>报价费用管理</template>
        </el-menu-item>
        <el-menu-item v-if="userStore.isManager" index="/upload">
          <el-icon><Upload /></el-icon>
          <template #title>上传模块</template>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/logs">
          <el-icon><Notebook /></el-icon>
          <template #title>日志管理</template>
        </el-menu-item>
        <el-menu-item v-if="userStore.isManager" index="/port-charge-manage">
          <el-icon><EditPen /></el-icon>
          <template #title>目的港费用管理</template>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/user-manage">
          <el-icon><Setting /></el-icon>
          <template #title>角色管理</template>
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
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar size="small" :style="{ background: '#409EFF' }">
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
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useTabsStore } from '@/store/tabs'
import { ElMessageBox } from 'element-plus'
import { SwitchButton, Menu, Close } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const tabsStore = useTabsStore()
const isCollapse = ref(false)
const drawerVisible = ref(false)

// 缓存视图名称列表（keep-alive include）
const cachedViews = computed(() => tabsStore.openedTabs.map(t => t.name).filter(Boolean))

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
.aside {
  background: #001529;
  transition: width 0.3s;
  overflow: hidden;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-bottom: 1px solid #ffffff15;
}
.logo-text { color: #fff; font-weight: 600; font-size: 15px; white-space: nowrap; }
.el-menu { border-right: none; }
.header {
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}
.header-left { display: flex; align-items: center; gap: 16px; }
.collapse-btn { font-size: 20px; cursor: pointer; color: #606266; }
.menu-btn { display: none; }
@media (max-width: 768px) {
  .menu-btn { display: block; font-size: 22px; cursor: pointer; color: #606266; }
}
.header-right .user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #606266;
}
.username { font-size: 14px; }
.main { background: #f5f7fa; padding: 0 20px 20px; }

/* ── 标签栏 ── */
.tab-bar {
  display: flex;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  margin: 0 -20px 12px;
  padding: 0 12px;
  height: 38px;
  overflow: hidden;
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
.tab-item:hover { color: #409EFF; background: #f5f7fa; }
.tab-item.active {
  color: #409EFF;
  background: #f5f7fa;
}
.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0; left: 0; right: 0;
  height: 2px;
  background: #409EFF;
}
.tab-close {
  font-size: 12px;
  border-radius: 50%;
  padding: 2px;
  transition: all 0.2s;
}
.tab-close:hover { background: #ddd; color: #f56c6c; }
.tab-more {
  flex-shrink: 0;
  cursor: pointer;
  color: #999;
  padding: 4px 8px;
  border-left: 1px solid #f0f0f0;
}
.tab-more:hover { color: #409EFF; }
.tab-content { min-height: calc(100vh - 200px); }

@media (max-width: 768px) {
  .tab-bar { margin: 0 -10px 10px; padding: 0 6px; }
  .tab-item { padding: 0 10px; font-size: 12px; }
}

/* ── 移动端抽屉 ── */
.drawer-logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-bottom: 1px solid #ffffff15;
  background: #001529;
}
.drawer-logo-text { color: #fff; font-weight: 600; font-size: 15px; white-space: nowrap; }
:deep(.el-drawer__body) { padding: 0; background: #001529; }

/* ── 移动端适配：只叠加，不改动原结构 ── */
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
  .main {
    padding: 10px;
  }
}
</style>

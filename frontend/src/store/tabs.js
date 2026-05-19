import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useTabsStore = defineStore('tabs', () => {
  const openedTabs = ref([])
  const activeTab = ref('')

  function openTab(route) {
    const path = route.path
    const exist = openedTabs.value.find(t => t.path === path)
    if (!exist) {
      openedTabs.value.push({
        path: path,
        name: route.name,
        title: route.meta?.title || path,
        fullPath: route.fullPath
      })
    }
    activeTab.value = path
  }

  function closeTab(path) {
    const idx = openedTabs.value.findIndex(t => t.path === path)
    if (idx === -1) return
    openedTabs.value.splice(idx, 1)
    if (activeTab.value === path) {
      activeTab.value = openedTabs.value[idx]?.path || openedTabs.value[idx - 1]?.path || ''
    }
  }

  function closeOther(path) {
    const tab = openedTabs.value.find(t => t.path === path)
    openedTabs.value = tab ? [tab] : []
    activeTab.value = path
  }

  function closeAll() {
    openedTabs.value = []
    activeTab.value = ''
  }

  function setActive(path) {
    activeTab.value = path
  }

  return { openedTabs, activeTab, openTab, closeTab, closeOther, closeAll, setActive }
})

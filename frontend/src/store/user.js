import { defineStore } from 'pinia'
import { authApi } from '@/api'

export const useUserStore = defineStore('user', {
  state: () => ({
    accessToken: localStorage.getItem('accessToken') || '',
    refreshToken: localStorage.getItem('refreshToken') || '',
    username: localStorage.getItem('username') || '',
    realName: localStorage.getItem('realName') || '',
    role: localStorage.getItem('role') || '',
    userId: localStorage.getItem('userId') || ''
  }),
  getters: {
    isAdmin: (state) => state.role === 'ADMIN',
    isManager: (state) => state.role === 'ADMIN' || state.role === 'MAINTAINER',
    isLoggedIn: (state) => !!state.accessToken
  },
  actions: {
    async login(data) {
      const res = await authApi.login(data)
      const { accessToken, refreshToken, username, realName, role, userId } = res.data
      this.accessToken = accessToken
      this.refreshToken = refreshToken
      this.username = username
      this.realName = realName
      this.role = role
      this.userId = String(userId || '')
      localStorage.setItem('accessToken', accessToken)
      localStorage.setItem('refreshToken', refreshToken)
      localStorage.setItem('username', username)
      localStorage.setItem('realName', realName)
      localStorage.setItem('role', role)
      localStorage.setItem('userId', String(userId || ''))
    },
    logout() {
      this.accessToken = ''
      this.refreshToken = ''
      this.username = ''
      this.realName = ''
      this.role = ''
      this.userId = ''
      localStorage.clear()
    }
  }
})

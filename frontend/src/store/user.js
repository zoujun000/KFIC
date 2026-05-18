import { defineStore } from 'pinia'
import { authApi } from '@/api'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    username: localStorage.getItem('username') || '',
    realName: localStorage.getItem('realName') || '',
    role: localStorage.getItem('role') || ''
  }),
  getters: {
    isAdmin: (state) => state.role === 'ADMIN',
    isManager: (state) => state.role === 'ADMIN' || state.role === 'MAINTAINER',
    isLoggedIn: (state) => !!state.token
  },
  actions: {
    async login(data) {
      const res = await authApi.login(data)
      const { token, username, realName, role, userId } = res.data
      this.token = token
      this.username = username
      this.realName = realName
      this.role = role
      localStorage.setItem('token', token)
      localStorage.setItem('username', username)
      localStorage.setItem('realName', realName)
      localStorage.setItem('role', role)
      localStorage.setItem('userId', String(userId || ''))
    },
    logout() {
      this.token = ''
      this.username = ''
      this.realName = ''
      this.role = ''
      localStorage.clear()
    }
  }
})

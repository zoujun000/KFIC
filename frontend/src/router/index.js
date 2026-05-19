import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/login/RegisterView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    meta: { requiresAuth: true },
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '工作台', icon: 'Odometer' }
      },
      {
        path: 'customers',
        name: 'Customers',
        component: () => import('@/views/customer/CustomerView.vue'),
        meta: { title: '客户管理', icon: 'User' }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/order/OrderView.vue'),
        meta: { title: '订单管理', icon: 'Document' }
      },
      {
        path: 'quotes',
        name: 'Quotes',
        component: () => import('@/views/quote/QuoteView.vue'),
        meta: { title: '报价费用管理', icon: 'PriceTag' }
      },
      {
        path: 'quote-manage',
        name: 'QuoteManage',
        component: () => import('@/views/quote/QuoteManageView.vue'),
        meta: { title: '报价费用管理(编辑)', icon: 'EditPen', requiresManager: true }
      },
      {
        path: 'upload',
        name: 'Upload',
        component: () => import('@/views/upload/UploadView.vue'),
        meta: { title: '上传模块', icon: 'Upload', requiresManager: true }
      },
      {
        path: 'logs',
        name: 'Logs',
        component: () => import('@/views/log/LogView.vue'),
        meta: { title: '日志管理', icon: 'Notebook', requiresManager: true }
      },
      {
        path: 'port-charge-manage',
        name: 'PortChargeManage',
        component: () => import('@/views/portcharge/PortChargeManageView.vue'),
        meta: { title: '目的港费用管理', icon: 'EditPen', requiresManager: true }
      },
      {
        path: 'user-manage',
        name: 'UserManage',
        component: () => import('@/views/user/UserManageView.vue'),
        meta: { title: '角色管理', icon: 'Setting', requiresManager: true }
      },
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')

  if (to.meta.requiresAuth !== false && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else if (to.meta.requiresAdmin && role !== 'ADMIN') {
    next('/dashboard')
  } else if (to.meta.requiresManager && role !== 'ADMIN' && role !== 'MAINTAINER') {
    next('/dashboard')
  } else {
    next()
  }
})

export default router

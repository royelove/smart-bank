import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/',
    redirect: '/user/login',
  },
  {
    path: '/login',
    redirect: '/user/login',
  },
  // Public auth pages
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { public: true, layout: 'auth' },
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/auth/ForgotPassword.vue'),
    meta: { public: true, layout: 'auth' },
  },
  // User auth pages
  {
    path: '/user/login',
    name: 'UserLogin',
    component: () => import('@/views/auth/UserLogin.vue'),
    meta: { public: true, layout: 'auth' },
  },
  {
    path: '/user/register',
    name: 'UserRegister',
    component: () => import('@/views/auth/UserRegister.vue'),
    meta: { public: true, layout: 'auth' },
  },
  // Support auth
  {
    path: '/support/login',
    name: 'SupportLogin',
    component: () => import('@/views/auth/SupportLogin.vue'),
    meta: { public: true, layout: 'auth' },
  },
  // Protected user routes
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    meta: { requiresAuth: true, loginType: 'user' },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/user/Dashboard.vue'),
        meta: { title: '账户总览', subtitle: '资产、收支与最近交易', parent: '我的账户' },
      },
      {
        path: 'user/dashboard',
        name: 'UserDashboard',
        component: () => import('@/views/user/UserDashboard.vue'),
        meta: { title: '我的银行卡', subtitle: '管理名下的银行卡账户', parent: '我的账户' },
      },
      {
        path: 'trade/:accountNumber',
        name: 'CardTrade',
        component: () => import('@/views/user/CardTrade.vue'),
        meta: {
          title: '交易中心',
          subtitle: '存取款、转账与交易明细',
          parent: '我的账户',
          // Drill-down page: the sidebar has no entry of its own, so highlight
          // the list it was opened from.
          activePath: '/user/dashboard',
        },
      },
      {
        path: 'market',
        name: 'Market',
        component: () => import('@/views/user/Market.vue'),
        meta: { title: '投资理财', subtitle: '理财产品、基金与股票行情', parent: '财富管理' },
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/user/Settings.vue'),
        meta: { title: '设置', subtitle: '个人资料与账户安全', parent: '账户设置' },
      },
      {
        // Must NOT live under /support: both the layout and the guard treat
        // that prefix as staff identity. See LAYOUT_PLAN.md P0-1.
        path: 'help',
        name: 'UserSupport',
        component: () => import('@/views/user/Support.vue'),
        meta: { title: '客服中心', subtitle: '智能客服与我的工单', parent: '服务支持' },
      },
    ],
  },
  // Protected support routes
  {
    path: '/support',
    component: () => import('@/layout/MainLayout.vue'),
    meta: { requiresAuth: true, loginType: 'staff' },
    children: [
      {
        path: '',
        name: 'SupportDashboard',
        component: () => import('@/views/support/Dashboard.vue'),
        meta: { title: '工单管理', subtitle: '受理、回复与关闭客户工单', parent: '客服工作台' },
      },
      {
        path: 'users',
        name: 'SupportUsers',
        component: () => import('@/views/support/Users.vue'),
        meta: { title: '用户管理', subtitle: '实名核验与证件资料', parent: '客服工作台' },
      },
    ],
  },
  // 404
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  },
})

router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore()
  authStore.initializeAuth()

  const isPublic = to.meta.public === true
  const requiresAuth = to.meta.requiresAuth === true
  const requiredLoginType = to.meta.loginType as 'user' | 'staff' | undefined

  // Public pages - allow access
  if (isPublic) {
    // Redirect authenticated users away from auth pages
    if (to.path.startsWith('/support')) {
      if (authStore.isStaffAuthenticated) {
        return next('/support')
      }
    } else {
      if (authStore.isAuthenticated) {
        return next('/dashboard')
      }
    }
    return next()
  }

  // Protected routes
  if (requiresAuth) {
    if (to.path.startsWith('/support')) {
      // Support staff routes
      if (!authStore.isStaffAuthenticated) {
        return next({ path: '/support/login', query: { redirect: to.fullPath } })
      }
      return next()
    } else {
      // User routes
      if (!authStore.isAuthenticated) {
        // The login page validates this with `safeRedirect`, which only accepts
        // same-origin absolute paths.
        return next({ path: '/user/login', query: { redirect: to.fullPath } })
      }

      // Check login type matches route
      if (requiredLoginType && authStore.loginType !== requiredLoginType) {
        return next('/dashboard')
      }
      return next()
    }
  }

  next()
})

export default router
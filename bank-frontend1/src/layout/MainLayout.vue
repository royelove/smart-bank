<script setup lang="ts">
/**
 * MainLayout — the single application shell.
 *
 * Layout contract:
 *   .app-shell   flex row: [rail] [main column]
 *   rail         sticky full-height sidebar on lg+, off-canvas drawer below lg
 *   main column  flex-1 min-w-0 so wide tables scroll instead of pushing the
 *                rail off screen; owns the sticky topbar and the scroll area
 *
 * Pages are responsible only for their own content: render a `.app-container`
 * wrapper and put a <PageHeader> at the top of it. Nothing about the chrome
 * is decided per page anymore.
 */
import { computed, onMounted, ref, watch } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import {
  CreditCard,
  LayoutDashboard,
  LifeBuoy,
  Settings as SettingsIcon,
  TrendingUp,
  Users,
} from 'lucide-vue-next'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import AppTopbar from '@/components/layout/AppTopbar.vue'
import type { NavSection } from '@/components/layout/types'
import { useAuthStore } from '@/stores/auth'
import { getUserInfo } from '@/api/user'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const COLLAPSE_KEY = 'smartbank-sidebar-collapsed'

const collapsed = ref(localStorage.getItem(COLLAPSE_KEY) === '1')
const mobileOpen = ref(false)

const isSupport = computed(() => route.path.startsWith('/support'))

const userSections: NavSection[] = [
  {
    title: '我的账户',
    items: [
      { label: '账户总览', to: '/dashboard', icon: LayoutDashboard, exact: true },
      { label: '我的银行卡', to: '/user/dashboard', icon: CreditCard },
    ],
  },
  {
    title: '财富管理',
    items: [{ label: '投资理财', to: '/market', icon: TrendingUp }],
  },
  {
    title: '账户设置',
    items: [{ label: '设置', to: '/settings', icon: SettingsIcon }],
  },
  {
    title: '服务支持',
    items: [{ label: '客服中心', to: '/help', icon: LifeBuoy }],
  },
]

const staffSections: NavSection[] = [
  {
    title: '客服工作台',
    items: [
      { label: '工单管理', to: '/support', icon: LifeBuoy, exact: true },
      { label: '用户管理', to: '/support/users', icon: Users },
    ],
  },
]

const sections = computed(() => (isSupport.value ? staffSections : userSections))

const displayName = computed(() => {
  if (isSupport.value) return authStore.staff?.name || '客服'
  return authStore.user?.userName || authStore.user?.userPhone || '用户'
})

const displayMeta = computed(() => {
  if (isSupport.value) return '在线客服'
  return authStore.user?.idCardMask || authStore.user?.userPhone || ''
})

const pageTitle = computed(() => (route.meta.title as string | undefined) || '')
const breadcrumb = computed(() => [
  isSupport.value ? '' : '个人中心',
  (route.meta.parent as string | undefined) || '',
  pageTitle.value,
])

function toggleCollapse() {
  collapsed.value = !collapsed.value
  localStorage.setItem(COLLAPSE_KEY, collapsed.value ? '1' : '0')
}

function handleLogout() {
  const wasStaff = isSupport.value
  authStore.logout()
  router.push(wasStaff ? '/support/login' : '/user/login')
}

// Close the drawer whenever the route changes (a nav click already did this,
// but browser back/forward and programmatic navigation must too).
watch(
  () => route.fullPath,
  () => {
    mobileOpen.value = false
  },
)

// The auth store persists only the token, so a hard refresh would leave the
// topbar without a name. Rehydrate the profile once per app load.
onMounted(async () => {
  if (isSupport.value || authStore.user || !authStore.token) return
  try {
    const info = await getUserInfo()
    if (info) authStore.setAuth(authStore.token as string, info)
  } catch {
    /* non-fatal: the shell falls back to the phone number */
  }
})
</script>

<template>
  <div class="app-shell">
    <!-- Desktop rail -->
    <AppSidebar
      class="hidden lg:flex lg:sticky lg:top-0 lg:h-screen"
      :sections="sections"
      :collapsed="collapsed"
      :user-name="displayName"
      :user-meta="displayMeta"
      :brand-tagline="isSupport ? '客服工作台' : '智慧银行'"
      @toggle-collapse="toggleCollapse"
      @logout="handleLogout"
    />

    <!-- Mobile drawer -->
    <Teleport to="body">
      <Transition
        enter-active-class="transition-opacity duration-200"
        leave-active-class="transition-opacity duration-150"
        enter-from-class="opacity-0"
        leave-to-class="opacity-0"
      >
        <div
          v-if="mobileOpen"
          class="fixed inset-0 z-40 bg-navy-950/40 backdrop-blur-[2px] lg:hidden"
          @click="mobileOpen = false"
        />
      </Transition>

      <Transition
        enter-active-class="transition-transform duration-200 ease-out"
        leave-active-class="transition-transform duration-150 ease-in"
        enter-from-class="-translate-x-full"
        leave-to-class="-translate-x-full"
      >
        <AppSidebar
          v-if="mobileOpen"
          class="fixed inset-y-0 left-0 z-50 lg:hidden"
          mobile
          :sections="sections"
          :user-name="displayName"
          :user-meta="displayMeta"
          :brand-tagline="isSupport ? '客服工作台' : '智慧银行'"
          @close="mobileOpen = false"
          @logout="handleLogout"
        />
      </Transition>
    </Teleport>

    <!-- Main column -->
    <div class="app-main">
      <AppTopbar
        :breadcrumb="breadcrumb"
        :user-name="displayName"
        :user-role="isSupport ? '客服' : '个人客户'"
        @open-menu="mobileOpen = true"
        @profile="router.push(isSupport ? '/support' : '/settings')"
        @logout="handleLogout"
      />

      <main class="flex-1 min-w-0">
        <RouterView v-slot="{ Component }">
          <Transition
            mode="out-in"
            enter-active-class="route-fade-enter-active"
            leave-active-class="route-fade-leave-active"
            enter-from-class="route-fade-enter-from"
            leave-to-class="route-fade-leave-to"
          >
            <component :is="Component" />
          </Transition>
        </RouterView>
      </main>
    </div>
  </div>
</template>

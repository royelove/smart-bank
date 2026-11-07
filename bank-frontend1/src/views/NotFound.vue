<script setup lang="ts">
/**
 * 404 — standalone (no app shell), because it also has to render for
 * unauthenticated visitors hitting a stale link.
 *
 * The primary action is resolved from the auth store so nobody is bounced to a
 * login form they are already past: staff -> 工单管理, user -> 账户总览,
 * visitor -> 登录.
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Compass, Home } from 'lucide-vue-next'
import { Button } from '@/components/ui'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const home = computed(() => {
  if (authStore.isStaffAuthenticated) return { path: '/support', label: '返回工单管理' }
  if (authStore.isAuthenticated) return { path: '/dashboard', label: '返回账户总览' }
  return { path: '/user/login', label: '前往登录' }
})

const attemptedPath = computed(() => route.fullPath)
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-navy-50 px-4 dark:bg-navy-950">
    <div class="w-full max-w-lg text-center">
      <div class="mx-auto mb-6 grid h-20 w-20 place-items-center rounded-2xl bg-primary-50 text-primary-600 dark:bg-primary-500/10 dark:text-primary-400">
        <Compass class="h-10 w-10" />
      </div>

      <p class="font-mono text-display-md text-navy-300 dark:text-navy-700">
        404
      </p>
      <h1 class="mt-2 text-heading-lg text-navy-900 dark:text-navy-50">
        页面未找到
      </h1>
      <p class="mx-auto mt-3 max-w-md text-body-md text-navy-500 dark:text-navy-400">
        您访问的地址不存在，或该页面已经被移动。请确认链接是否正确，也可以直接返回首页继续操作。
      </p>

      <p
        v-if="attemptedPath && attemptedPath !== '/'"
        class="mx-auto mt-4 max-w-md truncate rounded-lg bg-white px-3 py-2 font-mono text-body-xs text-navy-500 ring-1 ring-navy-200 dark:bg-navy-900 dark:text-navy-400 dark:ring-navy-700"
        :title="attemptedPath"
      >
        {{ attemptedPath }}
      </p>

      <div class="mt-8 flex flex-col items-center justify-center gap-3 sm:flex-row">
        <Button @click="router.push(home.path)">
          <Home class="h-4 w-4" />
          {{ home.label }}
        </Button>
        <Button
          variant="ghost"
          @click="router.back()"
        >
          <ArrowLeft class="h-4 w-4" />
          返回上一页
        </Button>
      </div>
    </div>
  </div>
</template>

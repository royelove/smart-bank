<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { LogOut, PanelLeftClose, PanelLeftOpen, ShieldCheck, X } from 'lucide-vue-next'
import Avatar from '@/components/ui/Avatar.vue'
import type { NavItem, NavSection } from './types'

const props = withDefaults(
  defineProps<{
    sections: NavSection[]
    collapsed?: boolean
    /** Renders the close button + hides the collapse toggle (mobile drawer). */
    mobile?: boolean
    brandName?: string
    brandTagline?: string
    userName?: string
    userMeta?: string
    avatarSrc?: string
  }>(),
  {
    collapsed: false,
    mobile: false,
    brandName: 'SmartBank',
    brandTagline: '智慧银行',
    userName: '',
    userMeta: '',
    avatarSrc: '',
  },
)

const emit = defineEmits<{
  (e: 'toggle-collapse'): void
  (e: 'close'): void
  (e: 'logout'): void
}>()

const route = useRoute()

const showLabels = computed(() => props.mobile || !props.collapsed)

function isActive(item: NavItem) {
  // Drill-down pages (e.g. /trade/:accountNumber) declare which sidebar entry
  // should light up, since they have no entry of their own.
  const override = route.meta.activePath as string | undefined
  if (override) return item.to === override

  const path = item.to
  if (item.exact) return route.path === path
  return route.path === path || route.path.startsWith(`${path}/`)
}
</script>

<template>
  <aside
    class="app-sidebar w-60 transition-[width] duration-200 ease-out"
    :class="collapsed && !mobile ? 'lg:w-[76px]' : 'lg:w-60'"
  >
    <!-- Brand -->
    <div class="h-14 shrink-0 flex items-center gap-3 px-4 border-b border-navy-200 dark:border-navy-800">
      <div class="w-8 h-8 shrink-0 rounded-lg bg-primary-600 text-white grid place-items-center">
        <ShieldCheck class="w-[18px] h-[18px]" />
      </div>

      <div
        v-if="showLabels"
        class="min-w-0 leading-tight"
      >
        <p class="font-semibold text-navy-900 dark:text-navy-50 truncate">
          {{ brandName }}
        </p>
        <p class="text-xs text-navy-500 dark:text-navy-400 truncate">
          {{ brandTagline }}
        </p>
      </div>

      <button
        v-if="mobile"
        type="button"
        class="ml-auto p-2 rounded-lg text-navy-500 hover:bg-navy-100 dark:hover:bg-navy-800"
        aria-label="关闭菜单"
        @click="emit('close')"
      >
        <X class="w-5 h-5" />
      </button>
    </div>

    <!-- Navigation -->
    <nav class="flex-1 overflow-y-auto px-3 pb-4 scrollbar-hide">
      <template
        v-for="section in sections"
        :key="section.title"
      >
        <p
          v-if="showLabels"
          class="nav-section-title"
        >
          {{ section.title }}
        </p>
        <div
          v-else
          class="mx-3 my-3 h-px bg-navy-200 dark:bg-navy-800"
        />

        <ul class="space-y-1">
          <li
            v-for="item in section.items"
            :key="item.to"
          >
            <RouterLink
              :to="item.to"
              class="sidebar-link"
              :class="{ 'sidebar-link-active': isActive(item), 'justify-center px-0': !showLabels }"
              :title="showLabels ? undefined : item.label"
              @click="mobile && emit('close')"
            >
              <component
                :is="item.icon"
                class="sidebar-link-icon"
              />
              <span
                v-if="showLabels"
                class="truncate"
              >
                {{ item.label }}
              </span>
              <span
                v-if="showLabels && item.badge"
                class="ml-auto badge badge-neutral"
              >
                {{ item.badge }}
              </span>
            </RouterLink>
          </li>
        </ul>
      </template>
    </nav>

    <!-- Footer -->
    <div class="shrink-0 border-t border-navy-200 dark:border-navy-800 p-3 space-y-2">
      <div
        v-if="userName"
        class="flex items-center gap-3 px-2 py-1.5"
        :class="{ 'justify-center px-0': !showLabels }"
      >
        <Avatar
          :name="userName"
          :src="avatarSrc"
          size="sm"
        />
        <div
          v-if="showLabels"
          class="min-w-0 leading-tight"
        >
          <p class="text-sm font-medium text-navy-800 dark:text-navy-100 truncate">
            {{ userName }}
          </p>
          <p class="text-xs text-navy-500 dark:text-navy-400 truncate">
            {{ userMeta }}
          </p>
        </div>
      </div>

      <div class="flex items-center gap-1">
        <button
          type="button"
          class="sidebar-link flex-1"
          :class="{ 'justify-center px-0': !showLabels }"
          :title="showLabels ? undefined : '退出登录'"
          @click="emit('logout')"
        >
          <LogOut class="sidebar-link-icon" />
          <span v-if="showLabels">退出登录</span>
        </button>

        <button
          v-if="!mobile"
          type="button"
          class="hidden lg:grid place-items-center w-10 h-10 shrink-0 rounded-lg text-navy-500
                 hover:bg-navy-100 dark:hover:bg-navy-800"
          :aria-label="collapsed ? '展开侧边栏' : '收起侧边栏'"
          @click="emit('toggle-collapse')"
        >
          <PanelLeftOpen
            v-if="collapsed"
            class="w-5 h-5"
          />
          <PanelLeftClose
            v-else
            class="w-5 h-5"
          />
        </button>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ChevronRight, Menu, Moon, Sun } from 'lucide-vue-next'
import Dropdown from '@/components/ui/Dropdown.vue'
import Avatar from '@/components/ui/Avatar.vue'
import { useTheme } from '@/composables/useTheme'

const props = withDefaults(
  defineProps<{
    /** Breadcrumb trail, e.g. ['我的账户', '账户总览']. */
    breadcrumb?: string[]
    userName?: string
    userRole?: string
    avatarSrc?: string
    hideMenuButton?: boolean
  }>(),
  {
    breadcrumb: () => [],
    userName: '',
    userRole: '',
    avatarSrc: '',
    hideMenuButton: false,
  },
)

const emit = defineEmits<{
  (e: 'open-menu'): void
  (e: 'profile'): void
  (e: 'logout'): void
}>()

const { isDark, toggleTheme } = useTheme()

const menuItems = computed(() => [
  { label: '个人设置', action: () => emit('profile') },
  { label: '', action: () => undefined, divider: true },
  { label: '退出登录', danger: true, action: () => emit('logout') },
])

const trail = computed(() => props.breadcrumb.filter(Boolean))
</script>

<template>
  <header class="app-topbar">
    <button
      v-if="!hideMenuButton"
      type="button"
      class="-ml-2 p-2 rounded-lg text-navy-600 dark:text-navy-300
             hover:bg-navy-100 dark:hover:bg-navy-800 lg:hidden"
      aria-label="打开菜单"
      @click="emit('open-menu')"
    >
      <Menu class="w-5 h-5" />
    </button>

    <!-- Breadcrumb: orientation while the page header scrolls away -->
    <nav
      class="min-w-0 flex-1 flex items-center gap-1.5 text-sm"
      aria-label="面包屑"
    >
      <template
        v-for="(crumb, index) in trail"
        :key="crumb"
      >
        <ChevronRight
          v-if="index > 0"
          class="w-3.5 h-3.5 shrink-0 text-navy-300 dark:text-navy-600"
        />
        <span
          class="truncate"
          :class="index === trail.length - 1
            ? 'font-medium text-navy-800 dark:text-navy-100'
            : 'text-navy-500 dark:text-navy-400'"
        >
          {{ crumb }}
        </span>
      </template>
    </nav>

    <button
      type="button"
      class="p-2 rounded-lg text-navy-600 dark:text-navy-300 hover:bg-navy-100 dark:hover:bg-navy-800"
      :aria-label="isDark ? '切换到浅色模式' : '切换到深色模式'"
      @click="toggleTheme"
    >
      <Sun
        v-if="isDark"
        class="w-5 h-5"
      />
      <Moon
        v-else
        class="w-5 h-5"
      />
    </button>

    <Dropdown
      v-if="userName"
      :items="menuItems"
    >
      <template #trigger>
        <div class="flex items-center gap-2 pl-3 ml-1 border-l border-navy-200 dark:border-navy-800">
          <Avatar
            :name="userName"
            :src="avatarSrc"
            size="sm"
          />
          <div class="hidden sm:block leading-tight text-left">
            <p class="text-sm font-medium text-navy-800 dark:text-navy-100">
              {{ userName }}
            </p>
            <p
              v-if="userRole"
              class="text-xs text-navy-500 dark:text-navy-400"
            >
              {{ userRole }}
            </p>
          </div>
        </div>
      </template>
    </Dropdown>
  </header>
</template>

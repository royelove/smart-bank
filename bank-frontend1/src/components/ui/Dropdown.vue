<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'

interface DropdownItem {
  label: string
  icon?: any
  action: () => void
  disabled?: boolean
  danger?: boolean
  divider?: boolean
}

interface DropdownProps {
  trigger: any
  items: DropdownItem[]
  align?: 'left' | 'right'
}

const props = withDefaults(defineProps<DropdownProps>(), {
  align: 'right',
})

const isOpen = ref(false)
const dropdownRef = ref<HTMLElement | null>(null)

const toggle = () => {
  isOpen.value = !isOpen.value
}

const close = () => {
  isOpen.value = false
}

const handleItemClick = (item: DropdownItem) => {
  if (!item.disabled && !item.divider) {
    item.action()
    close()
  }
}

const handleClickOutside = (event: MouseEvent) => {
  if (dropdownRef.value && !dropdownRef.value.contains(event.target as Node)) {
    close()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

watch(isOpen, (open) => {
  if (open) {
    document.addEventListener('keydown', handleKeydown)
  } else {
    document.removeEventListener('keydown', handleKeydown)
  }
})

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape') {
    close()
  }
}
</script>

<template>
  <div
    ref="dropdownRef"
    class="relative"
  >
    <div
      class="cursor-pointer"
      @click="toggle"
    >
      <slot
        name="trigger"
        :open="isOpen"
      >
        {{ trigger }}
      </slot>
    </div>

    <Transition
      enter-active-class="animate-fade-in"
      leave-active-class="animate-fade-out"
    >
      <div
        v-show="isOpen"
        :class="['dropdown', { 'right-0': align === 'right', 'left-0': align === 'left' }]"
        role="menu"
      >
        <template
          v-for="(item, index) in items"
          :key="index"
        >
          <hr
            v-if="item.divider"
            class="dropdown-divider"
            role="separator"
          >
          <button
            v-else
            type="button"
            :disabled="item.disabled"
            :class="['dropdown-item', { 'text-danger-600 dark:text-danger-400': item.danger, 'opacity-50 cursor-not-allowed': item.disabled }]"
            role="menuitem"
            @click="handleItemClick(item)"
          >
            <component
              :is="item.icon"
              v-if="item.icon"
              class="w-4 h-4"
            />
            {{ item.label }}
          </button>
        </template>
      </div>
    </Transition>
  </div>
</template>

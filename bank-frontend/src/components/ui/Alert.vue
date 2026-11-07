<script setup lang="ts">
import { ref, computed } from 'vue'
import { Info, CheckCircle, AlertTriangle, AlertCircle, X } from 'lucide-vue-next'

interface AlertProps {
  variant?: 'info' | 'success' | 'warning' | 'danger'
  title?: string
  description?: string
  dismissible?: boolean
  class?: string
}

const props = withDefaults(defineProps<AlertProps>(), {
  variant: 'info',
  dismissible: false,
})

const emit = defineEmits<{ dismiss: [] }>()

const visible = ref(true)

// Soft tint + hairline, not a saturated block. `warning` uses the project
// palette rather than Tailwind's stock `amber`, which drifted from it.
const variants = {
  info: 'bg-primary-50 border-primary-100 text-primary-800 dark:bg-primary-500/10 dark:border-primary-500/20 dark:text-primary-200',
  success: 'bg-success-50 border-success-100 text-success-800 dark:bg-success-500/10 dark:border-success-500/20 dark:text-success-200',
  warning: 'bg-warning-50 border-warning-100 text-warning-800 dark:bg-warning-500/10 dark:border-warning-500/20 dark:text-warning-200',
  danger: 'bg-danger-50 border-danger-100 text-danger-800 dark:bg-danger-500/10 dark:border-danger-500/20 dark:text-danger-200',
}

const icons = {
  info: Info,
  success: CheckCircle,
  warning: AlertTriangle,
  danger: AlertCircle,
}

const alertClasses = computed(() => [
  'flex gap-3 px-4 py-3 rounded-lg border text-sm',
  variants[props.variant],
  props.class,
])

const handleDismiss = () => {
  visible.value = false
  emit('dismiss')
}
</script>

<template>
  <div
    v-show="visible"
    :class="alertClasses"
    role="alert"
  >
    <component
      :is="icons[variant]"
      class="w-[18px] h-[18px] flex-shrink-0 mt-0.5"
    />
    <div class="flex-1 min-w-0">
      <h4
        v-if="title"
        class="font-medium"
      >
        {{ title }}
      </h4>
      <p
        v-if="description"
        :class="title ? 'mt-1 opacity-90' : 'opacity-90'"
      >
        {{ description }}
      </p>
      <slot />
    </div>
    <button
      v-if="dismissible"
      type="button"
      class="flex-shrink-0 p-1 rounded hover:bg-black/5 dark:hover:bg-white/5 transition-colors"
      aria-label="Dismiss"
      @click="handleDismiss"
    >
      <X class="w-4 h-4" />
    </button>
  </div>
</template>
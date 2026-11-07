<script setup lang="ts">
import { computed } from 'vue'

interface BadgeProps {
  variant?: 'success' | 'warning' | 'danger' | 'info' | 'neutral'
  class?: string
  dot?: boolean
}

const props = withDefaults(defineProps<BadgeProps>(), {
  variant: 'neutral',
  dot: false,
})

const variantClasses = {
  success: 'badge-success',
  warning: 'badge-warning',
  danger: 'badge-danger',
  info: 'badge-info',
  neutral: 'badge-neutral',
}

const badgeClasses = computed(() => [
  variantClasses[props.variant],
  props.class,
])
</script>

<template>
  <span
    :class="badgeClasses"
    :data-variant="variant"
  >
    <span
      v-if="dot"
      class="w-1.5 h-1.5 rounded-full bg-current mr-1.5"
    />
    <slot />
  </span>
</template>

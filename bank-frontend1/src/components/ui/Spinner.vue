<script setup lang="ts">
import { computed } from 'vue'

interface SpinnerProps {
  size?: 'sm' | 'md' | 'lg'
  class?: string
}

const props = withDefaults(defineProps<SpinnerProps>(), {
  size: 'md',
})

const sizes = {
  sm: 'w-4 h-4',
  md: 'w-6 h-6',
  lg: 'w-8 h-8',
}

const spinnerClasses = computed(() => [
  sizes[props.size],
  'animate-spin text-primary-600',
  props.class,
])
</script>

<template>
  <svg
    :class="spinnerClasses"
    viewBox="0 0 24 24"
    fill="none"
    aria-hidden="true"
  >
    <circle
      class="opacity-25"
      cx="12"
      cy="12"
      r="10"
      stroke="currentColor"
      stroke-width="4"
    />
    <path
      class="opacity-75"
      fill="currentColor"
      d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
    />
  </svg>
</template>
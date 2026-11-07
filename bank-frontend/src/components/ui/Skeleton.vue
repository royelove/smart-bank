<script setup lang="ts">
import { computed } from 'vue'

interface SkeletonProps {
  variant?: 'text' | 'circular' | 'rectangular' | 'card'
  width?: string | number
  height?: string | number
  class?: string
}

const props = withDefaults(defineProps<SkeletonProps>(), {
  variant: 'text',
})

const skeletonClasses = computed(() => [
  'skeleton animate-pulse',
  {
    'h-4 w-full rounded': props.variant === 'text',
    'rounded-full': props.variant === 'circular',
    'rounded-lg': props.variant === 'rectangular' || props.variant === 'card',
  },
  props.class,
])
</script>

<template>
  <div
    :class="skeletonClasses"
    :style="{
      width: width ? (typeof width === 'number' ? `${width}px` : width) : undefined,
      height: height ? (typeof height === 'number' ? `${height}px` : height) : undefined,
    }"
    aria-hidden="true"
  />
</template>
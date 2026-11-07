<script setup lang="ts">
import { computed } from 'vue'

interface CardProps {
  variant?: 'default' | 'hover' | 'padded'
  class?: string
}

const props = withDefaults(defineProps<CardProps>(), {
  variant: 'default',
})

const variantClasses = {
  default: 'card',
  hover: 'card-hover',
  padded: 'card-padded',
}

const cardClasses = computed(() => [
  variantClasses[props.variant],
  props.class,
])
</script>

<template>
  <div :class="cardClasses">
    <slot />
  </div>
</template>

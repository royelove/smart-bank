<script setup lang="ts">
import { computed } from 'vue'
import { User } from 'lucide-vue-next'

interface AvatarProps {
  src?: string
  alt?: string
  name?: string
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl'
  shape?: 'circle' | 'square'
  class?: string
}

const props = withDefaults(defineProps<AvatarProps>(), {
  size: 'md',
  shape: 'circle',
})

const sizes = {
  xs: 'w-6 h-6 text-xs',
  sm: 'w-8 h-8 text-sm',
  md: 'w-10 h-10 text-base',
  lg: 'w-12 h-12 text-lg',
  xl: 'w-16 h-16 text-xl',
}

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2)
}

const bgColors = [
  'bg-primary-100 text-primary-700 dark:bg-primary-500/10 dark:text-primary-300',
  'bg-success-100 text-success-700 dark:bg-success-500/10 dark:text-success-300',
  'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300',
  'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300',
  'bg-pink-100 text-pink-700 dark:bg-pink-900/30 dark:text-pink-300',
  'bg-cyan-100 text-cyan-700 dark:bg-cyan-900/30 dark:text-cyan-300',
  'bg-indigo-100 text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-300',
  'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-300',
]

const getColor = (name: string) => {
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  return bgColors[Math.abs(hash) % bgColors.length]
}

const avatarClasses = computed(() => [
  'inline-flex items-center justify-center font-medium overflow-hidden',
  sizes[props.size],
  props.shape === 'circle' ? 'rounded-full' : 'rounded-lg',
  props.class,
])
</script>

<template>
  <div
    :class="avatarClasses"
    :role="src ? 'img' : undefined"
    :aria-label="alt || name"
  >
    <img
      v-if="src"
      :src="src"
      :alt="alt || name || 'Avatar'"
      class="w-full h-full object-cover"
    >
    <span
      v-else-if="name"
      :class="getColor(name)"
    >
      {{ getInitials(name) }}
    </span>
    <User
      v-else
      class="w-1/2 h-1/2 text-navy-400 dark:text-navy-500"
    />
  </div>
</template>

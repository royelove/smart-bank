<script setup lang="ts">
import type { Component } from 'vue'

withDefaults(
  defineProps<{
    label: string
    value: string | number
    hint?: string
    /** Small caption rendered next to the value, e.g. "较上月". */
    trend?: string
    trendTone?: 'up' | 'down' | 'flat'
    icon?: Component
    /**
     * `accent` tints the icon chip blue. Use it for the one figure the page is
     * about — a row of four blue chips is decoration, not emphasis.
     */
    tone?: 'neutral' | 'accent'
    loading?: boolean
  }>(),
  {
    hint: '',
    trend: '',
    trendTone: 'flat',
    tone: 'neutral',
    loading: false,
  },
)

const toneClass = {
  up: 'stat-trend-up',
  down: 'stat-trend-down',
  flat: 'text-navy-500 dark:text-navy-400',
}
</script>

<template>
  <div class="stat-card">
    <div class="flex items-start justify-between gap-3">
      <div class="min-w-0">
        <p class="stat-label mt-0">
          {{ label }}
        </p>

        <div
          v-if="loading"
          class="skeleton h-8 w-32 mt-2"
        />
        <p
          v-else
          class="stat-value mt-1 truncate"
        >
          {{ value }}
        </p>

        <p
          v-if="trend"
          class="stat-trend"
          :class="toneClass[trendTone]"
        >
          {{ trend }}
        </p>
        <p
          v-else-if="hint"
          class="stat-label"
        >
          {{ hint }}
        </p>
      </div>

      <div
        v-if="icon"
        :class="tone === 'accent' ? 'stat-icon-accent' : 'stat-icon'"
      >
        <component
          :is="icon"
          class="w-[18px] h-[18px]"
        />
      </div>
    </div>
  </div>
</template>

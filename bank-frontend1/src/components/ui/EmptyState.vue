<script setup lang="ts">
import type { Component } from 'vue'
import { Inbox } from 'lucide-vue-next'

withDefaults(
  defineProps<{
    title?: string
    description?: string
    icon?: Component
  }>(),
  {
    title: '暂无数据',
    description: '',
  },
)
</script>

<template>
  <div class="empty-state">
    <component
      :is="icon || Inbox"
      class="empty-state-icon"
    />
    <p class="empty-state-title">
      <slot name="title">{{ title }}</slot>
    </p>
    <p
      v-if="description || $slots.description"
      class="empty-state-description"
    >
      <slot name="description">{{ description }}</slot>
    </p>
    <div
      v-if="$slots.action"
      class="mt-5"
    >
      <slot name="action" />
    </div>
  </div>
</template>

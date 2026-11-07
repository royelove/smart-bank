<script setup lang="ts" generic="T extends string | number">
/**
 * SectionNav — in-page section switcher for long pages.
 *
 * Deliberately driven by local state (v-model), NOT by routes: the sections
 * share one loaded dataset, so switching must not remount the view and
 * re-fire the page's API calls. It renders one DOM for both shapes — a
 * scrollable pill row on narrow screens and a sticky vertical rail from
 * `lg` up — so a page only needs `.layout-split` as its root grid.
 *
 * The same rail doubles as a facet filter: `id` is generic, so a page can
 * bind it to a numeric status enum instead of a string section key.
 *
 * Usage:
 *   <div class="layout-split">
 *     <SectionNav v-model="section" title="设置" :items="sections" />
 *     <div class="min-w-0 space-y-6"> ...panels... </div>
 *   </div>
 */
import type { SectionNavItem } from './types'

withDefaults(
  defineProps<{
    modelValue: T
    items: SectionNavItem<T>[]
    /** Optional rail heading; hidden in the horizontal (narrow) shape. */
    title?: string
  }>(),
  { title: '' },
)

const emit = defineEmits<{ 'update:modelValue': [value: T] }>()
</script>

<template>
  <nav
    class="section-nav"
    :aria-label="title || '页面分区'"
  >
    <p
      v-if="title"
      class="section-nav-title"
    >
      {{ title }}
    </p>

    <button
      v-for="item in items"
      :key="item.id"
      type="button"
      class="section-nav-item"
      :class="{ 'section-nav-item-active': modelValue === item.id }"
      :aria-current="modelValue === item.id ? 'true' : undefined"
      @click="emit('update:modelValue', item.id)"
    >
      <component
        :is="item.icon"
        v-if="item.icon"
        class="section-nav-icon"
      />
      <span>{{ item.label }}</span>
      <span
        v-if="item.count !== undefined"
        class="ml-auto pl-2 text-xs tabular-nums"
        :class="modelValue === item.id
          ? 'text-primary-500 dark:text-primary-400'
          : 'text-navy-400 dark:text-navy-500'"
      >
        {{ item.count }}
      </span>
    </button>
  </nav>
</template>

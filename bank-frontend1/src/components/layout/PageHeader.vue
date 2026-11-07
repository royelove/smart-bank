<script setup lang="ts">
/**
 * PageHeader — the single source of page-level hierarchy.
 *
 * Every route renders exactly one PageHeader as the first child of
 * `.app-container`, which is what makes the app feel consistent:
 * same title size, same spacing, same action placement everywhere.
 */
withDefaults(
  defineProps<{
    title: string
    subtitle?: string
    /** Small label above the title, e.g. "账户 / 交易明细". */
    eyebrow?: string
  }>(),
  {
    subtitle: '',
    eyebrow: '',
  },
)
</script>

<template>
  <header class="page-header">
    <div class="min-w-0">
      <p
        v-if="eyebrow"
        class="text-xs font-medium uppercase tracking-wider text-navy-400 dark:text-navy-500 mb-1"
      >
        {{ eyebrow }}
      </p>

      <h1 class="page-title truncate">
        <slot name="title">{{ title }}</slot>
      </h1>

      <p
        v-if="subtitle || $slots.subtitle"
        class="page-subtitle"
      >
        <slot name="subtitle">{{ subtitle }}</slot>
      </p>
    </div>

    <div
      v-if="$slots.actions"
      class="flex flex-wrap items-center gap-2 shrink-0"
    >
      <slot name="actions" />
    </div>
  </header>
</template>

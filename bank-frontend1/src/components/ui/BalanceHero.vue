<script setup lang="ts">
/**
 * Balance hero — the one number a customer opens the app for.
 *
 * Replaces the saturated blue gradient block that used to sit on the home and
 * trade pages. On a white canvas the figure itself carries the emphasis, so the
 * surface stays plain and the accent is a thin blue rule on the left edge.
 */
withDefaults(
  defineProps<{
    label: string
    value: string
    meta?: string
  }>(),
  {
    meta: '',
  },
)
</script>

<template>
  <section class="hero-card">
    <div class="flex flex-wrap items-start justify-between gap-6">
      <div class="min-w-0">
        <slot name="eyebrow" />

        <p
          class="hero-label"
          :class="$slots.eyebrow ? 'mt-3' : ''"
        >
          {{ label }}
        </p>
        <p class="hero-value">
          {{ value }}
        </p>

        <slot name="meta">
          <p
            v-if="meta"
            class="hero-meta mt-2"
          >
            {{ meta }}
          </p>
        </slot>
      </div>

      <div
        v-if="$slots.actions"
        class="flex flex-wrap items-center gap-3"
      >
        <slot name="actions" />
      </div>
    </div>

    <slot />
  </section>
</template>

<script setup lang="ts">
interface Tab {
  id: string
  label: string
  icon?: any
  count?: number
  disabled?: boolean
}

interface TabsProps {
  modelValue: string
  tabs: Tab[]
  variant?: 'line' | 'pills'
}

const props = withDefaults(defineProps<TabsProps>(), {
  variant: 'line',
})

const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

const selectTab = (tab: Tab) => {
  if (!tab.disabled) {
    emit('update:modelValue', tab.id)
  }
}

const tabClasses = (tab: Tab) => {
  if (props.variant === 'pills') {
    return [
      'tab-pill',
      { 'tab-pill-active': props.modelValue === tab.id },
    ]
  }
  return [
    'tab',
    { 'tab-active': props.modelValue === tab.id },
  ]
}
</script>

<template>
  <div :class="variant === 'pills' ? 'tab-pills' : 'tabs'">
    <template
      v-for="tab in tabs"
      :key="tab.id"
    >
      <button
        :id="`tab-${tab.id}`"
        role="tab"
        :aria-selected="modelValue === tab.id"
        :aria-controls="`panel-${tab.id}`"
        :class="[
          tabClasses(tab),
          { 'opacity-40 cursor-not-allowed': tab.disabled },
        ]"
        :disabled="tab.disabled"
        @click="selectTab(tab)"
      >
        <span class="inline-flex items-center">
          <component
            :is="tab.icon"
            v-if="tab.icon"
            class="w-4 h-4 mr-2"
          />
          {{ tab.label }}
          <span
            v-if="tab.count !== undefined"
            :class="[
              'ml-2 px-1.5 py-0.5 text-xs font-medium rounded-full',
              variant === 'line' && modelValue === tab.id
                ? 'bg-primary-600 text-white'
                : 'bg-navy-100 text-navy-500 dark:bg-navy-700 dark:text-navy-300'
            ]"
          >
            {{ tab.count }}
          </span>
        </span>
      </button>
    </template>
  </div>

  <div
    v-for="tab in tabs"
    :id="`panel-${tab.id}`"
    :key="tab.id"
    :hidden="modelValue !== tab.id"
    role="tabpanel"
    :aria-labelledby="`tab-${tab.id}`"
  >
    <slot :name="tab.id" />
  </div>
</template>

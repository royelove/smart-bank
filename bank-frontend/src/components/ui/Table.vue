<script setup lang="ts">
import { computed, h } from 'vue'

interface Column<T> {
  key: string
  header: string
  render?: (row: T) => any
  class?: string
  width?: string
  align?: 'left' | 'center' | 'right'
}

interface TableProps<T> {
  columns: Column<T>[]
  data: T[]
  keyField: keyof T | ((row: T) => string)
  loading?: boolean
  emptyMessage?: string
  striped?: boolean
  hoverable?: boolean
  /**
   * Drop the table's own frame. Set this when the table already sits inside a
   * card: otherwise you get a border inside a border plus a 24px gutter,
   * which is the single most common visual defect in this app.
   */
  bleed?: boolean
  class?: string
}

const props = withDefaults(defineProps<TableProps<any>>(), {
  loading: false,
  emptyMessage: 'No data available',
  striped: false,
  hoverable: true,
  bleed: false,
})

const getRowKey = (row: any) => {
  if (typeof props.keyField === 'function') {
    return props.keyField(row)
  }
  return row[props.keyField]
}

const getAlignClass = (align?: string) => {
  switch (align) {
    case 'center': return 'text-center'
    case 'right': return 'text-right'
    default: return 'text-left'
  }
}

const containerClasses = computed(() => [
  props.bleed ? 'table-container-bleed' : 'table-container',
  props.class,
])

/**
 * `column.render` returns a VNode / VNode[] / string. Interpolating a VNode
 * with {{ }} stringifies it into "[object Object]", so render columns must go
 * through a component boundary instead. Wrapping the component in a functional
 * component keeps it render-only (no extra DOM node for VNodes).
 */
const RenderCell = (cellProps: { render: (row: any) => any; row: any }) => {
  const output = cellProps.render(cellProps.row)
  if (output === null || output === undefined || output === false) return null
  if (Array.isArray(output) || typeof output === 'object') return output
  return h('span', String(output))
}
</script>

<template>
  <div :class="containerClasses">
    <div
      v-if="loading"
      class="p-10 text-center"
    >
      <div class="flex items-center justify-center gap-2 text-navy-400 dark:text-navy-500">
        <svg
          class="animate-spin h-5 w-5"
          viewBox="0 0 24 24"
          fill="none"
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
        <span>Loading...</span>
      </div>
    </div>
    <table
      v-else
      class="table"
      :class="{ 'table-striped': striped, 'table-hover': hoverable }"
    >
      <thead>
        <tr>
          <th
            v-for="column in columns"
            :key="column.key"
            :style="{ width: column.width }"
            :class="[getAlignClass(column.align), column.class]"
            scope="col"
          >
            {{ column.header }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="row in data"
          :key="getRowKey(row)"
        >
          <td
            v-for="column in columns"
            :key="column.key"
            :class="[getAlignClass(column.align), column.class]"
          >
            <template v-if="column.render">
              <RenderCell
                :render="column.render"
                :row="row"
              />
            </template>
            <template v-else>
              {{ row[column.key] }}
            </template>
          </td>
        </tr>
        <tr v-if="data.length === 0 && !loading">
          <td
            :colspan="columns.length"
            class="text-center py-10 text-navy-400 dark:text-navy-500"
          >
            {{ emptyMessage }}
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
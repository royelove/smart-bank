<script setup lang="ts">
import { computed } from 'vue'
import { X } from 'lucide-vue-next'

interface ModalProps {
  modelValue: boolean
  title?: string
  size?: 'sm' | 'md' | 'lg' | 'xl' | 'full'
  closeOnOverlayClick?: boolean
  showClose?: boolean
}

const props = withDefaults(defineProps<ModalProps>(), {
  modelValue: false,
  size: 'md',
  closeOnOverlayClick: true,
  showClose: true,
})

const emit = defineEmits<{ 'update:modelValue': [value: boolean]; close: [] }>()

const sizes = {
  sm: 'max-w-sm',
  md: 'max-w-lg',
  lg: 'max-w-2xl',
  xl: 'max-w-4xl',
  full: 'max-w-[90vw]',
}

const handleOverlayClick = () => {
  if (props.closeOnOverlayClick) {
    emit('update:modelValue', false)
    emit('close')
  }
}

const handleClose = () => {
  emit('update:modelValue', false)
  emit('close')
}

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape') {
    handleClose()
  }
}

const modalContentClasses = computed(() => ['modal-content', sizes[props.size]])
</script>

<template>
  <Teleport to="body">
    <div
      v-if="modelValue"
      class="modal-overlay"
      role="dialog"
      aria-modal="true"
      :aria-labelledby="title ? 'modal-title' : undefined"
      @click="handleOverlayClick"
      @keydown="handleKeydown"
    >
      <div
        :class="modalContentClasses"
        @click.stop
      >
        <div
          v-if="title || showClose"
          class="modal-header"
        >
          <h2
            v-if="title"
            id="modal-title"
            class="text-heading-md text-navy-900 dark:text-navy-50"
          >
            {{ title }}
          </h2>
          <button
            v-if="showClose"
            type="button"
            class="btn-ghost btn-icon p-1 rounded-full hover:bg-navy-100 dark:hover:bg-navy-800"
            aria-label="Close modal"
            @click="handleClose"
          >
            <X class="w-5 h-5" />
          </button>
        </div>
        <div class="modal-body">
          <slot />
        </div>
        <div
          v-if="$slots.footer"
          class="modal-footer"
        >
          <slot name="footer" />
        </div>
      </div>
    </div>
  </Teleport>
</template>

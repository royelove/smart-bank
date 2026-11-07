<script setup lang="ts">
import { ref, computed } from 'vue'
import { Eye, EyeOff, AlertCircle } from 'lucide-vue-next'

interface InputProps {
  modelValue: string
  type?: string
  placeholder?: string
  label?: string
  error?: string
  hint?: string
  disabled?: boolean
  readonly?: boolean
  required?: boolean
  autocomplete?: string
  name?: string
  id?: string
  class?: string
  showPasswordToggle?: boolean
  /**
   * Optional value transform (e.g. `normalizePhone`). Applied to every typed,
   * pasted or autofilled value before it reaches the model.
   */
  transform?: (value: string) => string
}

const props = withDefaults(defineProps<InputProps>(), {
  type: 'text',
  disabled: false,
  readonly: false,
  required: false,
  showPasswordToggle: false,
})

const emit = defineEmits<{ 'update:modelValue': [value: string]; blur: [FocusEvent] }>()

const showPassword = ref(false)
const inputId = props.id || `input-${Math.random().toString(36).slice(2, 9)}`
const describedBy = computed(() => {
  const ids = []
  if (props.error) ids.push(`${inputId}-error`)
  if (props.hint) ids.push(`${inputId}-hint`)
  return ids.join(' ') || undefined
})

const wrapperClasses = computed(() => [props.class])

const inputClasses = computed(() => [
  'input',
  { 'input-error': props.error, 'input-success': !props.error && props.modelValue && !props.disabled },
  { 'pr-12': props.showPasswordToggle },
])

/**
 * Apply the optional `transform` and write the result back to the native input.
 *
 * Rewriting `target.value` matters: when a transform drops part of the raw text
 * (browser autofill handing over `+86 138 1234 5678`) the cleaned string can
 * equal the current model value, and Vue then skips the DOM patch — the rejected
 * `+86` would stay visible in the field.
 */
const commitValue = (target: HTMLInputElement, raw: string) => {
  const value = props.transform ? props.transform(raw) : raw
  if (target.value !== value) target.value = value
  emit('update:modelValue', value)
}

const handleInput = (event: Event) => {
  const target = event.target as HTMLInputElement
  commitValue(target, target.value)
}

const handleBlur = (event: FocusEvent) => {
  // Autofill can set the value without firing `input`, so normalise once more.
  const target = event.target as HTMLInputElement
  commitValue(target, target.value)
  emit('blur', event)
}

const togglePassword = () => {
  showPassword.value = !showPassword.value
}
</script>

<template>
  <div :class="wrapperClasses">
    <label
      v-if="label"
      :for="inputId"
      class="label"
    >
      {{ label }}
      <span
        v-if="required"
        class="text-danger-500 ml-1"
        aria-hidden="true"
      >*</span>
    </label>
    <div class="relative">
      <input
        :id="inputId"
        :type="showPasswordToggle && showPassword ? 'text' : type"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :readonly="readonly"
        :required="required"
        :autocomplete="autocomplete"
        :name="name"
        :aria-describedby="describedBy"
        :aria-invalid="!!error"
        :class="inputClasses"
        @input="handleInput"
        @blur="handleBlur"
      >
      <button
        v-if="showPasswordToggle"
        type="button"
        class="absolute right-3 top-1/2 -translate-y-1/2 text-navy-400 hover:text-navy-600 dark:text-navy-500 dark:hover:text-navy-300"
        :aria-label="showPassword ? 'Hide password' : 'Show password'"
        @click="togglePassword"
      >
        <Eye
          v-if="showPassword"
          class="w-5 h-5"
        />
        <EyeOff
          v-else
          class="w-5 h-5"
        />
      </button>
    </div>
    <p
      v-if="error"
      :id="`${inputId}-error`"
      class="form-error"
      role="alert"
    >
      <AlertCircle class="w-4 h-4 flex-shrink-0" />
      {{ error }}
    </p>
    <p
      v-else-if="hint"
      :id="`${inputId}-hint`"
      class="form-hint"
    >
      {{ hint }}
    </p>
  </div>
</template>

<template>
  <teleport to="body">
    <transition name="bank-modal">
      <div v-if="visible" class="modal-overlay" @click.self="handleCancel">
        <div class="modal-container">
          <div class="modal-header">
            <div class="security-badge">
              <el-icon :size="16" color="var(--color-success)"><Lock /></el-icon>
              <span>安全加密通道</span>
            </div>
            <button class="close-btn" @click="handleCancel">
              <el-icon :size="18"><Close /></el-icon>
            </button>
          </div>

          <div class="modal-body">
            <div class="bank-brand">
              <el-icon :size="32" color="var(--color-accent)"><CreditCard /></el-icon>
              <span class="brand-name">钱钱银行</span>
            </div>

            <div class="pin-display-area">
              <div class="pin-label">{{ title || '请输入交易密码' }}</div>
              <div class="pin-dots">
                <span
                  v-for="i in maxLength"
                  :key="i"
                  :class="['dot', { filled: i <= displayValue.length }]"
                >
                  <span v-if="i <= displayValue.length" class="dot-filled">●</span>
                  <span v-else class="dot-empty">○</span>
                </span>
              </div>
              <div class="pin-mask">
                <input
                  ref="hiddenInput"
                  v-model="pinValue"
                  type="password"
                  :maxlength="maxLength"
                  class="hidden-input"
                  autofocus
                  @input="onInput"
                  @keyup.enter="handleConfirm"
                />
              </div>
            </div>

            <div class="keypad">
              <div class="keypad-row">
                <button class="key-btn" @click="appendPin('1')">1</button>
                <button class="key-btn" @click="appendPin('2')">2</button>
                <button class="key-btn" @click="appendPin('3')">3</button>
              </div>
              <div class="keypad-row">
                <button class="key-btn" @click="appendPin('4')">4</button>
                <button class="key-btn" @click="appendPin('5')">5</button>
                <button class="key-btn" @click="appendPin('6')">6</button>
              </div>
              <div class="keypad-row">
                <button class="key-btn" @click="appendPin('7')">7</button>
                <button class="key-btn" @click="appendPin('8')">8</button>
                <button class="key-btn" @click="appendPin('9')">9</button>
              </div>
              <div class="keypad-row">
                <button class="key-btn key-btn-clear" @click="clearPin">清除</button>
                <button class="key-btn" @click="appendPin('0')">0</button>
                <button class="key-btn key-btn-back" @click="deletePin">
                  <el-icon :size="20"><Back /></el-icon>
                </button>
              </div>
            </div>

            <div class="confirm-row">
              <button class="btn-cancel" @click="handleCancel">取消</button>
              <button
                class="btn-confirm"
                :disabled="pinValue.length < minLength"
                @click="handleConfirm"
              >
                确认 {{ pinValue.length }}/{{ maxLength }}
              </button>
            </div>
          </div>

          <div class="modal-footer">
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { Lock, Close, Back, Warning } from '@element-plus/icons-vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '请输入交易密码' },
  maxLength: { type: Number, default: 6 },
  minLength: { type: Number, default: 6 }
})

const emit = defineEmits(['confirm', 'cancel', 'update:visible'])

const pinValue = ref('')
const displayValue = ref('')
const hiddenInput = ref(null)

watch(() => props.visible, (val) => {
  if (val) {
    pinValue.value = ''
    displayValue.value = ''
    nextTick(() => {
      hiddenInput.value?.focus()
    })
  }
})

const onInput = () => {
  pinValue.value = pinValue.value.replace(/\D/g, '').slice(0, props.maxLength)
  displayValue.value = pinValue.value
}

const appendPin = (num) => {
  if (pinValue.value.length >= props.maxLength) return
  pinValue.value += num
  displayValue.value = pinValue.value
  hiddenInput.value?.focus()
}

const deletePin = () => {
  pinValue.value = pinValue.value.slice(0, -1)
  displayValue.value = pinValue.value
  hiddenInput.value?.focus()
}

const clearPin = () => {
  pinValue.value = ''
  displayValue.value = ''
  hiddenInput.value?.focus()
}

const handleConfirm = () => {
  if (pinValue.value.length < props.minLength) return
  emit('confirm', pinValue.value)
  pinValue.value = ''
  displayValue.value = ''
  emit('update:visible', false)
}

const handleCancel = () => {
  pinValue.value = ''
  displayValue.value = ''
  emit('cancel')
  emit('update:visible', false)
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  justify-content: center;
  align-items: center;
  background: rgba(2, 5, 10, 0.72);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

.modal-container {
  position: relative;
  width: 380px;
  background: var(--color-card);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-xl);
  overflow: hidden;
  animation: modalSlideIn var(--transition-slow) ease-out;
}

@keyframes modalSlideIn {
  from { opacity: 0; transform: scale(0.9) translateY(20px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-3) var(--space-4);
  background: var(--color-muted);
  border-bottom: 1px solid var(--glass-border);
}

.security-badge {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-sm);
  color: var(--color-success);
  font-weight: 500;
  letter-spacing: 0.05em;
  font-family: var(--font-sans);
}

.close-btn {
  background: none;
  border: none;
  color: var(--color-muted-foreground);
  cursor: pointer;
  padding: var(--space-1);
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  color: var(--color-foreground);
  background: color-mix(in srgb, var(--color-primary) 10%, transparent);
}

.modal-body {
  padding: var(--space-6) var(--space-7) var(--space-5);
  text-align: center;
}

.bank-brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  margin-bottom: var(--space-5);
}

.brand-name {
  font-size: var(--font-h3);
  font-weight: 700;
  color: var(--color-foreground);
  letter-spacing: 2px;
  font-family: var(--font-sans);
}

.pin-display-area {
  margin-bottom: var(--space-5);
}

.pin-label {
  font-size: var(--font-base);
  color: var(--color-muted-foreground);
  margin-bottom: var(--space-3);
  font-family: var(--font-sans);
}

.pin-dots {
  display: flex;
  justify-content: center;
  gap: var(--space-3);
  margin-bottom: var(--space-2);
}

.dot {
  font-size: 28px;
  line-height: 1;
  transition: all var(--transition-fast);
}

.dot-filled {
  color: var(--color-accent);
  text-shadow: 0 0 10px color-mix(in srgb, var(--color-primary) 70%, transparent);
}

.dot-empty {
  color: var(--color-border);
}

.hidden-input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
  pointer-events: none;
}

.keypad {
  max-width: 280px;
  margin: 0 auto var(--space-4);
}

.keypad-row {
  display: flex;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
}

.key-btn {
  flex: 1;
  height: var(--space-13);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  background: var(--color-muted);
  font-size: var(--font-h3);
  font-weight: 600;
  color: var(--color-foreground);
  cursor: pointer;
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  justify-content: center;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
  font-family: var(--font-mono);
}

.key-btn:hover {
  background: color-mix(in srgb, var(--color-primary) 12%, var(--color-muted));
  border-color: var(--color-primary);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.key-btn:active {
  transform: translateY(0);
  box-shadow: none;
  background: color-mix(in srgb, var(--color-primary) 18%, var(--color-muted));
}

.key-btn-clear {
  font-size: var(--font-base);
  color: var(--color-muted-foreground);
  font-weight: 500;
  font-family: var(--font-sans);
}

.key-btn-back {
  color: var(--color-muted-foreground);
}

.confirm-row {
  display: flex;
  gap: var(--space-3);
}

.btn-cancel,
.btn-confirm {
  flex: 1;
  height: var(--space-11);
  border-radius: var(--radius-md);
  font-size: var(--font-base);
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all var(--transition-normal);
  font-family: var(--font-sans);
}

.btn-cancel {
  background: var(--color-muted);
  border: 1px solid var(--glass-border);
  color: var(--color-muted-foreground);
}

.btn-cancel:hover {
  background: var(--color-card);
  color: var(--color-foreground);
}

.btn-confirm {
  background: var(--color-primary);
  color: var(--color-on-primary);
  box-shadow: var(--shadow-md);
}

.btn-confirm:hover:not(:disabled) {
  background: var(--color-secondary);
  box-shadow: var(--shadow-lg);
  transform: translateY(-1px);
}

.btn-confirm:active:not(:disabled) {
  transform: translateY(0);
}

.btn-confirm:disabled {
  background: var(--color-muted);
  color: var(--color-muted-foreground);
  box-shadow: none;
  cursor: not-allowed;
}

.modal-footer {
  padding: var(--space-3) var(--space-4);
  background: var(--color-muted);
  border-top: 1px solid var(--glass-border);
}

.bank-modal-enter-active,
.bank-modal-leave-active {
  transition: opacity var(--transition-slow) ease;
}

.bank-modal-enter-from,
.bank-modal-leave-to {
  opacity: 0;
}
</style>
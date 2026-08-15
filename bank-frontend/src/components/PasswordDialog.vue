<template>
  <teleport to="body">
    <transition name="bank-modal">
      <div v-if="visible" class="modal-overlay" @click.self="handleCancel">
        <div class="modal-container">
          <div class="modal-header">
            <div class="security-badge">
              <el-icon :size="16" color="var(--vault-up)"><Lock /></el-icon>
              <span>安全加密通道</span>
            </div>
            <button class="close-btn" @click="handleCancel">
              <el-icon :size="18"><Close /></el-icon>
            </button>
          </div>

          <div class="modal-body">
            <div class="bank-brand">
              <el-icon :size="32" color="var(--vault-accent)"><CreditCard /></el-icon>
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
            <div class="security-note">
              <el-icon :size="12" color="var(--vault-text-3)"><Warning /></el-icon>
              <span>请注意周围环境，防止密码泄露</span>
            </div>
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
  background: linear-gradient(180deg, #131c2e 0%, #0d1524 100%);
  border: 1px solid var(--vault-line-strong);
  border-radius: 20px;
  box-shadow: 0 32px 64px rgba(0, 0, 0, 0.6), 0 0 0 1px rgba(212, 176, 106, 0.08), inset 0 1px 0 rgba(255, 255, 255, 0.05);
  overflow: hidden;
  animation: modalSlideIn 0.3s ease-out;
}

@keyframes modalSlideIn {
  from { opacity: 0; transform: scale(0.9) translateY(20px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: linear-gradient(90deg, #0c1524, #12203a);
  border-bottom: 1px solid var(--vault-line);
}

.security-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--vault-up);
  font-weight: 500;
  letter-spacing: 0.05em;
}

.close-btn {
  background: none;
  border: none;
  color: var(--vault-text-3);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
}
.close-btn:hover { color: #fff; background: rgba(212,176,106,0.1); }

.modal-body {
  padding: 24px 28px 20px;
  text-align: center;
}

.bank-brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 20px;
}
.brand-name {
  font-size: 18px;
  font-weight: 700;
  color: var(--vault-text);
  letter-spacing: 2px;
}

.pin-display-area { margin-bottom: 20px; }
.pin-label { font-size: 14px; color: var(--vault-text-2); margin-bottom: 12px; }
.pin-dots { display: flex; justify-content: center; gap: 12px; margin-bottom: 8px; }
.dot { font-size: 28px; line-height: 1; transition: all 0.2s; }
.dot-filled { color: var(--vault-accent); text-shadow: 0 0 10px rgba(212,176,106,0.7); }
.dot-empty { color: var(--vault-line-strong); }
.hidden-input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
  pointer-events: none;
}

.keypad {
  max-width: 280px;
  margin: 0 auto 16px;
}
.keypad-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
.key-btn {
  flex: 1;
  height: 52px;
  border: 1px solid var(--vault-line);
  border-radius: 10px;
  background: rgba(212,176,106,0.04);
  font-size: 22px;
  font-weight: 600;
  color: var(--vault-text);
  cursor: pointer;
  transition: all 0.15s;
  display: flex;
  align-items: center;
  justify-content: center;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
}
.key-btn:hover {
  background: rgba(212,176,106,0.12);
  border-color: var(--vault-accent);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(212,176,106,0.2);
}
.key-btn:active {
  transform: translateY(0);
  box-shadow: none;
  background: rgba(212,176,106,0.18);
}
.key-btn-clear { font-size: 14px; color: var(--vault-text-3); font-weight: 500; }
.key-btn-back { color: var(--vault-text-2); }

.confirm-row { display: flex; gap: 12px; }
.btn-cancel, .btn-confirm {
  flex: 1;
  height: 44px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}
.btn-cancel {
  background: rgba(212,176,106,0.06);
  border: 1px solid var(--vault-line);
  color: var(--vault-text-2);
}
.btn-cancel:hover { background: rgba(212,176,106,0.1); }
.btn-confirm {
  background: linear-gradient(135deg, #b08d4a, #d4b06a);
  color: #fff;
  box-shadow: 0 0 18px rgba(212,176,106,0.25), inset 0 0 0 1px rgba(255,255,255,0.08);
}
.btn-confirm:hover:not(:disabled) {
  box-shadow: 0 4px 16px rgba(212,176,106,0.4);
}
.btn-confirm:disabled {
  background: var(--vault-line-strong);
  color: var(--vault-text-dim);
  box-shadow: none;
  cursor: not-allowed;
}

.modal-footer {
  padding: 10px 16px;
  background: rgba(8,12,20,0.6);
  border-top: 1px solid var(--vault-line);
}
.security-note {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 11px;
  color: var(--vault-text-3);
}

.bank-modal-enter-active, .bank-modal-leave-active {
  transition: opacity 0.3s ease;
}
.bank-modal-enter-from, .bank-modal-leave-to {
  opacity: 0;
}
</style>

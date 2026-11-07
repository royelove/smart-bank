<template>
  <div class="chatbot-container">
    <transition name="chat-slide">
      <div v-if="open" class="chat-panel">
        <div class="chat-header">
          <div class="chat-header-left">
            <div class="chat-avatar">
              <el-icon :size="18" color="#fff"><Headset /></el-icon>
            </div>
            <div>
              <div class="chat-title">智能客服</div>
              <div class="chat-status">在线</div>
            </div>
          </div>
          <button class="chat-close" @click="open = false">
            <el-icon :size="18"><Close /></el-icon>
          </button>
        </div>

        <div class="chat-messages" ref="msgRef">
          <div v-for="(msg, i) in messages" :key="i" :class="['msg-row', msg.role]">
            <div v-if="msg.role === 'assistant'" class="msg-avatar mini">
              <el-icon :size="14" color="#fff"><Headset /></el-icon>
            </div>
            <div class="msg-bubble">{{ msg.content }}</div>
            <div v-if="msg.role === 'user'" class="msg-avatar user-avatar-mini">
              <span>{{ userShortName }}</span>
            </div>
          </div>
          <div v-if="loading" class="msg-row assistant">
            <div class="msg-avatar mini">
              <el-icon :size="14" color="#fff"><Headset /></el-icon>
            </div>
            <div class="msg-bubble typing">
              <span class="dot"></span><span class="dot"></span><span class="dot"></span>
            </div>
          </div>
        </div>

        <div class="chat-input">
          <el-input
            v-model="input"
            placeholder="输入您的问题..."
            :disabled="loading"
            size="large"
            @keyup.enter="send"
          >
            <template #append>
              <el-button :disabled="loading || !input.trim()" @click="send" class="send-btn">
                发送
              </el-button>
            </template>
          </el-input>
        </div>
      </div>
    </transition>

    <button v-if="!open" class="chat-fab" @click="open = true">
      <el-icon :size="24" color="#fff"><ChatDotRound /></el-icon>
    </button>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Headset, ChatDotRound, Close } from '@element-plus/icons-vue'

const open = ref(false)
const input = ref('')
const loading = ref(false)
const msgRef = ref(null)
const messages = ref([
  { role: 'assistant', content: '您好！我是钱钱银行智能客服，请问有什么可以帮您的？' }
])

const userInfo = computed(() => {
  try { return JSON.parse(localStorage.getItem('userInfo') || '{}') }
  catch { return {} }
})
const userShortName = computed(() => (userInfo.value?.accountHolderName || '用')[0])

let abortController = null

const scrollToBottom = async () => {
  await nextTick()
  if (msgRef.value) msgRef.value.scrollTop = msgRef.value.scrollHeight
}

const send = async () => {
  const text = input.value.trim()
  if (!text || loading.value) return
  messages.value.push({ role: 'user', content: text })
  input.value = ''
  loading.value = true
  scrollToBottom()

  const assistantMsg = { role: 'assistant', content: '' }
  messages.value.push(assistantMsg)

  abortController = new AbortController()
  try {
    const token = localStorage.getItem('token')
    const res = await fetch('http://localhost:8080/ai/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: JSON.stringify({ message: text }),
      signal: abortController.signal
    })

    if (!res.ok) {
      assistantMsg.content = '网络异常，请稍后再试。'
      loading.value = false
      scrollToBottom()
      return
    }

    const reader = res.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.substring(5).replace(/\r$/, '')
          if (data === '') continue
          assistantMsg.content += data
          scrollToBottom()
        }
      }
    }
  } catch (e) {
    if (e.name !== 'AbortError') {
      assistantMsg.content = assistantMsg.content || '网络异常，请稍后再试。'
    }
  } finally {
    loading.value = false
    abortController = null
    scrollToBottom()
  }
}

const cancel = () => {
  if (abortController) {
    abortController.abort()
    loading.value = false
  }
}
</script>

<style scoped>
.chatbot-container {
  position: fixed;
  bottom: var(--space-6);
  right: var(--space-6);
  z-index: 9999;
}

.chat-fab {
  width: var(--space-12);
  height: var(--space-12);
  border-radius: var(--radius-full);
  background: var(--color-primary);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-lg), 0 0 20px color-mix(in srgb, var(--color-primary) 40%, transparent);
  transition: all var(--transition-normal);
}

.chat-fab:hover {
  transform: scale(1.08);
  box-shadow: var(--shadow-xl), 0 0 30px color-mix(in srgb, var(--color-primary) 50%, transparent);
}

.chat-panel {
  width: 380px;
  height: 540px;
  background: var(--color-card);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-xl);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-5);
  background: var(--color-muted);
  border-bottom: 1px solid var(--glass-border);
  color: var(--color-foreground);
}

.chat-header-left {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.chat-avatar {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  background: color-mix(in srgb, var(--color-primary) 15%, transparent);
  border: 1px solid var(--glass-border);
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-title {
  font-size: var(--font-base);
  font-weight: 600;
  font-family: var(--font-sans);
}

.chat-status {
  font-size: var(--font-sm);
  color: var(--color-success);
  font-family: var(--font-sans);
}

.chat-close {
  background: none;
  border: none;
  color: var(--color-muted-foreground);
  cursor: pointer;
  width: var(--space-8);
  height: var(--space-8);
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}

.chat-close:hover {
  background: color-mix(in srgb, var(--color-primary) 10%, transparent);
  color: var(--color-foreground);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  background: var(--color-background);
}

.msg-row {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
  max-width: 85%;
}

.msg-row.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-full);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
}

.msg-avatar.mini {
  background: var(--color-muted);
  border: 1px solid var(--glass-border);
}

.user-avatar-mini {
  background: color-mix(in srgb, var(--color-primary) 20%, var(--color-muted));
  color: var(--color-primary);
  font-family: var(--font-sans);
}

.msg-bubble {
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-lg);
  font-size: var(--font-base);
  line-height: 1.5;
  word-break: break-word;
  font-family: var(--font-sans);
}

.msg-row.assistant .msg-bubble {
  background: var(--color-card);
  color: var(--color-foreground);
  border: 1px solid var(--glass-border);
  border-top-left-radius: var(--radius-sm);
}

.msg-row.user .msg-bubble {
  background: var(--color-primary);
  color: var(--color-on-primary);
  border-top-right-radius: var(--radius-sm);
  box-shadow: var(--shadow-md);
}

.typing {
  display: flex;
  gap: 4px;
  align-items: center;
  padding: var(--space-3) var(--space-4) !important;
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: var(--radius-full);
  background: var(--color-accent);
  animation: bounce 1.4s infinite;
}

.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

.chat-input {
  padding: var(--space-3);
  border-top: 1px solid var(--glass-border);
}

.send-btn {
  border: none !important;
  background: var(--color-primary) !important;
  color: var(--color-on-primary) !important;
  font-weight: 600 !important;
  cursor: pointer !important;
  transition: all var(--transition-normal) !important;
  font-family: var(--font-sans);
}

.send-btn:hover:not(:disabled) {
  background: var(--color-secondary) !important;
}

:deep(.el-input__wrapper) {
  background: var(--color-muted);
  border: 1px solid var(--glass-border);
  box-shadow: none;
}

:deep(.el-input__inner) {
  color: var(--color-foreground);
  font-family: var(--font-sans);
}

:deep(.el-input-group__append) {
  padding: 0;
}

:deep(.el-button) {
  height: 100%;
  border-radius: 0;
  min-width: 60px;
}

.chat-slide-enter-active,
.chat-slide-leave-active {
  transition: all var(--transition-slow) ease;
}

.chat-slide-enter-from,
.chat-slide-leave-to {
  opacity: 0;
  transform: translateY(var(--space-4)) scale(0.95);
}
</style>
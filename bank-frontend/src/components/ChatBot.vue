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
              <el-button :disabled="loading || !input.trim()" @click="send" style="border:none;background:linear-gradient(135deg,#1a3a6b,#2d5a8c);color:#fff;font-weight:600">
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
  { role: 'assistant', content: '您好！我是智慧银行智能客服，请问有什么可以帮您的？' }
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
  bottom: 24px;
  right: 24px;
  z-index: 9999;
}

.chat-fab {
  width: 54px; height: 54px; border-radius: 50%;
  background: linear-gradient(135deg, #1a3a6b, #2d5a8c);
  border: none; cursor: pointer; display: flex;
  align-items: center; justify-content: center;
  box-shadow: 0 4px 16px rgba(26,58,107,0.35);
  transition: transform 0.2s;
}
.chat-fab:hover { transform: scale(1.08); }

.chat-panel {
  width: 360px; height: 520px;
  background: #fff; border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.15);
  display: flex; flex-direction: column;
  overflow: hidden;
}

.chat-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 18px;
  background: linear-gradient(135deg, #1a3a6b, #2d5a8c);
  color: #fff;
}
.chat-header-left { display: flex; align-items: center; gap: 10px; }
.chat-avatar {
  width: 36px; height: 36px; border-radius: 50%;
  background: rgba(255,255,255,0.2);
  display: flex; align-items: center; justify-content: center;
}
.chat-title { font-size: 15px; font-weight: 600; }
.chat-status { font-size: 11px; opacity: 0.8; }
.chat-close {
  background: none; border: none; color: #fff; cursor: pointer;
  width: 32px; height: 32px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  transition: background 0.2s;
}
.chat-close:hover { background: rgba(255,255,255,0.15); }

.chat-messages {
  flex: 1; overflow-y: auto; padding: 16px;
  display: flex; flex-direction: column; gap: 12px;
  background: #f7f8fa;
}

.msg-row {
  display: flex; align-items: flex-start; gap: 8px;
  max-width: 85%;
}
.msg-row.user { align-self: flex-end; flex-direction: row-reverse; }

.msg-avatar {
  width: 28px; height: 28px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 700;
}
.msg-avatar.mini { background: #1a3a6b; }
.user-avatar-mini {
  background: linear-gradient(135deg, #f5af19, #e6a23c); color: #fff;
}

.msg-bubble {
  padding: 10px 14px; border-radius: 12px;
  font-size: 13px; line-height: 1.5; word-break: break-word;
}
.msg-row.assistant .msg-bubble {
  background: #fff; color: #333;
  border-top-left-radius: 4px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.msg-row.user .msg-bubble {
  background: linear-gradient(135deg, #1a3a6b, #2d5a8c);
  color: #fff; border-top-right-radius: 4px;
}

.typing { display: flex; gap: 4px; align-items: center; padding: 12px 16px !important; }
.dot {
  width: 7px; height: 7px; border-radius: 50%;
  background: #999; animation: bounce 1.4s infinite;
}
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

.chat-input { padding: 12px; border-top: 1px solid #eee; }
.chat-input :deep(.el-input-group__append) { padding: 0; }
.chat-input :deep(.el-button) { height: 100%; border-radius: 0; min-width: 60px; }

.chat-slide-enter-active, .chat-slide-leave-active {
  transition: all 0.3s ease;
}
.chat-slide-enter-from, .chat-slide-leave-to {
  opacity: 0; transform: translateY(16px) scale(0.95);
}
</style>

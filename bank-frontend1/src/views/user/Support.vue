<script setup lang="ts">
/**
 * 在线客服 — user-facing.
 *
 * Two real backend surfaces, no mocks:
 *
 * 1. 智能客服  POST /ai/chat/stream (SSE)  — AiController:29
 *              AiServiceImpl.chatStream 流式转发大模型；未配置 API Key 时
 *              直接以一条 `message` 事件返回本地关键词回复。
 *              事件协议：event:message / data:<片段> …，最后 event:done。
 *              注意 POST /ai/chat（非流式）在后端永远是 mockReply，所以默认走流式。
 *
 * 2. 我的工单  POST /support/ticket          — CustomerSupportServiceImpl:90
 *              GET  /support/tickets?accountNumber=  — :135
 *              对 U_ 用户 token，LoginContext.getAccount() 为 null、只有 userId，
 *              所以 accountNumber 必须显式传本人名下账户（:106-116 会校验归属）。
 *              工单落库时后端会用 AI 做情感/有效性预审（:126）。
 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  Bot,
  CircleStop,
  Inbox,
  LifeBuoy,
  RefreshCw,
  Send,
  Sparkles,
  TicketCheck,
  UserRound,
  Wand2,
} from 'lucide-vue-next'
import { Alert, Badge, Button, Card, EmptyState, Input, Modal, Table } from '@/components/ui'
import { PageHeader, SectionNav } from '@/components/layout'
import { usePageMeta } from '@/composables/usePageMeta'
import { chatStream } from '@/api/ai'
import { getMyTickets, submitTicket } from '@/api/support'
import { getUserAccounts } from '@/api/user'
import { errorMessage } from '@/utils/validation'
import { formatDateTime, maskAccountNumber } from '@/utils/format'
import { TICKET_CATEGORIES, aiValidLabel, aiValidTone, sentimentTone, ticketStatusLabel, ticketStatusTone } from '@/utils/support'
import type { Account, SupportTicket } from '@/types'

const { title, subtitle } = usePageMeta()

const activeTab = ref('assistant')

/* ------------------------------------------------------------- 智能客服 */

interface ChatMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
}

const QUICK_QUESTIONS = ['怎么开通账户？', '转账需要手续费吗？', '如何修改交易密码？', '理财产品怎么赎回？', '账户被冻结了怎么办？']

const messages = ref<ChatMessage[]>([
  {
    id: 0,
    role: 'assistant',
    content: '您好，我是钱钱银行智能客服助手。您可以问我开户、登录、存取款、转账、交易记录、理财产品与赎回、冻结解冻、修改密码等问题。',
  },
])
const draft = ref('')
const streaming = ref(false)
const streamError = ref('')
const transcript = ref<HTMLElement | null>(null)
let abortStream: (() => void) | null = null
let messageId = 1

async function scrollToBottom() {
  await nextTick()
  if (transcript.value) transcript.value.scrollTop = transcript.value.scrollHeight
}

function send(text?: string) {
  const content = (text ?? draft.value).trim()
  if (!content || streaming.value) return

  streamError.value = ''
  messages.value.push({ id: messageId++, role: 'user', content })
  draft.value = ''

  const replyId = messageId++
  messages.value.push({ id: replyId, role: 'assistant', content: '' })
  streaming.value = true
  scrollToBottom()

  const reply = messages.value[messages.value.length - 1]

  abortStream = chatStream(content, {
    onChunk: (chunk) => {
      reply.content += chunk
      scrollToBottom()
    },
    onDone: () => {
      streaming.value = false
      abortStream = null
      if (!reply.content) {
        reply.content = '抱歉，我暂时无法回答这个问题，您可以提交工单由人工客服跟进。'
      }
      scrollToBottom()
    },
    onError: (error: unknown) => {
      streaming.value = false
      abortStream = null
      reply.content = ''
      streamError.value = errorMessage(error, '智能客服连接失败，请稍后重试')
    },
  })
}

function stopStream() {
  if (abortStream) abortStream()
  abortStream = null
  streaming.value = false
}

/* ------------------------------------------------------------- 我的工单 */

const accounts = ref<Account[]>([])
const tickets = ref<SupportTicket[]>([])
const loading = ref(false)
const pageError = ref('')
const pageNotice = ref('')

const selectedAccount = ref('')
const category = ref<string>(TICKET_CATEGORIES[0])
const ticketTitle = ref('')
const ticketDescription = ref('')
const submitErrors = ref<Record<string, string>>({})
const submitting = ref(false)

const detailOpen = ref(false)
const activeTicket = ref<SupportTicket | null>(null)

const canSubmit = computed(() => Boolean(selectedAccount.value))

async function loadAccounts() {
  try {
    accounts.value = (await getUserAccounts()) ?? []
    if (!selectedAccount.value && accounts.value.length) {
      selectedAccount.value = accounts.value[0].accountNumber
    }
  } catch (e) {
    pageError.value = errorMessage(e, '无法加载银行卡列表')
  }
}

async function loadTickets() {
  if (!selectedAccount.value) {
    tickets.value = []
    return
  }
  loading.value = true
  try {
    tickets.value = (await getMyTickets(selectedAccount.value)) ?? []
  } catch (e) {
    pageError.value = errorMessage(e, '工单列表加载失败')
    tickets.value = []
  } finally {
    loading.value = false
  }
}

async function onAccountChange() {
  pageNotice.value = ''
  await loadTickets()
}

function validateTicket(): boolean {
  const errors: Record<string, string> = {}
  if (!selectedAccount.value) errors.account = '请选择关联账户'
  if (!ticketTitle.value.trim()) errors.title = '请输入标题'
  else if (ticketTitle.value.trim().length > 50) errors.title = '标题不能超过 50 个字'
  if (!ticketDescription.value.trim()) errors.description = '请描述您遇到的问题'
  else if (ticketDescription.value.trim().length < 5) errors.description = '描述太短了，请补充细节'
  submitErrors.value = errors
  return Object.keys(errors).length === 0
}

async function onSubmitTicket() {
  pageError.value = ''
  pageNotice.value = ''
  if (!validateTicket()) return

  submitting.value = true
  try {
    await submitTicket({
      accountNumber: selectedAccount.value,
      category: category.value,
      title: ticketTitle.value.trim(),
      description: ticketDescription.value.trim(),
    })
    pageNotice.value = '工单已提交，系统已自动完成 AI 预审，客服会尽快处理'
    ticketTitle.value = ''
    ticketDescription.value = ''
    submitErrors.value = {}
    await loadTickets()
  } catch (e) {
    pageError.value = errorMessage(e, '工单提交失败')
  } finally {
    submitting.value = false
  }
}

function openTicket(ticket: SupportTicket) {
  activeTicket.value = ticket
  detailOpen.value = true
}

const columns = [
  {
    key: 'ticketNo',
    header: '工单号',
    width: '150px',
    render: (row: SupportTicket) => row.ticketNo || '--',
  },
  {
    key: 'title',
    header: '标题',
    render: (row: SupportTicket) => row.title || '--',
  },
  {
    key: 'categoryText',
    header: '类型',
    width: '100px',
    render: (row: SupportTicket) => row.categoryText || row.category || '--',
  },
  {
    key: 'aiValid',
    header: 'AI 预审',
    width: '150px',
    render: (row: SupportTicket) =>
      h('div', { class: 'flex items-center gap-1.5' }, [
        h(Badge, { variant: sentimentTone(row.sentiment) }, () => row.sentiment || '未分析'),
        h(Badge, { variant: aiValidTone(row.aiValid) }, () => aiValidLabel(row.aiValid)),
      ]),
  },
  {
    key: 'status',
    header: '状态',
    width: '100px',
    render: (row: SupportTicket) =>
      h(Badge, { variant: ticketStatusTone(row.status) }, () => row.statusText || ticketStatusLabel(row.status)),
  },
  {
    key: 'creationDate',
    header: '提交时间',
    width: '150px',
    render: (row: SupportTicket) => formatDateTime(row.creationDate),
  },
  {
    key: 'actions',
    header: '操作',
    width: '90px',
    align: 'right' as const,
    render: (row: SupportTicket) => h(Button, { variant: 'ghost', size: 'sm', onClick: () => openTicket(row) }, () => '详情'),
  },
]

onMounted(async () => {
  await loadAccounts()
  await loadTickets()
  scrollToBottom()
})

onBeforeUnmount(() => {
  if (abortStream) abortStream()
})
</script>

<script lang="ts">
import { h } from 'vue'
</script>

<template>
  <div class="app-container">
    <PageHeader
      :title="title"
      :subtitle="subtitle"
    />

    <Alert
      v-if="pageError"
      variant="danger"
      :description="pageError"
      dismissible
      class="mb-6"
      @dismiss="pageError = ''"
    />
    <Alert
      v-if="pageNotice"
      variant="success"
      :description="pageNotice"
      dismissible
      class="mb-6"
      @dismiss="pageNotice = ''"
    />

    <div class="layout-split">
      <SectionNav
        v-model="activeTab"
        title="服务支持"
        :items="[
          { id: 'assistant', label: '智能客服', icon: Bot },
          { id: 'tickets', label: '我的工单', icon: TicketCheck, count: tickets.length },
        ]"
      />

      <!-- 智能客服 -->
      <template v-if="activeTab === 'assistant'">
        <Card>
          <div class="card-header">
            <h2 class="section-title">
              <Sparkles class="h-4 w-4 text-primary-500" />
              AI 智能助手
            </h2>
            <span class="text-body-xs text-navy-500 dark:text-navy-400">
              回答由大模型生成，仅供参考
            </span>
          </div>

          <div
            ref="transcript"
            class="h-[26rem] space-y-4 overflow-y-auto bg-navy-50/60 px-6 py-5 dark:bg-navy-800/20"
          >
            <div
              v-for="message in messages"
              :key="message.id"
              class="flex gap-3"
              :class="message.role === 'user' ? 'flex-row-reverse' : ''"
            >
              <div
                class="mt-0.5 grid h-8 w-8 shrink-0 place-items-center rounded-full"
                :class="message.role === 'user'
                  ? 'bg-navy-200 text-navy-700 dark:bg-navy-700 dark:text-navy-100'
                  : 'bg-primary-100 text-primary-700 dark:bg-primary-500/10 dark:text-primary-300'"
              >
                <UserRound
                  v-if="message.role === 'user'"
                  class="h-4 w-4"
                />
                <Bot
                  v-else
                  class="h-4 w-4"
                />
              </div>

              <div
                class="max-w-[75%] whitespace-pre-line rounded-2xl px-4 py-2.5 text-body-sm"
                :class="message.role === 'user'
                  ? 'bg-primary-600 text-white'
                  : 'bg-white text-navy-800 ring-1 ring-navy-200 dark:bg-navy-900 dark:text-navy-100 dark:ring-navy-700'"
              >
                <span v-if="message.content">{{ message.content }}</span>
                <span
                  v-else
                  class="inline-flex gap-1 text-navy-400"
                >
                  <span class="animate-pulse-soft">●</span>
                  <span class="animate-pulse-soft [animation-delay:0.2s]">●</span>
                  <span class="animate-pulse-soft [animation-delay:0.4s]">●</span>
                </span>
              </div>
            </div>
          </div>

          <div class="card-body border-t border-navy-200 dark:border-navy-800">
            <Alert
              v-if="streamError"
              variant="danger"
              :description="streamError"
              class="mb-4"
            />

            <div class="mb-3 flex flex-wrap gap-2">
              <button
                v-for="question in QUICK_QUESTIONS"
                :key="question"
                type="button"
                class="rounded-full border border-navy-200 px-3 py-1 text-body-xs text-navy-600 transition-colors hover:border-primary-400 hover:text-primary-600 dark:border-navy-700 dark:text-navy-300 dark:hover:border-primary-500"
                :disabled="streaming"
                @click="send(question)"
              >
                {{ question }}
              </button>
            </div>

            <form
              class="flex items-end gap-3"
              @submit.prevent="send()"
            >
              <textarea
                v-model="draft"
                rows="2"
                class="input resize-none"
                placeholder="描述您的问题，回车发送（Shift + 回车换行）"
                :disabled="streaming"
                @keydown.enter.exact.prevent="send()"
              />
              <Button
                v-if="streaming"
                variant="secondary"
                @click="stopStream"
              >
                <CircleStop class="h-4 w-4" />
                停止
              </Button>
              <Button
                v-else
                type="submit"
                :disabled="!draft.trim()"
              >
                <Send class="h-4 w-4" />
                发送
              </Button>
            </form>
          </div>
        </Card>
      </template>

      <!-- 我的工单 -->
      <template v-else>
        <div class="grid grid-cols-1 gap-6 2xl:grid-cols-3">
          <Card>
            <div class="card-header">
              <h2 class="section-title">
                <Wand2 class="h-4 w-4 text-primary-500" />
                提交新工单
              </h2>
            </div>
            <div class="card-body space-y-4">
              <div>
                <label
                  class="label"
                  for="ticket-account"
                >关联账户</label>
                <select
                  id="ticket-account"
                  v-model="selectedAccount"
                  class="input"
                  @change="onAccountChange"
                >
                  <option
                    v-if="!accounts.length"
                    value=""
                  >
                    暂无可选账户
                  </option>
                  <option
                    v-for="account in accounts"
                    :key="account.accountNumber"
                    :value="account.accountNumber"
                  >
                    {{ maskAccountNumber(account.accountNumber) }}
                  </option>
                </select>
                <p
                  v-if="submitErrors.account"
                  class="form-error"
                >
                  {{ submitErrors.account }}
                </p>
                <p
                  v-else
                  class="form-hint"
                >
                  工单会记录在该账户名下，便于客服核对流水。
                </p>
              </div>

              <div>
                <label
                  class="label"
                  for="ticket-category"
                >问题类型</label>
                <select
                  id="ticket-category"
                  v-model="category"
                  class="input"
                >
                  <option
                    v-for="item in TICKET_CATEGORIES"
                    :key="item"
                    :value="item"
                  >
                    {{ item }}
                  </option>
                </select>
              </div>

              <div>
                <label
                  class="label"
                  for="ticket-title"
                >标题</label>
                <Input
                  id="ticket-title"
                  v-model="ticketTitle"
                  placeholder="一句话概括问题"
                  :error="submitErrors.title"
                  maxlength="50"
                />
              </div>

              <div>
                <label
                  class="label"
                  for="ticket-description"
                >问题描述</label>
                <textarea
                  id="ticket-description"
                  v-model="ticketDescription"
                  rows="5"
                  class="input resize-y"
                  placeholder="请补充时间、金额、对方账号等信息，便于客服快速定位"
                />
                <p
                  v-if="submitErrors.description"
                  class="form-error"
                >
                  {{ submitErrors.description }}
                </p>
              </div>

              <Button
                class="w-full"
                :loading="submitting"
                :disabled="!canSubmit"
                @click="onSubmitTicket"
              >
                <Send class="h-4 w-4" />
                提交工单
              </Button>
            </div>
          </Card>

          <Card class="2xl:col-span-2">
            <div class="card-header">
              <h2 class="section-title">
                <LifeBuoy class="h-4 w-4 text-navy-400" />
                我的工单
              </h2>
              <Button
                variant="ghost"
                size="sm"
                :loading="loading"
                @click="loadTickets"
              >
                <RefreshCw class="h-4 w-4" />
                刷新
              </Button>
            </div>

            <EmptyState
              v-if="!loading && !tickets.length"
              title="这条账户下还没有工单"
              description="提交后可以在这里跟踪处理进度和客服回复。"
              :icon="Inbox"
            />
            <Table
              v-else
              :columns="columns"
              :data="tickets"
              key-field="id"
              :loading="loading"
              :hoverable="true"
              :striped="true"
              bleed
              empty-message="暂无工单"
            />
          </Card>
        </div>
      </template>
    </div>

    <!-- 工单详情 -->
    <Modal
      v-model="detailOpen"
      title="工单详情"
      size="lg"
    >
      <div
        v-if="activeTicket"
        class="space-y-5"
      >
        <div class="flex flex-wrap items-center gap-2">
          <Badge :variant="ticketStatusTone(activeTicket.status)">
            {{ activeTicket.statusText || ticketStatusLabel(activeTicket.status) }}
          </Badge>
          <Badge :variant="sentimentTone(activeTicket.sentiment)">
            {{ activeTicket.sentiment || '未分析' }}
          </Badge>
          <Badge :variant="aiValidTone(activeTicket.aiValid)">
            AI {{ aiValidLabel(activeTicket.aiValid) }}
          </Badge>
          <span class="ml-auto font-mono text-body-xs text-navy-500 dark:text-navy-400">
            {{ activeTicket.ticketNo }}
          </span>
        </div>

        <div>
          <h3 class="text-heading-sm text-navy-900 dark:text-navy-50">
            {{ activeTicket.title }}
          </h3>
          <p class="mt-2 whitespace-pre-line text-body-sm text-navy-700 dark:text-navy-200">
            {{ activeTicket.description }}
          </p>
        </div>

        <dl class="grid grid-cols-1 gap-x-6 gap-y-3 rounded-lg bg-navy-50 p-4 sm:grid-cols-2 dark:bg-navy-800/40">
          <div>
            <dt class="text-body-xs text-navy-500 dark:text-navy-400">
              关联账户
            </dt>
            <dd class="mt-0.5 font-mono text-body-sm text-navy-900 dark:text-navy-50">
              {{ activeTicket.accountNumber || '--' }}
            </dd>
          </div>
          <div>
            <dt class="text-body-xs text-navy-500 dark:text-navy-400">
              提交时间
            </dt>
            <dd class="mt-0.5 text-body-sm text-navy-900 dark:text-navy-50">
              {{ formatDateTime(activeTicket.creationDate) }}
            </dd>
          </div>
          <div>
            <dt class="text-body-xs text-navy-500 dark:text-navy-400">
              处理人
            </dt>
            <dd class="mt-0.5 text-body-sm text-navy-900 dark:text-navy-50">
              {{ activeTicket.handlerName || '待分配' }}
            </dd>
          </div>
          <div>
            <dt class="text-body-xs text-navy-500 dark:text-navy-400">
              关闭时间
            </dt>
            <dd class="mt-0.5 text-body-sm text-navy-900 dark:text-navy-50">
              {{ activeTicket.closeDate ? formatDateTime(activeTicket.closeDate) : '--' }}
            </dd>
          </div>
        </dl>

        <div class="rounded-lg border border-navy-200 p-4 dark:border-navy-700">
          <p class="text-body-sm font-medium text-navy-700 dark:text-navy-200">
            客服回复
          </p>
          <p class="mt-2 whitespace-pre-line text-body-sm text-navy-600 dark:text-navy-300">
            {{ activeTicket.reply || '客服尚未回复，请耐心等待。' }}
          </p>
        </div>
      </div>

      <template #footer>
        <Button
          variant="secondary"
          @click="detailOpen = false"
        >
          关闭
        </Button>
      </template>
    </Modal>
  </div>
</template>

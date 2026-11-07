<script setup lang="ts">
/**
 * 工单管理 — the staff ticket desk.
 *
 * Endpoints (all staff-scoped: they resolve the handler from the `S_` JWT):
 *   GET  /support/admin/tickets?status=&valid=   list, newest first
 *   POST /support/admin/accept/{id}              待处理 -> 处理中, claims the ticket
 *   POST /support/admin/reply  {ticketId, reply} -> 已回复
 *   POST /support/admin/close/{id}               -> 已关闭 + closeDate
 *
 * The list endpoint also supports server-side `status` / `valid` filtering; the
 * page fetches once and filters locally so the per-status counters in the filter
 * bar always agree with the table that is on screen.
 */
import { computed, h, onMounted, ref } from 'vue'
import type { Component } from 'vue'
import {
  Bot,
  CheckCircle,
  Clock,
  Filter,
  Hourglass,
  Inbox,
  MessageSquare,
  RefreshCw,
  Search,
  TicketCheck,
  UserCheck,
  XCircle,
} from 'lucide-vue-next'
import { Alert, Badge, Button, Card, EmptyState, Input, Modal, Table } from '@/components/ui'
import { PageHeader, SectionNav } from '@/components/layout'
import { usePageMeta } from '@/composables/usePageMeta'
import { acceptTicket, closeTicket, getAllTickets, replyTicket } from '@/api/support'
import { errorMessage } from '@/utils/validation'
import { formatDateTime, maskAccountNumber } from '@/utils/format'
import {
  TICKET_STATUSES,
  TICKET_STATUS,
  aiValidLabel,
  aiValidTone,
  canAccept,
  canClose,
  canReply,
  sentimentTone,
  ticketStatusLabel,
  ticketStatusTone,
} from '@/utils/support'
import type { SupportTicket } from '@/types'

const { title, subtitle } = usePageMeta()

const loading = ref(true)
const pageError = ref('')
const tickets = ref<SupportTicket[]>([])

const statusFilter = ref<number | 'all'>('all')
const validFilter = ref<number | 'all'>('all')
const keyword = ref('')

const counts = computed<Record<number, number>>(() => {
  const result: Record<number, number> = {
    [TICKET_STATUS.PENDING]: 0,
    [TICKET_STATUS.PROCESSING]: 0,
    [TICKET_STATUS.REPLIED]: 0,
    [TICKET_STATUS.CLOSED]: 0,
  }
  tickets.value.forEach((ticket) => {
    if (result[ticket.status] !== undefined) result[ticket.status] += 1
  })
  return result
})

/** Status icons for the rail, keyed by ticket status. */
const STATUS_ICONS: Record<number, Component> = {
  [TICKET_STATUS.PENDING]: Hourglass,
  [TICKET_STATUS.PROCESSING]: Clock,
  [TICKET_STATUS.REPLIED]: CheckCircle,
  [TICKET_STATUS.CLOSED]: XCircle,
}

/**
 * The rail is the status filter: each entry carries its own count, so the
 * per-status totals live with the control that filters by them instead of
 * being duplicated in a separate row of stat cards.
 */
const statusFacets = computed(() => [
  { id: 'all' as const, label: '全部工单', icon: Inbox, count: tickets.value.length },
  ...TICKET_STATUSES.map((item) => ({
    id: item.value,
    label: item.label,
    icon: STATUS_ICONS[item.value] ?? TicketCheck,
    count: counts.value[item.value] ?? 0,
  })),
])

const filtered = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  return tickets.value.filter((ticket) => {
    if (statusFilter.value !== 'all' && ticket.status !== statusFilter.value) return false
    if (validFilter.value !== 'all' && ticket.aiValid !== validFilter.value) return false
    if (!q) return true
    return [ticket.ticketNo, ticket.userName, ticket.userPhone, ticket.title, ticket.description, ticket.accountNumber]
      .filter(Boolean)
      .some((field) => String(field).toLowerCase().includes(q))
  })
})

async function load() {
  loading.value = true
  pageError.value = ''
  try {
    tickets.value = (await getAllTickets()) ?? []
  } catch (e) {
    pageError.value = errorMessage(e, '工单列表加载失败')
    tickets.value = []
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  statusFilter.value = 'all'
  validFilter.value = 'all'
  keyword.value = ''
}

/* ------------------------------------------------------------- detail pane */

const detailOpen = ref(false)
const active = ref<SupportTicket | null>(null)
const replyText = ref('')
const actionError = ref('')
const actionNotice = ref('')
const acting = ref(false)
const confirmingClose = ref(false)

function openDetail(ticket: SupportTicket) {
  active.value = ticket
  replyText.value = ticket.reply ?? ''
  actionError.value = ''
  actionNotice.value = ''
  confirmingClose.value = false
  detailOpen.value = true
}

/** Re-reads the open ticket from the refreshed list so the modal never goes stale. */
function syncActive() {
  if (!active.value) return
  active.value = tickets.value.find((item) => item.id === active.value?.id) ?? active.value
}

async function runAction(action: () => Promise<unknown>, successText: string) {
  actionError.value = ''
  actionNotice.value = ''
  acting.value = true
  try {
    await action()
    await load()
    syncActive()
    actionNotice.value = successText
  } catch (e) {
    actionError.value = errorMessage(e, '操作失败，请稍后重试')
  } finally {
    acting.value = false
  }
}

function onAccept(ticket: SupportTicket) {
  openDetail(ticket)
  runAction(() => acceptTicket(ticket.id), '工单已认领，状态更新为处理中')
}

function onReply() {
  if (!active.value) return
  if (!replyText.value.trim()) {
    actionError.value = '请输入回复内容'
    return
  }
  runAction(() => replyTicket({ ticketId: active.value!.id, reply: replyText.value.trim() }), '回复已提交，工单状态更新为已回复')
}

function onClose() {
  if (!active.value) return
  runAction(() => closeTicket(active.value!.id), '工单已关闭')
  confirmingClose.value = false
}

/* ----------------------------------------------------------------- columns */

const columns = [
  {
    key: 'ticketNo',
    header: '工单号',
    width: '150px',
    render: (row: SupportTicket) =>
      h('span', { class: 'font-mono text-xs text-navy-600 dark:text-navy-300' }, row.ticketNo || '--'),
  },
  {
    key: 'userName',
    header: '提交人',
    width: '150px',
    render: (row: SupportTicket) =>
      h('div', { class: 'min-w-0' }, [
        h('p', { class: 'truncate text-navy-900 dark:text-navy-50' }, row.userName || '--'),
        h('p', { class: 'font-mono text-xs text-navy-500 dark:text-navy-400' }, row.userPhone || '--'),
      ]),
  },
  {
    key: 'accountNumber',
    header: '关联账户',
    width: '160px',
    render: (row: SupportTicket) =>
      h(
        'span',
        { class: 'font-mono text-xs text-navy-500 dark:text-navy-400' },
        row.accountNumber ? maskAccountNumber(row.accountNumber) : '--',
      ),
  },
  {
    key: 'title',
    header: '标题',
    render: (row: SupportTicket) =>
      h('div', { class: 'min-w-0' }, [
        h(
          'button',
          {
            type: 'button',
            class: 'block max-w-md truncate text-left font-medium text-navy-900 hover:text-primary-600 dark:text-navy-50 dark:hover:text-primary-400',
            title: row.title,
            onClick: () => openDetail(row),
          },
          row.title || '--',
        ),
        h('p', { class: 'mt-0.5 text-xs text-navy-500 dark:text-navy-400' }, row.categoryText || row.category || '--'),
      ]),
  },
  {
    key: 'aiEval',
    header: 'AI 评估',
    width: '190px',
    render: (row: SupportTicket) =>
      h('div', { class: 'flex flex-col gap-1' }, [
        h('div', { class: 'flex items-center gap-1.5' }, [
          h(Badge, { variant: sentimentTone(row.sentiment) }, () => row.sentiment || '未分析'),
          h(Badge, { variant: aiValidTone(row.aiValid) }, () => aiValidLabel(row.aiValid)),
        ]),
        h(
          'p',
          { class: 'truncate text-xs text-navy-500 dark:text-navy-400', title: row.aiEval || '' },
          `${row.aiScore ?? 0} 分 · ${row.aiEval || '暂无评估'}`,
        ),
      ]),
  },
  {
    key: 'status',
    header: '状态',
    width: '100px',
    render: (row: SupportTicket) =>
      h(Badge, { variant: ticketStatusTone(row.status) }, () => ticketStatusLabel(row.status)),
  },
  {
    key: 'handlerName',
    header: '处理人',
    width: '120px',
    render: (row: SupportTicket) =>
      row.handlerName
        ? h('span', { class: 'text-navy-700 dark:text-navy-200' }, row.handlerName)
        : h('span', { class: 'text-navy-400 dark:text-navy-500' }, '未分配'),
  },
  {
    key: 'creationDate',
    header: '提交时间',
    width: '150px',
    render: (row: SupportTicket) =>
      h('span', { class: 'text-xs text-navy-500 dark:text-navy-400' }, formatDateTime(row.creationDate)),
  },
  {
    key: 'actions',
    header: '操作',
    width: '160px',
    align: 'right' as const,
    render: (row: SupportTicket) =>
      h('div', { class: 'flex items-center justify-end gap-1' }, [
        h(Button, { variant: 'ghost', size: 'sm', onClick: () => openDetail(row) }, () => '详情'),
        canAccept(row.status)
          ? h(Button, { variant: 'ghost', size: 'sm', onClick: () => onAccept(row) }, () => '认领')
          : null,
      ]),
  },
]

onMounted(load)
</script>

<template>
  <div class="app-container">
    <PageHeader
      :title="title"
      :subtitle="subtitle"
    >
      <template #actions>
        <div class="relative">
          <Filter class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-navy-400" />
          <select
            v-model="validFilter"
            class="input py-2 pl-9 pr-8"
            aria-label="按 AI 有效性筛选"
          >
            <option value="all">
              全部有效性
            </option>
            <option :value="1">
              判定有效
            </option>
            <option :value="0">
              判定待定
            </option>
          </select>
        </div>

        <div class="relative w-full sm:w-64">
          <Search class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-navy-400" />
          <Input
            v-model="keyword"
            placeholder="搜索工单号 / 提交人 / 标题"
            class="pl-9"
          />
        </div>

        <Button
          variant="ghost"
          size="sm"
          @click="resetFilters"
        >
          清除筛选
        </Button>

        <Button
          variant="secondary"
          size="sm"
          :loading="loading"
          @click="load"
        >
          <RefreshCw class="h-4 w-4" />
          刷新
        </Button>
      </template>
    </PageHeader>

    <Alert
      v-if="pageError"
      variant="danger"
      title="无法加载工单"
      :description="pageError"
      class="mb-6"
    />

    <div class="layout-split">
      <SectionNav
        v-model="statusFilter"
        title="工单状态"
        :items="statusFacets"
      />

      <div class="min-w-0 space-y-6">
        <Card v-if="!loading && !tickets.length && !pageError">
          <EmptyState
            title="暂无工单"
            description="用户通过手机银行提交工单后，会实时出现在这里。"
            :icon="Inbox"
          >
            <template #action>
              <Button
                variant="secondary"
                :loading="loading"
                @click="load"
              >
                刷新列表
              </Button>
            </template>
          </EmptyState>
        </Card>

        <Card v-else>
          <div class="card-header">
            <h2 class="section-title">
              <TicketCheck class="w-4 h-4 text-primary-600 dark:text-primary-400" />
              工单队列
            </h2>
            <span class="text-sm text-navy-500 dark:text-navy-400">
              共 {{ filtered.length }} 条
            </span>
          </div>
          <Table
            :columns="columns"
            :data="filtered"
            key-field="id"
            :loading="loading"
            :hoverable="true"
            :striped="true"
            bleed
            empty-message="没有符合筛选条件的工单"
          />
        </Card>
      </div>
    </div>

    <!-- 工单详情 -->
    <Modal
      v-model="detailOpen"
      title="工单详情"
      size="lg"
    >
      <div
        v-if="active"
        class="space-y-5"
      >
        <Alert
          v-if="actionError"
          variant="danger"
          :description="actionError"
        />
        <Alert
          v-if="actionNotice"
          variant="success"
          :description="actionNotice"
        />

        <div class="flex flex-wrap items-center gap-2">
          <Badge :variant="ticketStatusTone(active.status)">
            {{ active.statusText || ticketStatusLabel(active.status) }}
          </Badge>
          <Badge :variant="sentimentTone(active.sentiment)">
            {{ active.sentiment || '未分析' }}
          </Badge>
          <Badge :variant="aiValidTone(active.aiValid)">
            AI {{ aiValidLabel(active.aiValid) }}
          </Badge>
          <span class="ml-auto font-mono text-xs text-navy-500 dark:text-navy-400">
            {{ active.ticketNo }}
          </span>
        </div>

        <div>
          <h3 class="section-title">
            <TicketCheck class="h-4 w-4 text-navy-400" />
            {{ active.title }}
          </h3>
          <p class="mt-2 whitespace-pre-line text-body-md text-navy-700 dark:text-navy-200">
            {{ active.description }}
          </p>
        </div>

        <dl class="grid grid-cols-1 gap-x-6 gap-y-4 rounded-lg bg-navy-50 p-4 sm:grid-cols-2 dark:bg-navy-800/40">
          <div>
            <dt class="text-body-xs text-navy-500 dark:text-navy-400">
              提交人
            </dt>
            <dd class="mt-0.5 text-navy-900 dark:text-navy-50">
              {{ active.userName || '--' }} · {{ active.userPhone || '--' }}
            </dd>
          </div>
          <div>
            <dt class="text-body-xs text-navy-500 dark:text-navy-400">
              关联账户
            </dt>
            <dd class="mt-0.5 font-mono text-navy-900 dark:text-navy-50">
              {{ active.accountNumber || '--' }}
            </dd>
          </div>
          <div>
            <dt class="text-body-xs text-navy-500 dark:text-navy-400">
              分类
            </dt>
            <dd class="mt-0.5 text-navy-900 dark:text-navy-50">
              {{ active.categoryText || active.category || '--' }}
            </dd>
          </div>
          <div>
            <dt class="text-body-xs text-navy-500 dark:text-navy-400">
              处理人
            </dt>
            <dd class="mt-0.5 text-navy-900 dark:text-navy-50">
              {{ active.handlerName || '未分配' }}
            </dd>
          </div>
          <div>
            <dt class="text-body-xs text-navy-500 dark:text-navy-400">
              提交时间
            </dt>
            <dd class="mt-0.5 text-navy-900 dark:text-navy-50">
              {{ formatDateTime(active.creationDate) }}
            </dd>
          </div>
          <div>
            <dt class="text-body-xs text-navy-500 dark:text-navy-400">
              关闭时间
            </dt>
            <dd class="mt-0.5 text-navy-900 dark:text-navy-50">
              {{ active.closeDate ? formatDateTime(active.closeDate) : '--' }}
            </dd>
          </div>
        </dl>

        <div class="rounded-lg border border-navy-200 p-4 dark:border-navy-700">
          <div class="flex items-center gap-2 text-body-sm font-medium text-navy-700 dark:text-navy-200">
            <Bot class="h-4 w-4 text-navy-400" />
            AI 预审
          </div>
          <p class="mt-2 text-body-sm text-navy-600 dark:text-navy-300">
            {{ active.aiEval || '暂无评估' }}
          </p>
          <p class="mt-1 text-body-xs text-navy-500 dark:text-navy-400">
            情感倾向 {{ active.sentiment || '未分析' }} · 评分 {{ active.aiScore ?? 0 }}/100
          </p>
        </div>

        <div>
          <label
            class="label"
            for="ticket-reply"
          >客服回复</label>
          <textarea
            id="ticket-reply"
            v-model="replyText"
            rows="4"
            class="input resize-y"
            :disabled="!canReply(active.status)"
            placeholder="填写处理结果，提交后工单状态将变为「已回复」"
          />
          <p
            v-if="!canReply(active.status)"
            class="form-hint"
          >
            工单已关闭，无法再回复。
          </p>
        </div>

        <Alert
          v-if="confirmingClose"
          variant="warning"
          title="确认关闭该工单？"
          description="关闭后状态将变为「已关闭」，并且不能再回复。"
        />
      </div>

      <template #footer>
        <template v-if="active">
          <Button
            v-if="confirmingClose"
            variant="secondary"
            @click="confirmingClose = false"
          >
            取消
          </Button>
          <Button
            v-else
            variant="secondary"
            @click="detailOpen = false"
          >
            关闭窗口
          </Button>

          <Button
            v-if="canAccept(active.status) && !confirmingClose"
            variant="secondary"
            :loading="acting"
            @click="onAccept(active)"
          >
            <UserCheck class="h-4 w-4" />
            认领工单
          </Button>

          <Button
            v-if="canClose(active.status) && !confirmingClose"
            variant="danger"
            :disabled="acting"
            @click="confirmingClose = true"
          >
            关闭工单
          </Button>
          <Button
            v-if="canClose(active.status) && confirmingClose"
            variant="danger"
            :loading="acting"
            @click="onClose"
          >
            确认关闭
          </Button>

          <Button
            v-if="canReply(active.status)"
            :loading="acting"
            @click="onReply"
          >
            <MessageSquare class="h-4 w-4" />
            提交回复
          </Button>
        </template>
      </template>
    </Modal>
  </div>
</template>

/**
 * Customer-support ticket mappings.
 *
 * Java evidence:
 *   CustomerSupportServiceImpl:38-41
 *     STATUS_PENDING = 0, STATUS_PROCESSING = 1, STATUS_REPLIED = 2, STATUS_CLOSED = 3
 *   CustomerSupportServiceImpl:253-262  statusText() — same four labels
 *   CustomerSupportServiceImpl:43       CATEGORIES = 业务办理 / 咨询 / 投诉 / 其他
 *   acceptTicket:167  only when status == PENDING
 *   replyTicket:180   rejected when status == CLOSED
 *   closeTicket:196   rejected when status == CLOSED (already closed)
 *   TicketAnalysisVO.sentiment          积极 / 消极 / 中性 / 恶意, aiValid 0|1, aiScore 0-100
 */

export const TICKET_STATUS = {
  PENDING: 0,
  PROCESSING: 1,
  REPLIED: 2,
  CLOSED: 3,
} as const

export const TICKET_CATEGORIES = ['业务办理', '咨询', '投诉', '其他'] as const

export const TICKET_STATUSES = [
  { value: TICKET_STATUS.PENDING, label: '待处理' },
  { value: TICKET_STATUS.PROCESSING, label: '处理中' },
  { value: TICKET_STATUS.REPLIED, label: '已回复' },
  { value: TICKET_STATUS.CLOSED, label: '已关闭' },
] as const

export type BadgeTone = 'success' | 'warning' | 'danger' | 'info' | 'neutral'

export function ticketStatusLabel(status?: number | null): string {
  return TICKET_STATUSES.find((item) => item.value === status)?.label ?? '--'
}

export function ticketStatusTone(status?: number | null): BadgeTone {
  switch (status) {
    case TICKET_STATUS.PENDING:
      return 'warning'
    case TICKET_STATUS.PROCESSING:
      return 'info'
    case TICKET_STATUS.REPLIED:
      return 'success'
    case TICKET_STATUS.CLOSED:
      return 'neutral'
    default:
      return 'neutral'
  }
}

/** 积极 -> success, 中性 -> neutral, 消极 -> warning, 恶意 -> danger */
export function sentimentTone(sentiment?: string | null): BadgeTone {
  switch (sentiment) {
    case '积极':
      return 'success'
    case '消极':
      return 'warning'
    case '恶意':
      return 'danger'
    default:
      return 'neutral'
  }
}

export function aiValidLabel(valid?: number | null): string {
  return valid === 1 ? '有效' : '待定'
}

export function aiValidTone(valid?: number | null): BadgeTone {
  return valid === 1 ? 'success' : 'neutral'
}

/** 认领 is only legal while the ticket is still 待处理. */
export function canAccept(status?: number | null): boolean {
  return status === TICKET_STATUS.PENDING
}

/** 回复 and 关闭 are rejected once a ticket is 已关闭. */
export function canReply(status?: number | null): boolean {
  return status !== TICKET_STATUS.CLOSED
}

export function canClose(status?: number | null): boolean {
  return status !== TICKET_STATUS.CLOSED
}

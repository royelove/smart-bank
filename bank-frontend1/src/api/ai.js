import request from '@/utils/request'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || ''

/**
 * 非流式问答。
 *
 * 注意：后端 `AiServiceImpl.chat()` 目前固定返回本地关键词回复
 * （`return mockReply(message)`），配置了 API Key 也不会调用大模型；
 * 真正走大模型的是 `/ai/chat/stream`。页面的默认通道因此是流式的。
 */
export function chat(message) {
    return request({
        url: '/ai/chat',
        method: 'post',
        data: { message }
    })
}

/**
 * 流式问答。
 *
 * 后端用 `SseEmitter`，事件序列为：
 *   event:message / data:<片段>   （可能多次）
 *   event:done    / data:         （结束）
 * 然后 complete。
 *
 * `EventSource` 只能发 GET 且不能自定义请求头，所以这里用 fetch +
 * ReadableStream 手工解析 SSE，顺便带上 Authorization。
 *
 * @returns {() => void} 调用即中断本次流式请求
 */
export function chatStream(message, { onChunk, onDone, onError } = {}) {
    const controller = new AbortController()

    const parseEvent = (raw) => {
        let name = ''
        const data = []
        for (const line of raw.split(/\r?\n/)) {
            if (line.startsWith('event:')) name = line.slice(6).trim()
            else if (line.startsWith('data:')) data.push(line.slice(5).replace(/^ /, ''))
        }
        if (!name && data.length === 0) return null
        return { name: name || 'message', data: data.join('\n') }
    }

    const run = async () => {
        try {
            const token = localStorage.getItem('token')
            const response = await fetch(`${apiBaseUrl}/ai/chat/stream`, {
                method: 'post',
                headers: {
                    'Content-Type': 'application/json',
                    Accept: 'text/event-stream',
                    ...(token ? { Authorization: `Bearer ${token}` } : {})
                },
                body: JSON.stringify({ message }),
                signal: controller.signal
            })

            if (!response.ok || !response.body) {
                throw new Error(`AI 服务响应异常（HTTP ${response.status}）`)
            }

            const reader = response.body.getReader()
            const decoder = new TextDecoder('utf-8')
            let buffer = ''

            for (;;) {
                const { value, done } = await reader.read()
                if (done) break
                buffer += decoder.decode(value, { stream: true })

                let separator = buffer.indexOf('\n\n')
                while (separator !== -1) {
                    const event = parseEvent(buffer.slice(0, separator))
                    buffer = buffer.slice(separator + 2)
                    if (event) {
                        if (event.name === 'done') {
                            onDone && onDone()
                            return
                        }
                        if (event.name === 'message' && event.data) {
                            onChunk && onChunk(event.data)
                        }
                    }
                    separator = buffer.indexOf('\n\n')
                }
            }
            onDone && onDone()
        } catch (error) {
            if (error && error.name === 'AbortError') return
            onError && onError(error)
        }
    }

    run()

    return () => controller.abort()
}

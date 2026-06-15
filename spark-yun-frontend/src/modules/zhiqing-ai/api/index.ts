import { http } from '@/app/utils/http'
import { useAuthStore } from '@/app/store/useAuth'

interface StreamChatHandlers {
    onContent: (content: string) => void
    onDone?: () => void
}

export function ChatWithAi(params: any): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/workspace/ai/chat',
        params
    })
}

export function ListWorkspaceAiConfig(): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/workspace/ai/configs'
    })
}

export async function StreamChatWithAi(
    params: any,
    handlers: StreamChatHandlers,
    signal?: AbortSignal
): Promise<void> {
    const authStore = useAuthStore()
    const urlPrefix = import.meta.env.VITE_VUE_APP_BASE_DOMAIN || ''
    const response = await fetch(`${urlPrefix}/api/workspace/ai/chat/stream`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            Authorization: authStore.token || '',
            tenant: authStore.tenantId || ''
        },
        body: JSON.stringify(params),
        signal
    })

    const contentType = response.headers.get('content-type') || ''
    if (contentType.includes('application/json')) {
        const result = await response.json()
        throw new Error(result.msg || 'AI对话失败')
    }
    if (!response.ok || !response.body) {
        throw new Error('AI对话失败')
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
        const { done, value } = await reader.read()
        if (done) {
            break
        }
        buffer += decoder.decode(value, { stream: true })
        buffer = resolveSseBuffer(buffer, handlers)
    }
    resolveSseBuffer(buffer + '\n\n', handlers)
}

function resolveSseBuffer(buffer: string, handlers: StreamChatHandlers): string {
    const blocks = buffer.split(/\n\n/)
    const rest = blocks.pop() || ''
    blocks.forEach((block) => {
        if (!block.trim()) {
            return
        }
        const event = resolveEventName(block)
        const data = resolveEventData(block)
        if (!data) {
            return
        }
        const payload = JSON.parse(data)
        if (event === 'error') {
            throw new Error(payload.message || 'AI对话失败')
        }
        if (event === 'done') {
            handlers.onDone?.()
            return
        }
        handlers.onContent(payload.content || '')
    })
    return rest
}

function resolveEventName(block: string): string {
    const eventLine = block.split(/\n/).find((line) => line.startsWith('event:'))
    return eventLine ? eventLine.replace(/^event:\s*/, '').trim() : 'message'
}

function resolveEventData(block: string): string {
    return block
        .split(/\n/)
        .filter((line) => line.startsWith('data:'))
        .map((line) => line.replace(/^data:\s*/, ''))
        .join('\n')
}

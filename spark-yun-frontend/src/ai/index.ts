import { Chat } from '@ai-sdk/vue'
import { DefaultChatTransport, type ChatInit, type HttpChatTransportInitOptions, type UIMessage } from 'ai'

import { useAuthStore } from '@/store/useAuth'

type ChatTransportOptions<UI_MESSAGE extends UIMessage> = HttpChatTransportInitOptions<UI_MESSAGE>

export interface CreateAiChatOptions<UI_MESSAGE extends UIMessage = UIMessage> {
    chat?: Omit<ChatInit<UI_MESSAGE>, 'transport'>
    transport?: ChatTransportOptions<UI_MESSAGE>
}

async function resolveHeaders(headers: ChatTransportOptions<UIMessage>['headers']): Promise<Headers> {
    const resolvedHeaders = typeof headers === 'function' ? await headers() : await headers

    return new Headers(resolvedHeaders)
}

export function createAiChat<UI_MESSAGE extends UIMessage = UIMessage>(
    options: CreateAiChatOptions<UI_MESSAGE> = {}
): Chat<UI_MESSAGE> {
    const { chat, transport = {} } = options

    return new Chat<UI_MESSAGE>({
        ...chat,
        transport: new DefaultChatTransport<UI_MESSAGE>({
            ...transport,
            api: transport.api ?? '/api/ai/chat',
            credentials: transport.credentials ?? 'same-origin',
            headers: async () => {
                const headers = await resolveHeaders(transport.headers)
                const authStore = useAuthStore()

                if (authStore.token && !headers.has('authorization')) {
                    headers.set('authorization', authStore.token)
                }
                if (authStore.tenantId && !headers.has('tenant')) {
                    headers.set('tenant', authStore.tenantId)
                }

                return headers
            }
        })
    })
}

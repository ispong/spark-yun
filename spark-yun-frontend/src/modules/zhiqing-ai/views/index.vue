<template>
    <div class="zhiqing-ai">
        <section class="zhiqing-ai__chat">
            <div ref="messagePanelRef" class="zhiqing-ai__messages">
                <div v-if="!messages.length" class="zhiqing-ai__welcome">
                    <strong>你好，我是至轻智能</strong>
                    <span>让AI更懂数据，从快速开始对话做起。</span>
                </div>
                <div
                    v-for="(message, index) in messages"
                    :key="`${message.role}-${index}`"
                    class="zhiqing-ai-message"
                    :class="[`is-${message.role}`, { 'is-loading': isAssistantLoading(message, index) }]"
                >
                    <div v-if="isAssistantLoading(message, index)" class="zhiqing-ai-message__content">
                        <span class="zhiqing-ai-message__loading">思考中</span>
                    </div>
                    <div v-else class="zhiqing-ai-message__content" v-html="renderMarkdown(message.content)" />
                </div>
            </div>

            <div class="zhiqing-ai__composer">
                <el-select
                    v-model="currentConfigId"
                    class="zhiqing-ai__select"
                    filterable
                    :disabled="sending"
                    placeholder="选择智能体"
                    @change="handleConfigChange"
                >
                    <el-option
                        v-for="config in aiConfigs"
                        :key="config.id"
                        :label="`${config.name} / ${config.modelName}`"
                        :value="config.id"
                    />
                </el-select>
                <el-input
                    v-model="inputText"
                    :disabled="sending"
                    placeholder="输入问题，按 Enter 发送"
                    @compositionend="handleCompositionEnd"
                    @compositionstart="isComposing = true"
                    @keydown.enter="handleInputEnter"
                />
                <el-button v-if="sending" type="danger" @click="stopGenerating">中止</el-button>
                <el-button v-else type="primary" :disabled="!canSend" @click="sendMessage">发送</el-button>
                <el-button :disabled="sending" type="primary" plain @click="createNewChat">新对话</el-button>
            </div>
        </section>

        <aside class="zhiqing-ai__history">
            <div class="zhiqing-ai__history-list">
                <div
                    v-for="history in chatHistories"
                    :key="history.id"
                    class="zhiqing-ai-history-item"
                    :class="{ 'is-active': history.id === currentSessionId }"
                    @click="switchHistory(history.id)"
                >
                    <div class="zhiqing-ai-history-item__main">
                        <span>{{ history.title }}</span>
                        <small>{{ history.updatedAt }}</small>
                    </div>
                    <el-button
                        :disabled="sending"
                        :icon="Delete"
                        circle
                        text
                        @click.stop="removeHistory(history.id)"
                    />
                </div>
            </div>
        </aside>
    </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
    DeleteChatSession,
    ListChatSessions,
    ListWorkspaceAiConfig,
    SaveChatSession,
    StreamChatWithAi
} from '@/modules/zhiqing-ai/api'

interface AiConfig {
    id: string
    name: string
    modelName: string
}

interface ChatMessage {
    role: 'user' | 'assistant'
    content: string
}

interface ChatHistory {
    id: string
    configId: string
    title: string
    updatedAt: string
    messages: ChatMessage[]
}

const aiConfigs = ref<AiConfig[]>([])
const currentConfigId = ref('')
const inputText = ref('')
const sending = ref(false)
const isComposing = ref(false)
const loadingConfigs = ref(false)
const messages = ref<ChatMessage[]>([])
const chatHistories = ref<ChatHistory[]>([])
const currentSessionId = ref('')
const messagePanelRef = ref<HTMLElement>()
const abortController = ref<AbortController>()
const canSend = computed(() => !!currentConfigId.value && !!inputText.value.trim() && !sending.value)
let saveHistoryTimer: ReturnType<typeof window.setTimeout> | undefined

function handleCompositionEnd() {
    nextTick(() => {
        isComposing.value = false
    })
}

function handleInputEnter(event: KeyboardEvent) {
    if (event.isComposing || isComposing.value || event.keyCode === 229) {
        return
    }
    event.preventDefault()
    sendMessage()
}

function isAssistantLoading(message: ChatMessage, index: number): boolean {
    return sending.value && message.role === 'assistant' && !message.content && index === messages.value.length - 1
}

function renderMarkdown(markdown: string): string {
    const lines = markdown.replace(/\r\n/g, '\n').split('\n')
    const html: string[] = []
    let index = 0

    while (index < lines.length) {
        const line = lines[index]
        if (!line.trim()) {
            index += 1
            continue
        }

        const fenceMatch = line.match(/^```(\w+)?\s*$/)
        if (fenceMatch) {
            const codeLines: string[] = []
            index += 1
            while (index < lines.length && !lines[index].match(/^```\s*$/)) {
                codeLines.push(lines[index])
                index += 1
            }
            if (index < lines.length) {
                index += 1
            }
            html.push(
                `<pre><code${fenceMatch[1] ? ` class="language-${escapeHtml(fenceMatch[1])}"` : ''}>${escapeHtml(
                    codeLines.join('\n')
                )}</code></pre>`
            )
            continue
        }

        if (isTableStart(lines, index)) {
            const headers = splitTableLine(lines[index])
            const rows: string[][] = []
            index += 2
            while (index < lines.length && lines[index].trim().startsWith('|')) {
                rows.push(splitTableLine(lines[index]))
                index += 1
            }
            html.push(renderTable(headers, rows))
            continue
        }

        const headingMatch = line.match(/^(#{1,6})\s+(.+)$/)
        if (headingMatch) {
            const level = headingMatch[1].length
            html.push(`<h${level}>${renderInlineMarkdown(headingMatch[2])}</h${level}>`)
            index += 1
            continue
        }

        if (/^>\s?/.test(line)) {
            const quoteLines: string[] = []
            while (index < lines.length && /^>\s?/.test(lines[index])) {
                quoteLines.push(lines[index].replace(/^>\s?/, ''))
                index += 1
            }
            html.push(`<blockquote>${renderMarkdown(quoteLines.join('\n'))}</blockquote>`)
            continue
        }

        if (/^\s*[-*+]\s+/.test(line)) {
            const items: string[] = []
            while (index < lines.length && /^\s*[-*+]\s+/.test(lines[index])) {
                items.push(lines[index].replace(/^\s*[-*+]\s+/, ''))
                index += 1
            }
            html.push(`<ul>${items.map((item) => `<li>${renderInlineMarkdown(item)}</li>`).join('')}</ul>`)
            continue
        }

        if (/^\s*\d+\.\s+/.test(line)) {
            const items: string[] = []
            while (index < lines.length && /^\s*\d+\.\s+/.test(lines[index])) {
                items.push(lines[index].replace(/^\s*\d+\.\s+/, ''))
                index += 1
            }
            html.push(`<ol>${items.map((item) => `<li>${renderInlineMarkdown(item)}</li>`).join('')}</ol>`)
            continue
        }

        const paragraphLines: string[] = []
        while (
            index < lines.length &&
            lines[index].trim() &&
            !lines[index].match(/^```(\w+)?\s*$/) &&
            !lines[index].match(/^(#{1,6})\s+/) &&
            !/^>\s?/.test(lines[index]) &&
            !/^\s*[-*+]\s+/.test(lines[index]) &&
            !/^\s*\d+\.\s+/.test(lines[index]) &&
            !isTableStart(lines, index)
        ) {
            paragraphLines.push(lines[index])
            index += 1
        }
        html.push(`<p>${renderInlineMarkdown(paragraphLines.join('\n'))}</p>`)
    }

    return `<div class="zhiqing-ai-markdown">${html.join('')}</div>`
}

function renderInlineMarkdown(text: string): string {
    const codeValues: string[] = []
    let html = escapeHtml(text).replace(/`([^`]+)`/g, (_match, code) => {
        codeValues.push(`<code>${code}</code>`)
        return `@@CODE_${codeValues.length - 1}@@`
    })

    html = html
        .replace(/!\[([^\]]*)\]\((https?:\/\/[^)\s]+)\)/g, '<img alt="$1" src="$2" />')
        .replace(/\[([^\]]+)\]\((https?:\/\/[^)\s]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')
        .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
        .replace(/~~([^~]+)~~/g, '<del>$1</del>')
        .replace(/(^|[^*])\*([^*]+)\*/g, '$1<em>$2</em>')
        .replace(/\n/g, '<br />')

    codeValues.forEach((code, codeIndex) => {
        html = html.replace(`@@CODE_${codeIndex}@@`, code)
    })

    return html
}

function escapeHtml(text: string): string {
    return text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;')
}

function isTableStart(lines: string[], index: number): boolean {
    return (
        index + 1 < lines.length &&
        lines[index].trim().startsWith('|') &&
        /^\s*\|?[\s:-]+\|[\s|:-]*$/.test(lines[index + 1])
    )
}

function splitTableLine(line: string): string[] {
    return line
        .trim()
        .replace(/^\|/, '')
        .replace(/\|$/, '')
        .split('|')
        .map((cell) => cell.trim())
}

function renderTable(headers: string[], rows: string[][]): string {
    return `<table><thead><tr>${headers
        .map((header) => `<th>${renderInlineMarkdown(header)}</th>`)
        .join('')}</tr></thead><tbody>${rows
        .map((row) => `<tr>${headers.map((_header, cellIndex) => `<td>${renderInlineMarkdown(row[cellIndex] || '')}</td>`).join('')}</tr>`)
        .join('')}</tbody></table>`
}

function loadAiConfigs() {
    loadingConfigs.value = true
    ListWorkspaceAiConfig()
        .then((res: any) => {
            aiConfigs.value = res.data || []
            if (!currentConfigId.value && aiConfigs.value.length) {
                currentConfigId.value = aiConfigs.value[0].id
            }
            if (currentConfigId.value && !aiConfigs.value.some((config) => config.id === currentConfigId.value)) {
                currentConfigId.value = aiConfigs.value[0]?.id || ''
                clearMessages()
            }
        })
        .finally(() => {
            loadingConfigs.value = false
        })
}

function clearMessages() {
    stopGenerating()
    currentSessionId.value = ''
    messages.value = []
}

function loadChatHistories() {
    ListChatSessions().then((res: any) => {
        chatHistories.value = (res.data || []).map((history: any) => ({
            id: history.id,
            configId: history.configId || '',
            title: history.title || '新对话',
            updatedAt: resolveHistoryDisplayTime(history.updatedAt),
            messages: (history.messages || []).map((message: ChatMessage) => ({ ...message }))
        }))
    })
}

function handleConfigChange() {
    clearMessages()
}

function createNewChat() {
    stopGenerating()
    currentSessionId.value = ''
    messages.value = []
    inputText.value = ''
}

function switchHistory(id: string) {
    if (sending.value || id === currentSessionId.value) {
        return
    }
    const history = chatHistories.value.find((item) => item.id === id)
    if (!history) {
        return
    }
    currentSessionId.value = history.id
    if (history.configId) {
        currentConfigId.value = history.configId
    }
    messages.value = history.messages.map((message) => ({ ...message }))
    scrollToBottom()
}

function removeHistory(id: string) {
    chatHistories.value = chatHistories.value.filter((history) => history.id !== id)
    DeleteChatSession({ id }).catch(() => {
        loadChatHistories()
    })
    if (currentSessionId.value === id) {
        currentSessionId.value = ''
        messages.value = []
    }
}

function ensureCurrentHistory(firstMessage: string) {
    if (currentSessionId.value) {
        return
    }
    const now = new Date()
    currentSessionId.value = `${now.getTime()}`
    chatHistories.value.unshift({
        id: currentSessionId.value,
        configId: currentConfigId.value,
        title: resolveHistoryTitle(firstMessage),
        updatedAt: formatHistoryTime(now),
        messages: []
    })
}

function updateCurrentHistory() {
    const history = chatHistories.value.find((item) => item.id === currentSessionId.value)
    if (!history) {
        return
    }
    history.configId = currentConfigId.value
    history.messages = messages.value.map((message) => ({ ...message }))
    history.updatedAt = formatHistoryTime(new Date())
    scheduleSaveCurrentHistory()
}

function resolveHistoryTitle(content: string): string {
    return content.length > 18 ? `${content.slice(0, 18)}...` : content
}

function formatHistoryTime(date: Date): string {
    const month = `${date.getMonth() + 1}`.padStart(2, '0')
    const day = `${date.getDate()}`.padStart(2, '0')
    const hour = `${date.getHours()}`.padStart(2, '0')
    const minute = `${date.getMinutes()}`.padStart(2, '0')
    return `${month}-${day} ${hour}:${minute}`
}

function resolveHistoryDisplayTime(updatedAt?: string): string {
    if (!updatedAt) {
        return ''
    }
    const match = updatedAt.match(/^(\d{4})-(\d{2})-(\d{2})\s+(\d{2}):(\d{2})/)
    return match ? `${match[2]}-${match[3]} ${match[4]}:${match[5]}` : updatedAt
}

function scheduleSaveCurrentHistory() {
    if (saveHistoryTimer) {
        window.clearTimeout(saveHistoryTimer)
    }
    saveHistoryTimer = window.setTimeout(() => {
        persistCurrentHistory().catch(() => {
            ElMessage.error('聊天历史保存失败')
        })
    }, 500)
}

async function persistCurrentHistory() {
    if (saveHistoryTimer) {
        window.clearTimeout(saveHistoryTimer)
        saveHistoryTimer = undefined
    }
    const history = chatHistories.value.find((item) => item.id === currentSessionId.value)
    if (!history || !history.configId || !history.messages.length) {
        return
    }
    const res = await SaveChatSession({
        id: history.id,
        configId: history.configId,
        title: history.title,
        messages: history.messages
    })
    if (res?.data?.id && res.data.id !== history.id) {
        if (currentSessionId.value === history.id) {
            currentSessionId.value = res.data.id
        }
        history.id = res.data.id
    }
    if (res?.data?.updatedAt) {
        history.updatedAt = resolveHistoryDisplayTime(res.data.updatedAt)
    }
}

function scrollToBottom() {
    nextTick(() => {
        if (messagePanelRef.value) {
            messagePanelRef.value.scrollTop = messagePanelRef.value.scrollHeight
        }
    })
}

function stopGenerating() {
    if (abortController.value) {
        abortController.value.abort()
        abortController.value = undefined
    }
    sending.value = false
}

async function sendMessage() {
    if (!canSend.value) {
        return
    }
    const userMessage: ChatMessage = {
        role: 'user',
        content: inputText.value.trim()
    }
    const requestMessages = [...messages.value, userMessage]
    const assistantMessage: ChatMessage = {
        role: 'assistant',
        content: ''
    }
    ensureCurrentHistory(userMessage.content)
    messages.value.push(userMessage)
    messages.value.push(assistantMessage)
    updateCurrentHistory()
    inputText.value = ''
    sending.value = true
    abortController.value = new AbortController()
    scrollToBottom()
    let pendingAssistantContent = ''
    let typingTimer: ReturnType<typeof window.setInterval> | undefined

    const stopTyping = () => {
        if (typingTimer) {
            window.clearInterval(typingTimer)
            typingTimer = undefined
        }
    }
    const flushTypingContent = () => {
        if (!pendingAssistantContent) {
            stopTyping()
            return
        }
        const chunkSize = Math.min(pendingAssistantContent.length, Math.max(1, Math.ceil(pendingAssistantContent.length / 12)))
        assistantMessage.content += pendingAssistantContent.slice(0, chunkSize)
        pendingAssistantContent = pendingAssistantContent.slice(chunkSize)
        updateCurrentHistory()
        scrollToBottom()
    }
    const startTyping = () => {
        if (!typingTimer) {
            typingTimer = window.setInterval(flushTypingContent, 24)
        }
    }
    const drainTypingContent = () => {
        return new Promise<void>((resolve) => {
            const drain = () => {
                if (!pendingAssistantContent) {
                    stopTyping()
                    resolve()
                    return
                }
                flushTypingContent()
                window.requestAnimationFrame(drain)
            }
            drain()
        })
    }

    try {
        await StreamChatWithAi(
            {
                configId: currentConfigId.value,
                messages: requestMessages
            },
            {
                onContent(content: string) {
                    pendingAssistantContent += content
                    startTyping()
                }
            },
            abortController.value.signal
        )
        await drainTypingContent()
    } catch (error: any) {
        stopTyping()
        pendingAssistantContent = ''
        if (error?.name === 'AbortError') {
            if (!assistantMessage.content) {
                messages.value.pop()
            }
            ElMessage.info('已停止生成')
        } else {
            messages.value.pop()
            ElMessage.error(error?.message || 'AI对话失败')
        }
    } finally {
        stopTyping()
        abortController.value = undefined
        sending.value = false
        if (!assistantMessage.content && messages.value[messages.value.length - 1] === assistantMessage) {
            messages.value.pop()
        }
        updateCurrentHistory()
        await persistCurrentHistory()
        if (assistantMessage.content) {
            scrollToBottom()
        }
    }
}

onMounted(() => {
    loadAiConfigs()
    loadChatHistories()
})
</script>

<style scoped lang="scss">
.zhiqing-ai {
    height: calc(100vh - 32px);
    display: grid;
    grid-template-columns: minmax(0, 1fr) 300px;
    gap: 16px;
    padding: 16px 20px;
    background-color: #ffffff;
}

.zhiqing-ai__chat {
    min-width: 0;
    min-height: 0;
    display: grid;
    grid-template-rows: minmax(0, 1fr) auto;
}

.zhiqing-ai__composer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
}

.zhiqing-ai__select {
    width: 220px;
    flex: 0 0 220px;
}

.zhiqing-ai__messages {
    min-height: 0;
    display: flex;
    flex-direction: column;
    gap: 14px;
    padding: 20px 44px;
    overflow: auto;
}

.zhiqing-ai__history {
    min-width: 0;
    min-height: 0;
    border-left: 1px solid var(--el-border-color-lighter);
    padding-left: 16px;
}

.zhiqing-ai__history-list {
    height: 100%;
    overflow: auto;
}

.zhiqing-ai-history-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px;
    border-radius: 8px;
    cursor: pointer;

    &:hover,
    &.is-active {
        background-color: var(--el-fill-color-light);
    }

    &.is-active {
        .zhiqing-ai-history-item__main span {
            color: var(--el-color-primary);
        }
    }
}

.zhiqing-ai-history-item__main {
    min-width: 0;
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 4px;

    span,
    small {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    span {
        color: var(--el-text-color-primary);
        font-size: 14px;
    }

    small {
        color: var(--el-text-color-secondary);
        font-size: 12px;
    }
}

.zhiqing-ai__welcome {
    margin: auto;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    color: var(--el-text-color-secondary);

    strong {
        color: var(--el-text-color-primary);
        font-size: 20px;
    }
}

.zhiqing-ai-message {
    display: flex;
    max-width: 860px;

    &.is-loading {
        .zhiqing-ai-message__content {
            color: var(--el-text-color-secondary);
        }
    }

    &.is-user {
        align-self: flex-end;

        .zhiqing-ai-message__content {
            background-color: var(--el-color-primary-light-9);
        }
    }
}

.zhiqing-ai-message__content {
    min-height: 40px;
    box-sizing: border-box;
    padding: 8px 12px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    color: var(--el-text-color-primary);
    line-height: 20px;
    word-break: break-word;
    background-color: #ffffff;
}

.zhiqing-ai-message__loading {
    display: inline-flex;
    align-items: center;

    &::after {
        width: 18px;
        content: '';
        animation: zhiqing-ai-loading-dots 1.2s steps(4, end) infinite;
    }
}

.zhiqing-ai-message__content :deep(.zhiqing-ai-markdown) {
    width: 100%;

    > :first-child {
        margin-top: 0;
    }

    > :last-child {
        margin-bottom: 0;
    }

    p {
        margin: 8px 0;
    }

    h1,
    h2,
    h3,
    h4,
    h5,
    h6 {
        margin: 12px 0 8px;
        color: var(--el-text-color-primary);
        font-weight: 600;
        line-height: 1.35;
    }

    h1 {
        font-size: 22px;
    }

    h2 {
        font-size: 20px;
    }

    h3 {
        font-size: 18px;
    }

    h4,
    h5,
    h6 {
        font-size: 16px;
    }

    ul,
    ol {
        margin: 8px 0;
        padding-left: 22px;
    }

    li {
        margin: 4px 0;
    }

    blockquote {
        margin: 8px 0;
        padding: 6px 12px;
        border-left: 3px solid var(--el-border-color);
        color: var(--el-text-color-secondary);
        background-color: var(--el-fill-color-light);
    }

    pre {
        margin: 10px 0;
        padding: 12px;
        overflow: auto;
        border-radius: 6px;
        background-color: #f6f8fa;
    }

    code {
        padding: 2px 5px;
        border-radius: 4px;
        font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
        font-size: 13px;
        background-color: #f6f8fa;
    }

    pre code {
        padding: 0;
        background-color: transparent;
        white-space: pre;
    }

    table {
        width: 100%;
        margin: 10px 0;
        border-collapse: collapse;
        font-size: 14px;
    }

    th,
    td {
        padding: 7px 9px;
        border: 1px solid var(--el-border-color-lighter);
        text-align: left;
    }

    th {
        background-color: var(--el-fill-color-light);
        font-weight: 600;
    }

    a {
        color: var(--el-color-primary);
        text-decoration: none;
    }

    a:hover {
        text-decoration: underline;
    }

    img {
        max-width: 100%;
        border-radius: 6px;
    }
}

@keyframes zhiqing-ai-loading-dots {
    0% {
        content: '';
    }

    25% {
        content: '.';
    }

    50% {
        content: '..';
    }

    75%,
    100% {
        content: '...';
    }
}

.zhiqing-ai__composer {
    box-sizing: border-box;
    padding: 12px 44px 0;

    .el-textarea {
        flex: 1;
    }

    .el-input {
        flex: 1;
    }

    .el-button {
        height: 32px;
        width: 88px;
        align-self: center;
        margin-left: 0;
    }

    :deep(.el-select__wrapper),
    :deep(.el-input__wrapper) {
        min-height: 32px;
        height: 32px;
    }
}

@media (max-width: 720px) {
    .zhiqing-ai {
        height: calc(100vh - 16px);
        grid-template-columns: minmax(0, 1fr);
        grid-template-rows: minmax(0, 1fr) 220px;
        padding: 12px;
    }

    .zhiqing-ai__composer {
        align-items: stretch;
        flex-direction: column;
        padding-right: 0;
        padding-left: 0;
    }

    .zhiqing-ai__select {
        width: 100%;
        flex-basis: auto;
    }

    .zhiqing-ai__history {
        border-left: 0;
        border-top: 1px solid var(--el-border-color-lighter);
        padding-left: 0;
    }
}
</style>

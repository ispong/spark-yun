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
                    :class="[
                        `is-${message.role}`,
                        {
                            'is-loading': isAssistantLoading(message, index),
                            'is-typing': isAssistantTyping(message, index)
                        }
                    ]"
                >
                    <div v-if="isAssistantLoading(message, index)" class="zhiqing-ai-message__content">
                        <span class="zhiqing-ai-message__loading">思考中</span>
                    </div>
                    <div v-else-if="isAssistantTyping(message, index)" class="zhiqing-ai-message__content">
                        <div class="zhiqing-ai-markdown" v-html="typingStableMarkdownHtml" />
                        <p v-if="typingTailContent" class="zhiqing-ai-markdown__tail">{{ typingTailContent }}</p>
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
                <div class="zhiqing-ai-composer-input">
                    <div v-if="selectedPrompts.length || selectedFiles.length" class="zhiqing-ai-composer-tags">
                        <span v-for="prompt in selectedPrompts" :key="prompt.id" class="zhiqing-ai-chip">
                            {{ prompt.name }}
                            <el-icon @click="removeSelectedPrompt(prompt.id)"><Close /></el-icon>
                        </span>
                        <span v-for="file in selectedFiles" :key="file.id" class="zhiqing-ai-chip is-file">
                            {{ file.name }}
                            <el-icon @click="removeSelectedFile(file.id)"><Close /></el-icon>
                        </span>
                    </div>
                    <el-input
                        v-model="inputText"
                        :disabled="sending"
                        placeholder="输入问题，按 Enter 发送"
                        @compositionend="handleCompositionEnd"
                        @compositionstart="isComposing = true"
                        @keydown.enter="handleInputEnter"
                    >
                        <template #prefix>
                            <el-dropdown trigger="click" :disabled="sending || uploadingFile" @command="handleComposerAction">
                                <el-button class="zhiqing-ai-plus" :icon="Plus" circle text />
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item command="upload-file" :icon="Upload">上传文件</el-dropdown-item>
                                        <el-dropdown-item command="select-prompt" :icon="Search">提示词</el-dropdown-item>
                                        <el-dropdown-item command="save-prompt" :icon="Edit">存为提示词</el-dropdown-item>
                                        <el-dropdown-item command="mcp-share" :icon="Share">Mcp一键分享</el-dropdown-item>
                                    </el-dropdown-menu>
                                </template>
                            </el-dropdown>
                        </template>
                    </el-input>
                    <input ref="fileInputRef" type="file" multiple hidden @change="handleFileChange" />
                </div>
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

        <el-dialog v-model="promptDialogVisible" title="选择提示词" width="760px">
            <div class="zhiqing-ai-prompt-dialog">
                <div class="zhiqing-ai-prompt-dialog__toolbar">
                    <el-input v-model="promptSearchText" :prefix-icon="Search" clearable placeholder="搜索提示词" />
                    <el-button type="primary" @click="openCreatePrompt">新建</el-button>
                </div>
                <div class="zhiqing-ai-prompt-list">
                    <div
                        v-for="prompt in filteredPrompts"
                        :key="prompt.id"
                        class="zhiqing-ai-prompt-card"
                        :class="{ 'is-selected': selectedPrompts.some((item) => item.id === prompt.id) }"
                        @click="togglePrompt(prompt)"
                    >
                        <div class="zhiqing-ai-prompt-card__header">
                            <strong>{{ prompt.name }}</strong>
                            <div class="zhiqing-ai-prompt-card__actions">
                                <el-button :icon="Edit" circle text @click.stop="openEditPrompt(prompt)" />
                                <el-button :icon="Delete" circle text @click.stop="removePrompt(prompt)" />
                            </div>
                        </div>
                        <p>{{ prompt.content }}</p>
                    </div>
                    <el-empty v-if="!filteredPrompts.length" description="暂无提示词" />
                </div>
            </div>
        </el-dialog>

        <el-dialog v-model="promptEditDialogVisible" :title="promptForm.id ? '编辑提示词' : '新建提示词'" width="560px">
            <el-form class="zhiqing-ai-prompt-form" label-position="top">
                <el-form-item label="名称">
                    <el-input v-model="promptForm.name" maxlength="50" show-word-limit />
                </el-form-item>
                <el-form-item label="内容">
                    <el-input v-model="promptForm.content" type="textarea" :rows="8" maxlength="5000" show-word-limit />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="promptEditDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="submitPromptForm">保存</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="savePromptDialogVisible" title="保存提示词" width="420px">
            <el-form label-position="top">
                <el-form-item label="名称">
                    <el-input v-model="savePromptName" maxlength="50" show-word-limit />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="savePromptDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="saveInputAsPrompt">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { Close, Delete, Edit, Plus, Search, Share, Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
    DeleteAiPrompt,
    DeleteChatSession,
    ListChatSessions,
    ListAiPrompts,
    ListWorkspaceAiConfig,
    ParseChatFile,
    SaveAiPrompt,
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

interface AiPrompt {
    id: string
    name: string
    content: string
    updatedAt?: string
}

interface ChatFile {
    id: string
    name: string
    contentType?: string
    size: number
    content: string
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
let saveHistoryTimer: ReturnType<typeof window.setTimeout> | undefined
let typingTimer: ReturnType<typeof window.setTimeout> | undefined
let pendingAssistantContent = ''
let typingStableContent = ''
const currentTypingMessageIndex = ref(-1)
const typingPlayback = ref(false)
const typingStableMarkdownHtml = ref('')
const typingTailContent = ref('')
const aiPrompts = ref<AiPrompt[]>([])
const selectedPrompts = ref<AiPrompt[]>([])
const selectedFiles = ref<ChatFile[]>([])
const promptDialogVisible = ref(false)
const promptSearchText = ref('')
const promptEditDialogVisible = ref(false)
const promptForm = ref({
    id: '',
    name: '',
    content: ''
})
const savePromptDialogVisible = ref(false)
const savePromptName = ref('')
const fileInputRef = ref<HTMLInputElement>()
const uploadingFile = ref(false)
const canSend = computed(
    () =>
        !!currentConfigId.value &&
        (inputText.value.trim() || selectedPrompts.value.length || selectedFiles.value.length) &&
        !sending.value &&
        !typingPlayback.value
)
const filteredPrompts = computed(() => {
    const keyword = promptSearchText.value.trim().toLowerCase()
    if (!keyword) {
        return aiPrompts.value
    }
    return aiPrompts.value.filter(
        (prompt) => prompt.name.toLowerCase().includes(keyword) || prompt.content.toLowerCase().includes(keyword)
    )
})

const TYPING_INTERVAL = 18
const FIRST_CONTENT_TIMEOUT = 30000
const CONTENT_IDLE_TIMEOUT = 60000
const MAX_CHAT_FILE_COUNT = 5
const MAX_CHAT_FILE_SIZE = 10 * 1024 * 1024

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

function isAssistantTyping(message: ChatMessage, index: number): boolean {
    return (
        (sending.value || typingPlayback.value) &&
        message.role === 'assistant' &&
        !!message.content &&
        index === messages.value.length - 1 &&
        currentTypingMessageIndex.value === index
    )
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

function loadPrompts() {
    ListAiPrompts().then((res: any) => {
        aiPrompts.value = res.data || []
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
    selectedPrompts.value = []
    selectedFiles.value = []
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

function handleComposerAction(command: string) {
    if (command === 'upload-file') {
        fileInputRef.value?.click()
        return
    }
    if (command === 'select-prompt') {
        promptDialogVisible.value = true
        loadPrompts()
        return
    }
    if (command === 'save-prompt') {
        if (!inputText.value.trim()) {
            ElMessage.warning('请输入要保存的提示词内容')
            return
        }
        savePromptName.value = resolveHistoryTitle(inputText.value.trim())
        savePromptDialogVisible.value = true
        return
    }
    if (command === 'mcp-share') {
        ElMessage.info('Mcp一键分享功能准备中')
    }
}

async function handleFileChange(event: Event) {
    const input = event.target as HTMLInputElement
    const files = Array.from(input.files || [])
    input.value = ''
    if (!files.length) {
        return
    }
    if (selectedFiles.value.length + files.length > MAX_CHAT_FILE_COUNT) {
        ElMessage.warning(`最多上传${MAX_CHAT_FILE_COUNT}个文件`)
        return
    }
    const oversized = files.find((file) => file.size > MAX_CHAT_FILE_SIZE)
    if (oversized) {
        ElMessage.warning(`${oversized.name} 超过10MB`)
        return
    }

    uploadingFile.value = true
    try {
        for (const file of files) {
            const res = await ParseChatFile(file)
            selectedFiles.value.push({
                id: `${Date.now()}-${Math.random()}`,
                name: res.data.name,
                contentType: res.data.contentType,
                size: res.data.size,
                content: res.data.content || ''
            })
        }
    } catch (error: any) {
        ElMessage.error(error?.message || '文件解析失败')
    } finally {
        uploadingFile.value = false
    }
}

function removeSelectedFile(id: string) {
    selectedFiles.value = selectedFiles.value.filter((file) => file.id !== id)
}

function togglePrompt(prompt: AiPrompt) {
    if (selectedPrompts.value.some((item) => item.id === prompt.id)) {
        selectedPrompts.value = selectedPrompts.value.filter((item) => item.id !== prompt.id)
        return
    }
    selectedPrompts.value.push(prompt)
}

function removeSelectedPrompt(id: string) {
    selectedPrompts.value = selectedPrompts.value.filter((prompt) => prompt.id !== id)
}

function openCreatePrompt() {
    promptForm.value = {
        id: '',
        name: '',
        content: ''
    }
    promptEditDialogVisible.value = true
}

function openEditPrompt(prompt: AiPrompt) {
    promptForm.value = {
        id: prompt.id,
        name: prompt.name,
        content: prompt.content
    }
    promptEditDialogVisible.value = true
}

async function submitPromptForm() {
    if (!promptForm.value.name.trim() || !promptForm.value.content.trim()) {
        ElMessage.warning('请输入提示词名称和内容')
        return
    }
    const res = await SaveAiPrompt({
        id: promptForm.value.id,
        name: promptForm.value.name.trim(),
        content: promptForm.value.content.trim()
    })
    const savedPrompt = res.data
    const index = aiPrompts.value.findIndex((prompt) => prompt.id === savedPrompt.id)
    if (index >= 0) {
        aiPrompts.value[index] = savedPrompt
    } else {
        aiPrompts.value.unshift(savedPrompt)
    }
    selectedPrompts.value = selectedPrompts.value.map((prompt) => (prompt.id === savedPrompt.id ? savedPrompt : prompt))
    promptEditDialogVisible.value = false
}

async function removePrompt(prompt: AiPrompt) {
    await DeleteAiPrompt({ id: prompt.id })
    aiPrompts.value = aiPrompts.value.filter((item) => item.id !== prompt.id)
    removeSelectedPrompt(prompt.id)
}

async function saveInputAsPrompt() {
    if (!savePromptName.value.trim()) {
        ElMessage.warning('请输入提示词名称')
        return
    }
    const res = await SaveAiPrompt({
        name: savePromptName.value.trim(),
        content: inputText.value.trim()
    })
    aiPrompts.value.unshift(res.data)
    savePromptDialogVisible.value = false
    savePromptName.value = ''
    ElMessage.success('提示词已保存')
}

function buildRequestContent(userContent: string): string {
    const sections: string[] = []
    if (selectedPrompts.value.length) {
        sections.push(
            `以下是本次对话选择的提示词，请遵循：\n${selectedPrompts.value
                .map((prompt, index) => `【提示词${index + 1}：${prompt.name}】\n${prompt.content}`)
                .join('\n\n')}`
        )
    }
    if (selectedFiles.value.length) {
        sections.push(
            `以下是用户上传的附件内容，请结合分析：\n${selectedFiles.value
                .map(
                    (file, index) =>
                        `【附件${index + 1}：${file.name}】\n类型：${file.contentType || '未知'}\n大小：${formatFileSize(
                            file.size
                        )}\n内容：\n${file.content}`
                )
                .join('\n\n')}`
        )
    }
    if (userContent) {
        sections.push(`用户问题：\n${userContent}`)
    }
    return sections.join('\n\n')
}

function buildDisplayContent(userContent: string): string {
    const lines: string[] = []
    if (selectedPrompts.value.length) {
        lines.push(`提示词：${selectedPrompts.value.map((prompt) => prompt.name).join('、')}`)
    }
    if (selectedFiles.value.length) {
        lines.push(`附件：${selectedFiles.value.map((file) => file.name).join('、')}`)
    }
    if (userContent) {
        lines.push(userContent)
    }
    return lines.join('\n\n')
}

function formatFileSize(size: number): string {
    if (size < 1024) {
        return `${size}B`
    }
    if (size < 1024 * 1024) {
        return `${(size / 1024).toFixed(1)}KB`
    }
    return `${(size / 1024 / 1024).toFixed(1)}MB`
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
    stopTyping()
    pendingAssistantContent = ''
    typingStableContent = ''
    currentTypingMessageIndex.value = -1
    typingPlayback.value = false
    typingStableMarkdownHtml.value = ''
    typingTailContent.value = ''
    sending.value = false
}

function stopTyping() {
    if (typingTimer) {
        window.clearTimeout(typingTimer)
        typingTimer = undefined
    }
}

function appendTypingContent(content: string, messageIndex: number) {
    pendingAssistantContent += content
    currentTypingMessageIndex.value = messageIndex
    typingPlayback.value = true
    startTyping()
}

function startTyping() {
    if (typingTimer || currentTypingMessageIndex.value < 0) {
        return
    }
    typingTimer = window.setTimeout(typeNextCharacter, TYPING_INTERVAL)
}

function typeNextCharacter() {
    typingTimer = undefined
    const message = messages.value[currentTypingMessageIndex.value]
    if (!message || !pendingAssistantContent) {
        currentTypingMessageIndex.value = -1
        typingPlayback.value = false
        return
    }

    const firstCodePoint = pendingAssistantContent.codePointAt(0) || 0
    const nextCharacter = String.fromCodePoint(firstCodePoint)
    message.content += nextCharacter
    pendingAssistantContent = pendingAssistantContent.slice(nextCharacter.length)
    updateTypingMarkdownPreview(message.content)
    scrollToBottom()

    if (pendingAssistantContent) {
        startTyping()
    } else if (sending.value) {
        typingPlayback.value = true
    } else {
        finishTypingMarkdownPreview(message.content)
        currentTypingMessageIndex.value = -1
        typingPlayback.value = false
    }
}

function updateTypingMarkdownPreview(content: string) {
    const stableBoundary = resolveStableMarkdownBoundary(content)
    if (stableBoundary > typingStableContent.length) {
        const stableDelta = content.slice(typingStableContent.length, stableBoundary)
        typingStableMarkdownHtml.value += renderMarkdown(stableDelta)
        typingStableContent = content.slice(0, stableBoundary)
    }
    typingTailContent.value = content.slice(typingStableContent.length)
}

function finishTypingMarkdownPreview(content: string) {
    typingStableMarkdownHtml.value = renderMarkdown(content)
    typingStableContent = content
    typingTailContent.value = ''
}

function resolveStableMarkdownBoundary(content: string): number {
    const lastLineBreak = content.lastIndexOf('\n')
    return lastLineBreak >= 0 ? lastLineBreak + 1 : 0
}

function waitTypingDone() {
    return new Promise<void>((resolve) => {
        const check = () => {
            if (!pendingAssistantContent && !typingTimer) {
                const message = messages.value[currentTypingMessageIndex.value]
                if (message) {
                    finishTypingMarkdownPreview(message.content)
                }
                currentTypingMessageIndex.value = -1
                typingPlayback.value = false
                resolve()
                return
            }
            window.setTimeout(check, TYPING_INTERVAL)
        }
        check()
    })
}

function clearTimer(timer?: ReturnType<typeof window.setTimeout>) {
    if (timer) {
        window.clearTimeout(timer)
    }
}

async function sendMessage() {
    if (!canSend.value) {
        return
    }
    const rawInputText = inputText.value.trim()
    const requestContent = buildRequestContent(rawInputText)
    const displayContent = buildDisplayContent(rawInputText)
    const userMessage: ChatMessage = {
        role: 'user',
        content: displayContent
    }
    const requestMessages = [
        ...messages.value,
        {
            role: 'user',
            content: requestContent
        }
    ]
    const assistantMessage: ChatMessage = {
        role: 'assistant',
        content: ''
    }
    ensureCurrentHistory(userMessage.content)
    messages.value.push(userMessage)
    messages.value.push(assistantMessage)
    updateCurrentHistory()
    inputText.value = ''
    selectedPrompts.value = []
    selectedFiles.value = []
    sending.value = true
    abortController.value = new AbortController()
    scrollToBottom()
    pendingAssistantContent = ''
    typingStableContent = ''
    const assistantMessageIndex = messages.value.length - 1
    currentTypingMessageIndex.value = assistantMessageIndex
    typingPlayback.value = false
    typingStableMarkdownHtml.value = ''
    typingTailContent.value = ''
    let hasAssistantContent = false
    let timeoutAbort = false
    let contentTimeoutTimer: ReturnType<typeof window.setTimeout> | undefined
    const resetContentTimeout = (timeout: number) => {
        clearTimer(contentTimeoutTimer)
        contentTimeoutTimer = window.setTimeout(() => {
            timeoutAbort = true
            abortController.value?.abort()
        }, timeout)
    }

    try {
        resetContentTimeout(FIRST_CONTENT_TIMEOUT)
        await StreamChatWithAi(
            {
                configId: currentConfigId.value,
                messages: requestMessages
            },
            {
                onContent(content: string) {
                    hasAssistantContent = true
                    resetContentTimeout(CONTENT_IDLE_TIMEOUT)
                    appendTypingContent(content, assistantMessageIndex)
                }
            },
            abortController.value.signal
        )
        clearTimer(contentTimeoutTimer)
        sending.value = false
        await waitTypingDone()
    } catch (error: any) {
        clearTimer(contentTimeoutTimer)
        stopTyping()
        pendingAssistantContent = ''
        typingStableContent = ''
        currentTypingMessageIndex.value = -1
        typingPlayback.value = false
        typingStableMarkdownHtml.value = ''
        typingTailContent.value = ''
        if (error?.name === 'AbortError') {
            if (!assistantMessage.content || timeoutAbort) {
                messages.value.pop()
            }
            if (timeoutAbort) {
                ElMessage.error(hasAssistantContent ? 'AI响应中断，请稍后重试' : 'AI响应超时，请稍后重试')
            } else {
                ElMessage.info('已停止生成')
            }
        } else {
            messages.value.pop()
            ElMessage.error(error?.message || 'AI对话失败')
        }
    } finally {
        clearTimer(contentTimeoutTimer)
        stopTyping()
        pendingAssistantContent = ''
        typingStableContent = ''
        currentTypingMessageIndex.value = -1
        typingPlayback.value = false
        typingStableMarkdownHtml.value = ''
        typingTailContent.value = ''
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
    loadPrompts()
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

.zhiqing-ai-composer-input {
    min-width: 0;
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.zhiqing-ai-composer-tags {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 6px;
}

.zhiqing-ai-chip {
    max-width: 180px;
    display: inline-flex;
    align-items: center;
    gap: 5px;
    box-sizing: border-box;
    padding: 3px 8px;
    border: 1px solid var(--el-color-primary-light-7);
    border-radius: 999px;
    color: var(--el-color-primary);
    font-size: 12px;
    line-height: 18px;
    background-color: var(--el-color-primary-light-9);

    &.is-file {
        border-color: var(--el-color-success-light-7);
        color: var(--el-color-success);
        background-color: var(--el-color-success-light-9);
    }

    .el-icon {
        flex: 0 0 auto;
        cursor: pointer;
    }
}

:deep(.zhiqing-ai-plus) {
    width: 24px;
    min-width: 24px;
    height: 24px;
    padding: 0;
    color: var(--el-text-color-secondary);
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

    &.is-typing {
        .zhiqing-ai-message__content::after {
            display: inline-block;
            width: 1px;
            height: 1em;
            margin-left: 2px;
            vertical-align: -2px;
            background-color: var(--el-text-color-secondary);
            content: '';
            animation: zhiqing-ai-cursor 1s steps(2, start) infinite;
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
    padding: 10px 14px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    color: var(--el-text-color-primary);
    line-height: 1.65;
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

.zhiqing-ai-markdown__tail {
    margin: 10px 0 0;
    color: var(--el-text-color-primary);
    line-height: 1.65;
    white-space: pre-wrap;
}

.zhiqing-ai-message__content :deep(.zhiqing-ai-markdown) {
    width: 100%;
    color: var(--el-text-color-primary);
    font-size: 14px;
    line-height: 1.7;

    > :first-child {
        margin-top: 0;
    }

    > :last-child {
        margin-bottom: 0;
    }

    p {
        margin: 10px 0;
    }

    h1,
    h2,
    h3,
    h4,
    h5,
    h6 {
        margin: 18px 0 10px;
        color: var(--el-text-color-primary);
        font-weight: 650;
        line-height: 1.35;
    }

    h1 {
        padding-bottom: 8px;
        border-bottom: 1px solid var(--el-border-color-lighter);
        font-size: 21px;
    }

    h2 {
        padding-bottom: 6px;
        border-bottom: 1px solid var(--el-border-color-lighter);
        font-size: 18px;
    }

    h3 {
        font-size: 16px;
    }

    h4,
    h5,
    h6 {
        font-size: 15px;
    }

    ul,
    ol {
        margin: 10px 0;
        padding-left: 24px;
    }

    li {
        margin: 6px 0;

        &::marker {
            color: var(--el-color-primary);
            font-weight: 600;
        }
    }

    li > p {
        margin: 4px 0;
    }

    blockquote {
        margin: 12px 0;
        padding: 8px 12px;
        border-left: 4px solid var(--el-color-primary-light-5);
        border-radius: 0 6px 6px 0;
        color: var(--el-text-color-secondary);
        background-color: var(--el-color-primary-light-9);
    }

    pre {
        margin: 12px 0;
        padding: 14px 16px;
        overflow: auto;
        border: 1px solid #e5e7eb;
        border-radius: 8px;
        background-color: #f8fafc;
    }

    code {
        padding: 2px 6px;
        border-radius: 4px;
        font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
        font-size: 13px;
        color: #334155;
        background-color: #eef2f7;
    }

    pre code {
        padding: 0;
        color: #1f2937;
        background-color: transparent;
        line-height: 1.6;
        white-space: pre;
    }

    table {
        width: 100%;
        margin: 12px 0;
        border-collapse: collapse;
        font-size: 14px;
    }

    th,
    td {
        padding: 9px 11px;
        border: 1px solid var(--el-border-color-lighter);
        text-align: left;
        vertical-align: top;
    }

    th {
        color: var(--el-text-color-primary);
        background-color: #f8fafc;
        font-weight: 600;
    }

    tr:nth-child(even) td {
        background-color: #fcfcfd;
    }

    a {
        color: var(--el-color-primary);
        text-decoration: underline;
        text-underline-offset: 3px;
    }

    a:hover {
        color: var(--el-color-primary-dark-2);
    }

    img {
        max-width: 100%;
        margin: 8px 0;
        border-radius: 8px;
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

@keyframes zhiqing-ai-cursor {
    0%,
    45% {
        opacity: 1;
    }

    46%,
    100% {
        opacity: 0;
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

    > :deep(.el-button) {
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

.zhiqing-ai-prompt-dialog {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.zhiqing-ai-prompt-dialog__toolbar {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 10px;
}

.zhiqing-ai-prompt-list {
    max-height: 460px;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
    overflow: auto;
    padding-right: 4px;
}

.zhiqing-ai-prompt-card {
    min-width: 0;
    min-height: 126px;
    box-sizing: border-box;
    padding: 12px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    cursor: pointer;
    background-color: #ffffff;

    &:hover,
    &.is-selected {
        border-color: var(--el-color-primary-light-5);
        background-color: var(--el-color-primary-light-9);
    }

    &.is-selected {
        box-shadow: inset 0 0 0 1px var(--el-color-primary-light-5);
    }

    p {
        height: 60px;
        margin: 8px 0 0;
        overflow: hidden;
        color: var(--el-text-color-secondary);
        font-size: 13px;
        line-height: 20px;
    }
}

.zhiqing-ai-prompt-card__header {
    min-width: 0;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;

    strong {
        min-width: 0;
        overflow: hidden;
        color: var(--el-text-color-primary);
        font-size: 14px;
        text-overflow: ellipsis;
        white-space: nowrap;
    }
}

.zhiqing-ai-prompt-card__actions {
    flex: 0 0 auto;
    display: flex;
    align-items: center;
    gap: 2px;
}

.zhiqing-ai-prompt-form {
    :deep(.el-textarea__inner) {
        line-height: 1.6;
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

    .zhiqing-ai-prompt-list {
        grid-template-columns: minmax(0, 1fr);
    }

    .zhiqing-ai__history {
        border-left: 0;
        border-top: 1px solid var(--el-border-color-lighter);
        padding-left: 0;
    }
}
</style>

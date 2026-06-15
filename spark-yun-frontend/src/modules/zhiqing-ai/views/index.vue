<template>
    <div class="zhiqing-ai">
        <div class="zhiqing-ai__toolbar">
            <div class="zhiqing-ai__title">
                <strong>至轻智能</strong>
                <span>基于租户智能配置的 AI 对话</span>
            </div>
            <div class="zhiqing-ai__actions">
                <el-select
                    v-model="currentConfigId"
                    class="zhiqing-ai__select"
                    filterable
                    :disabled="sending"
                    placeholder="选择智能配置"
                    @change="clearMessages"
                >
                    <el-option
                        v-for="config in aiConfigs"
                        :key="config.id"
                        :label="`${config.name} / ${config.modelName}`"
                        :value="config.id"
                    />
                </el-select>
                <el-button :disabled="sending" :icon="Refresh" @click="loadAiConfigs">刷新</el-button>
                <el-button :disabled="sending" :icon="Delete" @click="clearMessages">清空</el-button>
            </div>
        </div>

        <div ref="messagePanelRef" class="zhiqing-ai__messages">
            <el-empty
                v-if="!aiConfigs.length && !loadingConfigs"
                description="暂无可用智能配置，请先到后台管理配置"
            />
            <div v-else-if="!messages.length" class="zhiqing-ai__welcome">
                <strong>你好，我是至轻智能</strong>
                <span>选择一个智能配置后，可以开始对话。</span>
            </div>
            <div
                v-for="(message, index) in messages"
                :key="`${message.role}-${index}`"
                class="zhiqing-ai-message"
                :class="`is-${message.role}`"
            >
                <div class="zhiqing-ai-message__role">
                    {{ message.role === 'assistant' ? 'AI' : '我' }}
                </div>
                <div class="zhiqing-ai-message__content">{{ message.content }}</div>
            </div>
        </div>

        <div class="zhiqing-ai__composer">
            <el-input
                v-model="inputText"
                :autosize="{ minRows: 3, maxRows: 6 }"
                :disabled="sending || !currentConfigId"
                placeholder="输入问题，Enter 换行，点击发送提交"
                type="textarea"
            />
            <el-button v-if="sending" type="danger" @click="stopGenerating">停止</el-button>
            <el-button v-else type="primary" :disabled="!canSend" @click="sendMessage">
                发送
            </el-button>
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { Delete, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { ListWorkspaceAiConfig, StreamChatWithAi } from '@/modules/zhiqing-ai/api'

interface AiConfig {
    id: string
    name: string
    modelName: string
}

interface ChatMessage {
    role: 'user' | 'assistant'
    content: string
}

const aiConfigs = ref<AiConfig[]>([])
const currentConfigId = ref('')
const inputText = ref('')
const sending = ref(false)
const loadingConfigs = ref(false)
const messages = ref<ChatMessage[]>([])
const messagePanelRef = ref<HTMLElement>()
const abortController = ref<AbortController>()
const canSend = computed(() => !!currentConfigId.value && !!inputText.value.trim() && !sending.value)

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
    messages.value = []
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
    messages.value.push(userMessage)
    messages.value.push(assistantMessage)
    inputText.value = ''
    sending.value = true
    abortController.value = new AbortController()
    scrollToBottom()

    try {
        await StreamChatWithAi(
            {
                configId: currentConfigId.value,
                messages: requestMessages
            },
            {
                onContent(content: string) {
                    assistantMessage.content += content
                    scrollToBottom()
                }
            },
            abortController.value.signal
        )
    } catch (error: any) {
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
        abortController.value = undefined
        sending.value = false
        if (!assistantMessage.content && messages.value[messages.value.length - 1] === assistantMessage) {
            messages.value.pop()
        }
        if (assistantMessage.content) {
            scrollToBottom()
        }
    }
}

onMounted(loadAiConfigs)
</script>

<style scoped lang="scss">
.zhiqing-ai {
    height: calc(100vh - 32px);
    display: grid;
    grid-template-rows: auto minmax(0, 1fr) auto;
    padding: 16px 20px;
    background-color: #ffffff;
}

.zhiqing-ai__toolbar,
.zhiqing-ai__composer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
}

.zhiqing-ai__toolbar {
    padding-bottom: 12px;
    border-bottom: 1px solid var(--el-border-color-lighter);
}

.zhiqing-ai__title {
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 4px;

    strong {
        font-size: 18px;
        color: var(--el-text-color-primary);
    }

    span {
        color: var(--el-text-color-secondary);
        font-size: 13px;
    }
}

.zhiqing-ai__actions {
    display: flex;
    align-items: center;
    gap: 8px;
}

.zhiqing-ai__select {
    width: 280px;
}

.zhiqing-ai__messages {
    min-height: 0;
    display: flex;
    flex-direction: column;
    gap: 14px;
    padding: 20px 0;
    overflow: auto;
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
    display: grid;
    grid-template-columns: 40px minmax(0, 1fr);
    gap: 10px;
    max-width: 860px;

    &.is-user {
        align-self: flex-end;
        grid-template-columns: minmax(0, 1fr) 40px;

        .zhiqing-ai-message__role {
            order: 2;
            background-color: var(--el-color-primary);
            color: #ffffff;
        }

        .zhiqing-ai-message__content {
            order: 1;
            background-color: var(--el-color-primary-light-9);
        }
    }
}

.zhiqing-ai-message__role {
    width: 36px;
    height: 36px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background-color: var(--el-fill-color);
    color: var(--el-text-color-primary);
    font-weight: 600;
}

.zhiqing-ai-message__content {
    min-height: 36px;
    padding: 10px 12px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    color: var(--el-text-color-primary);
    line-height: 1.7;
    white-space: pre-wrap;
    word-break: break-word;
    background-color: #ffffff;
}

.zhiqing-ai__composer {
    padding-top: 12px;
    border-top: 1px solid var(--el-border-color-lighter);

    .el-textarea {
        flex: 1;
    }

    .el-button {
        width: 88px;
        align-self: stretch;
    }
}

@media (max-width: 720px) {
    .zhiqing-ai {
        height: calc(100vh - 16px);
        padding: 12px;
    }

    .zhiqing-ai__toolbar,
    .zhiqing-ai__composer {
        align-items: stretch;
        flex-direction: column;
    }

    .zhiqing-ai__actions,
    .zhiqing-ai__select {
        width: 100%;
    }
}
</style>

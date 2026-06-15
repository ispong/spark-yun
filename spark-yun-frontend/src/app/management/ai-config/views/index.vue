<template>
    <Breadcrumb :bread-crumb-list="[{ name: '智能配置', code: 'ai-config' }]" />
    <div class="ai-config-page">
        <div class="ai-config-page__toolbar">
            <el-button type="primary" :icon="Plus" @click="openEditor()">新增配置</el-button>
            <el-input
                v-model="keyword"
                clearable
                :prefix-icon="Search"
                placeholder="搜索配置名称、供应商或模型"
                @keyup.enter="loadConfigs"
                @clear="loadConfigs"
            />
        </div>

        <el-table v-loading="loading" :data="configs" height="calc(100vh - 220px)">
            <el-table-column prop="name" label="配置名称" min-width="150" />
            <el-table-column label="供应商" width="150">
                <template #default="{ row }">
                    {{ providerLabels[row.providerType] || row.providerType }}
                </template>
            </el-table-column>
            <el-table-column prop="modelName" label="模型" min-width="160" />
            <el-table-column prop="baseUrl" label="接口地址" min-width="240" show-overflow-tooltip />
            <el-table-column prop="temperature" label="温度" width="90" />
            <el-table-column prop="maxTokens" label="最大Token" width="110" />
            <el-table-column label="状态" width="100">
                <template #default="{ row }">
                    <el-tag :type="row.status === 'ENABLE' ? 'success' : 'info'">
                        {{ row.status === 'ENABLE' ? '启用' : '禁用' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
            <el-table-column label="操作" width="210" fixed="right">
                <template #default="{ row }">
                    <el-button link type="primary" :loading="testingId === row.id" @click="testConfig(row)">
                        测试
                    </el-button>
                    <el-button link type="primary" @click="openEditor(row)">编辑</el-button>
                    <el-button link type="danger" @click="removeConfig(row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>

        <el-pagination
            v-model:current-page="page"
            v-model:page-size="pageSize"
            class="ai-config-page__pagination"
            layout="total, sizes, prev, pager, next"
            :total="total"
            @current-change="loadConfigs"
            @size-change="loadConfigs"
        />
    </div>

    <el-dialog v-model="editorVisible" :title="form.id ? '编辑智能配置' : '新增智能配置'" width="620px">
        <el-form label-position="top">
            <div class="ai-config-form__grid">
                <el-form-item label="配置名称">
                    <el-input v-model="form.name" maxlength="80" />
                </el-form-item>
                <el-form-item label="供应商">
                    <el-select v-model="form.providerType" @change="handleProviderChange">
                        <el-option
                            v-for="provider in providers"
                            :key="provider.value"
                            :label="provider.label"
                            :value="provider.value"
                        />
                    </el-select>
                </el-form-item>
            </div>
            <el-form-item label="接口地址">
                <el-input v-model="form.baseUrl" placeholder="OpenAI 兼容接口地址" />
            </el-form-item>
            <div class="ai-config-form__grid">
                <el-form-item label="模型">
                    <el-input v-model="form.modelName" placeholder="例如 gpt-4o-mini、deepseek-chat" />
                </el-form-item>
                <el-form-item label="API Key">
                    <el-input
                        v-model="form.apiKey"
                        show-password
                        placeholder="编辑时留空则保留原值"
                        type="password"
                    />
                </el-form-item>
            </div>
            <div class="ai-config-form__grid">
                <el-form-item label="温度">
                    <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" />
                </el-form-item>
                <el-form-item label="最大Token">
                    <el-input-number v-model="form.maxTokens" :min="1" :step="100" />
                </el-form-item>
            </div>
            <el-form-item label="状态">
                <el-switch v-model="form.enabled" active-text="启用" inactive-text="禁用" />
            </el-form-item>
            <el-form-item label="备注">
                <el-input v-model="form.remark" maxlength="500" type="textarea" :rows="3" />
            </el-form-item>
        </el-form>
        <template #footer>
            <el-button @click="editorVisible = false">取消</el-button>
            <el-button type="primary" :loading="saving" @click="saveConfig">保存</el-button>
        </template>
    </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import { DeleteAiConfig, PageAiConfig, SaveAiConfig, TestAiConfig } from '@/app/management/ai-config/api'

interface AiConfig {
    id: string
    name: string
    providerType: string
    baseUrl: string
    modelName: string
    temperature: number
    maxTokens: number
    status: string
    remark?: string
}

const providerLabels: Record<string, string> = {
    OPENAI: 'OpenAI',
    DEEPSEEK: 'DeepSeek',
    DASHSCOPE: '通义千问',
    OLLAMA: 'Ollama',
    OPENAI_COMPATIBLE: 'OpenAI兼容'
}

const providers = [
    { label: 'OpenAI兼容', value: 'OPENAI_COMPATIBLE', baseUrl: '', modelName: '' },
    { label: 'OpenAI', value: 'OPENAI', baseUrl: 'https://api.openai.com', modelName: 'gpt-4o-mini' },
    { label: 'DeepSeek', value: 'DEEPSEEK', baseUrl: 'https://api.deepseek.com', modelName: 'deepseek-chat' },
    {
        label: '通义千问',
        value: 'DASHSCOPE',
        baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode',
        modelName: 'qwen-plus'
    },
    { label: 'Ollama', value: 'OLLAMA', baseUrl: 'http://localhost:11434', modelName: 'llama3.1' }
]

const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const testingId = ref('')
const editorVisible = ref(false)
const configs = ref<AiConfig[]>([])
const form = reactive({
    id: '',
    name: '',
    providerType: 'OPENAI_COMPATIBLE',
    baseUrl: '',
    apiKey: '',
    modelName: '',
    temperature: 0.7,
    maxTokens: 2000,
    enabled: true,
    remark: ''
})

function loadConfigs() {
    loading.value = true
    PageAiConfig({
        page: page.value - 1,
        pageSize: pageSize.value,
        searchKeyWord: keyword.value
    })
        .then((res: any) => {
            configs.value = res.data.content || []
            total.value = res.data.totalElements || 0
        })
        .finally(() => {
            loading.value = false
        })
}

function openEditor(config?: AiConfig) {
    form.id = config?.id || ''
    form.name = config?.name || ''
    form.providerType = config?.providerType || 'OPENAI_COMPATIBLE'
    form.baseUrl = config?.baseUrl || ''
    form.apiKey = ''
    form.modelName = config?.modelName || ''
    form.temperature = config?.temperature ?? 0.7
    form.maxTokens = config?.maxTokens ?? 2000
    form.enabled = config?.status !== 'DISABLE'
    form.remark = config?.remark || ''
    editorVisible.value = true
}

function handleProviderChange(providerType: string) {
    const provider = providers.find((item) => item.value === providerType)
    if (!provider) {
        return
    }
    if (provider.baseUrl) {
        form.baseUrl = provider.baseUrl
    }
    if (provider.modelName) {
        form.modelName = provider.modelName
    }
}

function saveConfig() {
    if (!form.name.trim() || !form.providerType || !form.modelName.trim()) {
        ElMessage.warning('请填写配置名称、供应商和模型')
        return
    }
    if (form.providerType === 'OPENAI_COMPATIBLE' && !form.baseUrl.trim()) {
        ElMessage.warning('请填写接口地址')
        return
    }
    if (!form.id && form.providerType !== 'OLLAMA' && !form.apiKey.trim()) {
        ElMessage.warning('请填写API Key')
        return
    }
    saving.value = true
    SaveAiConfig({
        id: form.id || undefined,
        name: form.name,
        providerType: form.providerType,
        baseUrl: form.baseUrl,
        apiKey: form.apiKey,
        modelName: form.modelName,
        temperature: form.temperature,
        maxTokens: form.maxTokens,
        status: form.enabled ? 'ENABLE' : 'DISABLE',
        remark: form.remark
    })
        .then((res: any) => {
            ElMessage.success(res.msg)
            editorVisible.value = false
            loadConfigs()
        })
        .finally(() => {
            saving.value = false
        })
}

function removeConfig(config: AiConfig) {
    ElMessageBox.confirm(`确定删除智能配置“${config.name}”吗？`, '提示', {
        type: 'warning'
    }).then(() => {
        DeleteAiConfig({
            id: config.id
        }).then((res: any) => {
            ElMessage.success(res.msg)
            loadConfigs()
        })
    })
}

function testConfig(config: AiConfig) {
    testingId.value = config.id
    TestAiConfig({
        id: config.id
    })
        .then((res: any) => {
            ElMessage.success(res.msg)
        })
        .finally(() => {
            testingId.value = ''
        })
}

onMounted(loadConfigs)
</script>

<style scoped lang="scss">
.ai-config-page {
    padding: 16px;
    border: 1px solid var(--el-border-color-lighter);
    background-color: #ffffff;
}

.ai-config-page__toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 12px;

    .el-input {
        width: 320px;
    }
}

.ai-config-page__pagination {
    margin-top: 12px;
    justify-content: flex-end;
}

.ai-config-form__grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
}
</style>

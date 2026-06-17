<template>
    <Breadcrumb :bread-crumb-list="[{ name: '智能配置', code: 'ai-config' }]" />
    <div class="zqy-seach-table ai-config-page">
        <div class="zqy-table-top">
            <el-button type="primary" @click="openEditor()">新增配置</el-button>
            <div class="zqy-seach">
                <el-input
                    v-model="keyword"
                    clearable
                    placeholder="请输入配置名称、供应商或模型 回车进行搜索"
                    @input="inputEvent"
                    @keyup.enter="loadConfigs(false)"
                />
            </div>
            <Transition name="ai-config-batch-slide">
                <div v-if="selectedRows.length" class="ai-config-batch-mask">
                    <div class="ai-config-batch-actions">
                        <el-button class="ai-config-batch-action" :loading="batchLoading" @click="batchEnableConfigs">
                            启用
                        </el-button>
                        <el-button class="ai-config-batch-action" :loading="batchLoading" @click="batchDisableConfigs">
                            禁用
                        </el-button>
                        <el-button class="ai-config-batch-action" :loading="batchLoading" @click="batchDeleteConfigs">
                            删除
                        </el-button>
                        <el-button class="ai-config-batch-cancel" :disabled="batchLoading" @click="cancelSelection">
                            取消选择
                        </el-button>
                    </div>
                </div>
            </Transition>
        </div>

        <LoadingPage :visible="loading" :network-error="networkError" @loading-refresh="loadConfigs(false)">
            <div class="zqy-table">
                <BlockTable
                    :table-config="tableConfig"
                    @size-change="handleSizeChange"
                    @current-change="handleCurrentChange"
                    @checkbox-change="handleSelectionChange"
                >
                    <template #name="scopeSlot">
                        <span class="name-click" @click="openEditor(scopeSlot.row)">{{ scopeSlot.row.name }}</span>
                    </template>
                    <template #providerType="scopeSlot">
                        {{ providerLabels[scopeSlot.row.providerType] || scopeSlot.row.providerType }}
                    </template>
                    <template #statusTag="scopeSlot">
                        <div class="btn-group">
                            <el-tag v-if="scopeSlot.row.status === 'ENABLE'" class="ml-2" type="success">启用</el-tag>
                            <el-tag v-else class="ml-2" type="danger">禁用</el-tag>
                        </div>
                    </template>
                    <template #options="scopeSlot">
                        <div class="btn-group ai-config-action-group">
                            <span class="ai-config-action-button" @click="openEditor(scopeSlot.row)">编辑</span>
                            <el-dropdown trigger="click" popper-class="ai-config-action-dropdown">
                                <span class="click-show-more ai-config-action-button">更多</span>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item
                                            :disabled="testingId === scopeSlot.row.id"
                                            @click="testingId !== scopeSlot.row.id && testConfig(scopeSlot.row)"
                                        >
                                            <span v-if="testingId !== scopeSlot.row.id">测试</span>
                                            <el-icon v-else class="is-loading">
                                                <Loading />
                                            </el-icon>
                                        </el-dropdown-item>
                                        <el-dropdown-item
                                            :disabled="scopeSlot.row.statusLoading"
                                            @click="
                                                !scopeSlot.row.statusLoading &&
                                                    changeStatus(scopeSlot.row, scopeSlot.row.status !== 'ENABLE')
                                            "
                                        >
                                            <span v-if="!scopeSlot.row.statusLoading">
                                                {{ scopeSlot.row.status === 'ENABLE' ? '禁用' : '启用' }}
                                            </span>
                                            <el-icon v-else class="is-loading">
                                                <Loading />
                                            </el-icon>
                                        </el-dropdown-item>
                                        <el-dropdown-item @click="removeConfig(scopeSlot.row)">删除</el-dropdown-item>
                                    </el-dropdown-menu>
                                </template>
                            </el-dropdown>
                        </div>
                    </template>
                </BlockTable>
            </div>
        </LoadingPage>
    </div>

    <el-dialog
        v-model="editorVisible"
        :title="form.id ? '编辑智能配置' : '新增智能配置'"
        width="520px"
        class="ai-config-editor-dialog zqy-block-modal"
        append-to-body
        destroy-on-close
        :close-on-click-modal="false"
    >
        <el-form class="ai-config-form" label-position="top">
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
            <el-form-item label="接口地址">
                <el-input v-model="form.baseUrl" placeholder="OpenAI 兼容接口地址" />
            </el-form-item>
            <el-form-item label="模型">
                <el-input v-model="form.modelName" placeholder="例如 gpt-4o-mini、deepseek-chat" />
            </el-form-item>
            <el-form-item label="API Key">
                <el-input v-model="form.apiKey" show-password placeholder="编辑时留空则保留原值" type="password" />
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
import { Loading } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import BlockTable from '@/app/components/block-table/index.vue'
import LoadingPage from '@/app/components/loading/index.vue'
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
    statusLoading?: boolean
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
const defaultProvider = providers.find((item) => item.value === 'DEEPSEEK') || providers[0]

const keyword = ref('')
const loading = ref(false)
const networkError = ref(false)
const saving = ref(false)
const batchLoading = ref(false)
const testingId = ref('')
const editorVisible = ref(false)
const selectedRows = ref<AiConfig[]>([])

const tableConfig: any = reactive({
    tableData: [],
    colConfigs: [
        {
            prop: 'name',
            title: '配置名称',
            minWidth: 140,
            customSlot: 'name',
            showOverflowTooltip: true
        },
        {
            prop: 'providerType',
            title: '供应商',
            minWidth: 110,
            customSlot: 'providerType'
        },
        {
            prop: 'modelName',
            title: '模型',
            minWidth: 140,
            showOverflowTooltip: true
        },
        {
            prop: 'baseUrl',
            title: '接口地址',
            minWidth: 220,
            showOverflowTooltip: true
        },
        {
            prop: 'status',
            title: '状态',
            minWidth: 90,
            customSlot: 'statusTag'
        },
        {
            prop: 'remark',
            title: '备注',
            minWidth: 120,
            showOverflowTooltip: true
        },
        {
            title: '操作',
            align: 'center',
            customSlot: 'options',
            width: 128,
            fixed: 'right'
        }
    ],
    checkbox: true,
    pagination: {
        currentPage: 1,
        pageSize: 10,
        total: 0
    },
    seqType: 'seq',
    loading: false
})

const form = reactive({
    id: '',
    name: '',
    providerType: defaultProvider.value,
    baseUrl: defaultProvider.baseUrl,
    apiKey: '',
    modelName: defaultProvider.modelName,
    temperature: 0.7,
    maxTokens: 2000,
    enabled: true,
    remark: ''
})

function loadConfigs(tableLoading?: boolean) {
    loading.value = tableLoading ? false : true
    networkError.value = networkError.value || false
    PageAiConfig({
        page: tableConfig.pagination.currentPage - 1,
        pageSize: tableConfig.pagination.pageSize,
        searchKeyWord: keyword.value
    })
        .then((res: any) => {
            tableConfig.tableData = res.data.content || []
            tableConfig.pagination.total = res.data.totalElements || 0
            selectedRows.value = []
            tableConfig.loading = false
            networkError.value = false
        })
        .catch(() => {
            tableConfig.tableData = []
            tableConfig.pagination.total = 0
            selectedRows.value = []
            tableConfig.loading = false
            networkError.value = true
        })
        .finally(() => {
            loading.value = false
        })
}

function openEditor(config?: AiConfig) {
    form.id = config?.id || ''
    form.name = config?.name || ''
    form.providerType = config?.providerType || defaultProvider.value
    form.baseUrl = config?.baseUrl || defaultProvider.baseUrl
    form.apiKey = ''
    form.modelName = config?.modelName || defaultProvider.modelName
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

function saveRowStatus(config: AiConfig, enabled: boolean) {
    return SaveAiConfig({
        id: config.id,
        name: config.name,
        providerType: config.providerType,
        baseUrl: config.baseUrl,
        apiKey: '',
        modelName: config.modelName,
        temperature: config.temperature,
        maxTokens: config.maxTokens,
        status: enabled ? 'ENABLE' : 'DISABLE',
        remark: config.remark
    })
}

function changeStatus(config: AiConfig, enabled: boolean) {
    config.statusLoading = true
    saveRowStatus(config, enabled)
        .then((res: any) => {
            ElMessage.success(res.msg)
            loadConfigs(true)
        })
        .finally(() => {
            config.statusLoading = false
        })
}

function removeConfig(config: AiConfig) {
    ElMessageBox.confirm(`确定删除智能配置“${config.name}”吗？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
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

function handleSelectionChange(records: AiConfig[]) {
    selectedRows.value = records || []
}

function cancelSelection() {
    selectedRows.value = []
    tableConfig.tableData = [...tableConfig.tableData]
}

function batchEnableConfigs() {
    const disableRows = selectedRows.value.filter((row) => row.status === 'DISABLE')
    if (!disableRows.length) {
        ElMessage.warning('请选择禁用状态的智能配置')
        return
    }
    batchLoading.value = true
    Promise.all(disableRows.map((row) => saveRowStatus(row, true)))
        .then(() => {
            ElMessage.success('批量启用成功')
            loadConfigs(true)
        })
        .catch(() => {})
        .finally(() => {
            batchLoading.value = false
        })
}

function batchDisableConfigs() {
    const enableRows = selectedRows.value.filter((row) => row.status === 'ENABLE')
    if (!enableRows.length) {
        ElMessage.warning('请选择启用状态的智能配置')
        return
    }
    batchLoading.value = true
    Promise.all(enableRows.map((row) => saveRowStatus(row, false)))
        .then(() => {
            ElMessage.success('批量禁用成功')
            loadConfigs(true)
        })
        .catch(() => {})
        .finally(() => {
            batchLoading.value = false
        })
}

function batchDeleteConfigs() {
    if (!selectedRows.value.length) {
        return
    }
    ElMessageBox.confirm(`确定删除选中的 ${selectedRows.value.length} 个智能配置吗？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        batchLoading.value = true
        Promise.all(selectedRows.value.map((row) => DeleteAiConfig({ id: row.id })))
            .then(() => {
                ElMessage.success('批量删除成功')
                loadConfigs()
            })
            .catch(() => {})
            .finally(() => {
                batchLoading.value = false
            })
    })
}

function inputEvent(value: string) {
    if (value === '') {
        loadConfigs()
    }
}

function handleSizeChange(pageSize: number) {
    tableConfig.pagination.pageSize = pageSize
    loadConfigs()
}

function handleCurrentChange(currentPage: number) {
    tableConfig.pagination.currentPage = currentPage
    loadConfigs()
}

onMounted(() => {
    tableConfig.pagination.currentPage = 1
    tableConfig.pagination.pageSize = 10
    loadConfigs()
})
</script>

<style scoped lang="scss">
.zqy-seach-table.ai-config-page {
    .zqy-table-top {
        position: relative;
        overflow: hidden;
    }

    .ai-config-batch-mask {
        position: absolute;
        z-index: 2;
        inset: 0;
        display: flex;
        align-items: center;
        justify-content: flex-start;
        padding: 0 20px;
        box-sizing: border-box;
        background-color: #fff;
    }

    .ai-config-batch-slide-enter-active,
    .ai-config-batch-slide-leave-active {
        transition:
            transform 0.18s ease,
            opacity 0.18s ease;
        will-change: transform, opacity;
    }

    .ai-config-batch-slide-enter-from,
    .ai-config-batch-slide-leave-to {
        opacity: 0;
        transform: translateY(-100%);
    }

    .ai-config-batch-slide-enter-to,
    .ai-config-batch-slide-leave-from {
        opacity: 1;
        transform: translateY(0);
    }

    .ai-config-batch-actions {
        display: flex;
        align-items: center;
        gap: 12px;
    }

    .ai-config-batch-action {
        min-width: 66px;
        height: 32px;
        line-height: 30px;
        border-color: var(--el-color-primary);
        color: var(--el-color-primary);
        background-color: #fff;

        &:hover,
        &:focus {
            border-color: var(--el-color-primary);
            color: #fff;
            background-color: var(--el-color-primary);
        }
    }

    .ai-config-batch-cancel {
        height: 32px;
        line-height: 30px;
    }

    .ai-config-action-group {
        justify-content: center;
        gap: 16px;

        .ai-config-action-button {
            display: inline-flex;
            align-items: center;
            line-height: 1;
            font-size: var(--el-font-size-extra-small);
            color: var(--el-color-primary);
            cursor: pointer;
            white-space: nowrap;
        }
    }
}

</style>

<style lang="scss">
.ai-config-editor-dialog.zqy-block-modal {
    --ai-config-modal-x-padding: 20px;
    --ai-config-modal-border-color: #ebeef5;

    overflow: unset !important;
    border-radius: 2px;

    .el-dialog__header {
        position: relative;
        min-height: 46px;
        padding: 9px var(--ai-config-modal-x-padding) 8px !important;
        margin-right: 0;
        border-bottom: none;

        &::after {
            position: absolute;
            right: 0;
            bottom: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--ai-config-modal-border-color);
        }
    }

    .el-dialog__title {
        display: block;
        line-height: 28px;
        font-size: getCssVar('font-size', 'medium');
        color: getCssVar('text-color', 'primary');
    }

    .el-dialog__headerbtn {
        top: 0;
        width: 42px;
        height: 46px;
    }

    .el-dialog__body {
        max-height: calc(74vh - 102px);
        padding: 0 !important;
        overflow: auto;
    }

    .el-dialog__footer {
        position: relative;
        display: flex;
        justify-content: flex-end;
        min-height: 56px;
        padding: 12px var(--ai-config-modal-x-padding);
        border-top: none;
        box-shadow: unset !important;

        &::before {
            position: absolute;
            top: 0;
            right: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--ai-config-modal-border-color);
        }

        .el-button + .el-button {
            margin-left: 12px;
        }
    }

    .ai-config-form {
        padding: 14px var(--ai-config-modal-x-padding) 4px;
        box-sizing: border-box;

        .el-form-item {
            margin-bottom: 20px;
        }

        .el-form-item__label {
            width: 100%;
            padding: 0;
            margin-bottom: 4px;
            line-height: 16px;
            color: getCssVar('text-color', 'regular');
        }

        .el-form-item__content,
        .el-input,
        .el-select,
        .el-input-number,
        .el-textarea {
            width: 100%;
        }

        .el-input__wrapper,
        .el-textarea__inner {
            border-radius: 2px;
        }
    }
}

.ai-config-action-dropdown {
    .el-dropdown-menu {
        min-width: 76px;
        padding: 4px 0;
    }

    .el-dropdown-menu__item {
        height: 26px;
        line-height: 26px;
        font-family: Avenir, Helvetica, Arial, sans-serif;
        font-size: var(--el-font-size-extra-small);
    }

    .el-icon {
        font-size: var(--el-font-size-extra-small);
    }
}
</style>

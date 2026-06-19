<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-user-log zqy-seach-table user-log-page">
        <div class="zqy-table-top">
            <div class="zqy-user-log__filters">
                <el-select v-model="logType" clearable placeholder="全部类型" @change="handleLogTypeChange">
                    <el-option label="平台" value="PLATFORM" />
                    <el-option label="租户" value="TENANT" />
                </el-select>
                <el-select v-model="moduleCode" clearable placeholder="全部模块" @change="handleModuleChange">
                    <el-option
                        v-for="item in moduleOptions"
                        :key="item.moduleCode"
                        :label="item.moduleName"
                        :value="item.moduleCode"
                    />
                </el-select>
                <el-select
                    v-model="account"
                    class="zqy-user-log__filter-input"
                    clearable
                    filterable
                    remote
                    reserve-keyword
                    :remote-show-suffix="true"
                    :remote-method="searchUserOptions"
                    :loading="userOptionsLoading"
                    placeholder="账号"
                    @clear="queryData"
                    @change="queryData"
                    @visible-change="handleUserVisibleChange"
                >
                    <el-option
                        v-for="item in userOptions"
                        :key="item.id || item.account"
                        :label="item.account"
                        :value="item.account"
                    />
                </el-select>
                <el-select
                    v-model="tenantId"
                    class="zqy-user-log__filter-input"
                    clearable
                    filterable
                    remote
                    reserve-keyword
                    :remote-show-suffix="true"
                    :remote-method="searchTenantOptions"
                    :loading="tenantOptionsLoading"
                    placeholder="租户"
                    @clear="queryData"
                    @change="queryData"
                    @visible-change="handleTenantVisibleChange"
                >
                    <el-option
                        v-for="item in tenantOptions"
                        :key="item.id"
                        :label="item.name"
                        :value="item.id"
                    />
                </el-select>
                <el-date-picker
                    v-model="timeRange"
                    type="datetimerange"
                    range-separator="-"
                    start-placeholder="开始时间"
                    end-placeholder="结束时间"
                    value-format="YYYY-MM-DDTHH:mm:ss"
                    @change="queryData"
                />
            </div>
            <div class="zqy-seach">
                <el-input
                    v-model="keyword"
                    placeholder="请输入接口/路径/IP/User-Agent 回车搜索"
                    clearable
                    maxlength="200"
                    @clear="queryData"
                    @keyup.enter="queryData"
                />
            </div>
        </div>
        <LoadingPage :visible="loading" :network-error="networkError" @loading-refresh="initData(false)">
            <div class="zqy-table">
                <BlockTable
                    :table-config="tableConfig"
                    :column-resizable="false"
                    @size-change="handleSizeChange"
                    @current-change="handleCurrentChange"
                >
                    <template #logType="scopeSlot">
                        <el-tag>{{ scopeSlot.row.logType === 'PLATFORM' ? '平台' : '租户' }}</el-tag>
                    </template>
                    <template #tenantName="scopeSlot">
                        {{ scopeSlot.row.logType === 'PLATFORM' ? '平台系统' : scopeSlot.row.tenantName || '-' }}
                    </template>
                    <template #status="scopeSlot">
                        <ZStatusTag :status="scopeSlot.row.status === 'SUCCESS' ? 'SUCCESS' : 'FAIL'" />
                    </template>
                    <template #duration="scopeSlot">{{ scopeSlot.row.duration ?? 0 }} ms</template>
                    <template #options="scopeSlot">
                        <div class="zqy-user-log__options">
                            <el-button link type="primary" @click="openDetail(scopeSlot.row)">详情</el-button>
                        </div>
                    </template>
                </BlockTable>
            </div>
        </LoadingPage>
        <el-dialog
            v-model="detailVisible"
            class="zqy-user-log__detail-dialog"
            title="行为日志详情"
            width="760px"
        >
            <div v-if="currentRow" class="zqy-user-log__detail">
                <el-tabs v-model="activeDetailTab">
                    <el-tab-pane label="请求参数" name="request">
                        <pre>{{ formatJson(currentRow.reqBody) }}</pre>
                    </el-tab-pane>
                    <el-tab-pane label="响应结果" name="response">
                        <pre>{{ formatJson(currentRow.resBody) }}</pre>
                    </el-tab-pane>
                    <el-tab-pane label="异常信息" name="exception">
                        <pre>{{ currentRow.exceptionMessage || '-' }}</pre>
                    </el-tab-pane>
                </el-tabs>
            </div>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'

import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import BlockTable from '@/app/components/block-table/index.vue'
import LoadingPage from '@/app/components/loading/index.vue'
import ZStatusTag from '@/app/components/z-status-tag/index.vue'
import { GetTenantList } from '@/app/management/tenant-list/api'
import { GetUserCenterList } from '@/app/management/user-center/api'
import { GetUserLogDefinitions, PageUserLog } from '../api'
import { BreadCrumbList, TableConfig } from './user-log.config'

interface UserLogDefinition {
    moduleCode: string
    moduleName: string
    logType: string
    apiName: string
}

interface UserOption {
    id?: string
    account: string
    username?: string
}

interface TenantOption {
    id: string
    name: string
}

const keyword = ref('')
const logType = ref('')
const moduleCode = ref('')
const account = ref('')
const tenantId = ref('')
const timeRange = ref<string[]>([])
const loading = ref(false)
const networkError = ref(false)
const detailVisible = ref(false)
const currentRow = ref<any>(null)
const activeDetailTab = ref('request')
const definitions = ref<UserLogDefinition[]>([])
const userOptions = ref<UserOption[]>([])
const tenantOptions = ref<TenantOption[]>([])
const userOptionsLoading = ref(false)
const tenantOptionsLoading = ref(false)

const breadCrumbList = reactive(BreadCrumbList)
const tableConfig: any = reactive(TableConfig)

const moduleOptions = computed(() => {
    const map = new Map<string, UserLogDefinition>()
    definitions.value.forEach((item) => {
        if ((!logType.value || item.logType === logType.value) && !map.has(item.moduleCode)) {
            map.set(item.moduleCode, item)
        }
    })
    return Array.from(map.values())
})

function initData(tableLoading?: boolean) {
    loading.value = tableLoading ? false : true
    tableConfig.loading = true
    networkError.value = false
    PageUserLog({
        page: tableConfig.pagination.currentPage - 1,
        pageSize: tableConfig.pagination.pageSize,
        searchKeyWord: keyword.value,
        logType: logType.value,
        moduleCode: moduleCode.value,
        account: account.value,
        tenantId: tenantId.value,
        startDateTime: timeRange.value?.[0],
        endDateTime: timeRange.value?.[1]
    })
        .then((res: any) => {
            tableConfig.tableData = res.data.content
            tableConfig.pagination.total = res.data.totalElements
            loading.value = false
            tableConfig.loading = false
            networkError.value = false
        })
        .catch(() => {
            tableConfig.tableData = []
            tableConfig.pagination.total = 0
            loading.value = false
            tableConfig.loading = false
            networkError.value = true
        })
}

function initDefinitions() {
    GetUserLogDefinitions().then((res: any) => {
        definitions.value = res.data || []
    })
}

function searchUserOptions(keyword = '') {
    userOptionsLoading.value = true
    GetUserCenterList({
        page: 0,
        pageSize: 20,
        searchKeyWord: keyword
    })
        .then((res: any) => {
            userOptions.value = res.data?.content || []
        })
        .finally(() => {
            userOptionsLoading.value = false
        })
}

function searchTenantOptions(keyword = '') {
    tenantOptionsLoading.value = true
    GetTenantList({
        page: 0,
        pageSize: 20,
        searchKeyWord: keyword
    })
        .then((res: any) => {
            tenantOptions.value = res.data?.content || []
        })
        .finally(() => {
            tenantOptionsLoading.value = false
        })
}

function handleUserVisibleChange(visible: boolean) {
    if (visible && userOptions.value.length === 0) {
        searchUserOptions()
    }
}

function handleTenantVisibleChange(visible: boolean) {
    if (visible && tenantOptions.value.length === 0) {
        searchTenantOptions()
    }
}

function handleModuleChange() {
    queryData()
}

function handleLogTypeChange() {
    moduleCode.value = ''
    queryData()
}

function queryData() {
    tableConfig.pagination.currentPage = 1
    initData(true)
}

function handleSizeChange(size: number) {
    tableConfig.pagination.pageSize = size
    tableConfig.pagination.currentPage = 1
    initData(true)
}

function handleCurrentChange(page: number) {
    tableConfig.pagination.currentPage = page
    initData(true)
}

function openDetail(row: any) {
    currentRow.value = row
    activeDetailTab.value = 'request'
    detailVisible.value = true
}

function formatJson(value: string) {
    if (!value) return '-'
    try {
        return JSON.stringify(JSON.parse(value), null, 2)
    } catch {
        return value
    }
}

onMounted(() => {
    tableConfig.pagination.currentPage = 1
    tableConfig.pagination.pageSize = 10
    initDefinitions()
    searchUserOptions()
    searchTenantOptions()
    initData()
})
</script>

<style lang="scss">
.zqy-user-log.user-log-page {
    .zqy-table-top {
        height: auto;
        min-height: 60px;
        padding-top: 10px;
        padding-bottom: 10px;
        align-items: flex-start;
        gap: 8px 12px;
    }

    .zqy-user-log__filters {
        flex: 1 1 884px;
        display: flex;
        flex-wrap: wrap;
        gap: 8px;

        .el-select {
            width: 128px;
        }

        .el-date-editor {
            width: 340px;
        }
    }

    .zqy-user-log__filter-input {
        width: 128px;
    }

    .zqy-seach {
        flex: 0 1 330px;

        .el-input {
            width: 100%;
        }
    }

    .zqy-user-log__options {
        display: flex;
        justify-content: center;
    }
}

@media screen and (max-width: 1280px) {
    .zqy-user-log.user-log-page {
        .zqy-table-top {
            flex-wrap: wrap;
            justify-content: flex-start;
        }

        .zqy-user-log__filters {
            flex-basis: 100%;
        }

        .zqy-seach {
            flex-basis: 100%;
        }
    }
}

.zqy-user-log__detail-dialog {
    .el-dialog__body {
        padding-top: 8px;
    }

    pre {
        min-height: 220px;
        max-height: 360px;
        padding: 12px;
        margin: 0;
        overflow: auto;
        border: 1px solid getCssVar('border-color', 'lighter');
        border-radius: 4px;
        background-color: getCssVar('fill-color', 'lighter');
        white-space: pre-wrap;
        word-break: break-word;
        font-size: 12px;
        line-height: 18px;
    }
}
</style>

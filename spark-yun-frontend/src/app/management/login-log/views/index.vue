<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-login-log zqy-seach-table">
        <div class="zqy-table-top">
            <div class="zqy-login-log__filters">
                <el-select v-model="loginMethod" clearable :placeholder="t('loginLog.allMethods')" @change="initData(true)">
                    <el-option :label="t('loginLog.accountPassword')" value="ACCOUNT_PASSWORD" />
                    <el-option :label="t('loginLog.phonePassword')" value="PHONE_PASSWORD" />
                    <el-option :label="t('loginLog.emailPassword')" value="EMAIL_PASSWORD" />
                    <el-option :label="t('loginLog.phoneCode')" value="PHONE_CODE" />
                    <el-option :label="t('loginLog.emailCode')" value="EMAIL_CODE" />
                </el-select>
                <el-select v-model="loginStatus" clearable :placeholder="t('loginLog.allResults')" @change="initData(true)">
                    <el-option :label="t('loginLog.success')" value="SUCCESS" />
                    <el-option :label="t('loginLog.fail')" value="FAIL" />
                </el-select>
            </div>
            <div class="zqy-seach">
                <el-input
                    v-model="keyword"
                    :placeholder="t('loginLog.searchPlaceholder')"
                    clearable
                    @input="inputEvent"
                    @keyup.enter="initData(true)"
                />
            </div>
        </div>
        <LoadingPage :visible="loading" :network-error="networkError" @loading-refresh="initData(false)">
            <div class="zqy-table">
                <BlockTable
                    :table-config="tableConfig"
                    @size-change="handleSizeChange"
                    @current-change="handleCurrentChange"
                >
                    <template #loginMethod="scopeSlot">
                        <el-tag>{{ getLoginMethodText(scopeSlot.row.loginMethod) }}</el-tag>
                    </template>
                    <template #loginStatus="scopeSlot">
                        <el-tag v-if="scopeSlot.row.loginStatus === 'SUCCESS'" type="success">
                            {{ t('loginLog.success') }}
                        </el-tag>
                        <el-tag v-else type="danger">{{ t('loginLog.fail') }}</el-tag>
                    </template>
                    <template #registered="scopeSlot">
                        <el-tag v-if="scopeSlot.row.registered" type="success">{{ t('common.yes') }}</el-tag>
                        <el-tag v-else>{{ t('common.no') }}</el-tag>
                    </template>
                    <template #failureReason="scopeSlot">
                        <span>{{ getFailureReasonText(scopeSlot.row.errorMessage) }}</span>
                    </template>
                </BlockTable>
            </div>
        </LoadingPage>
    </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import BlockTable from '@/app/components/block-table/index.vue'
import LoadingPage from '@/app/components/loading/index.vue'
import { PageLoginLog } from '@/app/management/login-method/api'
import { createBreadCrumbList, createColConfigs, createTableConfig } from './login-log.config'

const { t, locale } = useI18n()
const keyword = ref('')
const loginMethod = ref('')
const loginStatus = ref('')
const loading = ref(false)
const networkError = ref(false)

const breadCrumbList = reactive(createBreadCrumbList(t))
const tableConfig: any = reactive(createTableConfig(t))

watch(locale, () => {
    const nextBreadCrumbList = createBreadCrumbList(t)
    breadCrumbList.splice(0, breadCrumbList.length, ...nextBreadCrumbList)
    tableConfig.colConfigs = createColConfigs(t)
})

function initData(tableLoading?: boolean) {
    loading.value = tableLoading ? false : true
    tableConfig.loading = true
    networkError.value = false
    PageLoginLog({
        page: tableConfig.pagination.currentPage - 1,
        pageSize: tableConfig.pagination.pageSize,
        searchKeyWord: keyword.value,
        loginMethod: loginMethod.value,
        loginStatus: loginStatus.value
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

function inputEvent(value: string) {
    if (value === '') {
        initData(true)
    }
}

function handleSizeChange(size: number) {
    tableConfig.pagination.pageSize = size
    initData(true)
}

function handleCurrentChange(page: number) {
    tableConfig.pagination.currentPage = page
    initData(true)
}

function getLoginMethodText(method: string) {
    const methodText: Record<string, string> = {
        ACCOUNT_PASSWORD: t('loginLog.accountPassword'),
        PHONE_PASSWORD: t('loginLog.phonePassword'),
        EMAIL_PASSWORD: t('loginLog.emailPassword'),
        PHONE_CODE: t('loginLog.phoneCode'),
        EMAIL_CODE: t('loginLog.emailCode')
    }
    return methodText[method] || method
}

function getFailureReasonText(errorMessage?: string) {
    if (!errorMessage) return ''
    const reasonText: Record<string, string> = {
        账号或者密码不正确: t('loginLog.incorrectAccountOrPassword')
    }
    return reasonText[errorMessage] || errorMessage
}

onMounted(() => {
    tableConfig.pagination.currentPage = 1
    tableConfig.pagination.pageSize = 10
    initData()
})
</script>

<style lang="scss">
.zqy-login-log {
    .zqy-login-log__filters {
        display: flex;
        gap: 8px;

        .el-select {
            width: 132px;
        }
    }
}
</style>

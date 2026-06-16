<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-login-log zqy-seach-table">
        <div class="zqy-table-top">
            <div class="zqy-login-log__filters">
                <el-select v-model="loginMethod" clearable placeholder="全部方式" @change="initData(true)">
                    <el-option label="账号+密码" value="ACCOUNT_PASSWORD" />
                    <el-option label="手机号+密码" value="PHONE_PASSWORD" />
                    <el-option label="邮箱+密码" value="EMAIL_PASSWORD" />
                    <el-option label="短信验证码" value="PHONE_CODE" />
                    <el-option label="邮箱验证码" value="EMAIL_CODE" />
                </el-select>
                <el-select v-model="loginStatus" clearable placeholder="全部结果" @change="initData(true)">
                    <el-option label="成功" value="SUCCESS" />
                    <el-option label="失败" value="FAIL" />
                </el-select>
            </div>
            <div class="zqy-seach">
                <el-input
                    v-model="keyword"
                    placeholder="请输入账号/IP/设备/失败原因 回车搜索"
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
                        <el-tag v-if="scopeSlot.row.loginStatus === 'SUCCESS'" type="success">成功</el-tag>
                        <el-tag v-else type="danger">失败</el-tag>
                    </template>
                    <template #registered="scopeSlot">
                        <el-tag v-if="scopeSlot.row.registered" type="success">是</el-tag>
                        <el-tag v-else>否</el-tag>
                    </template>
                </BlockTable>
            </div>
        </LoadingPage>
    </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'

import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import BlockTable from '@/app/components/block-table/index.vue'
import LoadingPage from '@/app/components/loading/index.vue'
import { PageLoginLog } from '@/app/management/login-method/api'
import { BreadCrumbList, TableConfig } from './login-log.config'

const keyword = ref('')
const loginMethod = ref('')
const loginStatus = ref('')
const loading = ref(false)
const networkError = ref(false)

const breadCrumbList = reactive(BreadCrumbList)
const tableConfig: any = reactive(TableConfig)

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
        ACCOUNT_PASSWORD: '账号+密码',
        PHONE_PASSWORD: '手机号+密码',
        EMAIL_PASSWORD: '邮箱+密码',
        PHONE_CODE: '短信验证码',
        EMAIL_CODE: '邮箱验证码'
    }
    return methodText[method] || method
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

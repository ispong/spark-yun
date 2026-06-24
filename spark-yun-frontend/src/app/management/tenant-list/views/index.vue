<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-seach-table tenant-list-page">
        <div class="zqy-table-top">
            <el-button type="primary" @click="addData">{{ t('tenantList.addTenant') }}</el-button>
            <div class="zqy-seach">
                <el-input
                    v-model="keyword"
                    :placeholder="t('tenantList.searchPlaceholder')"
                    :maxlength="200"
                    clearable
                    @input="inputEvent"
                    @keyup.enter="initData(false)"
                />
            </div>
            <Transition name="tenant-batch-slide">
                <div v-if="selectedRows.length" class="tenant-batch-mask">
                    <div class="tenant-batch-actions">
                        <el-button class="tenant-batch-action" :loading="batchLoading" @click="batchEnableTenants">
                            {{ t('tenantList.enable') }}
                        </el-button>
                        <el-button class="tenant-batch-action" :loading="batchLoading" @click="batchDisableTenants">
                            {{ t('tenantList.disable') }}
                        </el-button>
                        <el-button class="tenant-batch-action" :loading="batchLoading" @click="batchCheckTenants">
                            {{ t('tenantList.check') }}
                        </el-button>
                        <el-button class="tenant-batch-action" :loading="batchLoading" @click="batchDeleteTenants">
                            {{ t('tenantList.delete') }}
                        </el-button>
                        <el-button class="tenant-batch-cancel" :disabled="batchLoading" @click="cancelSelection">
                            {{ t('tenantList.cancelSelection') }}
                        </el-button>
                    </div>
                </div>
            </Transition>
        </div>
        <LoadingPage :visible="loading" :network-error="networkError" @loading-refresh="initData(true)">
            <div class="zqy-table">
                <BlockTable
                    :table-config="tableConfig"
                    @size-change="handleSizeChange"
                    @current-change="handleCurrentChange"
                    @checkbox-change="handleSelectionChange"
                >
                    <template #name="scopeSlot">
                        <span class="name-click" @click="editData(scopeSlot.row)">{{ scopeSlot.row.name }}</span>
                    </template>
                    <template #memberProgress="scopeSlot">
                        <div class="resource-progress">
                            <el-progress
                                :percentage="
                                    getUsagePercentage(scopeSlot.row.usedMemberNum, scopeSlot.row.maxMemberNum)
                                "
                                :color="getPercentColor()"
                                :show-text="false"
                                :stroke-width="12"
                            />
                            <span class="resource-progress__value">
                                {{ getUsageText(scopeSlot.row.usedMemberNum, scopeSlot.row.maxMemberNum) }}
                            </span>
                        </div>
                    </template>
                    <template #workflowProgress="scopeSlot">
                        <div class="resource-progress">
                            <el-progress
                                :percentage="
                                    getUsagePercentage(scopeSlot.row.usedWorkflowNum, scopeSlot.row.maxWorkflowNum)
                                "
                                :color="getPercentColor()"
                                :show-text="false"
                                :stroke-width="12"
                            />
                            <span class="resource-progress__value">
                                {{ getUsageText(scopeSlot.row.usedWorkflowNum, scopeSlot.row.maxWorkflowNum) }}
                            </span>
                        </div>
                    </template>
                    <template #statusTag="scopeSlot">
                        <el-tag v-if="scopeSlot.row.status === 'ENABLE'" type="success">
                            {{ t('tenantList.enable') }}
                        </el-tag>
                        <el-tag v-if="scopeSlot.row.status === 'DISABLE'" type="danger">
                            {{ t('tenantList.disable') }}
                        </el-tag>
                    </template>
                    <template #options="scopeSlot">
                        <div class="btn-group tenant-action-group">
                            <span class="tenant-action-button" @click="editData(scopeSlot.row)">
                                {{ t('tenantList.edit') }}
                            </span>
                            <el-dropdown trigger="click" popper-class="tenant-action-dropdown">
                                <span class="click-show-more tenant-action-button">{{ t('common.more') }}</span>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item
                                            :disabled="scopeSlot.row.statusLoading"
                                            @click="
                                                !scopeSlot.row.statusLoading &&
                                                    changeStatus(scopeSlot.row, scopeSlot.row.status !== 'ENABLE')
                                            "
                                        >
                                            <span v-if="!scopeSlot.row.statusLoading">
                                                {{
                                                    scopeSlot.row.status === 'ENABLE'
                                                        ? t('tenantList.disable')
                                                        : t('tenantList.enable')
                                                }}
                                            </span>
                                            <el-icon v-else class="is-loading">
                                                <Loading />
                                            </el-icon>
                                        </el-dropdown-item>
                                        <el-dropdown-item
                                            v-if="!scopeSlot.row.checkLoding"
                                            @click="checkTenant(scopeSlot.row)"
                                        >
                                            {{ t('tenantList.check') }}
                                        </el-dropdown-item>
                                        <el-dropdown-item @click="deleteData(scopeSlot.row)">
                                            {{ t('tenantList.delete') }}
                                        </el-dropdown-item>
                                    </el-dropdown-menu>
                                </template>
                            </el-dropdown>
                        </div>
                    </template>
                </BlockTable>
            </div>
        </LoadingPage>
        <AddModal ref="addModalRef" />
    </div>
</template>

<script lang="ts" setup>
import { reactive, ref, onMounted, watch } from 'vue'
import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import BlockTable from '@/app/components/block-table/index.vue'
import LoadingPage from '@/app/components/loading/index.vue'
import AddModal from './add-modal/index.vue'

import { createBreadCrumbList, createColConfigs, createTableConfig } from './tenant-list.config'
import {
    GetTenantList,
    AddTenantData,
    DeleteTenantData,
    CheckTenantData,
    DisableTenantData,
    EnableTenantData,
    UpdateTenantData,
    ReplaceTenantAdminData
} from '@/app/management/tenant-list/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import eventBus from '@/app/utils/eventBus'
import { useAuthStore } from '@/app/store/useAuth'
import { useI18n } from 'vue-i18n'

interface FormTenant {
    adminUserId?: string
    id?: string
    maxMemberNum: string
    maxWorkflowNum: string
    name: string
    remark: string
    validDateTime?: string[]
}

const authStore = useAuthStore()
const { t, locale } = useI18n()

// const state = useState(['tenantId'], 'authStoreModule')
// const mutations = useMutations(['setTenantId'], 'authStoreModule')

const keyword = ref('')
const loading = ref(false)
const networkError = ref(false)
const selectedRows = ref<any[]>([])
const batchLoading = ref(false)
const addModalRef = ref(null)
const breadCrumbList = reactive(createBreadCrumbList(t))
const tableConfig: any = reactive(createTableConfig(t))

watch(locale, () => {
    const nextBreadCrumbList = createBreadCrumbList(t)
    breadCrumbList.splice(0, breadCrumbList.length, ...nextBreadCrumbList)
    tableConfig.colConfigs = createColConfigs(t)
})

function initData(tableLoading?: boolean) {
    loading.value = tableLoading ? false : true
    networkError.value = networkError.value || false
    GetTenantList({
        page: tableConfig.pagination.currentPage - 1,
        pageSize: tableConfig.pagination.pageSize,
        searchKeyWord: keyword.value
    })
        .then((res: any) => {
            tableConfig.tableData = res.data.content
            tableConfig.pagination.total = res.data.totalElements
            selectedRows.value = []
            loading.value = false
            tableConfig.loading = false
            networkError.value = false
            if (!authStore.tenantId && tableConfig.tableData.length === 1) {
                authStore.setTenantId(tableConfig.tableData[0].id)
            }
        })
        .catch(() => {
            tableConfig.tableData = []
            tableConfig.pagination.total = 0
            loading.value = false
            tableConfig.loading = false
            networkError.value = true
        })
}

function addData() {
    addModalRef.value.showModal((formData: FormTenant) => {
        return new Promise((resolve: any, reject: any) => {
            AddTenantData(formData)
                .then((res: any) => {
                    ElMessage.success(res.msg)
                    // 这里发送eventbus，刷新当前打开的页面
                    eventBus.emit('tenantListUpdate')

                    initData()
                    resolve()
                })
                .catch((error: any) => {
                    reject(error)
                })
        })
    })
}

function editData(data: any) {
    addModalRef.value.showModal((formData: FormTenant) => {
        const updateTenantFormData = {
            id: formData.id,
            maxMemberNum: formData.maxMemberNum,
            maxWorkflowNum: formData.maxWorkflowNum,
            name: formData.name,
            remark: formData.remark,
            validDateTime: formData.validDateTime
        }
        const adminChanged = !!formData.adminUserId && formData.adminUserId !== data.adminUserId

        return new Promise((resolve: any, reject: any) => {
            UpdateTenantData(updateTenantFormData)
                .then((res: any) => {
                    if (!adminChanged) {
                        ElMessage.success(res.msg)
                        initData()
                        resolve()
                        return
                    }

                    ReplaceTenantAdminData({
                        tenantId: data.id,
                        newAdminUserId: formData.adminUserId as string,
                        oldAdminAction: 'KEEP'
                    })
                        .then((replaceRes: any) => {
                            ElMessage.success(replaceRes?.msg || res.msg)
                            initData()
                            resolve()
                        })
                        .catch((error: any) => {
                            reject(error)
                        })
                })
                .catch((error: any) => {
                    reject(error)
                })
        })
    }, data)
}

function checkTenant(data: any) {
    data.checkLoding = true
    CheckTenantData({
        tenantId: data.id
    })
        .then((res: any) => {
            data.checkLoding = false
            ElMessage.success(res.msg)
            initData(true)
        })
        .catch(() => {
            data.checkLoding = false
        })
}

function handleSelectionChange(records: any[]) {
    selectedRows.value = records || []
}

function cancelSelection() {
    selectedRows.value = []
    tableConfig.tableData = [...tableConfig.tableData]
}

function batchEnableTenants() {
    const disableRows = selectedRows.value.filter((row: any) => row.status === 'DISABLE')
    if (!disableRows.length) {
        ElMessage.warning(t('tenantList.selectDisabledTenants'))
        return
    }

    batchLoading.value = true
    Promise.all(disableRows.map((row: any) => EnableTenantData({ tenantId: row.id })))
        .then(() => {
            ElMessage.success(t('tenantList.batchEnableSuccess'))
            initData(true)
        })
        .catch(() => {})
        .finally(() => {
            batchLoading.value = false
        })
}

function batchDisableTenants() {
    const enableRows = selectedRows.value.filter((row: any) => row.status === 'ENABLE')
    if (!enableRows.length) {
        ElMessage.warning(t('tenantList.selectEnabledTenants'))
        return
    }

    batchLoading.value = true
    Promise.all(enableRows.map((row: any) => DisableTenantData({ tenantId: row.id })))
        .then(() => {
            ElMessage.success(t('tenantList.batchDisableSuccess'))
            initData(true)
        })
        .catch(() => {})
        .finally(() => {
            batchLoading.value = false
        })
}

function batchCheckTenants() {
    if (!selectedRows.value.length) {
        return
    }

    batchLoading.value = true
    Promise.all(selectedRows.value.map((row: any) => CheckTenantData({ tenantId: row.id })))
        .then(() => {
            ElMessage.success(t('tenantList.batchCheckSuccess'))
            initData(true)
        })
        .catch(() => {})
        .finally(() => {
            batchLoading.value = false
        })
}

function batchDeleteTenants() {
    if (!selectedRows.value.length) {
        return
    }

    ElMessageBox.confirm(t('tenantList.deleteSelectedConfirm', { count: selectedRows.value.length }), t('common.warning'), {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning'
    }).then(() => {
        batchLoading.value = true
        Promise.all(
            selectedRows.value.map((row: any) =>
                DeleteTenantData({
                    tenantId: row.id,
                    tenantName: row.name
                })
            )
        )
            .then(() => {
                ElMessage.success(t('tenantList.batchDeleteSuccess'))
                initData()
            })
            .catch(() => {})
            .finally(() => {
                batchLoading.value = false
            })
    })
}

function changeStatus(data: any, status: boolean) {
    data.statusLoading = true
    if (status) {
        EnableTenantData({
            tenantId: data.id
        })
            .then((res: any) => {
                ElMessage.success(res.msg)
                data.statusLoading = false
                initData(true)
            })
            .catch(() => {
                data.statusLoading = false
            })
    } else {
        DisableTenantData({
            tenantId: data.id
        })
            .then((res: any) => {
                ElMessage.success(res.msg)
                data.statusLoading = false
                initData(true)
            })
            .catch(() => {
                data.statusLoading = false
            })
    }
}

function deleteData(data: any) {
    ElMessageBox.prompt(t('tenantList.deleteTenantPrompt', { name: data.name }), t('common.warning'), {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        inputPattern: new RegExp(`^${data.name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`),
        inputErrorMessage: t('tenantList.tenantNameMismatch'),
        type: 'warning'
    }).then(({ value }) => {
        DeleteTenantData({
            tenantId: data.id,
            tenantName: value
        })
            .then((res: any) => {
                ElMessage.success(res.msg)
                initData()
            })
            .catch(() => {})
    })
}

function inputEvent(e: string) {
    if (e === '') {
        initData()
    }
}

function handleSizeChange(e: number) {
    tableConfig.pagination.pageSize = e
    initData()
}

function handleCurrentChange(e: number) {
    tableConfig.pagination.currentPage = e
    initData()
}

function getUsagePercentage(usedValue: number | string, totalValue: number | string) {
    const used = Number(usedValue) || 0
    const total = Number(totalValue) || 0
    if (total <= 0) {
        return used > 0 ? 100 : 0
    }
    return Math.min(100, Number(((used / total) * 100).toFixed(2)))
}

function getUsageText(usedValue: number | string, totalValue: number | string) {
    const used = Number(usedValue) || 0
    const total = Number(totalValue) || 0
    return `${used} / ${total}`
}

function getPercentColor(): string {
    return 'var(--el-color-primary-light-3)'
}

onMounted(() => {
    tableConfig.pagination.currentPage = 1
    tableConfig.pagination.pageSize = 10
    initData()
})
</script>

<style lang="scss">
.zqy-seach-table.tenant-list-page {
    .zqy-table-top {
        position: relative;
        overflow: hidden;
        .tenant-batch-mask {
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
        .tenant-batch-slide-enter-active,
        .tenant-batch-slide-leave-active {
            transition:
                transform 0.18s ease,
                opacity 0.18s ease;
            will-change: transform, opacity;
        }
        .tenant-batch-slide-enter-from,
        .tenant-batch-slide-leave-to {
            opacity: 0;
            transform: translateY(-100%);
        }
        .tenant-batch-slide-enter-to,
        .tenant-batch-slide-leave-from {
            opacity: 1;
            transform: translateY(0);
        }
    }
    .tenant-batch-actions {
        display: flex;
        align-items: center;
        gap: 10px;
        .tenant-batch-action {
            min-width: 66px;
            height: 32px;
            line-height: 30px;
            border-color: getCssVar('color', 'primary');
            color: getCssVar('color', 'primary');
            background-color: #fff;
            &:hover,
            &:focus {
                border-color: getCssVar('color', 'primary');
                color: #fff;
                background-color: getCssVar('color', 'primary');
            }
        }
        .tenant-batch-cancel {
            min-width: 74px;
            height: 32px;
            line-height: 30px;
            border-color: getCssVar('border-color');
            color: getCssVar('text-color', 'regular');
            background-color: #fff;
            &:hover,
            &:focus {
                border-color: getCssVar('border-color');
                color: getCssVar('text-color', 'regular');
                background-color: #fff;
            }
        }
    }
    .resource-progress {
        min-width: 120px;
        padding-right: 6px;
        display: flex;
        align-items: center;
        gap: 8px;

        .el-progress {
            flex: 1;
        }
    }

    .resource-progress__value {
        color: getCssVar('text-color', 'secondary');
        white-space: nowrap;
        font-size: getCssVar('font-size', 'extra-small');
    }

    .tenant-action-group {
        justify-content: center;
        gap: 16px;
        .tenant-action-button {
            display: inline-flex;
            align-items: center;
            line-height: 1;
            font-size: getCssVar('font-size', 'extra-small');
        }
    }
}

.tenant-action-dropdown {
    .el-dropdown-menu {
        padding: 4px 0;
    }
    .el-dropdown-menu__item {
        height: 26px;
        line-height: 26px;
        font-family: Avenir, Helvetica, Arial, sans-serif;
        font-size: getCssVar('font-size', 'extra-small');
    }
}
</style>

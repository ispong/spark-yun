<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-seach-table tenant-list-page">
        <div class="zqy-table-top">
            <el-button type="primary" @click="addData">新建租户</el-button>
            <div class="zqy-seach">
                <el-input
                    v-model="keyword"
                    placeholder="请输入租户名 回车进行搜索"
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
                            启用
                        </el-button>
                        <el-button class="tenant-batch-action" :loading="batchLoading" @click="batchDisableTenants">
                            禁用
                        </el-button>
                        <el-button class="tenant-batch-action" :loading="batchLoading" @click="batchCheckTenants">
                            检测
                        </el-button>
                        <el-button class="tenant-batch-action" :loading="batchLoading" @click="batchDeleteTenants">
                            删除
                        </el-button>
                        <el-button class="tenant-batch-cancel" :disabled="batchLoading" @click="cancelSelection">
                            取消选择
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
                        <el-tag v-if="scopeSlot.row.status === 'ENABLE'" type="success">启用</el-tag>
                        <el-tag v-if="scopeSlot.row.status === 'DISABLE'" type="danger">禁用</el-tag>
                    </template>
                    <template #options="scopeSlot">
                        <div class="btn-group tenant-action-group">
                            <span class="tenant-action-button" @click="editData(scopeSlot.row)">编辑</span>
                            <el-dropdown trigger="click" popper-class="tenant-action-dropdown">
                                <span class="click-show-more tenant-action-button">更多</span>
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
                                                {{ scopeSlot.row.status === 'ENABLE' ? '禁用' : '启用' }}
                                            </span>
                                            <el-icon v-else class="is-loading">
                                                <Loading />
                                            </el-icon>
                                        </el-dropdown-item>
                                        <el-dropdown-item
                                            v-if="!scopeSlot.row.checkLoding"
                                            @click="checkTenant(scopeSlot.row)"
                                        >
                                            检测
                                        </el-dropdown-item>
                                        <el-dropdown-item @click="deleteData(scopeSlot.row)">删除</el-dropdown-item>
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
import { reactive, ref, onMounted } from 'vue'
import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import BlockTable from '@/app/components/block-table/index.vue'
import LoadingPage from '@/app/components/loading/index.vue'
import AddModal from './add-modal/index.vue'

import { BreadCrumbList, TableConfig } from './tenant-list.config'
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

// const state = useState(['tenantId'], 'authStoreModule')
// const mutations = useMutations(['setTenantId'], 'authStoreModule')

const keyword = ref('')
const loading = ref(false)
const networkError = ref(false)
const selectedRows = ref<any[]>([])
const batchLoading = ref(false)
const addModalRef = ref(null)
const breadCrumbList = reactive(BreadCrumbList)
const tableConfig: any = reactive(TableConfig)

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
        ElMessage.warning('请选择禁用状态的租户')
        return
    }

    batchLoading.value = true
    Promise.all(disableRows.map((row: any) => EnableTenantData({ tenantId: row.id })))
        .then(() => {
            ElMessage.success('批量启用成功')
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
        ElMessage.warning('请选择启用状态的租户')
        return
    }

    batchLoading.value = true
    Promise.all(enableRows.map((row: any) => DisableTenantData({ tenantId: row.id })))
        .then(() => {
            ElMessage.success('批量禁用成功')
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
            ElMessage.success('批量检测成功')
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

    ElMessageBox.confirm(`确定删除选中的 ${selectedRows.value.length} 个租户吗？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
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
                ElMessage.success('批量删除成功')
                initData()
            })
            .catch(() => {})
            .finally(() => {
                batchLoading.value = false
            })
    })
}

// 启用 or 禁用
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

// 删除
function deleteData(data: any) {
    ElMessageBox.prompt(`请输入租户名称“${data.name}”确认删除`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: new RegExp(`^${data.name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`),
        inputErrorMessage: '租户名称不一致',
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

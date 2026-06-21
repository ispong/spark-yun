<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-seach-table workflow-page-list">
        <div class="zqy-table-top">
            <el-button type="primary" @click="addGroup">新建作业流</el-button>
            <div class="zqy-seach">
                <el-input
                    v-model="keyword"
                    placeholder="请输入名称/备注 回车进行搜索"
                    :maxlength="200"
                    clearable
                    @input="inputEvent"
                    @keyup.enter="initData(false)"
                />
            </div>
            <Transition name="workflow-batch-slide">
                <div v-if="selectedRows.length" class="workflow-batch-mask">
                    <div class="workflow-batch-actions">
                        <el-button class="workflow-batch-action" :loading="batchLoading" @click="batchPublishWorkflows">
                            发布
                        </el-button>
                        <el-button class="workflow-batch-action" :loading="batchLoading" @click="batchUnderlineWorkflows">
                            下线
                        </el-button>
                        <el-button class="workflow-batch-action" :loading="batchLoading" @click="batchDeleteData">
                            删除
                        </el-button>
                        <el-button class="workflow-batch-cancel" :disabled="batchLoading" @click="cancelSelection">
                            取消选择
                        </el-button>
                    </div>
                </div>
            </Transition>
        </div>
        <LoadingPage :visible="loading" :network-error="networkError" @loading-refresh="initData(false)">
            <div class="zqy-table">
                <BlockTable
                    :table-config="tableConfig"
                    @size-change="handleSizeChange"
                    @current-change="handleCurrentChange"
                    @checkbox-change="handleSelectionChange"
                >
                    <template #nameSlot="scopeSlot">
                        <span class="name-click" @click.stop="showDetail(scopeSlot.row)">{{ scopeSlot.row.name }}</span>
                    </template>
                    <template #statusTag="scopeSlot">
                        <ZStatusTag :status="scopeSlot.row.status === 'STOP' ? 'UN_PUBLISHED' : scopeSlot.row.status" />
                    </template>
                    <template #options="scopeSlot">
                        <div class="btn-group workflow-action-group">
                            <span class="workflow-action-button" @click="editData(scopeSlot.row)">编辑</span>
                            <el-dropdown trigger="click" popper-class="workflow-action-dropdown">
                                <span class="click-show-more workflow-action-button">更多</span>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item
                                            v-if="canUnderlineWorkflow(scopeSlot.row)"
                                            @click="underlineWorkFlow(scopeSlot.row)"
                                        >
                                            下线
                                        </el-dropdown-item>
                                        <el-dropdown-item v-else @click="publishWorkFlow(scopeSlot.row)">发布</el-dropdown-item>
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

import { BreadCrumbList, TableConfig, FormData } from './workflow.config'
import {
    GetWorkflowList,
    AddWorkflowData,
    UpdateWorkflowData,
    DeleteWorkflowData,
    UnderlineWorkflowData,
    PublishWorkflowData
} from '@/modules/workflow/api'
import { CheckLicenseStatus } from '@/app/shared/api/license'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/app/store/useAuth'

const router = useRouter()

const authStore = useAuthStore()
// const state = useState(['tenantId' ], 'authStoreModule')

const breadCrumbList = reactive(BreadCrumbList)
const tableConfig: any = reactive(TableConfig)
const keyword = ref('')
const loading = ref(false)
const networkError = ref(false)
const selectedRows = ref<any[]>([])
const batchLoading = ref(false)
const addModalRef = ref(null)

function refreshLicenseAndReload() {
    CheckLicenseStatus()
        .catch(() => {
            // 发布/下线后仅做许可证状态刷新，失败也继续刷新页面
        })
        .finally(() => {
            window.location.reload()
        })
}

function initData(tableLoading?: boolean) {
    loading.value = tableLoading ? false : true
    networkError.value = networkError.value || false
    GetWorkflowList({
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
        })
        .catch(() => {
            tableConfig.tableData = []
            tableConfig.pagination.total = 0
            selectedRows.value = []
            loading.value = false
            tableConfig.loading = false
            networkError.value = true
        })
}

function addGroup() {
    addModalRef.value.showModal((formData: FormData) => {
        return new Promise((resolve: any, reject: any) => {
            AddWorkflowData(formData)
                .then((res: any) => {
                    ElMessage.success(res.msg)
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
    addModalRef.value.showModal((formData: FormData) => {
        return new Promise((resolve: any, reject: any) => {
            UpdateWorkflowData(formData)
                .then((res: any) => {
                    ElMessage.success(res.msg)
                    initData()
                    resolve()
                })
                .catch((error: any) => {
                    reject(error)
                })
        })
    }, data)
}

function canPublishWorkflow(data: any) {
    return ['UN_AUTO', 'STOP'].includes(data?.status)
}

function canUnderlineWorkflow(data: any) {
    return !canPublishWorkflow(data)
}

// 下线工作流
function underlineWorkFlow(data: any) {
    UnderlineWorkflowData({
        workflowId: data.id
    })
        .then((res: any) => {
            initData()
            ElMessage({
                type: 'success',
                message: res?.msg || '操作成功',
                onClose: () => {
                    refreshLicenseAndReload()
                }
            })
        })
        .catch(() => {})
}

// 发布作业流
function publishWorkFlow(data: any) {
    PublishWorkflowData({
        workflowId: data.id
    })
        .then((res: any) => {
            initData()
            ElMessage({
                type: 'success',
                message: res?.msg || '操作成功',
                onClose: () => {
                    refreshLicenseAndReload()
                }
            })
        })
        .catch(() => {})
}

// 删除
function deleteData(data: any) {
    ElMessageBox.confirm('确定删除该作业流吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        DeleteWorkflowData({
            workflowId: data.id,
            Tenant: authStore.tenantId
        })
            .then((res: any) => {
                ElMessage.success(res.msg)
                initData()
            })
            .catch(() => {})
    })
}

function handleSelectionChange(records: any[]) {
    selectedRows.value = records || []
}

function cancelSelection() {
    selectedRows.value = []
    tableConfig.tableData = [...tableConfig.tableData]
}

function batchStatusAction(
    rows: any[],
    action: (params: { workflowId: string }) => Promise<any>,
    successMessage: string,
    emptyMessage: string
) {
    if (!rows.length) {
        ElMessage.warning(emptyMessage)
        return
    }

    batchLoading.value = true
    Promise.all(rows.map((row: any) => action({ workflowId: row.id })))
        .then(() => {
            initData()
            ElMessage({
                type: 'success',
                message: successMessage,
                onClose: () => {
                    refreshLicenseAndReload()
                }
            })
        })
        .catch(() => {})
        .finally(() => {
            batchLoading.value = false
        })
}

function batchPublishWorkflows() {
    batchStatusAction(
        selectedRows.value.filter(canPublishWorkflow),
        PublishWorkflowData,
        '批量发布成功',
        '请选择可发布的作业流'
    )
}

function batchUnderlineWorkflows() {
    batchStatusAction(
        selectedRows.value.filter(canUnderlineWorkflow),
        UnderlineWorkflowData,
        '批量下线成功',
        '请选择可下线的作业流'
    )
}

function batchDeleteData() {
    if (!selectedRows.value.length) {
        return
    }

    ElMessageBox.confirm(`确定删除选中的 ${selectedRows.value.length} 个作业流吗？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        batchLoading.value = true
        Promise.all(
            selectedRows.value.map((row: any) =>
                DeleteWorkflowData({
                    workflowId: row.id,
                    Tenant: authStore.tenantId
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

function showDetail(data: any) {
    if (!data?.id) {
        ElMessage.warning('作业流信息不完整')
        return
    }
    router.push({
        name: 'workflow-page',
        query: {
            id: data.id,
            name: data.name
        }
    })
}

function inputEvent(e: string) {
    if (e === '') {
        initData()
    }
}

function handleSizeChange(e: number) {
    tableConfig.pagination.pageSize = e
    tableConfig.pagination.currentPage = 1
    initData()
}

function handleCurrentChange(e: number) {
    tableConfig.pagination.currentPage = e
    initData()
}

onMounted(() => {
    tableConfig.pagination.currentPage = 1
    tableConfig.pagination.pageSize = 10
    initData()
})
</script>

<style lang="scss">
.workflow-page-list {
    .zqy-table-top {
        position: relative;
        overflow: hidden;

        .workflow-batch-mask {
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
    }

    .workflow-batch-actions {
        display: flex;
        align-items: center;
        gap: 10px;

        .workflow-batch-action {
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

        .workflow-batch-cancel {
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

    .workflow-batch-slide-enter-active,
    .workflow-batch-slide-leave-active {
        transition:
            opacity 0.16s ease,
            transform 0.16s ease;
    }

    .workflow-batch-slide-enter-from,
    .workflow-batch-slide-leave-to {
        opacity: 0;
        transform: translateY(-4px);
    }

    .workflow-batch-slide-enter-to,
    .workflow-batch-slide-leave-from {
        opacity: 1;
        transform: translateY(0);
    }

    .name-click {
        cursor: pointer;
        color: getCssVar('color', 'primary', 'light-5');
        &:hover {
            color: getCssVar('color', 'primary');
        }
    }

    .zqy-table {
        .workflow-action-group {
            justify-content: center;
            gap: 16px;

            .workflow-action-button {
                display: inline-flex;
                align-items: center;
                line-height: 1;
                font-size: getCssVar('font-size', 'extra-small');
            }
        }
    }
}

.workflow-action-dropdown {
    .el-dropdown-menu {
        padding: 4px 0;
    }

    .el-dropdown-menu__item {
        height: 26px;
        line-height: 26px;
        font-size: getCssVar('font-size', 'extra-small');
    }
}
</style>

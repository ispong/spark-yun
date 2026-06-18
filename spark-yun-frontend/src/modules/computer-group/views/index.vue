<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-seach-table computer-group-page">
        <div class="zqy-table-top">
            <el-button type="primary" @click="addGroup">新建集群</el-button>
            <div class="zqy-seach">
                <el-input
                    v-model="keyword"
                    placeholder="请输入集群名称/备注 回车进行搜索"
                    :maxlength="200"
                    clearable
                    @input="inputEvent"
                    @keyup.enter="initData(false)"
                />
            </div>
            <Transition name="cluster-batch-slide">
                <div v-if="selectedRows.length" class="cluster-batch-mask">
                    <div class="cluster-batch-actions">
                        <el-button class="cluster-batch-action" :loading="batchLoading" @click="batchCheckData">
                            检测
                        </el-button>
                        <el-button class="cluster-batch-action" :loading="batchLoading" @click="batchDeleteData">
                            删除
                        </el-button>
                        <el-button class="cluster-batch-cancel" :disabled="batchLoading" @click="cancelSelection">
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
                        <span class="name-click" @click="showDetail(scopeSlot.row)">{{ scopeSlot.row.name }}</span>
                    </template>
                    <template #clusterTypeSlot="scopeSlot">
                        <el-tag class="cluster-type-tag">{{ getClusterTypeName(scopeSlot.row.clusterType) }}</el-tag>
                    </template>
                    <template #memorySlot="scopeSlot">
                        <div class="resource-progress">
                            <el-progress
                                :percentage="getPercentFromRatio(scopeSlot.row.memory)"
                                :color="getPercentColor()"
                                :stroke-width="12"
                                :show-text="false"
                            />
                            <span class="resource-progress__value">{{ getDisplayValue(scopeSlot.row.memory) }}</span>
                        </div>
                    </template>
                    <template #storageSlot="scopeSlot">
                        <div class="resource-progress">
                            <el-progress
                                :percentage="getPercentFromRatio(scopeSlot.row.storage)"
                                :color="getPercentColor()"
                                :stroke-width="12"
                                :show-text="false"
                            />
                            <span class="resource-progress__value">{{ getDisplayValue(scopeSlot.row.storage) }}</span>
                        </div>
                    </template>
                    <template #statusTag="scopeSlot">
                        <ZStatusTag :status="scopeSlot.row.status" />
                    </template>
                    <template #defaultTag="scopeSlot">
                        <el-tag v-if="scopeSlot.row.defaultCluster" type="warning">默认集群</el-tag>
                        <el-tag v-else class="normal-cluster-tag">普通集群</el-tag>
                    </template>
                    <template #options="scopeSlot">
                        <div class="btn-group cluster-action-group">
                            <span class="cluster-action-button" @click="editData(scopeSlot.row)">编辑</span>
                            <el-dropdown trigger="click" popper-class="cluster-action-dropdown">
                                <span class="click-show-more cluster-action-button">更多</span>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item
                                            :disabled="scopeSlot.row.checkLoading"
                                            @click="!scopeSlot.row.checkLoading && checkData(scopeSlot.row)"
                                        >
                                            <span v-if="!scopeSlot.row.checkLoading">检测</span>
                                            <el-icon v-else class="is-loading">
                                                <Loading />
                                            </el-icon>
                                        </el-dropdown-item>
                                        <el-dropdown-item @click="setDefaultNode(scopeSlot.row)">默认</el-dropdown-item>
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

import { BreadCrumbList, TableConfig, FormData } from './computer-group.config'
import {
    GetComputerGroupList,
    AddComputerGroupData,
    UpdateComputerGroupData,
    CheckComputerGroupData,
    DeleteComputerGroupData,
    SetDefaultComputerGroup
} from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'

const router = useRouter()
const breadCrumbList = reactive(BreadCrumbList)
const tableConfig: any = reactive(TableConfig)
const keyword = ref('')
const loading = ref(false)
const networkError = ref(false)
const selectedRows = ref<any[]>([])
const batchLoading = ref(false)
const addModalRef = ref(null)

function normalizePercent(value: number): number {
    if (!Number.isFinite(value)) {
        return 0
    }
    return Math.max(0, Math.min(100, Math.round(value)))
}

function getPercentFromRatio(valueText: string): number {
    if (!valueText) {
        return 0
    }
    const ratioMatch = String(valueText)
        .replace(/\s/g, '')
        .match(/^([\d.]+)[a-zA-Z]*\/([\d.]+)[a-zA-Z]*$/)
    if (!ratioMatch) {
        return 0
    }
    const used = Number(ratioMatch[1])
    const total = Number(ratioMatch[2])
    if (!Number.isFinite(used) || !Number.isFinite(total) || total <= 0) {
        return 0
    }
    return normalizePercent((used / total) * 100)
}

function getPercentColor(): string {
    return 'var(--el-color-primary-light-3)'
}

function getDisplayValue(valueText: string): string {
    if (!valueText) {
        return '--'
    }
    return String(valueText)
}

function getClusterTypeName(clusterType: string): string {
    const clusterTypeMap: Record<string, string> = {
        standalone: 'Standalone',
        yarn: 'Yarn',
        kubernetes: 'Kubernetes'
    }
    return clusterTypeMap[clusterType] || clusterType || '--'
}

function initData(tableLoading?: boolean) {
    loading.value = tableLoading ? false : true
    networkError.value = networkError.value || false
    GetComputerGroupList({
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
            AddComputerGroupData(formData)
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
            UpdateComputerGroupData(formData)
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

// 检测
function checkData(data: any) {
    data.checkLoading = true
    CheckComputerGroupData({
        engineId: data.id
    })
        .then((res: any) => {
            data.checkLoading = false
            ElMessage.success(res.msg)
            initData()
        })
        .catch(() => {
            data.checkLoading = false
        })
}

function handleSelectionChange(records: any[]) {
    selectedRows.value = records || []
}

function cancelSelection() {
    selectedRows.value = []
    tableConfig.tableData = [...tableConfig.tableData]
}

function batchCheckData() {
    if (!selectedRows.value.length) {
        return
    }

    batchLoading.value = true
    Promise.all(
        selectedRows.value.map((row: any) =>
            CheckComputerGroupData({
                engineId: row.id
            })
        )
    )
        .then(() => {
            ElMessage.success('批量检测成功')
            initData(true)
        })
        .catch(() => {})
        .finally(() => {
            batchLoading.value = false
        })
}

function batchDeleteData() {
    if (!selectedRows.value.length) {
        return
    }

    ElMessageBox.confirm(`确定删除选中的 ${selectedRows.value.length} 个集群吗？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        batchLoading.value = true
        Promise.all(
            selectedRows.value.map((row: any) =>
                DeleteComputerGroupData({
                    engineId: row.id
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

// 查看节点
function showPointDetail(data: any) {
    router.push({
        name: 'computer-pointer',
        query: {
            id: data.id
        }
    })
}

// 设置默认节点
function setDefaultNode(data: any) {
    SetDefaultComputerGroup({
        clusterId: data.id
    })
        .then((res: any) => {
            ElMessage.success(res.msg)
            initData(true)
        })
        .catch(() => {})
}

// 删除
function deleteData(data: any) {
    ElMessageBox.confirm('确定删除该集群吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        DeleteComputerGroupData({
            engineId: data.id
        })
            .then((res: any) => {
                ElMessage.success(res.msg)
                initData()
            })
            .catch(() => {})
    })
}

function showDetail(data: any) {
    router.push({
        name: 'computer-pointer',
        query: {
            id: data.id,
            type: data.clusterType
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
    initData(true)
}

onMounted(() => {
    tableConfig.pagination.currentPage = 1
    tableConfig.pagination.pageSize = 10
    initData()
})
</script>

<style lang="scss">
.zqy-seach-table.computer-group-page {
    .zqy-table-top {
        position: relative;
        overflow: hidden;
    }

    .cluster-batch-mask {
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

    .cluster-batch-actions {
        display: flex;
        align-items: center;
        gap: 10px;

        .cluster-batch-action {
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

        .cluster-batch-cancel {
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

    .cluster-batch-slide-enter-active,
    .cluster-batch-slide-leave-active {
        transition:
            transform 0.18s ease,
            opacity 0.18s ease;
        will-change: transform, opacity;
    }

    .cluster-batch-slide-enter-from,
    .cluster-batch-slide-leave-to {
        opacity: 0;
        transform: translateY(-100%);
    }

    .cluster-batch-slide-enter-to,
    .cluster-batch-slide-leave-from {
        opacity: 1;
        transform: translateY(0);
    }

    .cluster-type-tag,
    .normal-cluster-tag {
        max-width: 100%;
        color: getCssVar('text-color', 'regular');
        border-color: getCssVar('border-color', 'light');
        background-color: getCssVar('fill-color', 'lighter');
        white-space: nowrap;
    }

    .cluster-action-group {
        justify-content: center;
        gap: 16px;

        .cluster-action-button {
            display: inline-flex;
            align-items: center;
            line-height: 1;
            font-size: getCssVar('font-size', 'extra-small');
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
}

.cluster-action-dropdown {
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

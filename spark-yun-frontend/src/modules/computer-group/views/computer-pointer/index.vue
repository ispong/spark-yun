<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-seach-table zqy-computer-node">
        <div class="zqy-table-top">
            <el-button type="primary" @click="addData">添加节点</el-button>
            <div class="zqy-seach">
                <el-input
                    v-model="keyword"
                    placeholder="请输入节点名称/地址/备注 回车进行搜索"
                    :maxlength="200"
                    clearable
                    @input="inputEvent"
                    @keyup.enter="initData(false)"
                />
            </div>
            <Transition name="node-batch-slide">
                <div v-if="selectedRows.length" class="node-batch-mask">
                    <div class="node-batch-actions">
                        <el-button class="node-batch-action" :loading="batchLoading" @click="batchCheckData">
                            检测
                        </el-button>
                        <el-button class="node-batch-action" :loading="batchLoading" @click="batchDeleteData">
                            删除
                        </el-button>
                        <el-button class="node-batch-cancel" :disabled="batchLoading" @click="cancelSelection">
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
                        <span class="name-click" @click="editNodeData(scopeSlot.row)">{{ scopeSlot.row.name }}</span>
                    </template>
                    <template #connectTypeSlot="scopeSlot">
                        <el-tag>{{ getConnectTypeName(scopeSlot.row.connectType) }}</el-tag>
                    </template>
                    <template #cpuSlot="scopeSlot">
                        <div class="resource-progress">
                            <el-progress
                                :percentage="getPercentFromCpu(scopeSlot.row.cpu)"
                                :color="getPercentColor(getPercentFromCpu(scopeSlot.row.cpu))"
                                :stroke-width="12"
                                :show-text="false"
                            />
                            <span class="resource-progress__value">{{ getCpuDisplay(scopeSlot.row.cpu) }}</span>
                        </div>
                    </template>
                    <template #memorySlot="scopeSlot">
                        <div class="resource-progress">
                            <el-progress
                                :percentage="getPercentFromUsage(scopeSlot.row.memory)"
                                :color="getPercentColor(getPercentFromUsage(scopeSlot.row.memory))"
                                :stroke-width="12"
                                :show-text="false"
                            />
                            <span class="resource-progress__value">{{ getUsageDisplay(scopeSlot.row.memory) }}</span>
                        </div>
                    </template>
                    <template #storageSlot="scopeSlot">
                        <div class="resource-progress">
                            <el-progress
                                :percentage="getPercentFromUsage(scopeSlot.row.storage)"
                                :color="getPercentColor(getPercentFromUsage(scopeSlot.row.storage))"
                                :stroke-width="12"
                                :show-text="false"
                            />
                            <span class="resource-progress__value">{{ getUsageDisplay(scopeSlot.row.storage) }}</span>
                        </div>
                    </template>
                    <template #statusTag="scopeSlot">
                        <ZStatusTag :status="scopeSlot.row.status" />
                    </template>
                    <template #defaultNodeTag="scopeSlot">
                        <div class="btn-group">
                            <el-tag v-if="scopeSlot.row.defaultClusterNode" class="ml-2" type="success">是</el-tag>
                            <el-tag v-if="!scopeSlot.row.defaultClusterNode" class="ml-2" type="danger">否</el-tag>
                        </div>
                    </template>
                    <template #options="scopeSlot">
                        <div class="btn-group node-action-group">
                            <span class="node-action-button" @click="editNodeData(scopeSlot.row)">编辑</span>
                            <el-dropdown trigger="click" popper-class="node-action-dropdown">
                                <span class="click-show-more node-action-button">更多</span>
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
                                        <el-dropdown-item @click="showLog(scopeSlot.row)">日志</el-dropdown-item>
                                        <el-dropdown-item
                                            v-if="isSshNode(scopeSlot.row) && scopeSlot.row.status === 'RUNNING'"
                                            @click="stopAgent(scopeSlot.row)"
                                        >
                                            停止
                                        </el-dropdown-item>
                                        <el-dropdown-item
                                            v-if="isSshNode(scopeSlot.row) && scopeSlot.row.status === 'STOP'"
                                            @click="startAgent(scopeSlot.row)"
                                        >
                                            激活
                                        </el-dropdown-item>
                                        <el-dropdown-item
                                            v-if="
                                                isSshNode(scopeSlot.row) &&
                                                (scopeSlot.row.status === 'UN_INSTALL' ||
                                                    scopeSlot.row.status === 'INSTALL_ERROR')
                                            "
                                            @click="installData(scopeSlot.row)"
                                        >
                                            安装
                                        </el-dropdown-item>
                                        <el-dropdown-item v-if="isSshNode(scopeSlot.row)" @click="uninstallData(scopeSlot.row)">
                                            卸载
                                        </el-dropdown-item>
                                        <el-dropdown-item v-if="isSshNode(scopeSlot.row)" @click="cleanData(scopeSlot.row)">
                                            清理
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
        <ShowLog ref="showLogRef" />
    </div>
</template>

<script lang="ts" setup>
import { reactive, ref, onMounted, onUnmounted } from 'vue'
import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import BlockTable from '@/app/components/block-table/index.vue'
import LoadingPage from '@/app/components/loading/index.vue'
import AddModal from './add-modal/index.vue'
import ShowLog from '@/app/shared/components/show-log/index.vue'

import { PointTableConfig, FormData } from '../computer-group.config'
import {
    GetComputerPointData,
    CheckComputerPointData,
    AddComputerPointData,
    InstallComputerPointData,
    UninstallComputerPointData,
    DeleteComputerPointData,
    StopComputerPointData,
    StartComputerPointData,
    EditComputerPointData,
    CleanComputerPointData,
    SetDefaultComputerPointNode
} from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'

const route = useRoute()
const breadCrumbList = reactive([
    {
        name: '计算集群',
        code: 'computer-group'
    },
    {
        name: '节点',
        code: 'computer-pointer'
    }
])
const tableConfig: any = reactive(PointTableConfig)
const keyword = ref('')
const loading = ref(false)
const networkError = ref(false)
const selectedRows = ref<any[]>([])
const batchLoading = ref(false)
const addModalRef = ref(null)
const showLogRef = ref(null)
const timer = ref()

function normalizePercent(value: number): number {
    if (!Number.isFinite(value)) {
        return 0
    }
    return Math.max(0, Math.min(100, Math.round(value)))
}

function getPercentFromCpu(cpuValue: string): number {
    if (!cpuValue) {
        return 0
    }
    const percent = parseFloat(String(cpuValue).replace('%', '').trim())
    return normalizePercent(percent)
}

function getPercentFromUsage(usageText: string): number {
    if (!usageText) {
        return 0
    }
    const ratioMatch = String(usageText)
        .replace(/\s/g, '')
        .match(/^([\d.]+)[a-zA-Z]*\/([\d.]+)[a-zA-Z]*$/)
    if (!ratioMatch) {
        return getPercentFromCpu(usageText)
    }
    const used = Number(ratioMatch[1])
    const total = Number(ratioMatch[2])
    if (!Number.isFinite(used) || !Number.isFinite(total) || total <= 0) {
        return 0
    }
    return normalizePercent((used / total) * 100)
}

function getPercentColor(percent: number): string {
    return 'var(--el-color-primary-light-3)'
}

function getUsageDisplay(usageText: string): string {
    if (!usageText) {
        return '--'
    }
    return String(usageText)
}

function getCpuDisplay(cpuValue: string): string {
    if (!cpuValue) {
        return '--'
    }
    const text = String(cpuValue).trim()
    if (!text) {
        return '--'
    }
    return text.includes('%') ? text : `${text}%`
}

function getConnectTypeName(connectType?: string): string {
    const connectTypeMap: Record<string, string> = {
        SSH: 'SSH',
        HTTP: 'HTTP',
        AGENT_PORT: 'HTTP'
    }
    return connectTypeMap[connectType || 'SSH'] || connectType || 'SSH'
}

function isSshNode(row: any): boolean {
    return (row.connectType || 'SSH') === 'SSH'
}

function initData(tableLoading?: boolean, type?: string) {
    loading.value = tableLoading ? false : true
    networkError.value = networkError.value || false
    GetComputerPointData({
        page: tableConfig.pagination.currentPage - 1,
        pageSize: tableConfig.pagination.pageSize,
        searchKeyWord: keyword.value,
        clusterId: route.query.id
    })
        .then((res: any) => {
            if (type) {
                res.data.content.forEach((item: any) => {
                    tableConfig.tableData.forEach((col: any) => {
                        if (item.id === col.id) {
                            col.status = item.status
                            col.cpu = item.cpu
                            col.memory = item.memory
                            col.storage = item.storage
                        }
                    })
                })
            } else {
                tableConfig.tableData = res.data.content
                tableConfig.pagination.total = res.data.totalElements
                selectedRows.value = []
            }
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

// 添加节点数据
function addData() {
    addModalRef.value.showModal((formData: FormData) => {
        return new Promise((resolve: any, reject: any) => {
            AddComputerPointData({
                ...formData,
                clusterId: route.query.id
            })
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

// 编辑节点数据
function editNodeData(data: any) {
    addModalRef.value.showModal((formData: FormData) => {
        return new Promise((resolve: any, reject: any) => {
            EditComputerPointData({
                ...formData,
                clusterId: route.query.id
            })
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

// 查看日志
function showLog(e: any) {
    showLogRef.value.showModal(e.id, 'cluster')
}

// 停止
function stopAgent(data: any) {
    data.stopAgentLoading = true
    StopComputerPointData({
        engineNodeId: data.id
    })
        .then((res: any) => {
            ElMessage.success(res.msg)
            data.stopAgentLoading = false
            initData(true)
        })
        .catch(() => {
            data.stopAgentLoading = false
        })
}

// 停止
function startAgent(data: any) {
    data.stopAgentLoading = true
    StartComputerPointData({
        engineNodeId: data.id
    })
        .then((res: any) => {
            ElMessage.success(res.msg)
            data.stopAgentLoading = false
            initData(true)
        })
        .catch(() => {
            data.stopAgentLoading = false
        })
}

// 安装
function installData(data: any) {
    data.installLoading = true
    InstallComputerPointData({
        engineNodeId: data.id
    })
        .then((res: any) => {
            ElMessage.success(res.msg)
            data.installLoading = false
            initData(true)
        })
        .catch(() => {
            data.installLoading = false
        })
}

// 卸载
function uninstallData(data: any) {
    data.uninstallLoading = true
    UninstallComputerPointData({
        engineNodeId: data.id
    })
        .then((res: any) => {
            ElMessage.success(res.msg)
            data.uninstallLoading = false
            initData(true)
        })
        .catch(() => {
            data.uninstallLoading = false
        })
}

// 清理
function cleanData(data: any) {
    data.cleanLoading = true
    CleanComputerPointData({
        engineNodeId: data.id
    })
        .then((res: any) => {
            ElMessage.success(res.msg)
            data.cleanLoading = false
            initData(true)
        })
        .catch(() => {
            data.cleanLoading = false
        })
}

// 检测
function checkData(data: any) {
    data.checkLoading = true
    CheckComputerPointData({
        engineNodeId: data.id
    })
        .then((res: any) => {
            data.checkLoading = false
            ElMessage.success(res.msg)
            initData(true)
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
            CheckComputerPointData({
                engineNodeId: row.id
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

    ElMessageBox.confirm(`确定删除选中的 ${selectedRows.value.length} 个节点吗？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        batchLoading.value = true
        Promise.all(
            selectedRows.value.map((row: any) =>
                DeleteComputerPointData({
                    engineNodeId: row.id
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

// 设置默认节点
function setDefaultNode(data: any) {
    SetDefaultComputerPointNode({
        clusterNodeId: data.id
    })
        .then((res: any) => {
            data.checkLoading = false
            ElMessage.success(res.msg)
            initData(true)
        })
        .catch(() => {
            data.checkLoading = false
        })
}

// 删除
function deleteData(data: any) {
    ElMessageBox.confirm('确定删除该节点吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        DeleteComputerPointData({
            engineNodeId: data.id
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
    timer.value = setInterval(() => {
        initData(true, 'interval')
    }, 3000)
})
onUnmounted(() => {
    if (timer.value) {
        clearInterval(timer.value)
    }
    timer.value = null
})
</script>

<style lang="scss">
.zqy-seach-table {
    &.zqy-computer-node {
        .zqy-table-top {
            position: relative;
            overflow: hidden;
        }

        .node-batch-mask {
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

        .node-batch-actions {
            display: flex;
            align-items: center;
            gap: 10px;

            .node-batch-action {
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

            .node-batch-cancel {
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

        .node-batch-slide-enter-active,
        .node-batch-slide-leave-active {
            transition:
                transform 0.18s ease,
                opacity 0.18s ease;
            will-change: transform, opacity;
        }

        .node-batch-slide-enter-from,
        .node-batch-slide-leave-to {
            opacity: 0;
            transform: translateY(-100%);
        }

        .node-batch-slide-enter-to,
        .node-batch-slide-leave-from {
            opacity: 1;
            transform: translateY(0);
        }

        .node-action-group {
            justify-content: center;
            gap: 16px;

            .node-action-button {
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

        .zqy-seach {
            display: flex;
            align-items: center;
            .el-button {
                margin-left: 12px;
            }
        }
    }
}

.node-action-dropdown {
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

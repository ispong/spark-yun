<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-seach-table data-layer">
        <div class="zqy-table-top">
            <div class="btn-container">
                <el-button type="primary" @click="addData">新建分层</el-button>
            </div>
            <div class="zqy-seach">
                <el-radio-group v-model="tableType" @change="changeTypeEvent">
                    <el-radio-button label="all">全局搜索</el-radio-button>
                    <el-radio-button label="layer">分层搜索</el-radio-button>
                </el-radio-group>
                <el-input
                    v-model="keyword"
                    placeholder="请输入搜索条件 回车进行搜索"
                    :maxlength="200"
                    clearable
                    @input="inputEvent"
                    @keyup.enter="handleCurrentChange(1)"
                />
            </div>
            <Transition name="data-layer-batch-slide">
                <div v-if="selectedRows.length" class="data-layer-batch-mask">
                    <div class="data-layer-batch-actions">
                        <el-button class="data-layer-batch-action" :loading="batchLoading" @click="batchDeleteData">
                            删除
                        </el-button>
                        <el-button class="data-layer-batch-cancel" :disabled="batchLoading" @click="cancelSelection">
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
                        <span v-if="tableType === 'layer'" class="name-click" @click="showDetail(scopeSlot.row)">
                            {{ scopeSlot.row.name }}
                        </span>
                        <span v-else class="name-click" @click="dataModelPage(scopeSlot.row)">
                            {{ scopeSlot.row.name }}
                        </span>
                    </template>
                    <template #parentNameSlot="scopeSlot">
                        <span>{{ scopeSlot.row.parentNameList ?? '-' }}</span>
                    </template>
                    <template #options="scopeSlot">
                        <div class="btn-group data-layer-action-group">
                            <span
                                v-if="tableType === 'layer'"
                                class="data-layer-action-button"
                                @click="dataModelPage(scopeSlot.row)"
                            >
                                模型
                            </span>
                            <span v-else class="data-layer-action-button" @click="layerAreaView(scopeSlot.row)">
                                领域
                            </span>
                            <el-dropdown trigger="click" popper-class="data-layer-action-dropdown">
                                <span class="click-show-more data-layer-action-button">更多</span>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item @click="editData(scopeSlot.row)">编辑</el-dropdown-item>
                                        <el-dropdown-item @click="deleteData(scopeSlot.row)">删除</el-dropdown-item>
                                        <el-dropdown-item
                                            v-if="tableType === 'layer'"
                                            @click="layerAreaView(scopeSlot.row)"
                                        >
                                            领域
                                        </el-dropdown-item>
                                    </el-dropdown-menu>
                                </template>
                            </el-dropdown>
                        </div>
                    </template>
                </BlockTable>
                <div v-if="parentLayerId && tableType === 'layer'" class="back-btn-group">
                    <el-button @click="backHomeLayer">返回首页分层</el-button>
                    <el-button class="back-up" @click="showParentDetail">返回上一分层</el-button>
                </div>
            </div>
        </LoadingPage>
        <AddModal ref="addModalRef" />
        <DataModelDetail ref="dataModelDetailRef" />
    </div>
</template>

<script lang="ts" setup>
import { reactive, ref, onMounted } from 'vue'
import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import LoadingPage from '@/app/components/loading/index.vue'
import AddModal from './add-modal/index.vue'
import DataModelDetail from './layer-area/data-model-detail/index.vue'
import { BreadCrumbList, TableConfig } from './list.config'
import {
    DeleteDataLayerData,
    GetDataLayerTreeData,
    GetDataLayerList,
    SaveDataLayerData,
    UpdateDataLayerData,
    GetParentLayerNode
} from '../../api/data-layer'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const breadCrumbList = reactive(BreadCrumbList)
const tableConfig: any = reactive(TableConfig)
const keyword = ref('')
const loading = ref(false)
const networkError = ref(false)
const addModalRef = ref<any>(null)
const tableType = ref<string>('all')
const parentLayerId = ref<string>('')
const dataModelDetailRef = ref<any>(null)
const selectedRows = ref<any[]>([])
const batchLoading = ref(false)

function initData(tableLoading?: boolean) {
    loading.value = tableLoading ? false : true
    networkError.value = networkError.value || false
    if (tableType.value !== 'all') {
        GetDataLayerTreeData({
            page: tableConfig.pagination.currentPage - 1,
            pageSize: tableConfig.pagination.pageSize,
            searchKeyWord: keyword.value,
            parentLayerId: parentLayerId.value
        })
            .then((res: any) => {
                tableConfig.tableData = res.data.content
                tableConfig.pagination.total = res.data.totalElements
                selectedRows.value = []
                loading.value = false
                tableConfig.loading = false
                networkError.value = false

                if (route.query && route.query.parentLayerId) {
                    router.replace({
                        name: 'data-layer'
                    })
                }
            })
            .catch(() => {
                tableConfig.tableData = []
                tableConfig.pagination.total = 0
                selectedRows.value = []
                loading.value = false
                tableConfig.loading = false
                networkError.value = true
            })
    } else {
        GetDataLayerList({
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
                if (route.query && route.query.parentLayerId) {
                    router.replace({
                        name: 'data-layer'
                    })
                }
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
}

function changeTypeEvent(e: string) {
    parentLayerId.value = null
    tableConfig.pagination.currentPage = 1
    tableConfig.pagination.pageSize = 10

    tableConfig.tableData = []
    tableConfig.pagination.total = 0
    initData()
}

function addData() {
    addModalRef.value.showModal(
        (data: any) => {
            return new Promise((resolve: any, reject: any) => {
                const params = {
                    ...data,
                    parentLayerId: !data.parentLayerId ? null : data.parentLayerId
                }
                SaveDataLayerData(params)
                    .then((res: any) => {
                        ElMessage.success(res.msg)
                        initData()
                        resolve()
                    })
                    .catch((error: any) => {
                        reject(error)
                    })
            })
        },
        null,
        parentLayerId.value
    )
}
function editData(data: any) {
    addModalRef.value.showModal((data: any) => {
        return new Promise((resolve: any, reject: any) => {
            const params = {
                ...data,
                parentLayerId: !data.parentLayerId ? null : data.parentLayerId
            }
            UpdateDataLayerData(params)
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

// 删除
function deleteData(data: any) {
    ElMessageBox.confirm('确定删除该分层吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        DeleteDataLayerData({
            id: data.id
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

function batchDeleteData() {
    if (!selectedRows.value.length) {
        return
    }

    ElMessageBox.confirm(`确定删除选中的 ${selectedRows.value.length} 个分层吗？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        batchLoading.value = true
        Promise.all(
            selectedRows.value.map((row: any) =>
                DeleteDataLayerData({
                    id: row.id
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

// 跳转分层数据模型
function dataModelPage(data: any) {
    dataModelDetailRef.value.showModal(data)
}

// 跳转分层领域
function layerAreaView(data: any) {
    GetParentLayerNode({
        id: data.id
    })
        .then((res: any) => {
            router.push({
                name: 'layer-area',
                query: {
                    id: data.id,
                    parentLayerId: res.data.parentLayerId,
                    tableType: tableType.value
                }
            })
        })
        .catch((error: any) => {
            console.error('获取父级节点失败', error)
        })
}

function inputEvent(e: string) {
    if (e === '') {
        handleCurrentChange(1)
    }
}

// 跳转子集分层
function showDetail(data: any) {
    parentLayerId.value = data.id
    tableConfig.pagination.currentPage = 1
    tableConfig.pagination.pageSize = 10
    initData()
}

// 跳转父级分层
function showParentDetail() {
    GetParentLayerNode({
        id: parentLayerId.value
    })
        .then((res: any) => {
            parentLayerId.value = res.data.parentLayerId
            tableConfig.pagination.currentPage = 1
            tableConfig.pagination.pageSize = 10
            initData()
        })
        .catch((error: any) => {
            console.error('获取父级节点失败', error)
        })
}

// 返回首页分层
function backHomeLayer() {
    parentLayerId.value = null
    tableConfig.pagination.currentPage = 1
    tableConfig.pagination.pageSize = 10
    initData()
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
    if (route.query && (route.query.parentLayerId || route.query.tableType)) {
        tableType.value = route.query.tableType ?? 'all'
        parentLayerId.value = route.query.parentLayerId ?? null
    }

    tableConfig.pagination.currentPage = 1
    tableConfig.pagination.pageSize = 10
    initData()
})
</script>

<style lang="scss">
.data-layer {
    &.zqy-seach-table {
        .zqy-table-top {
            position: relative;
            overflow: hidden;

            .btn-container {
                height: 100%;
                display: flex;
                align-items: center;
            }
            .zqy-seach {
                display: flex;
                align-items: center;
                .el-radio-group {
                    margin-right: 8px;
                    .el-radio-button__inner {
                        font-size: getCssVar('font-size', 'extra-small');
                    }
                }
            }
        }
        .data-layer-batch-mask {
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
        .data-layer-batch-actions {
            display: flex;
            align-items: center;
            gap: 10px;
            .data-layer-batch-action {
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
            .data-layer-batch-cancel {
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
        .data-layer-batch-slide-enter-active,
        .data-layer-batch-slide-leave-active {
            transition:
                transform 0.18s ease,
                opacity 0.18s ease;
            will-change: transform, opacity;
        }
        .data-layer-batch-slide-enter-from,
        .data-layer-batch-slide-leave-to {
            opacity: 0;
            transform: translateY(-100%);
        }
        .data-layer-batch-slide-enter-to,
        .data-layer-batch-slide-leave-from {
            opacity: 1;
            transform: translateY(0);
        }
        .zqy-table {
            position: relative;
            .data-layer-action-group {
                justify-content: center;
                gap: 16px;
                .data-layer-action-button {
                    display: inline-flex;
                    align-items: center;
                    line-height: 1;
                    font-size: getCssVar('font-size', 'extra-small');
                }
            }
            .back-btn-group {
                position: absolute;
                bottom: 22px;
                left: 20px;
                display: flex;
                align-items: center;
            }
            .back-up {
                margin-left: 10px;
            }
        }
    }
}

.data-layer-action-dropdown {
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

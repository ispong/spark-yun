<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-seach-table data-model-page">
        <div class="zqy-table-top">
            <el-button class="data-model-toolbar-button" type="primary" @click="addData">新建模型</el-button>
            <div class="zqy-seach">
                <el-input
                    v-model="keyword"
                    placeholder="请输入搜索条件 回车进行搜索"
                    :maxlength="200"
                    clearable
                    @input="inputEvent"
                    @keyup.enter="handleCurrentChange(1)"
                />
            </div>
            <Transition name="data-model-batch-slide">
                <div v-if="selectedRows.length" class="data-model-batch-mask">
                    <div class="data-model-batch-actions">
                        <el-button class="data-model-batch-action" :loading="batchLoading" @click="batchDeleteData">
                            删除
                        </el-button>
                        <el-button class="data-model-batch-cancel" :disabled="batchLoading" @click="cancelSelection">
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
                    <template #statusTag="scopeSlot">
                        <ZStatusTag :status="scopeSlot.row.status" />
                    </template>
                    <template #nameSlot="scopeSlot">
                        <span class="name-click" @click="showDetail(scopeSlot.row)">{{ scopeSlot.row.name }}</span>
                    </template>
                    <template #layerNameSlot="scopeSlot">
                        <span>{{ scopeSlot.row.layerName }}</span>
                    </template>
                    <template #options="scopeSlot">
                        <div class="btn-group data-model-action-group">
                            <span
                                v-if="['INIT', 'FAIL', 'ERROR'].includes(scopeSlot.row.status)"
                                class="data-model-action-button"
                                @click="buildData(scopeSlot.row)"
                            >
                                构建
                            </span>
                            <span
                                v-if="scopeSlot.row.status === 'SUCCESS'"
                                class="data-model-action-button"
                                @click="showMetadataDetail(scopeSlot.row)"
                            >
                                详情
                            </span>
                            <el-dropdown trigger="click" popper-class="data-model-action-dropdown">
                                <span class="click-show-more data-model-action-button">更多</span>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item @click="editData(scopeSlot.row)">编辑</el-dropdown-item>
                                        <el-dropdown-item @click="deleteData(scopeSlot.row)">删除</el-dropdown-item>
                                        <el-dropdown-item @click="resetData(scopeSlot.row)">重置</el-dropdown-item>
                                        <el-dropdown-item
                                            v-if="scopeSlot.row.status !== 'INIT'"
                                            @click="buildData(scopeSlot.row)"
                                        >
                                            构建
                                        </el-dropdown-item>
                                        <el-dropdown-item @click="copyData(scopeSlot.row)">复制</el-dropdown-item>
                                        <el-dropdown-item @click="showLog(scopeSlot.row)">日志</el-dropdown-item>
                                    </el-dropdown-menu>
                                </template>
                            </el-dropdown>
                        </div>
                    </template>
                </BlockTable>
            </div>
        </LoadingPage>
        <AddModal ref="addModalRef" />
        <CopyModal ref="copyModalRef" />
        <ShowLog ref="showLogRef" />
    </div>
</template>

<script lang="ts" setup>
import { reactive, ref, onMounted } from 'vue'
import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import LoadingPage from '@/app/components/loading/index.vue'
import AddModal from './add-modal/index.vue'
import CopyModal from './copy-modal/index.vue'

import { BreadCrumbList, TableConfig } from './list.config'
import {
    GetDataModelList,
    GetDataModelTreeData,
    SaveDataModelData,
    UpdateDataModelData,
    DeleteDataModelData,
    ResetDataModel,
    BuildDataModel,
    CopyDataModelData
} from '../../api/data-model'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import ShowLog from '@/app/shared/components/show-log/index.vue'

const router = useRouter()
const route = useRoute()

const breadCrumbList = reactive(BreadCrumbList)
const tableConfig: any = reactive(TableConfig)
const keyword = ref('')
const loading = ref(false)
const networkError = ref(false)
const addModalRef = ref<any>(null)
const copyModalRef = ref<any>(null)
const showLogRef = ref<any>(null)
const selectedRows = ref<any[]>([])
const batchLoading = ref(false)

function initData(tableLoading?: boolean) {
    loading.value = tableLoading ? false : true
    networkError.value = networkError.value || false
    if (route.query.id) {
        GetDataModelTreeData({
            page: tableConfig.pagination.currentPage - 1,
            pageSize: tableConfig.pagination.pageSize,
            searchKeyWord: keyword.value,
            layerId: route.query.id
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
    } else {
        GetDataModelList({
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
}

function addData() {
    addModalRef.value.showModal((data: any) => {
        return new Promise((resolve: any, reject: any) => {
            SaveDataModelData(data)
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
    addModalRef.value.showModal((data: any) => {
        return new Promise((resolve: any, reject: any) => {
            UpdateDataModelData(data)
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
function copyData(data: any) {
    copyModalRef.value.showModal((data: any) => {
        return new Promise((resolve: any, reject: any) => {
            CopyDataModelData({
                modelId: data.id,
                name: data.name,
                layerId: data.layerId,
                tableName: data.tableName,
                remark: data.remark
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

// 删除
function deleteData(data: any) {
    ElMessageBox.confirm('确定删除该模型吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        DeleteDataModelData({
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

    ElMessageBox.confirm(`确定删除选中的 ${selectedRows.value.length} 个模型吗？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        batchLoading.value = true
        Promise.all(
            selectedRows.value.map((row: any) =>
                DeleteDataModelData({
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

function resetData(data: any) {
    ElMessageBox.confirm('是否确定重置？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        ResetDataModel({
            id: data.id
        })
            .then((res: any) => {
                ElMessage.success(res.msg)
                initData()
            })
            .catch(() => {})
    })
}

function buildData(data: any) {
    ElMessageBox.confirm('是否确定构建？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        BuildDataModel({
            modelId: data.id
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
        handleCurrentChange(1)
    }
}

function showDetail(data: any) {
    router.push({
        name: 'model-field',
        query: {
            id: data.id,
            modelType: data.modelType
        }
    })
}

function showMetadataDetail(data: any) {
    router.push({
        name: 'metadata-management',
        query: {
            tableType: 'table',
            datasourceId: data.datasourceId || '',
            tableName: data.tableName || ''
        }
    })
}

// 查看日志
function showLog(e: any) {
    showLogRef.value.showModal(e.buildLog)
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
    const routeSearchModelId = String(route.query.searchModelId || '')
    if (routeSearchModelId) {
        keyword.value = routeSearchModelId
    }
    initData()
})
</script>

<style lang="scss">
.data-model-page {
    &.zqy-seach-table {
        .zqy-table-top {
            position: relative;
            overflow: hidden;
        }
        .data-model-toolbar-button {
            width: 92px;
            height: 32px;
            padding: 8px 15px;
            box-sizing: border-box;
            line-height: 1;
        }
        .data-model-batch-mask {
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
        .data-model-batch-actions {
            display: flex;
            align-items: center;
            gap: 10px;
            .data-model-batch-action {
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
            .data-model-batch-cancel {
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
        .data-model-batch-slide-enter-active,
        .data-model-batch-slide-leave-active {
            transition:
                transform 0.18s ease,
                opacity 0.18s ease;
            will-change: transform, opacity;
        }
        .data-model-batch-slide-enter-from,
        .data-model-batch-slide-leave-to {
            opacity: 0;
            transform: translateY(-100%);
        }
        .data-model-batch-slide-enter-to,
        .data-model-batch-slide-leave-from {
            opacity: 1;
            transform: translateY(0);
        }
        .zqy-table {
            .data-model-action-group {
                justify-content: center;
                gap: 16px;
                .data-model-action-button {
                    display: inline-flex;
                    align-items: center;
                    line-height: 1;
                    font-size: getCssVar('font-size', 'extra-small');
                }
            }
        }
    }
}

.data-model-action-dropdown {
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

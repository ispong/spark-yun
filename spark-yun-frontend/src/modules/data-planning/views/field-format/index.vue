<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-seach-table field-format-page">
        <div class="zqy-table-top">
            <el-button class="field-format-toolbar-button" type="primary" @click="addData">新建标准</el-button>
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
            <Transition name="field-format-batch-slide">
                <div v-if="selectedRows.length" class="field-format-batch-mask">
                    <div class="field-format-batch-actions">
                        <el-button class="field-format-batch-action" :loading="batchLoading" @click="batchDeleteData">
                            删除
                        </el-button>
                        <el-button class="field-format-batch-cancel" :disabled="batchLoading" @click="cancelSelection">
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
                        <span class="name-click" @click="editData(scopeSlot.row)">{{ scopeSlot.row.name }}</span>
                    </template>
                    <template #fieldTypeSlot="scopeSlot">
                        <el-tag class="field-format-type-tag">
                            {{ getFieldTypeName(scopeSlot.row.columnTypeCode) }}
                        </el-tag>
                    </template>
                    <template #options="scopeSlot">
                        <div class="btn-group field-format-action-group">
                            <span class="field-format-action-button" @click="editData(scopeSlot.row)">编辑</span>
                            <el-dropdown trigger="click" popper-class="field-format-action-dropdown">
                                <span class="click-show-more field-format-action-button">更多</span>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item
                                            v-if="['DISABLE'].includes(scopeSlot.row.status)"
                                            @click="enableData(scopeSlot.row)"
                                        >
                                            启用
                                        </el-dropdown-item>
                                        <el-dropdown-item
                                            v-if="['ENABLE'].includes(scopeSlot.row.status)"
                                            @click="disableData(scopeSlot.row)"
                                        >
                                            禁用
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
import LoadingPage from '@/app/components/loading/index.vue'
import AddModal from './add-modal/index.vue'

import { BreadCrumbList, TableConfig } from './list.config'
import {
    GetFieldFormatList,
    SaveFieldFormatData,
    UpdateFieldFormatData,
    DeleteFieldFormatData,
    EnableFieldFormatData,
    DisabledFieldFormatData
} from '../../api/field-format'
import { ElMessage, ElMessageBox } from 'element-plus'

const breadCrumbList = reactive(BreadCrumbList)
const tableConfig: any = reactive(TableConfig)
const keyword = ref('')
const loading = ref(false)
const networkError = ref(false)
const addModalRef = ref<any>(null)
const selectedRows = ref<any[]>([])
const batchLoading = ref(false)
const fieldTypeList = [
    { label: '自定义', value: 'CUSTOM' },
    { label: '字符串', value: 'STRING' },
    { label: '整数', value: 'INT' },
    { label: '小数', value: 'DOUBLE' },
    { label: '大文本', value: 'TEXT' },
    { label: '日期', value: 'DATE' },
    { label: '日期时间', value: 'DATETIME' }
]

function getFieldTypeName(columnTypeCode: string) {
    return fieldTypeList.find((item) => item.value === columnTypeCode)?.label || columnTypeCode || '--'
}

function initData(tableLoading?: boolean) {
    loading.value = tableLoading ? false : true
    networkError.value = networkError.value || false
    GetFieldFormatList({
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

function addData() {
    addModalRef.value.showModal((data: any) => {
        return new Promise((resolve: any, reject: any) => {
            SaveFieldFormatData(data)
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
            UpdateFieldFormatData(data)
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

// 启用
function enableData(data: any) {
    ElMessageBox.confirm('确定启用该字段标准吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        EnableFieldFormatData({
            id: data.id
        })
            .then((res: any) => {
                ElMessage.success(res.msg)
                initData()
            })
            .catch(() => {})
    })
}
// 禁用
function disableData(data: any) {
    ElMessageBox.confirm('确定禁用该字段标准吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        DisabledFieldFormatData({
            id: data.id
        })
            .then((res: any) => {
                ElMessage.success(res.msg)
                initData()
            })
            .catch(() => {})
    })
}

// 删除
function deleteData(data: any) {
    ElMessageBox.confirm('确定删除该字段标准吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        DeleteFieldFormatData({
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

    ElMessageBox.confirm(`确定删除选中的 ${selectedRows.value.length} 个字段标准吗？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        batchLoading.value = true
        Promise.all(
            selectedRows.value.map((row: any) =>
                DeleteFieldFormatData({
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

function inputEvent(e: string) {
    if (e === '') {
        handleCurrentChange(1)
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
.field-format-page {
    &.zqy-seach-table {
        .zqy-table-top {
            position: relative;
            overflow: hidden;
        }
        .field-format-toolbar-button {
            width: 92px;
            height: 32px;
            padding: 8px 15px;
            box-sizing: border-box;
            line-height: 1;
        }
        .field-format-batch-mask {
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
        .field-format-batch-actions {
            display: flex;
            align-items: center;
            gap: 10px;
            .field-format-batch-action {
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
            .field-format-batch-cancel {
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
        .field-format-batch-slide-enter-active,
        .field-format-batch-slide-leave-active {
            transition:
                transform 0.18s ease,
                opacity 0.18s ease;
            will-change: transform, opacity;
        }
        .field-format-batch-slide-enter-from,
        .field-format-batch-slide-leave-to {
            opacity: 0;
            transform: translateY(-100%);
        }
        .field-format-batch-slide-enter-to,
        .field-format-batch-slide-leave-from {
            opacity: 1;
            transform: translateY(0);
        }
        .field-format-type-tag {
            max-width: 100%;
            white-space: nowrap;
        }
        .zqy-table {
            .vxe-table--header .field-format-type-column {
                position: relative;

                &::before {
                    content: '';
                    position: absolute;
                    top: 25%;
                    bottom: 25%;
                    left: 0;
                    width: 1px;
                    pointer-events: none;
                    background-color: var(--vxe-ui-table-resizable-line-color);
                }
            }
            .field-format-action-group {
                justify-content: center;
                gap: 16px;
                .field-format-action-button {
                    display: inline-flex;
                    align-items: center;
                    line-height: 1;
                    font-size: getCssVar('font-size', 'extra-small');
                }
            }
        }
    }
}

.field-format-action-dropdown {
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

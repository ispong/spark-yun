<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-seach-table global-variables-page">
        <div class="zqy-table-top">
            <el-button type="primary" @click="addData">新建变量</el-button>
            <div class="zqy-seach">
                <el-input
                    v-model="keyword"
                    placeholder="请输入备注 回车进行搜索"
                    :maxlength="200"
                    clearable
                    @input="inputEvent"
                    @keyup.enter="initData(false)"
                />
            </div>
            <Transition name="global-variables-batch-slide">
                <div v-if="selectedRows.length" class="global-variables-batch-mask">
                    <div class="global-variables-batch-actions">
                        <el-button
                            class="global-variables-batch-action"
                            :loading="batchLoading"
                            @click="batchDeleteData"
                        >
                            删除
                        </el-button>
                        <el-button
                            class="global-variables-batch-cancel"
                            :disabled="batchLoading"
                            @click="cancelSelection"
                        >
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
                    <template #varName="scopeSlot">
                        <span class="name-click" @click="editData(scopeSlot.row)">{{ scopeSlot.row.keyName }}</span>
                    </template>
                    <template #options="scopeSlot">
                        <div class="btn-group">
                            <span @click="copyData(scopeSlot.row)">复制</span>
                            <el-dropdown trigger="click" popper-class="global-variables-action-dropdown">
                                <span class="click-show-more global-variables-action-button">更多</span>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item @click="editData(scopeSlot.row)">编辑</el-dropdown-item>
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

import { BreadCrumbList, TableConfig } from './list.config'
import {
    CopyGlobalVariablesPath,
    DeleteGlobalVariablesData,
    GetGlobalVariablesList,
    SaveGlobalVariablesData,
    UpdateGlobalVariablesData
} from '@/modules/global-variables/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'

const breadCrumbList = reactive(BreadCrumbList)
const tableConfig: any = reactive(TableConfig)
const keyword = ref<string>('')
const loading = ref<boolean>(false)
const networkError = ref<boolean>(false)
const addModalRef = ref<any>(null)
const selectedRows = ref<any[]>([])
const batchLoading = ref(false)

function initData(tableLoading?: boolean) {
    loading.value = tableLoading ? false : true
    networkError.value = networkError.value || false
    GetGlobalVariablesList({
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
            SaveGlobalVariablesData(data)
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
            UpdateGlobalVariablesData(data)
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
    CopyGlobalVariablesPath({
        id: data.id
    })
        .then((res: any) => {
            copyParse(res.data.secretExpression)
        })
        .catch((error: any) => {})
}

async function copyParse(text: string) {
    if ('clipboard' in navigator) {
        try {
            await navigator.clipboard.writeText(text)
            ElMessage({
                duration: 800,
                message: '复制成功',
                type: 'success'
            })
        } catch (err) {
            console.error('Failed to copy: ', err)
        }
    } else {
        // 回退方案：使用document.execCommand('copy')
        const textArea = document.createElement('textarea')
        textArea.value = text
        document.body.appendChild(textArea)
        textArea.select()
        try {
            const successful = document.execCommand('copy')
            if (successful) {
                ElMessage({
                    duration: 800,
                    message: '复制成功',
                    type: 'success'
                })
            }
        } catch (err) {
            console.error('Failed to copy: ', err)
        }
        document.body.removeChild(textArea)
    }
}

// 删除
function deleteData(data: any) {
    ElMessageBox.confirm('确定删除该数据吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        DeleteGlobalVariablesData({
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

    ElMessageBox.confirm(`确定删除选中的 ${selectedRows.value.length} 个全局变量吗？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        batchLoading.value = true
        Promise.all(selectedRows.value.map((row: any) => DeleteGlobalVariablesData({ id: row.id })))
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
.zqy-seach-table.global-variables-page {
    .zqy-table-top {
        position: relative;
        overflow: hidden;

        .global-variables-batch-mask {
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

    .global-variables-batch-actions {
        display: flex;
        align-items: center;
        gap: 10px;

        .global-variables-batch-action {
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

        .global-variables-batch-cancel {
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

    .global-variables-batch-slide-enter-active,
    .global-variables-batch-slide-leave-active {
        transition:
            opacity 0.16s ease,
            transform 0.16s ease;
    }

    .global-variables-batch-slide-enter-from,
    .global-variables-batch-slide-leave-to {
        opacity: 0;
        transform: translateY(-4px);
    }

    .global-variables-batch-slide-enter-to,
    .global-variables-batch-slide-leave-from {
        opacity: 1;
        transform: translateY(0);
    }

    .btn-group {
        justify-content: center;
        gap: 16px;
    }

    .global-variables-action-button {
        display: inline-flex;
        align-items: center;
        line-height: 1;
        font-size: getCssVar('font-size', 'extra-small');
    }
}

.global-variables-action-dropdown {
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

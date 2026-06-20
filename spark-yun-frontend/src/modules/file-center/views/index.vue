<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-seach-table file-center-page">
        <div class="zqy-table-top">
            <div class="file-center-toolbar-left">
                <el-button class="file-center-toolbar-button" type="primary" @click="addData">上传资源</el-button>
                <div class="zqy-tenant__select">
                    <el-select
                        v-model="type"
                        clearable
                        placeholder="请选择类型进行搜索"
                        popper-class="file-type-select-dropdown"
                        @change="handleTypeChange"
                    >
                        <el-option v-for="item in typeList" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                </div>
            </div>
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
            <Transition name="file-batch-slide">
                <div v-if="selectedRows.length" class="file-batch-mask">
                    <div class="file-batch-actions">
                        <el-button class="file-batch-action" :loading="batchLoading" @click="batchDeleteData">
                            删除
                        </el-button>
                        <el-button class="file-batch-cancel" :disabled="batchLoading" @click="cancelSelection">
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
                    <template #fileNameSlot="scopeSlot">
                        <span class="name-click" @click="editData(scopeSlot.row)">
                            {{ scopeSlot.row.fileName }}
                        </span>
                    </template>
                    <template #fileTypeSlot="scopeSlot">
                        <el-tag class="file-type-tag">{{ getFileTypeName(scopeSlot.row.fileType) }}</el-tag>
                    </template>
                    <template #options="scopeSlot">
                        <div class="btn-group file-action-group">
                            <span
                                v-if="!scopeSlot.row.downloadLoading"
                                class="file-action-button"
                                @click="downloadFile(scopeSlot.row)"
                            >
                                下载
                            </span>
                            <el-icon v-else class="is-loading">
                                <Loading />
                            </el-icon>
                            <el-dropdown trigger="click" popper-class="file-action-dropdown">
                                <span class="click-show-more file-action-button">更多</span>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item @click="editData(scopeSlot.row)">备注</el-dropdown-item>
                                        <el-dropdown-item @click="deleteData(scopeSlot.row)">删除</el-dropdown-item>
                                    </el-dropdown-menu>
                                </template>
                            </el-dropdown>
                        </div>
                    </template>
                </BlockTable>
            </div>
        </LoadingPage>
        <AddModal ref="addModalRef" :show-excel-type="showExcelType" />
    </div>
</template>

<script lang="ts" setup>
import { computed, reactive, ref, onMounted } from 'vue'
import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import BlockTable from '@/app/components/block-table/index.vue'
import LoadingPage from '@/app/components/loading/index.vue'
import AddModal from './add-modal/index.vue'

import { BreadCrumbList, TableConfig } from './file-center.config'
import {
    GetFileCenterList,
    UploadFileData,
    DeleteFileData,
    DownloadFileData,
    UpdateFileData
} from '@/modules/file-center/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { getVipLicenseEnabled } from '@/app/utils/vip-license'

const breadCrumbList = reactive(BreadCrumbList)
const tableConfig: any = reactive(TableConfig)
const keyword = ref('')
const type = ref('')
const loading = ref(false)
const networkError = ref(false)
const selectedRows = ref<any[]>([])
const batchLoading = ref(false)
const addModalRef = ref<any>(null)
const showExcelType = ref(true)
const allTypeList = [
    {
        label: '作业',
        value: 'JOB'
    },
    {
        label: '函数',
        value: 'FUNC'
    },
    {
        label: '依赖',
        value: 'LIB'
    },
    {
        label: 'Excel',
        value: 'EXCEL'
    }
]
const typeList = computed(() =>
    showExcelType.value ? allTypeList : allTypeList.filter((item) => item.value !== 'EXCEL')
)

function initData(tableLoading?: boolean) {
    loading.value = tableLoading ? false : true
    networkError.value = networkError.value || false
    GetFileCenterList({
        page: tableConfig.pagination.currentPage - 1,
        pageSize: tableConfig.pagination.pageSize,
        searchKeyWord: keyword.value,
        type: type.value
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

function getFileTypeName(fileType: string): string {
    return allTypeList.find((item) => item.value === fileType)?.label || fileType || '--'
}

function addData() {
    addModalRef.value.showModal((data: any) => {
        return new Promise((resolve: any, reject: any) => {
            const formData = new FormData()
            formData.append('type', data.type)
            formData.append('remark', data.remark)
            data.fileData.forEach((file: File) => {
                formData.append('fileList', file)
            })
            UploadFileData(formData)
                .then((res: any) => {
                    ElMessage.success(res.data.msg)
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
    addModalRef.value.showModal((formData: any) => {
        return new Promise((resolve: any, reject: any) => {
            UpdateFileData({
                fileId: formData.id,
                remark: formData.remark
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

    ElMessageBox.confirm(`确定删除选中的 ${selectedRows.value.length} 个资源吗？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        batchLoading.value = true
        Promise.all(
            selectedRows.value.map((row: any) =>
                DeleteFileData({
                    fileId: row.id
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

// 下载
function downloadFile(data: any) {
    data.downloadLoading = true
    DownloadFileData({
        fileId: data.id
    })
        .then((res: any) => {
            const blobURL = URL.createObjectURL(res)

            // 创建一个链接元素并模拟点击下载
            const link = document.createElement('a')
            link.href = blobURL
            link.download = data.fileName // 根据实际情况设置下载文件的名称和扩展名
            link.click()

            // 释放Blob URL
            URL.revokeObjectURL(blobURL)

            data.downloadLoading = false
        })
        .catch(() => {
            data.downloadLoading = false
        })
}

// 删除
function deleteData(data: any) {
    ElMessageBox.confirm('确定删除该文件吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        DeleteFileData({
            fileId: data.id
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

function handleTypeChange() {
    tableConfig.pagination.currentPage = 1
    initData(false)
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
    getVipLicenseEnabled().then((enabled) => {
        showExcelType.value = enabled
        if (!enabled && type.value === 'EXCEL') {
            type.value = ''
            initData(false)
        }
    })
    tableConfig.pagination.currentPage = 1
    tableConfig.pagination.pageSize = 10
    initData()
})
</script>

<style lang="scss">
.zqy-seach-table.file-center-page {
    .zqy-table-top {
        position: relative;
        overflow: hidden;
        gap: 16px;
        justify-content: flex-start;
    }

    .file-center-toolbar-left {
        display: flex;
        align-items: center;
        gap: 12px;
        min-width: 0;

        .el-button + .el-button {
            margin-left: 0;
        }
    }

    .file-center-toolbar-button {
        width: 92px;
        height: 32px;
        padding: 8px 15px;
        box-sizing: border-box;
        line-height: 1;
    }

    .zqy-tenant__select {
        .el-select {
            width: 192px;
        }
    }

    .zqy-seach {
        margin-left: auto;
    }

    .name-click {
        cursor: pointer;
        color: getCssVar('color', 'primary', 'light-5');

        &:hover {
            color: getCssVar('color', 'primary');
        }
    }

    .file-batch-mask {
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

    .file-batch-actions {
        display: flex;
        align-items: center;
        gap: 10px;

        .file-batch-action {
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

        .file-batch-cancel {
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

    .file-batch-slide-enter-active,
    .file-batch-slide-leave-active {
        transition:
            transform 0.18s ease,
            opacity 0.18s ease;
        will-change: transform, opacity;
    }

    .file-batch-slide-enter-from,
    .file-batch-slide-leave-to {
        opacity: 0;
        transform: translateY(-100%);
    }

    .file-batch-slide-enter-to,
    .file-batch-slide-leave-from {
        opacity: 1;
        transform: translateY(0);
    }

    .file-type-tag {
        max-width: 100%;
        white-space: nowrap;
    }

    .file-action-group {
        justify-content: center;
        gap: 16px;

        .file-action-button {
            display: inline-flex;
            align-items: center;
            line-height: 1;
            font-size: getCssVar('font-size', 'extra-small');
        }
    }
}

.file-action-dropdown {
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

.file-type-select-dropdown {
    .el-select-dropdown__item {
        height: 30px;
        padding: 0 12px;
        line-height: 30px;
        text-align: left;
        font-size: getCssVar('font-size', 'extra-small');
    }
}
</style>

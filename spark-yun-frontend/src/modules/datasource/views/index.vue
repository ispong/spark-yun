<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-seach-table datasource-page">
        <div class="zqy-table-top">
            <div class="datasource-toolbar-left">
                <el-button
                    v-if="canDatasource('create')"
                    class="datasource-toolbar-button"
                    type="primary"
                    @click="addData"
                >
                    新建数据源
                </el-button>
                <el-button class="datasource-toolbar-button" @click="goDriverManagement">驱动管理</el-button>
                <div class="zqy-tenant__select">
                    <el-select
                        v-model="datasourceType"
                        placeholder="请选择数据源类型"
                        filterable
                        clearable
                        @change="handleChnage"
                    >
                        <el-option
                            v-for="item in datasourceTypeList"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value"
                        />
                    </el-select>
                </div>
            </div>
            <div class="zqy-seach">
                <el-input
                    v-model="keyword"
                    placeholder="请输入名称/类型/连接信息/用户名/备注 回车进行搜索"
                    :maxlength="200"
                    clearable
                    @input="inputEvent"
                    @keyup.enter="initData(false)"
                />
            </div>
            <Transition name="datasource-batch-slide">
                <div v-if="selectedRows.length" class="datasource-batch-mask">
                    <div class="datasource-batch-actions">
                        <el-button
                            v-if="canDatasource('execute')"
                            class="datasource-batch-action"
                            :loading="batchLoading"
                            @click="batchCheckData"
                        >
                            检测
                        </el-button>
                        <el-button
                            v-if="canDatasource('delete')"
                            class="datasource-batch-action"
                            :loading="batchLoading"
                            @click="batchDeleteData"
                        >
                            删除
                        </el-button>
                        <el-button
                            class="datasource-batch-cancel"
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
                    <template #nameSlot="scopeSlot">
                        <span
                            :class="{ 'name-click': canDatasource('edit') }"
                            @click="canDatasource('edit') && editData(scopeSlot.row)"
                        >
                            {{ scopeSlot.row.name }}
                        </span>
                    </template>
                    <template #dbTypeSlot="scopeSlot">
                        <el-tag class="datasource-type-tag">{{ getDatasourceTypeName(scopeSlot.row.dbType) }}</el-tag>
                    </template>
                    <template #statusTag="scopeSlot">
                        <ZStatusTag :status="scopeSlot.row.status" />
                    </template>
                    <template #options="scopeSlot">
                        <div class="btn-group datasource-action-group">
                            <span
                                v-if="canDatasource('edit')"
                                class="datasource-action-button"
                                @click="editData(scopeSlot.row)"
                            >
                                编辑
                            </span>
                            <el-dropdown
                                v-if="canDatasource('execute') || canDatasource('view') || canDatasource('delete')"
                                trigger="click"
                                popper-class="datasource-action-dropdown"
                            >
                                <span class="click-show-more datasource-action-button">更多</span>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item
                                            v-if="canDatasource('execute')"
                                            :disabled="scopeSlot.row.checkLoading"
                                            @click="!scopeSlot.row.checkLoading && checkData(scopeSlot.row)"
                                        >
                                            <span v-if="!scopeSlot.row.checkLoading">检测</span>
                                            <el-icon v-else class="is-loading">
                                                <Loading />
                                            </el-icon>
                                        </el-dropdown-item>
                                        <el-dropdown-item v-if="canDatasource('view')" @click="showLog(scopeSlot.row)">
                                            日志
                                        </el-dropdown-item>
                                        <el-dropdown-item
                                            v-if="canDatasource('delete')"
                                            @click="deleteData(scopeSlot.row)"
                                        >
                                            删除
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
        <ShowLog ref="showLogRef" />
    </div>
</template>

<script lang="ts" setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/app/store/useAuth'
import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import BlockTable from '@/app/components/block-table/index.vue'
import LoadingPage from '@/app/components/loading/index.vue'
import AddModal from './add-modal/index.vue'
import ShowLog from '@/app/shared/components/show-log/index.vue'

import { BreadCrumbList, TableConfig, FormData, typeList } from './datasource.config'
import {
    GetDatasourceList,
    AddDatasourceData,
    UpdateDatasourceData,
    CheckDatasourceData,
    DeleteDatasourceData
} from '@/modules/datasource/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'

const keyword = ref('')
const loading = ref(false)
const networkError = ref(false)
const selectedRows = ref<any[]>([])
const batchLoading = ref(false)
const addModalRef = ref(null)
const showLogRef = ref(null)
const datasourceType = ref('')
const datasourceTypeList = ref(typeList)
const router = useRouter()
const authStore = useAuthStore()

const breadCrumbList = reactive(BreadCrumbList)
const tableConfig: any = reactive(TableConfig)

function canDatasource(action: string) {
    return (
        !!authStore.userInfo?.workspaceAllPermissions ||
        (authStore.userInfo?.permissions || []).includes(`workspace:datasource:${action}`)
    )
}

function initData(tableLoading?: boolean) {
    loading.value = tableLoading ? false : true
    networkError.value = networkError.value || false
    GetDatasourceList({
        page: tableConfig.pagination.currentPage - 1,
        pageSize: tableConfig.pagination.pageSize,
        searchKeyWord: keyword.value,
        datasourceType: datasourceType.value
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

function getDatasourceTypeName(dbType: string): string {
    return typeList.find((item) => item.value === dbType)?.label || dbType || '--'
}

function addData() {
    addModalRef.value.showModal((formData: FormData) => {
        return new Promise((resolve: any, reject: any) => {
            AddDatasourceData(formData)
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

function goDriverManagement() {
    router.push({
        name: 'driver-management'
    })
}

// 查看日志
function showLog(e: any) {
    showLogRef.value.showModal(e.connectLog)
}

function editData(data: any) {
    addModalRef.value.showModal((formData: FormData) => {
        return new Promise((resolve: any, reject: any) => {
            UpdateDatasourceData(formData)
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
    CheckDatasourceData({
        datasourceId: data.id
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
            CheckDatasourceData({
                datasourceId: row.id
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

    ElMessageBox.confirm(`确定删除选中的 ${selectedRows.value.length} 个数据源吗？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        batchLoading.value = true
        Promise.all(
            selectedRows.value.map((row: any) =>
                DeleteDatasourceData({
                    datasourceId: row.id
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

// 删除
function deleteData(data: any) {
    ElMessageBox.confirm('确定删除该数据源吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        DeleteDatasourceData({
            datasourceId: data.id
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
    initData(true)
}

function handleChnage() {
    tableConfig.pagination.currentPage = 1
    initData()
}

onMounted(() => {
    tableConfig.pagination.currentPage = 1
    tableConfig.pagination.pageSize = 10
    initData()
})
</script>

<style lang="scss">
.zqy-seach-table.datasource-page {
    .zqy-table-top {
        position: relative;
        overflow: hidden;
        gap: 16px;
        justify-content: flex-start;
    }

    .datasource-toolbar-left {
        display: flex;
        align-items: center;
        gap: 12px;
        min-width: 0;

        .el-button + .el-button {
            margin-left: 0;
        }
    }

    .datasource-toolbar-button {
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

    .datasource-batch-mask {
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

    .datasource-batch-actions {
        display: flex;
        align-items: center;
        gap: 10px;

        .datasource-batch-action {
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

        .datasource-batch-cancel {
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

    .datasource-batch-slide-enter-active,
    .datasource-batch-slide-leave-active {
        transition:
            transform 0.18s ease,
            opacity 0.18s ease;
        will-change: transform, opacity;
    }

    .datasource-batch-slide-enter-from,
    .datasource-batch-slide-leave-to {
        opacity: 0;
        transform: translateY(-100%);
    }

    .datasource-batch-slide-enter-to,
    .datasource-batch-slide-leave-from {
        opacity: 1;
        transform: translateY(0);
    }

    .datasource-type-tag {
        max-width: 100%;
        color: getCssVar('text-color', 'regular');
        border-color: getCssVar('border-color', 'light');
        background-color: getCssVar('fill-color', 'lighter');
        white-space: nowrap;
    }

    .datasource-action-group {
        justify-content: center;
        gap: 16px;

        .datasource-action-button {
            display: inline-flex;
            align-items: center;
            line-height: 1;
            font-size: getCssVar('font-size', 'extra-small');
        }
    }
}

.datasource-action-dropdown {
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

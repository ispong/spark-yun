<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-seach-table driver-management-page">
        <div class="zqy-table-top">
            <el-button class="driver-toolbar-button" type="primary" @click="addData">新建驱动</el-button>
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
            <Transition name="driver-batch-slide">
                <div v-if="selectedRows.length" class="driver-batch-mask">
                    <div class="driver-batch-actions">
                        <el-button
                            class="driver-batch-action"
                            :loading="batchLoading"
                            @click="batchDeleteData"
                        >
                            删除
                        </el-button>
                        <el-button
                            class="driver-batch-cancel"
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
                        <span class="name-click" @click="editData(scopeSlot.row)">
                            {{ scopeSlot.row.name }}
                        </span>
                    </template>
                    <template #dbTypeSlot="scopeSlot">
                        <el-tag class="driver-type-tag">{{ getDriverTypeName(scopeSlot.row.dbType) }}</el-tag>
                    </template>
                    <template #defaultTag="scopeSlot">
                        <el-tag v-if="scopeSlot.row.isDefaultDriver" class="driver-default-tag" type="success">是</el-tag>
                        <el-tag v-else class="driver-default-tag" type="info">否</el-tag>
                    </template>
                    <template #options="scopeSlot">
                        <div class="btn-group driver-action-group">
                            <span
                                v-if="!scopeSlot.row.isDefaultDriver"
                                class="driver-action-button"
                                @click="setDefaultDriver(scopeSlot.row)"
                            >
                                默认
                            </span>
                            <span v-else class="driver-action-button" @click="setDefaultDriver(scopeSlot.row)">
                                取消
                            </span>
                            <el-dropdown trigger="click" popper-class="driver-action-dropdown">
                                <span class="click-show-more driver-action-button">更多</span>
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
        <AddModal ref="addModalRef" />
    </div>
</template>

<script lang="ts" setup>
import { reactive, ref, onMounted } from 'vue'
import AddModal from './add-modal/index.vue'
import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import BlockTable from '@/app/components/block-table/index.vue'
import LoadingPage from '@/app/components/loading/index.vue'

import { BreadCrumbList, TableConfig } from './driver.config'
import {
    GetDriverListData,
    DeleteDefaultDriverData,
    AddDefaultDriverData,
    SetDefaultDriverData,
    UpdateDefaultDriverRemark
} from '@/modules/driver-management/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const breadCrumbList = reactive(BreadCrumbList)
const tableConfig: any = reactive(TableConfig)
const keyword = ref('')
const loading = ref(false)
const networkError = ref(false)
const selectedRows = ref<any[]>([])
const batchLoading = ref(false)
const addModalRef = ref(null)
const typeList = [
    { label: 'Clickhouse', value: 'CLICKHOUSE' },
    { label: 'Db2', value: 'DB2' },
    { label: 'Doris', value: 'DORIS' },
    { label: 'DuckDB', value: 'DUCK_DB' },
    { label: '达梦', value: 'DM' },
    { label: 'Gauss', value: 'GAUSS' },
    { label: 'Gbase', value: 'GBASE' },
    { label: 'Greenplum', value: 'GREENPLUM' },
    { label: 'H2', value: 'H2' },
    { label: 'HanaSap', value: 'HANA_SAP' },
    { label: 'Hive', value: 'HIVE' },
    { label: 'Impala', value: 'IMPALA' },
    { label: 'Mysql', value: 'MYSQL' },
    { label: 'OceanBase', value: 'OCEANBASE' },
    { label: 'OpenGauss', value: 'OPEN_GAUSS' },
    { label: 'Oracle', value: 'ORACLE' },
    { label: 'PostgreSql', value: 'POSTGRE_SQL' },
    { label: 'Presto', value: 'PRESTO' },
    { label: 'SelectDB', value: 'SELECT_DB' },
    { label: 'SqlServer', value: 'SQL_SERVER' },
    { label: 'StarRocks', value: 'STAR_ROCKS' },
    { label: 'Sybase', value: 'SYBASE' },
    { label: 'TDengine', value: 'T_DENGINE' },
    { label: 'TiDB', value: 'TIDB' },
    { label: 'Trino', value: 'TRINO' }
]

function initData(tableLoading?: boolean) {
    loading.value = tableLoading ? false : true
    networkError.value = networkError.value || false
    GetDriverListData({
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

function getDriverTypeName(dbType: string): string {
    return typeList.find((item) => item.value === dbType)?.label || dbType || '--'
}

function addData() {
    addModalRef.value.showModal((data: any) => {
        return new Promise((resolve: any, reject: any) => {
            const formData = new FormData()
            formData.append('dbType', data.dbType)
            formData.append('name', data.name)
            formData.append('remark', data.remark)
            formData.append('driver', data.driver)
            AddDefaultDriverData(formData)
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

// 删除
function deleteData(data: any) {
    ElMessageBox.confirm('确定删除该驱动吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        DeleteDefaultDriverData({
            driverId: data.id
        })
            .then((res: any) => {
                ElMessage.success(res.msg)
                initData()
            })
            .catch(() => {})
    })
}

// 修改备注
function editData(data: any) {
    addModalRef.value.showModal((formData: any) => {
        return new Promise((resolve: any, reject: any) => {
            UpdateDefaultDriverRemark({
                id: formData.id,
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

    ElMessageBox.confirm(`确定删除选中的 ${selectedRows.value.length} 个驱动吗？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        batchLoading.value = true
        Promise.all(
            selectedRows.value.map((row: any) =>
                DeleteDefaultDriverData({
                    driverId: row.id
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

// 设置默认
function setDefaultDriver(data: any) {
    SetDefaultDriverData({
        driverId: data.id,
        isDefaultDriver: !data.isDefaultDriver
    })
        .then((res: any) => {
            ElMessage.success(res.msg)
            initData(true)
        })
        .catch(() => {})
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
.zqy-seach-table.driver-management-page {
    .zqy-table-top {
        position: relative;
        overflow: hidden;
    }

    .driver-toolbar-button {
        width: 92px;
        height: 32px;
        padding: 8px 15px;
        box-sizing: border-box;
        line-height: 1;
    }

    .name-click {
        cursor: pointer;
        color: getCssVar('color', 'primary', 'light-5');

        &:hover {
            color: getCssVar('color', 'primary');
        }
    }

    .driver-batch-mask {
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

    .driver-batch-actions {
        display: flex;
        align-items: center;
        gap: 10px;

        .driver-batch-action {
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

        .driver-batch-cancel {
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

    .driver-batch-slide-enter-active,
    .driver-batch-slide-leave-active {
        transition:
            transform 0.18s ease,
            opacity 0.18s ease;
        will-change: transform, opacity;
    }

    .driver-batch-slide-enter-from,
    .driver-batch-slide-leave-to {
        opacity: 0;
        transform: translateY(-100%);
    }

    .driver-batch-slide-enter-to,
    .driver-batch-slide-leave-from {
        opacity: 1;
        transform: translateY(0);
    }

    .driver-type-tag,
    .driver-default-tag {
        max-width: 100%;
        white-space: nowrap;
    }

    .driver-type-tag {
        color: getCssVar('text-color', 'regular');
        border-color: getCssVar('border-color', 'light');
        background-color: getCssVar('fill-color', 'lighter');
    }

    .driver-action-group {
        justify-content: center;
        gap: 16px;

        .driver-action-button {
            display: inline-flex;
            align-items: center;
            line-height: 1;
            font-size: getCssVar('font-size', 'extra-small');
        }
    }
}

.driver-action-dropdown {
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

<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-seach-table oauth-management-page">
        <div class="zqy-table-top">
            <el-button type="primary" @click="addData">新建免密</el-button>
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
            <Transition name="oauth-batch-slide">
                <div v-if="selectedRows.length" class="oauth-batch-mask">
                    <div class="oauth-batch-actions">
                        <el-button class="oauth-batch-action" :loading="batchLoading" @click="batchEnableOauth">
                            启用
                        </el-button>
                        <el-button class="oauth-batch-action" :loading="batchLoading" @click="batchDisableOauth">
                            禁用
                        </el-button>
                        <el-button class="oauth-batch-action" :loading="batchLoading" @click="batchDeleteOauth">
                            删除
                        </el-button>
                        <el-button class="oauth-batch-cancel" :disabled="batchLoading" @click="cancelSelection">
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
                    <template #name="scopeSlot">
                        <span class="name-click" @click="editData(scopeSlot.row)">{{ scopeSlot.row.name }}</span>
                    </template>
                    <template #ssoType="scopeSlot">
                        <el-tag>{{ getSsoTypeLabel(scopeSlot.row.ssoType) }}</el-tag>
                    </template>
                    <template #statusTag="scopeSlot">
                        <el-tag v-if="scopeSlot.row.status === 'ENABLE'" type="success">启用</el-tag>
                        <el-tag v-if="scopeSlot.row.status === 'DISABLE'" type="danger">禁用</el-tag>
                    </template>
                    <template #options="scopeSlot">
                        <div class="btn-group oauth-action-group">
                            <span class="oauth-action-button" @click="editData(scopeSlot.row)">编辑</span>
                            <el-dropdown trigger="click" popper-class="oauth-action-dropdown">
                                <span class="click-show-more oauth-action-button">更多</span>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item @click="getUrlEvent(scopeSlot.row)">
                                            复制链接
                                        </el-dropdown-item>
                                        <el-dropdown-item
                                            :disabled="scopeSlot.row.statusLoading"
                                            @click="
                                                !scopeSlot.row.statusLoading &&
                                                    changeStatus(scopeSlot.row, scopeSlot.row.status !== 'ENABLE')
                                            "
                                        >
                                            <span v-if="!scopeSlot.row.statusLoading">
                                                {{ scopeSlot.row.status === 'ENABLE' ? '禁用' : '启用' }}
                                            </span>
                                            <el-icon v-else class="is-loading">
                                                <Loading />
                                            </el-icon>
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
import BlockTable from '@/app/components/block-table/index.vue'
import LoadingPage from '@/app/components/loading/index.vue'
import AddModal from './add-modal/index.vue'
import { BreadCrumbList, TableConfig } from './list.config'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import {
    OauthQueryList,
    OauthCreateData,
    OauthUpdateData,
    OauthDeleteData,
    OauthEnableData,
    OauthDisableData,
    OauthUrlCopy
} from '@/app/api'

const keyword = ref<string>('')
const loading = ref<boolean>(false)
const networkError = ref<boolean>(false)
const selectedRows = ref<any[]>([])
const batchLoading = ref(false)
const addModalRef = ref<any>(null)

const breadCrumbList = reactive(BreadCrumbList)
const tableConfig: any = reactive(TableConfig)

function initData(tableLoading?: boolean) {
    loading.value = tableLoading ? false : true
    networkError.value = networkError.value || false
    OauthQueryList({
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
            OauthCreateData(data)
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
function editData(row: any) {
    addModalRef.value.showModal((data: any) => {
        return new Promise((resolve: any, reject: any) => {
            OauthUpdateData(data)
                .then((res: any) => {
                    ElMessage.success(res.msg)
                    initData()
                    resolve()
                })
                .catch((error: any) => {
                    reject(error)
                })
        })
    }, row)
}

function handleSelectionChange(records: any[]) {
    selectedRows.value = records || []
}

function cancelSelection() {
    selectedRows.value = []
    tableConfig.tableData = [...tableConfig.tableData]
}

function batchEnableOauth() {
    const disableRows = selectedRows.value.filter((row: any) => row.status === 'DISABLE')
    if (!disableRows.length) {
        ElMessage.warning('请选择禁用状态的免密配置')
        return
    }

    batchLoading.value = true
    Promise.all(disableRows.map((row: any) => OauthEnableData({ id: row.id })))
        .then(() => {
            ElMessage.success('批量启用成功')
            initData(true)
        })
        .catch(() => {})
        .finally(() => {
            batchLoading.value = false
        })
}

function batchDisableOauth() {
    const enableRows = selectedRows.value.filter((row: any) => row.status === 'ENABLE')
    if (!enableRows.length) {
        ElMessage.warning('请选择启用状态的免密配置')
        return
    }

    batchLoading.value = true
    Promise.all(enableRows.map((row: any) => OauthDisableData({ id: row.id })))
        .then(() => {
            ElMessage.success('批量禁用成功')
            initData(true)
        })
        .catch(() => {})
        .finally(() => {
            batchLoading.value = false
        })
}

function batchDeleteOauth() {
    if (!selectedRows.value.length) {
        return
    }

    ElMessageBox.confirm(`确定删除选中的 ${selectedRows.value.length} 个免密配置吗？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        batchLoading.value = true
        Promise.all(selectedRows.value.map((row: any) => OauthDeleteData({ id: row.id })))
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

// 启用 or 禁用
function changeStatus(data: any, status: boolean) {
    data.statusLoading = true
    if (status) {
        OauthEnableData({
            id: data.id
        })
            .then((res: any) => {
                ElMessage.success(res.msg)
                data.statusLoading = false
                initData(true)
            })
            .catch(() => {
                data.statusLoading = false
            })
    } else {
        OauthDisableData({
            id: data.id
        })
            .then((res: any) => {
                ElMessage.success(res.msg)
                data.statusLoading = false
                initData(true)
            })
            .catch(() => {
                data.statusLoading = false
            })
    }
}

// 删除
function deleteData(data: any) {
    ElMessageBox.confirm('确定删除该配置吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        OauthDeleteData({
            id: data.id
        })
            .then((res: any) => {
                ElMessage.success(res.msg)
                initData()
            })
            .catch(() => {})
    })
}
// 复制链接
function getUrlEvent(data: any) {
    OauthUrlCopy({
        id: data.id
    })
        .then(async (res: any) => {
            await navigator.clipboard.writeText(res.data.invokeUrl)
            ElMessage.success('复制成功')
        })
        .catch(() => {})
}

function inputEvent(e: string) {
    if (e === '') {
        initData()
    }
}

function getSsoTypeLabel(ssoType: string) {
    const ssoTypeLabelMap: Record<string, string> = {
        GITHUB: 'Github',
        KEYCLOAK: 'Keycloak'
    }
    return ssoTypeLabelMap[ssoType] || ssoType
}

function handleSizeChange(e: number) {
    tableConfig.pagination.pageSize = e
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
.zqy-seach-table.oauth-management-page {
    .zqy-table-top {
        position: relative;
        overflow: hidden;
    }

    .oauth-batch-mask {
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

    .oauth-batch-slide-enter-active,
    .oauth-batch-slide-leave-active {
        transition:
            transform 0.18s ease,
            opacity 0.18s ease;
        will-change: transform, opacity;
    }

    .oauth-batch-slide-enter-from,
    .oauth-batch-slide-leave-to {
        opacity: 0;
        transform: translateY(-100%);
    }

    .oauth-batch-slide-enter-to,
    .oauth-batch-slide-leave-from {
        opacity: 1;
        transform: translateY(0);
    }

    .oauth-batch-actions {
        display: flex;
        align-items: center;
        gap: 12px;
    }

    .oauth-batch-action {
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

    .oauth-batch-cancel {
        height: 32px;
        line-height: 30px;
    }

    .oauth-action-group {
        justify-content: center;
        gap: 16px;

        .oauth-action-button {
            display: inline-flex;
            align-items: center;
            line-height: 1;
            font-size: getCssVar('font-size', 'extra-small');
            color: getCssVar('color', 'primary');
            cursor: pointer;
            white-space: nowrap;
        }
    }
}

.oauth-action-dropdown {
    .el-dropdown-menu {
        padding: 4px 0;
        min-width: 76px;
    }

    .el-dropdown-menu__item {
        height: 26px;
        line-height: 26px;
        font-family: Avenir, Helvetica, Arial, sans-serif;
        font-size: getCssVar('font-size', 'extra-small');

        .el-icon {
            font-size: getCssVar('font-size', 'extra-small');
        }
    }
}
</style>

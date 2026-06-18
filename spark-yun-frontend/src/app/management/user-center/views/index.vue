<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-seach-table user-center-page">
        <div class="zqy-table-top">
            <el-button type="primary" @click="addData">新建用户</el-button>
            <div class="zqy-seach">
                <el-input
                    v-model="keyword"
                    placeholder="请输入名称/手机号/邮箱 回车进行搜索"
                    :maxlength="200"
                    clearable
                    @input="inputEvent"
                    @keyup.enter="initData(false)"
                />
            </div>
            <Transition name="user-batch-slide">
                <div v-if="selectedRows.length" class="user-batch-mask">
                    <div class="user-batch-actions">
                        <el-button class="user-batch-action" :loading="batchLoading" @click="batchEnableUsers">
                            启用
                        </el-button>
                        <el-button class="user-batch-action" :loading="batchLoading" @click="batchDisableUsers">
                            禁用
                        </el-button>
                        <el-button
                            v-if="canManagePlatformAdmin"
                            class="user-batch-action"
                            :loading="batchLoading"
                            @click="batchSetPlatformAdmin"
                        >
                            设为管理员
                        </el-button>
                        <el-button
                            v-if="canManagePlatformAdmin"
                            class="user-batch-action"
                            :loading="batchLoading"
                            @click="batchCancelPlatformAdmin"
                        >
                            取消管理员
                        </el-button>
                        <el-button class="user-batch-action" :loading="batchLoading" @click="batchDeleteUsers">
                            删除
                        </el-button>
                        <el-button class="user-batch-cancel" :disabled="batchLoading" @click="cancelSelection">
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
                    <template #account="scopeSlot">
                        <span class="name-click" @click="editData(scopeSlot.row)">{{ scopeSlot.row.account }}</span>
                    </template>
                    <template #statusTag="scopeSlot">
                        <div class="btn-group">
                            <el-tag v-if="scopeSlot.row.status === 'ENABLE'" class="ml-2" type="success">启用</el-tag>
                            <el-tag v-if="scopeSlot.row.status === 'DISABLE'" class="ml-2" type="danger">禁用</el-tag>
                        </div>
                    </template>
                    <template #platformAdmin="scopeSlot">
                        <el-tag v-if="scopeSlot.row.platformSuperAdmin" type="danger">平台超级管理员</el-tag>
                        <el-tag v-else-if="scopeSlot.row.platformAdmin" type="warning">平台管理员</el-tag>
                        <el-tag v-else class="platform-user-tag">平台用户</el-tag>
                    </template>
                    <template #options="scopeSlot">
                        <div class="btn-group user-action-group">
                            <span class="user-action-button" @click="editData(scopeSlot.row)">编辑</span>
                            <el-dropdown trigger="click" popper-class="user-action-dropdown">
                                <span class="click-show-more user-action-button">更多</span>
                                <template #dropdown>
                                    <el-dropdown-menu>
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
                                        <el-dropdown-item @click="changePassword(scopeSlot.row)">
                                            修改密码
                                        </el-dropdown-item>
                                        <el-dropdown-item
                                            v-if="canSetPlatformAdmin(scopeSlot.row)"
                                            @click="changePlatformAdmin(scopeSlot.row, true)"
                                        >
                                            设为管理员
                                        </el-dropdown-item>
                                        <el-dropdown-item
                                            v-if="canCancelPlatformAdmin(scopeSlot.row)"
                                            @click="changePlatformAdmin(scopeSlot.row, false)"
                                        >
                                            取消管理员
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
        <PasswordModal ref="passwordModalRef" />
    </div>
</template>

<script lang="ts" setup>
import { reactive, ref, onMounted, computed } from 'vue'
import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import BlockTable from '@/app/components/block-table/index.vue'
import LoadingPage from '@/app/components/loading/index.vue'
import AddModal from './add-modal/index.vue'
import PasswordModal from './password-modal/index.vue'

import { BreadCrumbList, TableConfig } from './user-center.config'
import {
    GetUserCenterList,
    DisableUser,
    EnableUser,
    DeleteUser,
    AddUserData,
    UpdateUserData,
    UpdateUserPassword,
    SetPlatformAdmin
} from '@/app/management/user-center/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/app/store/useAuth'

interface FormUser {
    account: string
    email: string
    passwd?: string
    phone: string
    remark: string
    username: string
    id?: string
}

const breadCrumbList = reactive(BreadCrumbList)
const tableConfig: any = reactive(TableConfig)
const keyword = ref('')
const loading = ref(false)
const networkError = ref(false)
const selectedRows = ref<any[]>([])
const batchLoading = ref(false)
const addModalRef = ref(null)
const passwordModalRef = ref(null)
const authStore = useAuthStore()
const canManagePlatformAdmin = computed(() => authStore.userInfo?.platformSuperAdmin || authStore.userInfo?.platformAdmin)

function initData(tableLoading?: boolean) {
    loading.value = tableLoading ? false : true
    networkError.value = networkError.value || false
    GetUserCenterList({
        page: tableConfig.pagination.currentPage - 1,
        pageSize: tableConfig.pagination.pageSize,
        searchKeyWord: keyword.value
    })
        .then((res: any) => {
            tableConfig.tableData = res.data.content
            tableConfig.pagination.total = res.data.totalElements ?? res.data.total ?? res.data.content?.length ?? 0
            selectedRows.value = []
            loading.value = false
            tableConfig.loading = false
            networkError.value = false
        })
        .catch(() => {
            tableConfig.tableData = []
            tableConfig.pagination.total = 0
            loading.value = false
            tableConfig.loading = false
            networkError.value = true
        })
}

function addData() {
    addModalRef.value.showModal((formData: FormUser) => {
        return new Promise((resolve: any, reject: any) => {
            AddUserData(formData)
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
    addModalRef.value.showModal((formData: FormUser) => {
        return new Promise((resolve: any, reject: any) => {
            UpdateUserData(formData)
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

function changePassword(data: any) {
    passwordModalRef.value.showModal((formData: any) => {
        return new Promise((resolve: any, reject: any) => {
            UpdateUserPassword(formData)
                .then((res: any) => {
                    ElMessage.success(res.msg)
                    resolve()
                })
                .catch((error: any) => {
                    reject(error)
                })
        })
    }, data)
}

function canSetPlatformAdmin(data: any) {
    return canManagePlatformAdmin.value && !data.platformSuperAdmin && !data.platformAdmin
}

function canCancelPlatformAdmin(data: any) {
    return canManagePlatformAdmin.value && !data.platformSuperAdmin && data.platformAdmin
}

function changePlatformAdmin(data: any, platformAdmin: boolean) {
    SetPlatformAdmin({
        userId: data.id,
        platformAdmin
    }).then((res: any) => {
        ElMessage.success(res.msg)
        initData(true)
    })
}

function handleSelectionChange(records: any[]) {
    selectedRows.value = records || []
}

function cancelSelection() {
    selectedRows.value = []
    tableConfig.tableData = [...tableConfig.tableData]
}

function batchEnableUsers() {
    const disableRows = selectedRows.value.filter((row: any) => row.status === 'DISABLE')
    if (!disableRows.length) {
        ElMessage.warning('请选择禁用状态的用户')
        return
    }

    batchLoading.value = true
    Promise.all(
        disableRows.map((row: any) =>
            EnableUser({
                userId: row.id
            })
        )
    )
        .then(() => {
            ElMessage.success('批量启用成功')
            initData(true)
        })
        .catch(() => {})
        .finally(() => {
            batchLoading.value = false
        })
}

function batchDisableUsers() {
    const enableRows = selectedRows.value.filter((row: any) => row.status === 'ENABLE')
    if (!enableRows.length) {
        ElMessage.warning('请选择启用状态的用户')
        return
    }

    batchLoading.value = true
    Promise.all(
        enableRows.map((row: any) =>
            DisableUser({
                userId: row.id
            })
        )
    )
        .then(() => {
            ElMessage.success('批量禁用成功')
            initData(true)
        })
        .catch(() => {})
        .finally(() => {
            batchLoading.value = false
        })
}

function batchSetPlatformAdmin() {
    const normalRows = selectedRows.value.filter((row: any) => !row.platformSuperAdmin && !row.platformAdmin)
    if (!normalRows.length) {
        ElMessage.warning('请选择可设为管理员的用户')
        return
    }

    batchLoading.value = true
    Promise.all(
        normalRows.map((row: any) =>
            SetPlatformAdmin({
                userId: row.id,
                platformAdmin: true
            })
        )
    )
        .then(() => {
            ElMessage.success('批量设为管理员成功')
            initData(true)
        })
        .catch(() => {})
        .finally(() => {
            batchLoading.value = false
        })
}

function batchCancelPlatformAdmin() {
    const platformAdminRows = selectedRows.value.filter((row: any) => !row.platformSuperAdmin && row.platformAdmin)
    if (!platformAdminRows.length) {
        ElMessage.warning('请选择可取消管理员的用户')
        return
    }

    batchLoading.value = true
    Promise.all(
        platformAdminRows.map((row: any) =>
            SetPlatformAdmin({
                userId: row.id,
                platformAdmin: false
            })
        )
    )
        .then(() => {
            ElMessage.success('批量取消管理员成功')
            initData(true)
        })
        .catch(() => {})
        .finally(() => {
            batchLoading.value = false
        })
}

function batchDeleteUsers() {
    if (!selectedRows.value.length) {
        return
    }

    ElMessageBox.confirm(`确定删除选中的 ${selectedRows.value.length} 个用户吗？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        batchLoading.value = true
        Promise.all(
            selectedRows.value.map((row: any) =>
                DeleteUser({
                    userId: row.id
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

// 启用 or 禁用
function changeStatus(data: any, status: boolean) {
    data.statusLoading = true
    if (status) {
        EnableUser({
            userId: data.id
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
        DisableUser({
            userId: data.id
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
    ElMessageBox.confirm('确定删除该成员吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        DeleteUser({
            userId: data.id
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
})
</script>

<style lang="scss">
.zqy-seach-table.user-center-page {
    .zqy-table-top {
        position: relative;
        overflow: hidden;
        .user-batch-mask {
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
        .user-batch-slide-enter-active,
        .user-batch-slide-leave-active {
            transition:
                transform 0.18s ease,
                opacity 0.18s ease;
            will-change: transform, opacity;
        }
        .user-batch-slide-enter-from,
        .user-batch-slide-leave-to {
            opacity: 0;
            transform: translateY(-100%);
        }
        .user-batch-slide-enter-to,
        .user-batch-slide-leave-from {
            opacity: 1;
            transform: translateY(0);
        }
    }
    .user-batch-actions {
        display: flex;
        align-items: center;
        gap: 10px;
        .user-batch-action {
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
        .user-batch-cancel {
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
    .zqy-table {
        .platform-user-tag {
            --el-tag-text-color: #2563eb;
            --el-tag-border-color: #93c5fd;
            --el-tag-bg-color: #eff6ff;
        }
        .user-action-group {
            justify-content: center;
            gap: 16px;
            .user-action-button {
                display: inline-flex;
                align-items: center;
                line-height: 1;
                font-size: getCssVar('font-size', 'extra-small');
            }
        }
    }
}

.user-action-dropdown {
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

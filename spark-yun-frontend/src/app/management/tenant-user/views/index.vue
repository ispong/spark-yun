<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-seach-table tenant-user-page">
        <div class="zqy-table-top">
            <div class="tenant-user-toolbar-left">
                <el-button type="primary" @click="handlePrimaryAction">
                    {{ isPlatformTenantMemberPage ? '添加成员' : '邀请码' }}
                </el-button>
                <el-select
                    v-if="isPlatformTenantMemberPage"
                    v-model="selectedTenantId"
                    class="tenant-user-tenant-select"
                    filterable
                    placeholder="请选择租户"
                    @change="handlePlatformTenantChange"
                >
                    <el-option
                        v-for="tenant in platformTenants"
                        :key="tenant.id"
                        :label="tenant.name"
                        :value="tenant.id"
                    />
                </el-select>
            </div>
            <div class="zqy-seach">
                <el-input
                    v-model="keyword"
                    placeholder="请输入用户名/手机号/邮箱 回车进行搜索"
                    :maxlength="200"
                    clearable
                    @input="inputEvent"
                    @keyup.enter="initData(false)"
                />
            </div>
            <Transition name="tenant-user-batch-slide">
                <div v-if="selectedRows.length" class="tenant-user-batch-mask">
                    <div class="tenant-user-batch-actions">
                        <el-button class="tenant-user-batch-action" :loading="batchLoading" @click="batchEnableMembers">
                            启用
                        </el-button>
                        <el-button class="tenant-user-batch-action" :loading="batchLoading" @click="batchDisableMembers">
                            禁用
                        </el-button>
                        <el-button class="tenant-user-batch-action" :loading="batchLoading" @click="batchGiveAuth">
                            设为管理员
                        </el-button>
                        <el-button class="tenant-user-batch-action" :loading="batchLoading" @click="batchRemoveAuth">
                            取消管理员
                        </el-button>
                        <el-button class="tenant-user-batch-action" :loading="batchLoading" @click="batchDeleteMembers">
                            移除
                        </el-button>
                        <el-button class="tenant-user-batch-cancel" :disabled="batchLoading" @click="cancelSelection">
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
                    <template #roleCode="scopeSlot">
                        <div class="btn-group">
                            <el-tag v-if="isTenantSuperAdmin(scopeSlot.row)" class="ml-2" type="danger">
                                超级管理员
                            </el-tag>
                            <el-tag v-else-if="isTenantAdmin(scopeSlot.row)" type="warning">管理员</el-tag>
                            <el-tag v-else type="success">成员</el-tag>
                        </div>
                    </template>
                    <template #status="scopeSlot">
                        <div class="btn-group">
                            <el-tag v-if="scopeSlot.row.status === 'ENABLE'" class="ml-2" type="success">启用</el-tag>
                            <el-tag v-if="scopeSlot.row.status === 'DISABLE'" class="ml-2" type="danger">禁用</el-tag>
                            <el-tag v-if="scopeSlot.row.status === 'APPLYING'" class="ml-2" type="warning">
                                申请中
                            </el-tag>
                        </div>
                    </template>
                    <template #options="scopeSlot">
                        <div class="btn-group tenant-user-action-group">
                            <span class="tenant-user-action-button" @click="editData(scopeSlot.row)">编辑</span>
                            <el-dropdown trigger="click" popper-class="tenant-user-action-dropdown">
                                <span class="click-show-more tenant-user-action-button">更多</span>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <template v-if="isApplying(scopeSlot.row)">
                                            <el-dropdown-item @click="approveApply(scopeSlot.row)">
                                                通过申请
                                            </el-dropdown-item>
                                            <el-dropdown-item @click="rejectApply(scopeSlot.row)">
                                                拒绝申请
                                            </el-dropdown-item>
                                        </template>
                                        <template v-else>
                                            <el-dropdown-item
                                                :disabled="scopeSlot.row.authLoading"
                                                @click="
                                                    !scopeSlot.row.authLoading &&
                                                        (scopeSlot.row.normalAdmin
                                                            ? removeAuth(scopeSlot.row)
                                                            : giveAuth(scopeSlot.row))
                                                "
                                            >
                                                <span v-if="!scopeSlot.row.authLoading">
                                                    {{
                                                        scopeSlot.row.normalAdmin ? '取消管理员' : '设为管理员'
                                                    }}
                                                </span>
                                                <el-icon v-else class="is-loading">
                                                    <Loading />
                                                </el-icon>
                                            </el-dropdown-item>
                                            <el-dropdown-item @click="changeMemberStatus(scopeSlot.row)">
                                                {{ scopeSlot.row.status === 'ENABLE' ? '禁用' : '启用' }}
                                            </el-dropdown-item>
                                            <el-dropdown-item @click="deleteData(scopeSlot.row)">移除</el-dropdown-item>
                                        </template>
                                    </el-dropdown-menu>
                                </template>
                            </el-dropdown>
                        </div>
                    </template>
                </BlockTable>
            </div>
        </LoadingPage>
        <AddModal ref="addModalRef" />
        <el-dialog
            v-model="inviteDialogVisible"
            class="tenant-user-invite-dialog"
            title="租户邀请码"
            width="520px"
        >
            <el-form class="tenant-user-invite-form" label-position="top">
                <el-form-item label="邀请码">
                    <el-input v-model="inviteForm.inviteCode" readonly>
                        <template #append>
                            <el-button @click="copyInviteCode">复制</el-button>
                        </template>
                    </el-input>
                </el-form-item>
                <el-form-item label="有效期">
                    <el-select v-model="inviteForm.validDays">
                        <el-option label="1 天" :value="1" />
                        <el-option label="7 天" :value="7" />
                        <el-option label="30 天" :value="30" />
                        <el-option label="永久有效" :value="0" />
                    </el-select>
                </el-form-item>
                <el-form-item label="绑定角色">
                    <el-select v-model="inviteForm.roleIds" multiple clearable placeholder="可不选择角色">
                        <el-option
                            v-for="role in availableRoles"
                            :key="role.id"
                            :label="role.name"
                            :value="role.id"
                        />
                    </el-select>
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="tenant-user-invite-footer">
                    <el-button @click="inviteDialogVisible = false">取消</el-button>
                    <el-button :loading="inviteSaving" @click="saveInviteCode(true)">重新生成</el-button>
                    <el-button type="primary" :loading="inviteSaving" @click="saveInviteCode(false)">
                        保存设置
                    </el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import BlockTable from '@/app/components/block-table/index.vue'
import LoadingPage from '@/app/components/loading/index.vue'
import AddModal from './add-modal/index.vue'

import { BreadCrumbList, TableConfig } from './tenant-user.config'
import {
    GetUserList,
    AddTenantUserData,
    DeleteTenantUser,
    GiveAuth,
    RemoveAuth,
    GetTenantInviteCode,
    SaveTenantInviteCode,
    ApproveTenantApply,
    RejectTenantApply,
    SetTenantMemberStatus
} from '@/app/management/tenant-user/api'
import { ElMessage, ElMessageBox } from 'element-plus'

import { useSwitchTenant } from '@/app/hooks/switch-tenant'
import { useAuthStore } from '@/app/store/useAuth'
import { ListRole } from '@/app/management/admin/api'
import { GetTenantList } from '@/app/management/tenant-list/api'

interface FormUser {
    isTenantAdmin: boolean
    userId: string
}

const breadCrumbList = reactive(BreadCrumbList)
const tableConfig: any = reactive(TableConfig)
const keyword = ref('')
const loading = ref(false)
const networkError = ref(false)
const selectedRows = ref<any[]>([])
const batchLoading = ref(false)
const addModalRef = ref(null)
const authStore = useAuthStore()
const availableRoles = ref<any[]>([])
const route = useRoute()
const platformTenants = ref<any[]>([])
const selectedTenantId = ref('')
const inviteDialogVisible = ref(false)
const inviteSaving = ref(false)
const inviteForm = reactive({
    inviteCode: '',
    validDays: 7,
    roleIds: [] as string[]
})

const { currentTenant, tenantList, initSwitchTenant, onTenantChange } = useSwitchTenant()
const isPlatformTenantMemberPage = computed(() => route.path.startsWith('/platform'))
const activeTenantId = computed(() => (isPlatformTenantMemberPage.value ? selectedTenantId.value : currentTenant.value.id))

function normalizeRoleCode(roleCode?: string) {
    return roleCode?.replace(/^ROLE_/, '')
}

function isTenantSuperAdmin(data: any) {
    return normalizeRoleCode(data.roleCode) === 'TENANT_SUPER_ADMIN'
}

function isTenantAdmin(data: any) {
    return normalizeRoleCode(data.roleCode) === 'TENANT_ADMIN' || data.normalAdmin
}

function isApplying(data: any) {
    return data.status === 'APPLYING'
}

function initData(tableLoading?: boolean) {
    if (!activeTenantId.value) {
        tableConfig.tableData = []
        tableConfig.pagination.total = 0
        loading.value = false
        tableConfig.loading = false
        networkError.value = false
        return
    }

    loading.value = tableLoading ? false : true
    networkError.value = networkError.value || false
    GetUserList({
        page: tableConfig.pagination.currentPage - 1,
        pageSize: tableConfig.pagination.pageSize,
        searchKeyWord: keyword.value,
        tenantId: activeTenantId.value
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
            loading.value = false
            tableConfig.loading = false
            networkError.value = true
        })
}

function initDefaultTenantUserData() {
    if (isPlatformTenantMemberPage.value) {
        initPlatformTenantUserData()
        return
    }

    initSwitchTenant()
        .then(() => {
            if (!tenantList.value.length) {
                tableConfig.tableData = []
                tableConfig.pagination.total = 0
                loading.value = false
                tableConfig.loading = false
                networkError.value = false
                return
            }
            const activeTenant = tenantList.value.find((item) => item.id === authStore.tenantId) || tenantList.value[0]
            onTenantChange(activeTenant.id)
            loadAvailableRoles()
            initData()
        })
        .catch(() => {
            tableConfig.tableData = []
            tableConfig.pagination.total = 0
            loading.value = false
            tableConfig.loading = false
            networkError.value = true
        })
}

function initPlatformTenantUserData() {
    GetTenantList({
        page: 0,
        pageSize: 999,
        searchKeyWord: ''
    })
        .then((res: any) => {
            platformTenants.value = res.data.content || []
            selectedTenantId.value = selectedTenantId.value || platformTenants.value[0]?.id || ''
            loadAvailableRoles()
            initData()
        })
        .catch(() => {
            platformTenants.value = []
            selectedTenantId.value = ''
            tableConfig.tableData = []
            tableConfig.pagination.total = 0
            loading.value = false
            tableConfig.loading = false
            networkError.value = true
        })
}

function handlePlatformTenantChange() {
    tableConfig.pagination.currentPage = 1
    loadAvailableRoles()
    initData()
}

function handlePrimaryAction() {
    if (isPlatformTenantMemberPage.value) {
        addData()
        return
    }
    openInviteDialog()
}

function addData() {
    addModalRef.value.showModal((formData: FormUser) => {
        return new Promise((resolve: any, reject: any) => {
            AddTenantUserData({
                ...formData,
                tenantId: activeTenantId.value
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

function editData(data: any) {
    addModalRef.value.showModal((formData: FormUser) => {
        return new Promise((resolve: any, reject: any) => {
            AddTenantUserData({
                ...formData,
                tenantId: activeTenantId.value
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

function openInviteDialog() {
    if (!activeTenantId.value) {
        ElMessage.warning('请先选择租户')
        return
    }
    inviteDialogVisible.value = true
    loadAvailableRoles()
    GetTenantInviteCode({
        tenantId: activeTenantId.value
    }).then((res: any) => {
        inviteForm.inviteCode = res.data.inviteCode || ''
        inviteForm.validDays = res.data.validDays ?? 7
        inviteForm.roleIds = [...(res.data.roleIds || [])]
    })
}

function saveInviteCode(regenerate: boolean) {
    inviteSaving.value = true
    SaveTenantInviteCode({
        tenantId: activeTenantId.value,
        validDays: inviteForm.validDays,
        roleIds: inviteForm.roleIds,
        regenerate
    })
        .then((res: any) => {
            inviteForm.inviteCode = res.data.inviteCode || ''
            inviteForm.validDays = res.data.validDays ?? inviteForm.validDays
            inviteForm.roleIds = [...(res.data.roleIds || [])]
            ElMessage.success(regenerate ? '邀请码已重新生成' : '邀请码设置已保存')
        })
        .finally(() => {
            inviteSaving.value = false
        })
}

function copyInviteCode() {
    if (!inviteForm.inviteCode) {
        return
    }
    navigator.clipboard
        ?.writeText(inviteForm.inviteCode)
        .then(() => {
            ElMessage.success('邀请码已复制')
        })
        .catch(() => {
            ElMessage.error('复制失败，请手动复制')
        })
}

// 授权
function giveAuth(data: any) {
    data.authLoading = true
    GiveAuth({
        tenantUserId: data.id
    })
        .then((res: any) => {
            data.authLoading = false
            ElMessage.success(res.msg)
            initData(true)
        })
        .catch(() => {
            data.authLoading = false
        })
}

// 取消授权
function removeAuth(data: any) {
    data.authLoading = true
    RemoveAuth({
        tenantUserId: data.id
    })
        .then((res: any) => {
            data.authLoading = false
            ElMessage.success(res.msg)
            initData(true)
        })
        .catch(() => {
            data.authLoading = false
        })
}

function changeMemberStatus(data: any) {
    SetTenantMemberStatus({
        tenantUserId: data.id,
        status: data.status === 'ENABLE' ? 'DISABLE' : 'ENABLE'
    }).then((res: any) => {
        ElMessage.success(res.msg)
        initData(true)
    })
}

function approveApply(data: any) {
    ApproveTenantApply({
        tenantUserId: data.id
    }).then((res: any) => {
        ElMessage.success(res.msg)
        initData(true)
    })
}

function rejectApply(data: any) {
    ElMessageBox.confirm('确定拒绝该租户申请吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        RejectTenantApply({
            tenantUserId: data.id
        }).then((res: any) => {
            ElMessage.success(res.msg)
            initData()
        })
    })
}

function handleSelectionChange(records: any[]) {
    selectedRows.value = records || []
}

function cancelSelection() {
    selectedRows.value = []
    tableConfig.tableData = [...tableConfig.tableData]
}

function batchEnableMembers() {
    const disableRows = selectedRows.value.filter((row: any) => row.status === 'DISABLE')
    if (!disableRows.length) {
        ElMessage.warning('请选择禁用状态的成员')
        return
    }

    batchLoading.value = true
    Promise.all(
        disableRows.map((row: any) =>
            SetTenantMemberStatus({
                tenantUserId: row.id,
                status: 'ENABLE'
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

function batchDisableMembers() {
    const enableRows = selectedRows.value.filter((row: any) => row.status === 'ENABLE')
    if (!enableRows.length) {
        ElMessage.warning('请选择启用状态的成员')
        return
    }

    batchLoading.value = true
    Promise.all(
        enableRows.map((row: any) =>
            SetTenantMemberStatus({
                tenantUserId: row.id,
                status: 'DISABLE'
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

function batchGiveAuth() {
    const memberRows = selectedRows.value.filter(
        (row: any) => !isApplying(row) && !isTenantSuperAdmin(row) && !row.normalAdmin
    )
    if (!memberRows.length) {
        ElMessage.warning('请选择非管理员成员')
        return
    }

    batchLoading.value = true
    Promise.all(memberRows.map((row: any) => GiveAuth({ tenantUserId: row.id })))
        .then(() => {
            ElMessage.success('批量设为管理员成功')
            initData(true)
        })
        .catch(() => {})
        .finally(() => {
            batchLoading.value = false
        })
}

function batchRemoveAuth() {
    const adminRows = selectedRows.value.filter(
        (row: any) => !isApplying(row) && !isTenantSuperAdmin(row) && row.normalAdmin
    )
    if (!adminRows.length) {
        ElMessage.warning('请选择管理员成员')
        return
    }

    batchLoading.value = true
    Promise.all(adminRows.map((row: any) => RemoveAuth({ tenantUserId: row.id })))
        .then(() => {
            ElMessage.success('批量取消管理员成功')
            initData(true)
        })
        .catch(() => {})
        .finally(() => {
            batchLoading.value = false
        })
}

function batchDeleteMembers() {
    if (!selectedRows.value.length) {
        return
    }

    ElMessageBox.confirm(`确定移除选中的 ${selectedRows.value.length} 个成员吗？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        batchLoading.value = true
        Promise.all(selectedRows.value.map((row: any) => DeleteTenantUser({ tenantUserId: row.id })))
            .then(() => {
                ElMessage.success('批量移除成功')
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
    ElMessageBox.confirm('确定移除该成员吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        DeleteTenantUser({
            tenantUserId: data.id
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
    initData()
}

function handleCurrentChange(e: number) {
    tableConfig.pagination.currentPage = e
    initData()
}

onMounted(() => {
    tableConfig.pagination.currentPage = 1
    tableConfig.pagination.pageSize = 10
    initDefaultTenantUserData()
})

function loadAvailableRoles() {
    if (!activeTenantId.value) {
        availableRoles.value = []
        return
    }
    ListRole({
        tenantId: activeTenantId.value
    })
        .then((res: any) => {
            availableRoles.value = res.data || []
        })
        .catch(() => {
            availableRoles.value = []
        })
}
</script>

<style lang="scss">
.zqy-seach-table.tenant-user-page {
    .zqy-table-top {
        position: relative;
        overflow: hidden;
        .tenant-user-batch-mask {
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
        .tenant-user-batch-slide-enter-active,
        .tenant-user-batch-slide-leave-active {
            transition:
                transform 0.18s ease,
                opacity 0.18s ease;
            will-change: transform, opacity;
        }
        .tenant-user-batch-slide-enter-from,
        .tenant-user-batch-slide-leave-to {
            opacity: 0;
            transform: translateY(-100%);
        }
        .tenant-user-batch-slide-enter-to,
        .tenant-user-batch-slide-leave-from {
            opacity: 1;
            transform: translateY(0);
        }
    }

    .tenant-user-toolbar-left,
    .tenant-user-batch-actions {
        display: flex;
        align-items: center;
        gap: 12px;
    }

    .tenant-user-tenant-select {
        width: 220px;
    }

    .tenant-user-batch-action {
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

    .tenant-user-batch-cancel {
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

    .tenant-user-action-group {
        justify-content: center;
        gap: 16px;
        .tenant-user-action-button {
            display: inline-flex;
            align-items: center;
            line-height: 1;
            font-size: getCssVar('font-size', 'extra-small');
        }
    }
}

.tenant-user-action-dropdown {
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

.tenant-user-invite-dialog {
    --tenant-user-invite-x-padding: 20px;
    --tenant-user-invite-border-color: #ebeef5;

    border-radius: 2px;

    .el-dialog__header {
        position: relative;
        min-height: 46px;
        padding: 9px var(--tenant-user-invite-x-padding) 8px !important;
        margin-right: 0;
        box-sizing: border-box;
        border-bottom: none;

        &::after {
            position: absolute;
            right: 0;
            bottom: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--tenant-user-invite-border-color);
        }
    }

    .el-dialog__title {
        display: block;
        font-size: 16px;
        line-height: 28px;
        color: getCssVar('text-color', 'primary');
    }

    .el-dialog__headerbtn {
        top: 0;
        width: 42px;
        height: 46px;
    }

    .el-dialog__body {
        padding: 18px var(--tenant-user-invite-x-padding) 4px !important;
    }

    .el-dialog__footer {
        position: relative;
        min-height: 56px;
        padding: 12px var(--tenant-user-invite-x-padding);
        box-sizing: border-box;
        border-top: none;

        &::before {
            position: absolute;
            top: 0;
            right: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--tenant-user-invite-border-color);
        }
    }

    .tenant-user-invite-form {
        .el-form-item {
            margin-bottom: 20px;
        }

        .el-form-item__label {
            width: 100%;
            padding: 0;
            margin-bottom: 4px;
            line-height: 16px;
            color: getCssVar('text-color', 'regular');
        }

        .el-form-item__content,
        .el-input,
        .el-select {
            width: 100%;
        }

        .el-input__wrapper,
        .el-input-group__append {
            border-radius: 0;
        }
    }

    .tenant-user-invite-footer {
        display: flex;
        align-items: center;
        justify-content: flex-end;
        gap: 12px;
        width: 100%;

        .el-button + .el-button {
            margin-left: 0;
        }
    }
}
</style>

<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-seach-table">
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
        </div>
        <LoadingPage :visible="loading" :network-error="networkError" @loading-refresh="initData(false)">
            <div class="zqy-table">
                <BlockTable
                    :table-config="tableConfig"
                    @size-change="handleSizeChange"
                    @current-change="handleCurrentChange"
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
                        <el-tag :type="statusTagType(scopeSlot.row.status)">
                            {{ statusText(scopeSlot.row.status) }}
                        </el-tag>
                    </template>
                    <template #options="scopeSlot">
                        <div v-if="isApplying(scopeSlot.row)" class="btn-group tenant-user-action-group">
                            <el-dropdown trigger="click">
                                <span class="click-show-more">更多</span>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item @click="approveApply(scopeSlot.row)">通过申请</el-dropdown-item>
                                        <el-dropdown-item @click="rejectApply(scopeSlot.row)">拒绝申请</el-dropdown-item>
                                    </el-dropdown-menu>
                                </template>
                            </el-dropdown>
                        </div>
                        <div v-else-if="!isTenantSuperAdmin(scopeSlot.row)" class="btn-group">
                            <template v-if="!scopeSlot.row.normalAdmin">
                                <span v-if="!scopeSlot.row.authLoading" @click="giveAuth(scopeSlot.row)">
                                    设为租户管理员
                                </span>
                                <el-icon v-else class="is-loading">
                                    <Loading />
                                </el-icon>
                            </template>
                            <template v-else>
                                <span v-if="!scopeSlot.row.authLoading" @click="removeAuth(scopeSlot.row)">
                                    取消租户管理员
                                </span>
                                <el-icon v-else class="is-loading">
                                    <Loading />
                                </el-icon>
                            </template>
                            <span @click="openRoleDialog(scopeSlot.row)">业务角色</span>
                            <span @click="changeMemberStatus(scopeSlot.row)">
                                {{ scopeSlot.row.status === 'ENABLE' ? '禁用' : '启用' }}
                            </span>
                            <span @click="deleteData(scopeSlot.row)">移除</span>
                        </div>
                        <span v-else class="tenant-admin-tip">平台租户管理维护</span>
                    </template>
                </BlockTable>
            </div>
        </LoadingPage>
        <AddModal ref="addModalRef" />
        <el-dialog v-model="inviteDialogVisible" title="租户邀请码" width="520px">
            <el-form label-position="top">
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
                <el-button @click="inviteDialogVisible = false">取消</el-button>
                <el-button :loading="inviteSaving" @click="saveInviteCode(true)">重新生成</el-button>
                <el-button type="primary" :loading="inviteSaving" @click="saveInviteCode(false)">保存设置</el-button>
            </template>
        </el-dialog>
        <el-dialog v-model="roleDialogVisible" title="分配业务角色" width="480px">
            <el-checkbox-group v-model="selectedRoleIds">
                <el-checkbox v-for="role in availableRoles" :key="role.id" :label="role.id">
                    {{ role.name }}
                </el-checkbox>
            </el-checkbox-group>
            <template #footer>
                <el-button @click="roleDialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="roleSaving" @click="saveMemberRoles">保存</el-button>
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
    SetMemberRoles,
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
const addModalRef = ref(null)
const authStore = useAuthStore()
const roleDialogVisible = ref(false)
const roleSaving = ref(false)
const selectedRoleIds = ref<string[]>([])
const selectedMember = ref<any>()
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

function statusText(status: string) {
    if (status === 'APPLYING') {
        return '申请中'
    }
    return status === 'ENABLE' ? '启用' : '禁用'
}

function statusTagType(status: string) {
    if (status === 'APPLYING') {
        return 'warning'
    }
    return status === 'ENABLE' ? 'success' : 'danger'
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

function openRoleDialog(data: any) {
    selectedMember.value = data
    selectedRoleIds.value = [...(data.roleIds || [])]
    roleDialogVisible.value = true
}

function saveMemberRoles() {
    roleSaving.value = true
    SetMemberRoles({
        userId: selectedMember.value.userId,
        roleIds: selectedRoleIds.value,
        tenantId: activeTenantId.value
    })
        .then((res: any) => {
            ElMessage.success(res.msg)
            roleDialogVisible.value = false
            initData(true)
        })
        .finally(() => {
            roleSaving.value = false
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
.tenant-user-toolbar-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .tenant-user-tenant-select {
        width: 220px;
    }
}

.tenant-user-action-group {
    justify-content: center;
}

.tenant-admin-tip {
    color: var(--el-text-color-secondary);
}
</style>

<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-seach-table">
        <div class="zqy-table-top">
            <el-button type="primary" @click="addData">添加成员</el-button>
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
                            <el-tag v-if="scopeSlot.row.roleCode === 'ROLE_TENANT_ADMIN'" class="ml-2" type="success">
                                租户管理员
                            </el-tag>
                            <el-tag v-else-if="scopeSlot.row.normalAdmin" type="warning">普通管理员</el-tag>
                            <el-tag v-else type="info">成员</el-tag>
                        </div>
                    </template>
                    <template #status="scopeSlot">
                        <el-tag :type="scopeSlot.row.status === 'ENABLE' ? 'success' : 'danger'">
                            {{ scopeSlot.row.status === 'ENABLE' ? '启用' : '禁用' }}
                        </el-tag>
                    </template>
                    <template #options="scopeSlot">
                        <div v-if="scopeSlot.row.roleCode !== 'ROLE_TENANT_ADMIN'" class="btn-group">
                            <template v-if="!scopeSlot.row.normalAdmin">
                                <span v-if="!scopeSlot.row.authLoading" @click="giveAuth(scopeSlot.row)">管理授权</span>
                                <el-icon v-else class="is-loading">
                                    <Loading />
                                </el-icon>
                            </template>
                            <template v-else>
                                <span v-if="!scopeSlot.row.authLoading" @click="removeAuth(scopeSlot.row)">
                                    取消授权
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
                        <span v-else class="tenant-admin-tip">平台管理维护</span>
                    </template>
                </BlockTable>
            </div>
        </LoadingPage>
        <AddModal ref="addModalRef" />
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
import { reactive, ref, onMounted } from 'vue'
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
    SetMemberRoles,
    SetTenantMemberStatus
} from '@/app/management/tenant-user/api'
import { ElMessage, ElMessageBox } from 'element-plus'

import { useSwitchTenant } from '@/app/hooks/switch-tenant'
import { useAuthStore } from '@/app/store/useAuth'
import { ListRole } from '@/app/management/admin/api'

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

const { currentTenant, tenantList, initSwitchTenant, onTenantChange } = useSwitchTenant()

function initData(tableLoading?: boolean) {
    if (!currentTenant.value.id) {
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
        tenantId: currentTenant.value.id
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

function addData() {
    addModalRef.value.showModal((formData: FormUser) => {
        return new Promise((resolve: any, reject: any) => {
            AddTenantUserData({
                ...formData,
                tenantId: currentTenant.value.id
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
        roleIds: selectedRoleIds.value
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
    ListRole().then((res: any) => {
        availableRoles.value = res.data || []
    })
})
</script>

<style lang="scss">
.zqy-tenant__select {
    flex: 1;
    display: flex;
    justify-content: flex-start;
    margin-left: 12px;
}

.tenant-admin-tip {
    color: var(--el-text-color-secondary);
}
</style>

<template>
    <Breadcrumb :bread-crumb-list="[{ name: '角色管理', code: 'role-management' }]" />
    <div class="role-page">
        <aside class="role-page__sidebar">
            <div class="role-page__search">
                <el-input
                    v-model="keyword"
                    clearable
                    :prefix-icon="Search"
                    placeholder="搜索角色名称或编码"
                    @keyup.enter="loadRoles()"
                    @clear="loadRoles()"
                />
                <el-button type="primary" :icon="Plus" class="role-page__add-button" @click="openRoleEditor()" />
            </div>
            <div v-loading="loading" class="role-list">
                <button
                    v-for="role in roles"
                    :key="role.id"
                    class="role-list__item"
                    :class="{ 'is-active': selectedRole?.id === role.id }"
                    type="button"
                    @click="selectRole(role)"
                >
                    <span class="role-list__name">{{ role.name }}</span>
                    <span class="role-list__code">{{ role.code }}</span>
                </button>
                <el-empty v-if="!roles.length && !loading" :image-size="80" description="暂无角色" />
            </div>
        </aside>

        <section class="role-page__detail">
            <el-empty v-if="!selectedRole" description="请选择左侧角色" />
            <template v-else>
                <div class="role-detail__header">
                    <div class="role-detail__title">
                        <strong>{{ selectedRole.name }}</strong>
                        <el-tag size="small" type="info">{{ selectedRole.code }}</el-tag>
                        <span v-if="selectedRole.remark" class="role-detail__remark">{{ selectedRole.remark }}</span>
                    </div>
                    <div class="role-detail__actions">
                        <el-button :icon="Edit" @click="openRoleEditor(selectedRole)">编辑</el-button>
                        <el-button :icon="Delete" type="danger" plain @click="removeRole(selectedRole)">删除</el-button>
                    </div>
                </div>

                <el-tabs v-model="activeTab" class="role-tabs" @tab-change="handleTabChange">
                    <el-tab-pane label="角色成员" name="members">
                        <div class="role-tab-toolbar">
                            <el-input
                                v-model="memberKeyword"
                                clearable
                                :prefix-icon="Search"
                                placeholder="搜索成员账号、姓名、手机或邮箱"
                                @keyup.enter="loadMembers()"
                                @clear="loadMembers()"
                            />
                            <el-button :icon="Refresh" @click="loadMembers()">刷新</el-button>
                        </div>
                        <el-table v-loading="memberLoading" :data="members" height="520">
                            <el-table-column label="加入角色" width="100">
                                <template #default="{ row }">
                                    <el-checkbox
                                        :model-value="hasMemberRole(row)"
                                        :disabled="!!memberSaving[row.userId]"
                                        @change="(checked) => saveMemberRole(row, checked)"
                                    />
                                </template>
                            </el-table-column>
                            <el-table-column prop="username" label="成员名称" min-width="140" />
                            <el-table-column prop="account" label="账号" min-width="160" />
                            <el-table-column prop="phone" label="手机号" min-width="140" />
                            <el-table-column prop="email" label="邮箱" min-width="180" />
                            <el-table-column label="状态" width="100">
                                <template #default="{ row }">
                                    <el-tag :type="row.status === 'ENABLE' ? 'success' : 'info'">
                                        {{ row.status === 'ENABLE' ? '启用' : '禁用' }}
                                    </el-tag>
                                </template>
                            </el-table-column>
                        </el-table>
                        <el-pagination
                            v-model:current-page="memberPage"
                            v-model:page-size="memberPageSize"
                            class="role-pagination"
                            layout="total, sizes, prev, pager, next"
                            :total="memberTotal"
                            @current-change="loadMembers"
                            @size-change="loadMembers"
                        />
                    </el-tab-pane>

                    <el-tab-pane label="菜单权限" name="menus">
                        <div class="role-tab-toolbar">
                            <span>控制工作台菜单是否展示</span>
                            <el-button type="primary" :loading="permissionSaving" @click="savePermissions">
                                保存权限
                            </el-button>
                        </div>
                        <div class="permission-list is-menu">
                            <el-checkbox
                                v-for="module in catalog.menuPermissions"
                                :key="module.code"
                                :model-value="hasPermission(module.permissions[0]?.permissionCode)"
                                @change="(checked) => setPermission(module.permissions[0]?.permissionCode, checked)"
                            >
                                {{ module.name }}
                            </el-checkbox>
                        </div>
                    </el-tab-pane>

                    <el-tab-pane label="按钮权限" name="buttons">
                        <PermissionMatrix
                            :modules="catalog.buttonPermissions"
                            :permission-codes="permissionCodes"
                            :labels="buttonLabels"
                            @change="setPermission"
                        />
                        <div class="role-tab-footer">
                            <el-button type="primary" :loading="permissionSaving" @click="savePermissions">
                                保存权限
                            </el-button>
                        </div>
                    </el-tab-pane>

                    <el-tab-pane label="接口权限" name="interfaces">
                        <div class="role-tab-toolbar">
                            <el-input
                                v-model="interfaceKeyword"
                                clearable
                                :prefix-icon="Search"
                                placeholder="搜索接口地址"
                            />
                            <el-button type="primary" :loading="permissionSaving" @click="savePermissions">
                                保存权限
                            </el-button>
                        </div>
                        <div class="permission-api-list">
                            <div v-for="module in filteredInterfacePermissions" :key="module.code" class="permission-api">
                                <div class="permission-api__module">
                                    <strong>{{ module.name }}</strong>
                                    <el-checkbox
                                        :model-value="isModuleChecked(module.permissions)"
                                        :indeterminate="isModuleIndeterminate(module.permissions)"
                                        @change="(checked) => setModulePermissions(module.permissions, checked)"
                                    >
                                        全选
                                    </el-checkbox>
                                </div>
                                <div class="permission-api__items">
                                    <el-checkbox
                                        v-for="permission in module.permissions"
                                        :key="permission.permissionCode"
                                        :model-value="hasPermission(permission.permissionCode)"
                                        @change="(checked) => setPermission(permission.permissionCode, checked)"
                                    >
                                        <span class="permission-api__method">{{ permission.method }}</span>
                                        <span>{{ permission.path }}</span>
                                    </el-checkbox>
                                </div>
                            </div>
                        </div>
                    </el-tab-pane>

                    <el-tab-pane label="数据权限" name="data">
                        <PermissionMatrix
                            :modules="catalog.dataPermissions"
                            :permission-codes="permissionCodes"
                            :labels="dataLabels"
                            @change="setPermission"
                        />
                        <div class="role-tab-footer">
                            <el-button type="primary" :loading="permissionSaving" @click="savePermissions">
                                保存权限
                            </el-button>
                        </div>
                    </el-tab-pane>
                </el-tabs>
            </template>
        </section>
    </div>

    <el-dialog
        v-model="roleEditorVisible"
        class="role-editor-dialog"
        :title="roleForm.id ? '编辑角色' : '新增角色'"
        width="520px"
    >
        <el-form class="role-editor-form" label-position="top">
            <el-form-item label="角色名称">
                <el-input v-model="roleForm.name" maxlength="80" />
            </el-form-item>
            <el-form-item label="角色编码">
                <el-input v-model="roleForm.code" maxlength="80" />
            </el-form-item>
            <el-form-item label="备注">
                <el-input v-model="roleForm.remark" maxlength="500" type="textarea" :rows="3" />
            </el-form-item>
        </el-form>
        <template #footer>
            <div class="role-editor-footer">
                <el-button @click="roleEditorVisible = false">取消</el-button>
                <el-button type="primary" :loading="roleSaving" @click="saveRoleBase">保存</el-button>
            </div>
        </template>
    </el-dialog>
</template>

<script lang="ts" setup>
import { computed, defineComponent, h, onMounted, reactive, ref, type PropType } from 'vue'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElCheckbox, ElMessage, ElMessageBox } from 'element-plus'
import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import { DeleteRole, GetPermissionCatalog, PageRole, SaveRole } from '@/app/management/admin/api'
import { GetUserList, SetMemberRoles } from '@/app/management/tenant-user/api'

interface RoleItem {
    id: string
    name: string
    code: string
    remark?: string
    status?: string
    permissionCodes?: string[]
}

interface PermissionItem {
    code: string
    name: string
    permissionCode: string
    method?: string
    path?: string
    action?: string
}

interface PermissionModule {
    code: string
    name: string
    permissions: PermissionItem[]
}

interface MemberItem {
    userId: string
    username: string
    account: string
    phone?: string
    email?: string
    status: string
    roleIds?: string[]
}

const buttonLabels: Record<string, string> = {
    view: '查看',
    create: '新增',
    edit: '编辑',
    delete: '删除',
    execute: '执行'
}

const dataLabels: Record<string, string> = {
    read: '查看',
    create: '新增',
    update: '修改',
    delete: '删除'
}

const PermissionMatrix = defineComponent({
    name: 'PermissionMatrix',
    props: {
        modules: {
            type: Array as PropType<PermissionModule[]>,
            required: true
        },
        permissionCodes: {
            type: Array as PropType<string[]>,
            required: true
        },
        labels: {
            type: Object as PropType<Record<string, string>>,
            required: true
        }
    },
    emits: ['change'],
    setup(props, { emit }) {
        const hasPermission = (code?: string) => !!code && props.permissionCodes.includes(code)
        const isModuleChecked = (permissions: PermissionItem[]) =>
            permissions.length > 0 && permissions.every((permission) => hasPermission(permission.permissionCode))
        const isModuleIndeterminate = (permissions: PermissionItem[]) => {
            const checkedCount = permissions.filter((permission) => hasPermission(permission.permissionCode)).length
            return checkedCount > 0 && checkedCount < permissions.length
        }
        const setPermission = (code: string, checked: unknown) => emit('change', code, checked)
        const setModulePermissions = (permissions: PermissionItem[], checked: unknown) => {
            permissions.forEach((permission) => emit('change', permission.permissionCode, checked))
        }

        return () =>
            h(
                'div',
                { class: 'permission-matrix' },
                props.modules.map((module) =>
                    h('div', { class: 'permission-matrix__row', key: module.code }, [
                        h('div', { class: 'permission-matrix__module' }, [
                            h(
                                ElCheckbox,
                                {
                                    modelValue: isModuleChecked(module.permissions),
                                    indeterminate: isModuleIndeterminate(module.permissions),
                                    onChange: (checked: unknown) => setModulePermissions(module.permissions, checked)
                                },
                                () => module.name
                            )
                        ]),
                        h(
                            'div',
                            { class: 'permission-matrix__actions' },
                            module.permissions.map((permission) =>
                                h(
                                    ElCheckbox,
                                    {
                                        key: permission.permissionCode,
                                        modelValue: hasPermission(permission.permissionCode),
                                        onChange: (checked: unknown) =>
                                            setPermission(permission.permissionCode, checked)
                                    },
                                    () => String((props.labels as Record<string, string>)[permission.code] || permission.name)
                                )
                            )
                        )
                    ])
                )
            )
    }
})

const keyword = ref('')
const loading = ref(false)
const roles = ref<RoleItem[]>([])
const selectedRole = ref<RoleItem | null>(null)
const activeTab = ref('members')
const roleEditorVisible = ref(false)
const roleSaving = ref(false)
const permissionSaving = ref(false)
const permissionCodes = ref<string[]>([])
const interfaceKeyword = ref('')
const memberKeyword = ref('')
const memberLoading = ref(false)
const members = ref<MemberItem[]>([])
const memberPage = ref(1)
const memberPageSize = ref(10)
const memberTotal = ref(0)
const memberSaving = reactive<Record<string, boolean>>({})

const roleForm = reactive({
    id: '',
    name: '',
    code: '',
    remark: ''
})

const catalog = reactive({
    menuPermissions: [] as PermissionModule[],
    buttonPermissions: [] as PermissionModule[],
    interfacePermissions: [] as PermissionModule[],
    dataPermissions: [] as PermissionModule[],
    permissionCodes: [] as string[]
})

const filteredInterfacePermissions = computed(() => {
    const searchKey = interfaceKeyword.value.trim().toLowerCase()
    if (!searchKey) {
        return catalog.interfacePermissions
    }
    return catalog.interfacePermissions
        .map((module) => ({
            ...module,
            permissions: module.permissions.filter((permission) =>
                `${permission.method || ''} ${permission.path || ''}`.toLowerCase().includes(searchKey)
            )
        }))
        .filter((module) => module.permissions.length)
})

function loadRoles(selectRoleCode?: string) {
    loading.value = true
    PageRole({
        page: 0,
        pageSize: 200,
        searchKeyWord: keyword.value
    })
        .then((res: any) => {
            roles.value = res.data.content || []
            const nextRole =
                roles.value.find((role) => role.code === selectRoleCode) ||
                roles.value.find((role) => role.id === selectedRole.value?.id) ||
                roles.value[0] ||
                null
            if (nextRole) {
                selectRole(nextRole)
            } else {
                selectedRole.value = null
                permissionCodes.value = []
            }
        })
        .finally(() => {
            loading.value = false
        })
}

function loadCatalog() {
    GetPermissionCatalog().then((res: any) => {
        catalog.menuPermissions = res.data.menuPermissions || []
        catalog.buttonPermissions = res.data.buttonPermissions || []
        catalog.interfacePermissions = res.data.interfacePermissions || []
        catalog.dataPermissions = res.data.dataPermissions || []
        catalog.permissionCodes = res.data.permissionCodes || []
    })
}

function selectRole(role: RoleItem) {
    selectedRole.value = role
    permissionCodes.value = [...(role.permissionCodes || [])]
    if (activeTab.value === 'members') {
        loadMembers()
    }
}

function openRoleEditor(role?: RoleItem) {
    roleForm.id = role?.id || ''
    roleForm.name = role?.name || ''
    roleForm.code = role?.code || ''
    roleForm.remark = role?.remark || ''
    roleEditorVisible.value = true
}

function saveRoleBase() {
    if (!roleForm.name.trim() || !roleForm.code.trim()) {
        ElMessage.warning('请填写角色名称和编码')
        return
    }
    roleSaving.value = true
    SaveRole({
        id: roleForm.id || undefined,
        name: roleForm.name,
        code: roleForm.code,
        remark: roleForm.remark,
        status: selectedRole.value?.status || 'ENABLE'
    })
        .then((res: any) => {
            ElMessage.success(res.msg)
            roleEditorVisible.value = false
            loadRoles(roleForm.code)
        })
        .finally(() => {
            roleSaving.value = false
        })
}

function removeRole(role: RoleItem) {
    ElMessageBox.confirm(`确定删除角色“${role.name}”吗？`, '提示', {
        type: 'warning'
    }).then(() => {
        DeleteRole({
            roleId: role.id
        }).then((res: any) => {
            ElMessage.success(res.msg)
            selectedRole.value = null
            loadRoles()
        })
    })
}

function hasPermission(code?: string) {
    return !!code && permissionCodes.value.includes(code)
}

function setPermission(code: string | undefined, checked: unknown) {
    if (!code) {
        return
    }
    const nextCodes = new Set(permissionCodes.value)
    if (checked) {
        nextCodes.add(code)
    } else {
        nextCodes.delete(code)
    }
    permissionCodes.value = Array.from(nextCodes)
}

function isModuleChecked(permissions: PermissionItem[]) {
    return permissions.length > 0 && permissions.every((permission) => hasPermission(permission.permissionCode))
}

function isModuleIndeterminate(permissions: PermissionItem[]) {
    const checkedCount = permissions.filter((permission) => hasPermission(permission.permissionCode)).length
    return checkedCount > 0 && checkedCount < permissions.length
}

function setModulePermissions(permissions: PermissionItem[], checked: unknown) {
    permissions.forEach((permission) => setPermission(permission.permissionCode, checked))
}

function savePermissions() {
    if (!selectedRole.value) {
        return
    }
    permissionSaving.value = true
    SaveRole({
        id: selectedRole.value.id,
        name: selectedRole.value.name,
        code: selectedRole.value.code,
        remark: selectedRole.value.remark,
        status: selectedRole.value.status || 'ENABLE',
        permissionCodes: permissionCodes.value
    })
        .then((res: any) => {
            ElMessage.success(res.msg)
            selectedRole.value!.permissionCodes = [...permissionCodes.value]
            loadRoles(selectedRole.value!.code)
        })
        .finally(() => {
            permissionSaving.value = false
        })
}

function handleTabChange(tab: string | number) {
    if (tab === 'members') {
        loadMembers()
    }
}

function loadMembers() {
    if (!selectedRole.value) {
        return
    }
    memberLoading.value = true
    GetUserList({
        page: memberPage.value - 1,
        pageSize: memberPageSize.value,
        searchKeyWord: memberKeyword.value
    })
        .then((res: any) => {
            members.value = res.data.content || []
            memberTotal.value = res.data.totalElements || 0
        })
        .finally(() => {
            memberLoading.value = false
        })
}

function hasMemberRole(member: MemberItem) {
    return !!selectedRole.value && !!member.roleIds?.includes(selectedRole.value.id)
}

function saveMemberRole(member: MemberItem, checked: unknown) {
    if (!selectedRole.value) {
        return
    }
    const roleIds = new Set(member.roleIds || [])
    if (checked) {
        roleIds.add(selectedRole.value.id)
    } else {
        roleIds.delete(selectedRole.value.id)
    }
    memberSaving[member.userId] = true
    SetMemberRoles({
        userId: member.userId,
        roleIds: Array.from(roleIds)
    })
        .then((res: any) => {
            ElMessage.success(res.msg)
            member.roleIds = Array.from(roleIds)
        })
        .finally(() => {
            memberSaving[member.userId] = false
        })
}

onMounted(() => {
    loadCatalog()
    loadRoles()
})
</script>

<style scoped lang="scss">
.role-page {
    height: calc(100vh - 64px);
    display: grid;
    grid-template-columns: 280px minmax(0, 1fr);
    border: 1px solid var(--el-border-color-lighter);
    background-color: #ffffff;
}

.role-page__sidebar {
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 16px;
    border-right: 1px solid var(--el-border-color-lighter);
}

.role-page__search {
    display: flex;
    align-items: center;
    gap: 8px;

    .el-input {
        flex: 1;
        min-width: 0;
    }
}

.role-page__add-button {
    width: 32px;
    height: 32px;
    padding: 0;
    flex-shrink: 0;
}

.role-detail__header,
.role-tab-toolbar,
.role-tab-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
}

.role-list {
    min-height: 0;
    flex: 1;
    overflow: auto;
}

.role-list__item {
    width: 100%;
    min-height: 56px;
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    justify-content: center;
    gap: 4px;
    padding: 10px 12px;
    border: 0;
    border-radius: 6px;
    color: var(--el-text-color-primary);
    background: transparent;
    cursor: pointer;
    text-align: left;

    &:hover,
    &.is-active {
        background-color: var(--el-fill-color-light);
    }

    &.is-active {
        color: var(--el-color-primary);
    }
}

.role-list__name {
    max-width: 100%;
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.role-list__code,
.role-detail__remark {
    max-width: 100%;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.role-page__detail {
    min-width: 0;
    padding: 16px 20px;
    overflow: hidden;
}

.role-detail__header {
    margin-bottom: 12px;
}

.role-detail__title {
    min-width: 0;
    display: flex;
    align-items: center;
    gap: 8px;
}

.role-detail__title strong {
    max-width: 260px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.role-tabs {
    height: calc(100% - 44px);
}

.role-tab-toolbar {
    margin-bottom: 12px;

    .el-input {
        max-width: 360px;
    }
}

.role-tab-footer {
    margin-top: 12px;
    justify-content: flex-end;
}

.role-pagination {
    margin-top: 12px;
    justify-content: flex-end;
}

.permission-list {
    max-height: 560px;
    overflow: auto;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 6px;
}

.permission-list.is-menu {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
    gap: 0;
    padding: 12px;
}

:deep(.permission-matrix) {
    max-height: 560px;
    overflow: auto;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 6px;
}

:deep(.permission-matrix__row) {
    display: grid;
    grid-template-columns: 180px minmax(0, 1fr);
    min-height: 48px;
    border-bottom: 1px solid var(--el-border-color-lighter);

    &:last-child {
        border-bottom: 0;
    }
}

:deep(.permission-matrix__module) {
    display: flex;
    align-items: center;
    padding: 10px 12px;
    background-color: var(--el-fill-color-lighter);
}

:deep(.permission-matrix__actions) {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 4px 18px;
    padding: 10px 12px;
}

.permission-api-list {
    max-height: 560px;
    overflow: auto;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 6px;
}

.permission-api {
    border-bottom: 1px solid var(--el-border-color-lighter);

    &:last-child {
        border-bottom: 0;
    }
}

.permission-api__module {
    min-height: 44px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 12px;
    background-color: var(--el-fill-color-lighter);
}

.permission-api__items {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 8px 16px;
    padding: 12px;
}

.permission-api__method {
    display: inline-flex;
    min-width: 44px;
    margin-right: 8px;
    color: var(--el-color-primary);
    font-weight: 600;
}

:deep(.role-editor-dialog) {
    --role-editor-x-padding: 20px;
    --role-editor-border-color: #ebeef5;

    border-radius: 2px;

    .el-dialog__header {
        position: relative;
        min-height: 46px;
        padding: 9px var(--role-editor-x-padding) 8px !important;
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
            background-color: var(--role-editor-border-color);
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
        padding: 20px var(--role-editor-x-padding) 0 !important;
    }

    .el-dialog__footer {
        position: relative;
        min-height: 56px;
        padding: 12px var(--role-editor-x-padding);
        box-sizing: border-box;
        border-top: none;

        &::before {
            position: absolute;
            top: 0;
            right: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--role-editor-border-color);
        }
    }

    .role-editor-form {
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
        .el-textarea {
            width: 100%;
        }

        .el-input__wrapper,
        .el-textarea__inner {
            border-radius: 0;
        }

        .el-textarea__inner {
            min-height: 96px !important;
            line-height: 20px;
            resize: none;
        }
    }

    .role-editor-footer {
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

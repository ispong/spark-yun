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
                <el-icon class="role-page__add-button" @click="openRoleEditor()">
                    <Plus />
                </el-icon>
            </div>
            <div v-loading="loading" class="role-list">
                <div
                    v-for="role in roles"
                    :key="role.id"
                    class="role-list__item"
                    :class="{ 'is-active': selectedRole?.id === role.id }"
                    role="button"
                    tabindex="0"
                    @click="selectRole(role)"
                    @keydown.enter.prevent="selectRole(role)"
                    @keydown.space.prevent="selectRole(role)"
                >
                    <span class="role-list__name">{{ role.name }}</span>
                    <span class="role-list__actions">
                        <el-button
                            link
                            :icon="Edit"
                            class="role-list__action"
                            title="编辑"
                            @click.stop="openRoleEditor(role)"
                        />
                        <el-button
                            link
                            :icon="Delete"
                            class="role-list__action role-list__action--danger"
                            title="删除"
                            @click.stop="removeRole(role)"
                        />
                    </span>
                </div>
                <el-empty v-if="!roles.length && !loading" :image-size="80" description="暂无角色" />
            </div>
        </aside>

        <section class="role-page__detail">
            <div v-if="!selectedRole" class="role-page__empty">
                <el-empty description="请选择左侧角色" />
            </div>
            <template v-else>
                <el-tabs v-model="activeTab" class="role-tabs" @tab-change="handleTabChange">
                    <el-tab-pane label="角色成员" name="members">
                        <div class="role-member-panel">
                            <div class="zqy-table-top role-member-toolbar">
                                <div class="role-member-toolbar__left">
                                    <el-button type="primary" @click="openMemberAdder">添加成员</el-button>
                                </div>
                                <div class="zqy-seach role-member-search">
                                    <el-input
                                        v-model="memberKeyword"
                                        clearable
                                        :prefix-icon="Search"
                                        placeholder="搜索成员账号、姓名、手机或邮箱"
                                        @keyup.enter="searchMembers"
                                        @clear="searchMembers"
                                    />
                                </div>
                                <Transition name="role-member-batch-slide">
                                    <div v-if="selectedMembers.length" class="role-member-batch-mask">
                                        <div class="role-member-batch-actions">
                                            <el-button
                                                class="role-member-batch-action"
                                                :loading="memberRemoving"
                                                @click="removeSelectedMembers"
                                            >
                                                删除
                                            </el-button>
                                            <el-button
                                                class="role-member-batch-cancel"
                                                :disabled="memberRemoving"
                                                @click="cancelMemberSelection"
                                            >
                                                取消选择
                                            </el-button>
                                        </div>
                                    </div>
                                </Transition>
                            </div>
                            <div class="zqy-table role-member-table">
                                <BlockTable
                                    :table-config="memberTableConfig"
                                    @size-change="handleMemberSizeChange"
                                    @current-change="handleMemberCurrentChange"
                                    @checkbox-change="handleMemberSelectionChange"
                                >
                                    <template #options="scopeSlot">
                                        <div class="btn-group role-member-action-group">
                                            <span class="role-member-action-button" @click="removeMember(scopeSlot.row)">
                                                删除
                                            </span>
                                        </div>
                                    </template>
                                </BlockTable>
                            </div>
                        </div>
                    </el-tab-pane>

                    <el-tab-pane label="功能权限" name="buttons">
                        <PermissionMatrix
                            :modules="catalog.buttonPermissions"
                            :permission-codes="visiblePermissionCodes"
                            :labels="buttonLabels"
                            :disabled="buttonAllChecked"
                            @change="setPermission"
                        />
                        <div class="role-permission-footer">
                            <el-checkbox :model-value="buttonAllChecked" @change="setButtonAllChecked">
                                全选
                            </el-checkbox>
                            <el-button type="primary" :loading="permissionSaving" @click="savePermissions">
                                保存
                            </el-button>
                        </div>
                    </el-tab-pane>

                    <el-tab-pane
                        v-for="resourceType in instanceResourceTypes"
                        :key="resourceType.code"
                        :label="resourceType.name"
                        :name="resourceType.code"
                    >
                        <div class="role-instance-panel">
                            <div class="role-instance-toolbar">
                                <el-input
                                    v-model="instancePermissionState[resourceType.code].keyword"
                                    clearable
                                    :prefix-icon="Search"
                                    placeholder="搜索名称、类型或备注"
                                    @keyup.enter="searchInstanceResources(resourceType.code)"
                                    @clear="searchInstanceResources(resourceType.code)"
                                />
                            </div>
                            <div
                                class="role-instance-table"
                                :class="{
                                    'role-instance-table--disabled':
                                        instancePermissionState[resourceType.code].allEnabled
                                }"
                            >
                                <BlockTable
                                    :table-config="getInstanceTableConfig(resourceType.code)"
                                    @size-change="(pageSize) => handleInstanceSizeChange(resourceType.code, pageSize)"
                                    @current-change="(page) => handleInstancePageChange(resourceType.code, page)"
                                    @checkbox-change="(rows) => handleInstanceSelectionChange(resourceType.code, rows)"
                                >
                                    <template #footerLeft>
                                        <div class="role-instance-actions">
                                            <el-checkbox
                                                v-model="instancePermissionState[resourceType.code].allEnabled"
                                                @change="handleInstanceAllChange(resourceType.code)"
                                            >
                                                全选
                                            </el-checkbox>
                                            <el-button
                                                type="primary"
                                                :loading="instancePermissionState[resourceType.code].saving"
                                                @click="saveInstancePermission(resourceType.code)"
                                            >
                                                保存
                                            </el-button>
                                        </div>
                                    </template>
                                </BlockTable>
                            </div>
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
        :close-on-click-modal="false"
        destroy-on-close
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

    <el-dialog
        v-model="memberAdderVisible"
        class="role-editor-dialog"
        title="添加成员"
        width="520px"
        :close-on-click-modal="false"
        destroy-on-close
    >
        <el-form class="role-editor-form" label-position="top">
            <el-form-item label="选择成员">
                <el-select
                    v-model="memberAdderUserIds"
                    multiple
                    filterable
                    placeholder="请选择要加入当前角色的成员"
                    :loading="memberAdderLoading"
                >
                    <el-option
                        v-for="member in memberAdderOptions"
                        :key="member.userId"
                        :label="member.username"
                        :value="member.userId"
                    />
                </el-select>
            </el-form-item>
        </el-form>
        <template #footer>
            <div class="role-editor-footer">
                <el-button @click="memberAdderVisible = false">取消</el-button>
                <el-button type="primary" :loading="memberAdding" @click="addSelectedMembers">保存</el-button>
            </div>
        </template>
    </el-dialog>
</template>

<script lang="ts" setup>
import { computed, defineComponent, h, onMounted, reactive, ref, type PropType } from 'vue'
import { Delete, Edit, Plus, Search } from '@element-plus/icons-vue'
import { ElCheckbox, ElMessage, ElMessageBox } from 'element-plus'
import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import BlockTable from '@/app/components/block-table/index.vue'
import { useAuthStore } from '@/app/store/useAuth'
import {
    DeleteRole,
    GetPermissionCatalog,
    PageRole,
    SaveRole
} from '@/app/management/admin/api'
import { GetUserList, PageRoleMember, SetMemberRoles } from '@/app/management/tenant-user/api'
import {
    GetComputerGroupList,
    GetDatasourceList,
    GetFileCenterList
} from '@/app/shared/api/resources'

interface RoleItem {
    id: string
    name: string
    code: string
    remark?: string
    status?: string
    permissionCodes?: string[]
    instancePermissions?: RoleInstancePermissionItem[]
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

type InstanceResourceType = 'CLUSTER' | 'DATASOURCE' | 'RESOURCE_FILE'

interface InstanceResourceItem {
    id: string
    name: string
    type?: string
    status?: string
    remark?: string
}

interface InstancePermissionState {
    allEnabled: boolean
    resourceIds: string[]
    tableData: InstanceResourceItem[]
    page: number
    pageSize: number
    total: number
    keyword: string
    loading: boolean
    saving: boolean
    loaded: boolean
}

interface RoleInstancePermissionItem {
    roleId?: string
    resourceType: InstanceResourceType
    allEnabled: boolean
    resourceIds?: string[]
}

const buttonLabels: Record<string, string> = {
    view: '查看',
    create: '新增',
    edit: '编辑',
    delete: '删除',
    execute: '执行'
}

const instanceResourceTypes: Array<{ code: InstanceResourceType; name: string }> = [
    { code: 'CLUSTER', name: '计算集群' },
    { code: 'DATASOURCE', name: '数据源' },
    { code: 'RESOURCE_FILE', name: '资源文件' }
]

function createInstancePermissionState(): InstancePermissionState {
    return {
        allEnabled: true,
        resourceIds: [],
        tableData: [],
        page: 1,
        pageSize: 10,
        total: 0,
        keyword: '',
        loading: false,
        saving: false,
        loaded: false
    }
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
        },
        disabled: {
            type: Boolean,
            default: false
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
                                    disabled: props.disabled,
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
                                        disabled: props.disabled,
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
const authStore = useAuthStore()
const activeTab = ref<'members' | 'buttons' | InstanceResourceType>('members')
const roleEditorVisible = ref(false)
const roleSaving = ref(false)
const permissionSaving = ref(false)
const permissionCodes = ref<string[]>([])
const buttonAllChecked = ref(true)
const memberKeyword = ref('')
const memberLoading = ref(false)
const members = ref<MemberItem[]>([])
const selectedMembers = ref<MemberItem[]>([])
const memberPage = ref(1)
const memberPageSize = ref(10)
const memberTotal = ref(0)
const memberAdderVisible = ref(false)
const memberAdderLoading = ref(false)
const memberAdding = ref(false)
const memberRemoving = ref(false)
const memberAdderOptions = ref<MemberItem[]>([])
const memberAdderUserIds = ref<string[]>([])
const instancePermissionState = reactive<Record<InstanceResourceType, InstancePermissionState>>({
    CLUSTER: createInstancePermissionState(),
    DATASOURCE: createInstancePermissionState(),
    RESOURCE_FILE: createInstancePermissionState()
})
const visiblePermissionCodes = computed(() => (buttonAllChecked.value ? [] : permissionCodes.value))

const roleForm = reactive({
    id: '',
    name: '',
    code: '',
    remark: ''
})

const catalog = reactive({
    buttonPermissions: [] as PermissionModule[],
    permissionCodes: [] as string[]
})

const memberTableConfig = computed(() => ({
    tableData: members.value,
    colConfigs: [
        {
            prop: 'account',
            title: '账号',
            minWidth: 120,
            showOverflowTooltip: true
        },
        {
            prop: 'username',
            title: '名称',
            minWidth: 120,
            showOverflowTooltip: true
        },
        {
            prop: 'phone',
            title: '手机号',
            minWidth: 120,
            showOverflowTooltip: true
        },
        {
            prop: 'email',
            title: '邮箱',
            minWidth: 160,
            showOverflowTooltip: true
        },
        {
            title: '操作',
            width: 90,
            align: 'center',
            customSlot: 'options'
        }
    ],
    pagination: {
        currentPage: memberPage.value,
        pageSize: memberPageSize.value,
        total: memberTotal.value
    },
    seqType: 'seq',
    checkbox: true,
    loading: memberLoading.value
}))

function getInstanceTableConfig(resourceType: InstanceResourceType) {
    const state = instancePermissionState[resourceType]
    return {
        tableData: state.tableData,
        colConfigs: [
            {
                prop: 'name',
                title: '名称',
                minWidth: 160,
                showOverflowTooltip: true
            },
            {
                prop: 'type',
                title: '类型',
                minWidth: 120,
                showOverflowTooltip: true
            },
            {
                prop: 'remark',
                title: '备注',
                minWidth: 180,
                showOverflowTooltip: true
            }
        ],
        pagination: {
            currentPage: state.page,
            pageSize: state.pageSize,
            total: state.total
        },
        seqType: 'seq',
        checkbox: true,
        checkboxDisabled: state.allEnabled,
        rowKey: 'id',
        selectedRowKeys: state.resourceIds,
        loading: state.loading
    }
}

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
                resetInstancePermissionStates()
            }
        })
        .finally(() => {
            loading.value = false
        })
}

function loadCatalog() {
    GetPermissionCatalog().then((res: any) => {
        catalog.buttonPermissions = res.data.buttonPermissions || []
        catalog.permissionCodes = res.data.permissionCodes || []
        syncButtonAllChecked()
    })
}

function selectRole(role: RoleItem) {
    selectedRole.value = role
    memberPage.value = 1
    permissionCodes.value = [...(role.permissionCodes || [])]
    syncButtonAllChecked()
    resetInstancePermissionStates()
    if (activeTab.value === 'members') {
        loadMembers()
    } else if (isInstanceResourceType(activeTab.value)) {
        loadInstancePermission(activeTab.value)
        loadInstanceResources(activeTab.value)
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

function getPermissionCodes(modules: PermissionModule[]) {
    return modules
        .flatMap((module) => module.permissions.map((permission) => permission.permissionCode))
        .filter((code): code is string => !!code)
}

function syncButtonAllChecked() {
    const allButtonCodes = getPermissionCodes(catalog.buttonPermissions)
    if (!selectedRole.value || !allButtonCodes.length) {
        buttonAllChecked.value = true
        return
    }
    const currentCodes = new Set(permissionCodes.value)
    buttonAllChecked.value = allButtonCodes.every((code) => currentCodes.has(code))
    if (buttonAllChecked.value) {
        permissionCodes.value = []
    }
}

function setButtonAllChecked(checked: unknown) {
    buttonAllChecked.value = !!checked
    if (buttonAllChecked.value) {
        permissionCodes.value = []
    }
}

function savePermissions() {
    if (!selectedRole.value) {
        return
    }
    const savePermissionCodes = buttonAllChecked.value ? getPermissionCodes(catalog.buttonPermissions) : permissionCodes.value
    permissionSaving.value = true
    SaveRole({
        id: selectedRole.value.id,
        name: selectedRole.value.name,
        code: selectedRole.value.code,
        remark: selectedRole.value.remark,
        status: selectedRole.value.status || 'ENABLE',
        permissionCodes: savePermissionCodes
    })
        .then((res: any) => {
            ElMessage.success(res.msg)
            selectedRole.value!.permissionCodes = [...savePermissionCodes]
            loadRoles(selectedRole.value!.code)
        })
        .finally(() => {
            permissionSaving.value = false
        })
}

function handleTabChange(tab: string | number) {
    if (tab === 'members') {
        loadMembers()
        return
    }
    if (isInstanceResourceType(tab)) {
        loadInstancePermission(tab)
        loadInstanceResources(tab)
    }
}

function isInstanceResourceType(value: unknown): value is InstanceResourceType {
    return instanceResourceTypes.some((resourceType) => resourceType.code === value)
}

function resetInstancePermissionStates() {
    instanceResourceTypes.forEach((resourceType) => {
        Object.assign(instancePermissionState[resourceType.code], createInstancePermissionState())
    })
}

function loadInstancePermission(resourceType: InstanceResourceType) {
    if (!selectedRole.value) return
    const state = instancePermissionState[resourceType]
    const permission = selectedRole.value.instancePermissions?.find(
        (item) => item.resourceType === resourceType
    )
    state.allEnabled = permission?.allEnabled !== false
    state.resourceIds = permission?.resourceIds || []
}

function getPagedContent(res: any) {
    return res?.data?.content || []
}

function getPagedTotal(res: any) {
    return res?.data?.totalElements || res?.data?.page?.totalElements || 0
}

function getResourcePageRequest(resourceType: InstanceResourceType, state: InstancePermissionState) {
    const pageParams = {
        page: state.page - 1,
        pageSize: state.pageSize,
        searchKeyWord: state.keyword
    }

    if (resourceType === 'CLUSTER') {
        return GetComputerGroupList(pageParams).then((res: any) => ({
            content: getPagedContent(res).map((cluster: any) => ({
                id: cluster.id,
                name: cluster.name,
                type: cluster.clusterType,
                status: cluster.status,
                remark: cluster.remark
            })),
            totalElements: getPagedTotal(res)
        }))
    }

    if (resourceType === 'DATASOURCE') {
        return GetDatasourceList(pageParams).then((res: any) => ({
            content: getPagedContent(res).map((datasource: any) => ({
                id: datasource.id,
                name: datasource.name,
                type: datasource.dbType,
                status: datasource.status,
                remark: datasource.remark
            })),
            totalElements: getPagedTotal(res)
        }))
    }

    return GetFileCenterList(pageParams).then((res: any) => ({
        content: getPagedContent(res).map((file: any) => ({
            id: file.id,
            name: file.fileName,
            type: file.fileType,
            status: '',
            remark: file.remark
        })),
        totalElements: getPagedTotal(res)
    }))
}

function loadInstanceResources(resourceType: InstanceResourceType) {
    const state = instancePermissionState[resourceType]
    state.loading = true
    getResourcePageRequest(resourceType, state)
        .then((res: any) => {
            state.tableData = res.content || []
            state.total = res.totalElements || 0
            state.loaded = true
        })
        .finally(() => {
            state.loading = false
        })
}

function searchInstanceResources(resourceType: InstanceResourceType) {
    instancePermissionState[resourceType].page = 1
    loadInstanceResources(resourceType)
}

function handleInstancePageChange(resourceType: InstanceResourceType, page: number) {
    instancePermissionState[resourceType].page = page
    loadInstanceResources(resourceType)
}

function handleInstanceSizeChange(resourceType: InstanceResourceType, pageSize: number) {
    instancePermissionState[resourceType].pageSize = pageSize
    instancePermissionState[resourceType].page = 1
    loadInstanceResources(resourceType)
}

function handleInstanceSelectionChange(resourceType: InstanceResourceType, rows: InstanceResourceItem[]) {
    const state = instancePermissionState[resourceType]
    if (state.allEnabled) return
    const currentPageIds = new Set(state.tableData.map((item) => item.id))
    const selectedIds = new Set(state.resourceIds.filter((id) => !currentPageIds.has(id)))
    rows.forEach((row) => selectedIds.add(row.id))
    state.resourceIds = Array.from(selectedIds)
}

function handleInstanceAllChange(resourceType: InstanceResourceType) {
    const state = instancePermissionState[resourceType]
    if (state.allEnabled) {
        state.resourceIds = []
    }
}

function saveInstancePermission(resourceType: InstanceResourceType) {
    if (!selectedRole.value) return
    const state = instancePermissionState[resourceType]
    state.saving = true
    const nextInstancePermissions = instanceResourceTypes.map((item) => {
        const itemState = instancePermissionState[item.code]
        return {
            roleId: selectedRole.value!.id,
            resourceType: item.code,
            allEnabled: itemState.allEnabled,
            resourceIds: itemState.allEnabled ? [] : itemState.resourceIds
        }
    })
    SaveRole({
        id: selectedRole.value.id,
        name: selectedRole.value.name,
        code: selectedRole.value.code,
        remark: selectedRole.value.remark,
        status: selectedRole.value.status || 'ENABLE',
        instancePermissions: nextInstancePermissions
    })
        .then((res: any) => {
            ElMessage.success(res.msg)
            selectedRole.value!.instancePermissions = nextInstancePermissions
        })
        .finally(() => {
            state.saving = false
        })
}

function loadMembers() {
    if (!selectedRole.value) {
        members.value = []
        memberTotal.value = 0
        return
    }
    if (!authStore.tenantId) {
        members.value = []
        memberTotal.value = 0
        return
    }
    memberLoading.value = true
    PageRoleMember({
        page: memberPage.value - 1,
        pageSize: memberPageSize.value,
        searchKeyWord: memberKeyword.value,
        tenantId: authStore.tenantId,
        roleId: selectedRole.value.id
    })
        .then((res: any) => {
            members.value = res.data.content || []
            memberTotal.value = res.data.totalElements || 0
            selectedMembers.value = []
        })
        .finally(() => {
            memberLoading.value = false
        })
}

function searchMembers() {
    memberPage.value = 1
    loadMembers()
}

function handleMemberSizeChange(pageSize: number) {
    memberPageSize.value = pageSize
    memberPage.value = 1
    loadMembers()
}

function handleMemberCurrentChange(currentPage: number) {
    memberPage.value = currentPage
    loadMembers()
}

function handleMemberSelectionChange(rows: MemberItem[]) {
    selectedMembers.value = rows
}

function cancelMemberSelection() {
    selectedMembers.value = []
    members.value = [...members.value]
}

function hasMemberRole(member: MemberItem) {
    return !!selectedRole.value && !!member.roleIds?.includes(selectedRole.value.id)
}

function openMemberAdder() {
    if (!selectedRole.value) {
        return
    }
    if (!authStore.tenantId) {
        ElMessage.warning('请先选择租户')
        return
    }
    memberAdderVisible.value = true
    memberAdderUserIds.value = []
    memberAdderLoading.value = true
    GetUserList({
        page: 0,
        pageSize: 500,
        searchKeyWord: '',
        tenantId: authStore.tenantId
    })
        .then((res: any) => {
            memberAdderOptions.value = (res.data.content || []).filter((member: MemberItem) => !hasMemberRole(member))
        })
        .finally(() => {
            memberAdderLoading.value = false
        })
}

function addSelectedMembers() {
    if (!selectedRole.value || !memberAdderUserIds.value.length) {
        ElMessage.warning('请选择成员')
        return
    }
    const membersToAdd = memberAdderOptions.value.filter((member) => memberAdderUserIds.value.includes(member.userId))
    memberAdding.value = true
    Promise.all(
        membersToAdd.map((member) => {
            const roleIds = new Set(member.roleIds || [])
            roleIds.add(selectedRole.value!.id)
            return SetMemberRoles({
                userId: member.userId,
                roleIds: Array.from(roleIds),
                tenantId: authStore.tenantId
            })
        })
    )
        .then(() => {
            ElMessage.success('添加成功')
            memberAdderVisible.value = false
            loadMembers()
        })
        .finally(() => {
            memberAdding.value = false
        })
}

function removeMember(member: MemberItem) {
    removeMembers([member])
}

function removeSelectedMembers() {
    if (!selectedMembers.value.length) {
        ElMessage.warning('请选择成员')
        return
    }
    removeMembers(selectedMembers.value)
}

function removeMembers(targetMembers: MemberItem[]) {
    if (!selectedRole.value || !authStore.tenantId || !targetMembers.length) {
        return
    }
    ElMessageBox.confirm(`确定从当前角色中删除选中的 ${targetMembers.length} 个成员吗？`, '提示', {
        type: 'warning'
    }).then(() => {
        memberRemoving.value = true
        Promise.all(
            targetMembers.map((member) =>
                SetMemberRoles({
                    userId: member.userId,
                    roleIds: (member.roleIds || []).filter((roleId) => roleId !== selectedRole.value!.id),
                    tenantId: authStore.tenantId
                })
            )
        )
            .then(() => {
                ElMessage.success('删除成功')
                loadMembers()
            })
            .finally(() => {
                memberRemoving.value = false
            })
    }).catch(() => undefined)
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
    border-top: 0;
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
    width: 28px;
    margin-left: 4px;
    margin-right: 4px;
    flex-shrink: 0;
    font-size: 18px;
    color: var(--el-color-primary);
    cursor: pointer;
}

.role-tab-toolbar,
.role-tab-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
}

.role-member-panel {
    height: auto;
    min-height: 0;
    display: flex;
    flex-direction: column;
}

.role-member-toolbar {
    position: relative;
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-shrink: 0;
    padding: 0;
    box-sizing: border-box;
}

.role-member-toolbar__left {
    display: flex;
    align-items: center;
    flex-shrink: 0;
}

.role-member-batch-mask {
    position: absolute;
    z-index: 2;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: flex-start;
    padding: 0;
    box-sizing: border-box;
    background-color: #fff;
}

.role-member-batch-slide-enter-active,
.role-member-batch-slide-leave-active {
    transition:
        transform 0.18s ease,
        opacity 0.18s ease;
    will-change: transform, opacity;
}

.role-member-batch-slide-enter-from,
.role-member-batch-slide-leave-to {
    opacity: 0;
    transform: translateY(-100%);
}

.role-member-batch-slide-enter-to,
.role-member-batch-slide-leave-from {
    opacity: 1;
    transform: translateY(0);
}

.role-member-batch-actions {
    display: flex;
    align-items: center;
    gap: 12px;
}

.role-member-batch-action {
    min-width: 66px;
    height: 32px;
    line-height: 30px;
    border-color: var(--el-color-primary);
    color: var(--el-color-primary);
    background-color: #fff;

    &:hover,
    &:focus {
        border-color: var(--el-color-primary);
        color: #fff;
        background-color: var(--el-color-primary);
    }
}

.role-member-batch-cancel {
    height: 32px;
    line-height: 30px;
}

.role-member-search {
    width: 320px;
    max-width: 48%;
}

.role-member-table {
    min-height: 0;
    height: auto;
    flex: none;
}

.role-member-table :deep(.block-table) {
    max-height: none;
}

.role-member-table :deep(.block-table.block-table__empty .vxe-table--render-wrapper) {
    min-height: 220px;
}

.role-member-table :deep(.vxe-table--empty-content) {
    position: relative;
    width: 100%;
    height: 176px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.role-member-table :deep(.empty-page) {
    position: static !important;
    right: auto;
    width: 120px;
    height: auto;
}

.role-member-action-group {
    width: 100%;
    height: 40px;
    align-items: center;
    justify-content: center;
    gap: 16px;

    .role-member-action-button {
        display: inline-flex;
        align-items: center;
        height: 40px;
        line-height: 1;
        font-size: var(--el-font-size-extra-small);
        color: var(--el-color-primary);
        cursor: pointer;
        white-space: nowrap;
    }
}

.role-list {
    min-height: 0;
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 0;
    overflow: auto;
}

.role-list__item {
    width: 100%;
    min-height: 44px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    padding: 10px 12px;
    border: 1px solid transparent;
    border-radius: 6px;
    box-sizing: border-box;
    color: var(--el-text-color-primary);
    font-size: var(--el-menu-item-font-size);
    font-family: inherit;
    background: transparent;
    cursor: pointer;
    text-align: left;
    outline: none;

    &:hover {
        background-color: var(--el-color-primary-light-9);
    }

    &:focus-visible {
        border-color: var(--el-color-primary-light-5);
    }

    &.is-active {
        border-color: transparent;
        color: var(--el-color-primary);
        background-color: var(--el-color-primary-light-9);
    }

    &:hover,
    &.is-active,
    &:focus-within {
        .role-list__actions {
            visibility: visible;
            opacity: 1;
            pointer-events: auto;
        }
    }
}

.role-list__name {
    min-width: 0;
    max-width: 100%;
    flex: 1;
    font-weight: 400;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.role-list__actions {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    width: 44px;
    justify-content: flex-end;
    flex: none;
    opacity: 0;
    visibility: hidden;
    pointer-events: none;
    transition: opacity 0.15s ease;
}

.role-list__action {
    width: 20px;
    height: 20px;
    padding: 0;
    margin-left: 0 !important;
    color: var(--el-text-color-secondary);

    &:hover {
        color: var(--el-color-primary);
    }

    &.role-list__action--danger:hover {
        color: var(--el-color-danger);
    }
}

:deep(.role-list__action .el-icon) {
    font-size: 14px;
}

.role-page__detail {
    min-width: 0;
    display: flex;
    flex-direction: column;
    padding: 16px 20px;
    overflow: hidden;
}

.role-page__empty {
    flex: 1;
    min-height: 0;
    display: flex;
    align-items: center;
    justify-content: center;
}

.role-tabs {
    height: 100%;
}

:deep(.role-tabs > .el-tabs__header) {
    border-bottom: 1px solid var(--el-border-color-lighter);
}

:deep(.role-tabs > .el-tabs__header .el-tabs__nav-wrap::after) {
    display: none;
}

:deep(.role-tabs > .el-tabs__header .el-tabs__item) {
    font-size: 14px;
    font-weight: 400;
    line-height: 40px;
}

:deep(.role-tabs > .el-tabs__content) {
    height: calc(100% - 55px);
}

:deep(.role-tabs > .el-tabs__content > .el-tab-pane) {
    height: 100%;
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

.role-permission-footer {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    gap: 16px;
    margin-top: 12px;
    padding-left: 12px;
}

.role-instance-panel {
    height: 100%;
    min-height: 0;
    display: flex;
    flex-direction: column;
}

.role-instance-toolbar {
    width: 100%;
    display: flex;
    justify-content: flex-end;
    margin-bottom: 12px;

    .el-input {
        width: 280px;
        max-width: 100%;
    }
}

.role-instance-table {
    flex: 1;
    min-height: 0;

    &.role-instance-table--disabled {
        :deep(.block-table__selection-cell),
        :deep(.block-table .el-checkbox) {
            opacity: 0.45;
        }
    }
}

.role-instance-actions {
    display: flex;
    align-items: center;
    gap: 16px;
    padding-left: 15px;
    flex-shrink: 0;
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

</style>

<style lang="scss">
.role-editor-dialog {
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

<template>
    <Breadcrumb :bread-crumb-list="[{ name: '角色管理', code: 'role-management' }]" />
    <div class="zqy-seach-table">
        <div class="zqy-table-top">
            <el-button type="primary" @click="openEditor()">新建角色</el-button>
            <div class="zqy-seach">
                <el-input v-model="keyword" clearable placeholder="搜索角色名称或编码" @keyup.enter="loadRoles" />
            </div>
        </div>
        <el-table v-loading="loading" :data="roles">
            <el-table-column prop="name" label="角色名称" min-width="160" />
            <el-table-column prop="code" label="角色编码" min-width="180" />
            <el-table-column label="状态" width="100">
                <template #default="{ row }">
                    <el-tag :type="row.status === 'ENABLE' ? 'success' : 'info'">
                        {{ row.status === 'ENABLE' ? '启用' : '禁用' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="权限数" width="100">
                <template #default="{ row }">
                    {{ row.permissionCodes?.length || 0 }}
                </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
                <template #default="{ row }">
                    <el-button link type="primary" @click="openEditor(row)">编辑</el-button>
                    <el-button link type="danger" @click="removeRole(row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>
        <el-pagination
            v-model:current-page="page"
            v-model:page-size="pageSize"
            class="role-pagination"
            layout="total, sizes, prev, pager, next"
            :total="total"
            @current-change="loadRoles"
            @size-change="loadRoles"
        />
    </div>

    <el-dialog v-model="editorVisible" title="角色配置" width="760px">
        <el-form label-position="top">
            <div class="role-form-row">
                <el-form-item label="角色名称">
                    <el-input v-model="form.name" />
                </el-form-item>
                <el-form-item label="角色编码">
                    <el-input v-model="form.code" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="form.status">
                        <el-option label="启用" value="ENABLE" />
                        <el-option label="禁用" value="DISABLE" />
                    </el-select>
                </el-form-item>
            </div>
            <el-form-item label="工作台权限">
                <div class="permission-grid">
                    <div v-for="module in catalog.modules" :key="module" class="permission-row">
                        <strong>{{ module }}</strong>
                        <el-checkbox-group v-model="form.permissionCodes">
                            <el-checkbox
                                v-for="action in catalog.actions"
                                :key="`${module}:${action}`"
                                :label="`workspace:${module}:${action}`"
                            >
                                {{ actionLabels[action] || action }}
                            </el-checkbox>
                        </el-checkbox-group>
                    </div>
                </div>
            </el-form-item>
        </el-form>
        <template #footer>
            <el-button @click="editorVisible = false">取消</el-button>
            <el-button type="primary" :loading="saving" @click="saveRole">保存</el-button>
        </template>
    </el-dialog>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import { DeleteRole, GetPermissionCatalog, PageRole, SaveRole } from '@/app/management/admin/api'

const actionLabels: Record<string, string> = {
    menu: '菜单',
    view: '查看',
    create: '新增',
    edit: '编辑',
    delete: '删除',
    execute: '执行'
}
const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const roles = ref<any[]>([])
const editorVisible = ref(false)
const catalog = reactive({
    modules: [] as string[],
    actions: [] as string[]
})
const form = reactive({
    id: '',
    name: '',
    code: '',
    status: 'ENABLE',
    permissionCodes: [] as string[]
})

function loadRoles() {
    loading.value = true
    PageRole({
        page: page.value - 1,
        pageSize: pageSize.value,
        searchKeyWord: keyword.value
    })
        .then((res: any) => {
            roles.value = res.data.content
            total.value = res.data.totalElements
        })
        .finally(() => {
            loading.value = false
        })
}

function openEditor(role?: any) {
    form.id = role?.id || ''
    form.name = role?.name || ''
    form.code = role?.code || ''
    form.status = role?.status || 'ENABLE'
    form.permissionCodes = [...(role?.permissionCodes || [])]
    editorVisible.value = true
}

function saveRole() {
    if (!form.name.trim() || !form.code.trim()) {
        ElMessage.warning('请填写角色名称和编码')
        return
    }
    saving.value = true
    SaveRole({
        ...form,
        id: form.id || undefined
    })
        .then((res: any) => {
            ElMessage.success(res.msg)
            editorVisible.value = false
            loadRoles()
        })
        .finally(() => {
            saving.value = false
        })
}

function removeRole(role: any) {
    ElMessageBox.confirm(`确定删除角色“${role.name}”吗？`, '提示', {
        type: 'warning'
    }).then(() => {
        DeleteRole({
            roleId: role.id
        }).then((res: any) => {
            ElMessage.success(res.msg)
            loadRoles()
        })
    })
}

onMounted(() => {
    GetPermissionCatalog().then((res: any) => {
        catalog.modules = res.data.modules || []
        catalog.actions = res.data.actions || []
    })
    loadRoles()
})
</script>

<style scoped lang="scss">
.role-form-row {
    display: grid;
    grid-template-columns: 1fr 1fr 140px;
    gap: 12px;
}

.permission-grid {
    width: 100%;
    max-height: 420px;
    overflow: auto;
    border: 1px solid var(--el-border-color);
    border-radius: 4px;
}

.permission-row {
    display: grid;
    grid-template-columns: 160px 1fr;
    gap: 16px;
    padding: 10px 12px;
    border-bottom: 1px solid var(--el-border-color-lighter);
}

.role-pagination {
    margin-top: 16px;
    justify-content: flex-end;
}
</style>

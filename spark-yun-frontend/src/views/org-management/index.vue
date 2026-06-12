<template>
    <Breadcrumb :bread-crumb-list="[{ name: '组织架构', code: 'org-management' }]" />
    <div class="zqy-seach-table">
        <div class="zqy-table-top">
            <el-button type="primary" @click="openEditor()">新建组织</el-button>
        </div>
        <el-table v-loading="loading" :data="orgs" row-key="id">
            <el-table-column prop="name" label="组织名称" min-width="180" />
            <el-table-column label="父组织" min-width="160">
                <template #default="{ row }">
                    {{ orgName(row.parentId) || '-' }}
                </template>
            </el-table-column>
            <el-table-column label="成员数" width="100">
                <template #default="{ row }">
                    {{ row.userIds?.length || 0 }}
                </template>
            </el-table-column>
            <el-table-column label="角色数" width="100">
                <template #default="{ row }">
                    {{ row.roleIds?.length || 0 }}
                </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
                <template #default="{ row }">
                    <el-button link type="primary" @click="openEditor(row)">编辑</el-button>
                    <el-button link type="danger" @click="removeOrg(row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>
    </div>

    <el-dialog v-model="editorVisible" title="组织配置" width="560px">
        <el-form label-position="top">
            <el-form-item label="组织名称">
                <el-input v-model="form.name" />
            </el-form-item>
            <el-form-item label="父组织">
                <el-select v-model="form.parentId" clearable filterable>
                    <el-option v-for="org in parentOptions" :key="org.id" :label="org.name" :value="org.id" />
                </el-select>
            </el-form-item>
            <el-form-item label="组织成员">
                <el-select v-model="form.userIds" multiple filterable>
                    <el-option
                        v-for="member in members"
                        :key="member.userId"
                        :label="`${member.username} (${member.account})`"
                        :value="member.userId"
                    />
                </el-select>
            </el-form-item>
            <el-form-item label="组织角色">
                <el-select v-model="form.roleIds" multiple filterable>
                    <el-option v-for="role in roles" :key="role.id" :label="role.name" :value="role.id" />
                </el-select>
            </el-form-item>
        </el-form>
        <template #footer>
            <el-button @click="editorVisible = false">取消</el-button>
            <el-button type="primary" :loading="saving" @click="saveOrg">保存</el-button>
        </template>
    </el-dialog>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Breadcrumb from '@/layout/bread-crumb/index.vue'
import { DeleteOrg, ListOrg, ListRole, SaveOrg } from '@/services/authorization.service'
import { GetUserList } from '@/services/tenant-user.service'
import { useAuthStore } from '@/store/useAuth'

const authStore = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const editorVisible = ref(false)
const orgs = ref<any[]>([])
const roles = ref<any[]>([])
const members = ref<any[]>([])
const form = reactive({
    id: '',
    parentId: '',
    name: '',
    userIds: [] as string[],
    roleIds: [] as string[]
})
const parentOptions = computed(() => orgs.value.filter((org) => org.id !== form.id))

function orgName(id: string) {
    return orgs.value.find((org) => org.id === id)?.name
}

function loadData() {
    loading.value = true
    Promise.all([
        ListOrg(),
        ListRole(),
        GetUserList({
            page: 0,
            pageSize: 999,
            searchKeyWord: '',
            tenantId: authStore.tenantId
        })
    ])
        .then(([orgRes, roleRes, memberRes]: any[]) => {
            orgs.value = orgRes.data || []
            roles.value = roleRes.data || []
            members.value = memberRes.data.content || []
        })
        .finally(() => {
            loading.value = false
        })
}

function openEditor(org?: any) {
    form.id = org?.id || ''
    form.parentId = org?.parentId || ''
    form.name = org?.name || ''
    form.userIds = [...(org?.userIds || [])]
    form.roleIds = [...(org?.roleIds || [])]
    editorVisible.value = true
}

function saveOrg() {
    if (!form.name.trim()) {
        ElMessage.warning('请填写组织名称')
        return
    }
    saving.value = true
    SaveOrg({
        ...form,
        id: form.id || undefined,
        parentId: form.parentId || undefined
    })
        .then((res: any) => {
            ElMessage.success(res.msg)
            editorVisible.value = false
            loadData()
        })
        .finally(() => {
            saving.value = false
        })
}

function removeOrg(org: any) {
    ElMessageBox.confirm(`删除“${org.name}”后将解除成员和角色关系，是否继续？`, '提示', {
        type: 'warning'
    }).then(() => {
        DeleteOrg({
            orgId: org.id
        }).then((res: any) => {
            ElMessage.success(res.msg)
            loadData()
        })
    })
}

onMounted(loadData)
</script>

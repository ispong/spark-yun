<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-seach-table">
        <div class="zqy-table-top">
            <el-button type="primary" @click="addData">新建租户</el-button>
            <div class="zqy-seach">
                <el-input
                    v-model="keyword"
                    placeholder="请输入租户名 回车进行搜索"
                    :maxlength="200"
                    clearable
                    @input="inputEvent"
                    @keyup.enter="initData(false)"
                />
            </div>
        </div>
        <LoadingPage :visible="loading" :network-error="networkError" @loading-refresh="initData(true)">
            <div class="zqy-table">
                <BlockTable
                    :table-config="tableConfig"
                    @size-change="handleSizeChange"
                    @current-change="handleCurrentChange"
                >
                    <template #name="scopeSlot">
                        <span class="name-click" @click="editData(scopeSlot.row)">{{ scopeSlot.row.name }}</span>
                    </template>
                    <template #memberProgress="scopeSlot">
                        <div class="resource-progress">
                            <el-progress
                                :percentage="
                                    getUsagePercentage(scopeSlot.row.usedMemberNum, scopeSlot.row.maxMemberNum)
                                "
                                :color="getPercentColor()"
                                :show-text="false"
                                :stroke-width="12"
                            />
                            <span class="resource-progress__value">
                                {{ getUsageText(scopeSlot.row.usedMemberNum, scopeSlot.row.maxMemberNum) }}
                            </span>
                        </div>
                    </template>
                    <template #workflowProgress="scopeSlot">
                        <div class="resource-progress">
                            <el-progress
                                :percentage="
                                    getUsagePercentage(scopeSlot.row.usedWorkflowNum, scopeSlot.row.maxWorkflowNum)
                                "
                                :color="getPercentColor()"
                                :show-text="false"
                                :stroke-width="12"
                            />
                            <span class="resource-progress__value">
                                {{ getUsageText(scopeSlot.row.usedWorkflowNum, scopeSlot.row.maxWorkflowNum) }}
                            </span>
                        </div>
                    </template>
                    <template #statusTag="scopeSlot">
                        <div class="btn-group">
                            <el-tag v-if="scopeSlot.row.status === 'ENABLE'" class="ml-2" type="success">启用</el-tag>
                            <el-tag v-if="scopeSlot.row.status === 'DISABLE'" class="ml-2" type="danger">禁用</el-tag>
                        </div>
                    </template>
                    <template #options="scopeSlot">
                        <div class="btn-group">
                            <template v-if="scopeSlot.row.status === 'ENABLE'">
                                <span v-if="!scopeSlot.row.statusLoading" @click="changeStatus(scopeSlot.row, false)">
                                    禁用
                                </span>
                                <el-icon v-else class="is-loading">
                                    <Loading />
                                </el-icon>
                            </template>
                            <template v-else>
                                <span v-if="!scopeSlot.row.statusLoading" @click="changeStatus(scopeSlot.row, true)">
                                    启用
                                </span>
                                <el-icon v-else class="is-loading">
                                    <Loading />
                                </el-icon>
                            </template>
                            <el-dropdown trigger="click">
                                <span class="click-show-more">更多</span>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item @click="editData(scopeSlot.row)">编辑</el-dropdown-item>
                                        <el-dropdown-item @click="openReplaceAdmin(scopeSlot.row)">
                                            设置租户超级管理员
                                        </el-dropdown-item>
                                        <el-dropdown-item
                                            v-if="!scopeSlot.row.checkLoding"
                                            @click="checkTenant(scopeSlot.row)"
                                        >
                                            检测
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
        <el-dialog v-model="replaceAdminVisible" title="设置租户超级管理员" width="480px">
            <el-form label-position="top">
                <el-form-item label="租户超级管理员">
                    <el-select v-model="replaceAdminForm.newAdminUserId" filterable>
                        <el-option
                            v-for="user in enabledUsers"
                            :key="user.id"
                            :label="`${user.username} (${user.account})`"
                            :value="user.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="原租户超级管理员处理方式">
                    <el-radio-group v-model="replaceAdminForm.oldAdminAction">
                        <el-radio label="KEEP">保留为租户成员</el-radio>
                        <el-radio label="REMOVE">移出租户</el-radio>
                    </el-radio-group>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="replaceAdminVisible = false">取消</el-button>
                <el-button type="primary" :loading="replaceAdminLoading" @click="replaceAdmin">确认设置</el-button>
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

import { BreadCrumbList, TableConfig } from './tenant-list.config'
import {
    GetTenantList,
    AddTenantData,
    DeleteTenantData,
    CheckTenantData,
    DisableTenantData,
    EnableTenantData,
    UpdateTenantData,
    ReplaceTenantAdminData
} from '@/app/management/tenant-list/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import eventBus from '@/app/utils/eventBus'
import { useAuthStore } from '@/app/store/useAuth'
import { GetUserInfoList } from '@/app/management/tenant-user/api'

interface FormTenant {
    adminUserId?: string
    maxMemberNum: string
    maxWorkflowNum: string
    name: string
    remark: string
}

const authStore = useAuthStore()

// const state = useState(['tenantId'], 'authStoreModule')
// const mutations = useMutations(['setTenantId'], 'authStoreModule')

const keyword = ref('')
const loading = ref(false)
const networkError = ref(false)
const addModalRef = ref(null)
const breadCrumbList = reactive(BreadCrumbList)
const tableConfig: any = reactive(TableConfig)
const replaceAdminVisible = ref(false)
const replaceAdminLoading = ref(false)
const enabledUsers = ref<any[]>([])
const replaceAdminForm = reactive({
    tenantId: '',
    newAdminUserId: '',
    oldAdminAction: 'KEEP' as 'KEEP' | 'REMOVE'
})

function initData(tableLoading?: boolean) {
    loading.value = tableLoading ? false : true
    networkError.value = networkError.value || false
    GetTenantList({
        page: tableConfig.pagination.currentPage - 1,
        pageSize: tableConfig.pagination.pageSize,
        searchKeyWord: keyword.value
    })
        .then((res: any) => {
            tableConfig.tableData = res.data.content
            tableConfig.pagination.total = res.data.totalElements
            loading.value = false
            tableConfig.loading = false
            networkError.value = false
            if (!authStore.tenantId && tableConfig.tableData.length === 1) {
                authStore.setTenantId(tableConfig.tableData[0].id)
            }
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
    addModalRef.value.showModal((formData: FormTenant) => {
        return new Promise((resolve: any, reject: any) => {
            AddTenantData(formData)
                .then((res: any) => {
                    ElMessage.success(res.msg)
                    // 这里发送eventbus，刷新当前打开的页面
                    eventBus.emit('tenantListUpdate')

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
            UpdateTenantData(formData)
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

function checkTenant(data: any) {
    data.checkLoding = true
    CheckTenantData({
        tenantId: data.id
    })
        .then((res: any) => {
            data.checkLoding = false
            ElMessage.success(res.msg)
            initData(true)
        })
        .catch(() => {
            data.checkLoding = false
        })
}

function openReplaceAdmin(data: any) {
    replaceAdminForm.tenantId = data.id
    replaceAdminForm.newAdminUserId = ''
    replaceAdminForm.oldAdminAction = 'KEEP'
    GetUserInfoList({
        page: 0,
        pageSize: 999,
        searchKeyWord: ''
    }).then((res: any) => {
        enabledUsers.value = res.data.content || []
        replaceAdminVisible.value = true
    })
}

function replaceAdmin() {
    if (!replaceAdminForm.newAdminUserId) {
        ElMessage.warning('请选择租户超级管理员')
        return
    }
    replaceAdminLoading.value = true
    ReplaceTenantAdminData(replaceAdminForm)
        .then((res: any) => {
            ElMessage.success(res.msg)
            replaceAdminVisible.value = false
            initData(true)
        })
        .finally(() => {
            replaceAdminLoading.value = false
        })
}

// 启用 or 禁用
function changeStatus(data: any, status: boolean) {
    data.statusLoading = true
    if (status) {
        EnableTenantData({
            tenantId: data.id
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
        DisableTenantData({
            tenantId: data.id
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
    ElMessageBox.prompt(`请输入租户名称“${data.name}”确认删除`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: new RegExp(`^${data.name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`),
        inputErrorMessage: '租户名称不一致',
        type: 'warning'
    }).then(({ value }) => {
        DeleteTenantData({
            tenantId: data.id,
            tenantName: value
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

function getUsagePercentage(usedValue: number | string, totalValue: number | string) {
    const used = Number(usedValue) || 0
    const total = Number(totalValue) || 0
    if (total <= 0) {
        return used > 0 ? 100 : 0
    }
    return Math.min(100, Number(((used / total) * 100).toFixed(2)))
}

function getUsageText(usedValue: number | string, totalValue: number | string) {
    const used = Number(usedValue) || 0
    const total = Number(totalValue) || 0
    return `${used} / ${total}`
}

function getPercentColor(): string {
    return 'var(--el-color-primary-light-3)'
}

onMounted(() => {
    tableConfig.pagination.currentPage = 1
    tableConfig.pagination.pageSize = 10
    initData()
})
</script>

<style lang="scss">
.zqy-seach-table {
    .resource-progress {
        min-width: 120px;
        padding-right: 6px;
        display: flex;
        align-items: center;
        gap: 8px;

        .el-progress {
            flex: 1;
        }
    }

    .resource-progress__value {
        color: getCssVar('text-color', 'secondary');
        white-space: nowrap;
        font-size: getCssVar('font-size', 'extra-small');
    }
}
</style>

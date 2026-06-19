<template>
    <el-dialog
        v-model="tenantDialogVisible"
        width="420px"
        align-center
        append-to-body
        :show-close="false"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        class="zqy-layout__tenant-dialog"
    >
        <div class="zqy-layout__tenant-dialog-header">
            <div class="zqy-layout__tenant-dialog-title">切换租户</div>
            <el-input
                v-model="tenantKeyword"
                class="zqy-layout__tenant-dialog-search"
                clearable
                placeholder="搜索租户"
                :prefix-icon="Search"
            />
        </div>
        <div class="zqy-layout__tenant-dialog-list">
            <div
                v-for="tenant in filteredTenantList"
                :key="tenant.id"
                class="zqy-layout__tenant-dialog-item"
                :class="{
                    'is-current': authStore.tenantId === tenant.id,
                    'is-selected': selectedTenantId === tenant.id
                }"
                @click="handleTenantSelect(tenant)"
            >
                <div class="zqy-layout__tenant-name">
                    <EllipsisTooltip class="zqy-layout__tenant-name-text" :label="tenant.name" />
                </div>
                <span v-if="authStore.tenantId === tenant.id" class="zqy-layout__tenant-current">当前</span>
            </div>
            <div v-if="!filteredTenantList.length" class="zqy-layout__tenant-dialog-empty">暂无匹配租户</div>
        </div>
        <template #footer>
            <div class="zqy-layout__tenant-dialog-footer">
                <el-button @click="closeTenantDialog">取消</el-button>
                <el-button
                    type="primary"
                    :loading="switchTenantLoading"
                    :disabled="!selectedTenantId"
                    @click="confirmTenantSwitch"
                >
                    确认切换
                </el-button>
            </div>
        </template>
    </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

import { ChangeTenantData } from '@/app/api'
import EllipsisTooltip from '@/app/components/ellipsis-tooltip/ellipsis-tooltip.vue'
import { useSwitchTenant, type TenantInfo } from '@/app/hooks/switch-tenant'
import { useAuthStore } from '@/app/store/useAuth'
import eventBus from '@/app/utils/eventBus'
import { http } from '@/app/utils/http'
import { getVipLicenseEnabled } from '@/app/utils/vip-license'

const emit = defineEmits<{
    (event: 'tenant-name-change', name: string): void
}>()

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const tenantDialogVisible = ref(false)
const selectedTenantId = ref('')
const switchTenantLoading = ref(false)
const tenantKeyword = ref('')

const { tenantList, initSwitchTenant, onTenantChange } = useSwitchTenant()

const canSwitchTenant = computed(() => !!authStore.tenantId && !authStore.userInfo?.platformSuperAdmin)
const activeTenantName = computed(() => {
    const current = tenantList.value.find((item) => item.id === authStore.tenantId)
    return current?.name || '切换租户'
})
const filteredTenantList = computed(() => {
    const keyword = tenantKeyword.value.trim().toLowerCase()
    if (!keyword) {
        return tenantList.value
    }
    return tenantList.value.filter((tenant) => (tenant.name || '').toLowerCase().includes(keyword))
})

function loadTenantList() {
    if (!canSwitchTenant.value) {
        return
    }
    initSwitchTenant().then(() => {
        onTenantChange(authStore.tenantId)
    })
}

function handleTenantSelect(tenant: TenantInfo) {
    selectedTenantId.value = tenant.id
}

function closeTenantDialog() {
    tenantDialogVisible.value = false
    selectedTenantId.value = authStore.tenantId
    tenantKeyword.value = ''
}

function confirmTenantSwitch() {
    if (!selectedTenantId.value || switchTenantLoading.value) {
        return
    }
    const targetTenantId = selectedTenantId.value
    if (authStore.tenantId === targetTenantId) {
        closeTenantDialog()
        return
    }

    switchTenantLoading.value = true
    onTenantChange(targetTenantId)

    ChangeTenantData(
        {
            tenantId: targetTenantId
        },
        targetTenantId
    )
        .then((res: any) => {
            return getVipLicenseEnabled(true).finally(() => {
                const needBackToWorkflowList = ['workflow-page', 'work-item', 'workflow-detail'].includes(
                    String(route.name || '')
                )
                const applyTenantContext = () => {
                    authStore.applyAuthResponse(res.data)
                    http.setHeader({
                        authorization: authStore.token,
                        tenant: targetTenantId
                    })
                }

                ElMessage.success('租户切换成功')
                closeTenantDialog()
                applyTenantContext()
                if (needBackToWorkflowList) {
                    router.replace({
                        name: 'workflow'
                    })
                } else if (
                    route.path.startsWith('/admin') &&
                    !res.data.tenantSuperAdmin &&
                    !res.data.tenantAdmin
                ) {
                    router.replace('/workspace')
                }
            })
        })
        .catch(() => {
            onTenantChange(authStore.tenantId)
        })
        .finally(() => {
            switchTenantLoading.value = false
        })
}

function open() {
    selectedTenantId.value = authStore.tenantId
    tenantKeyword.value = ''
    tenantDialogVisible.value = true
    loadTenantList()
}

onMounted(() => {
    loadTenantList()
    eventBus.on('tenantListUpdate', loadTenantList)
})

onUnmounted(() => {
    eventBus.off('tenantListUpdate', loadTenantList)
})

watch(
    activeTenantName,
    (name) => {
        emit('tenant-name-change', name)
    },
    {
        immediate: true
    }
)

watch(
    () => authStore.tenantId,
    (tenantId) => {
        onTenantChange(tenantId)
    }
)

defineExpose({
    open
})
</script>

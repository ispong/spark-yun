<template>
    <div class="system-page">
        <el-result icon="warning" title="暂无可访问租户" sub-title="当前账号暂无可访问租户，请联系管理员。">
            <template #extra>
                <el-button :loading="loading" type="primary" @click="recoverTenant">重新检测</el-button>
                <el-button @click="logout">退出登录</el-button>
            </template>
        </el-result>
    </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ChangeTenantData, QueryTenantList } from '@/app/api'
import { useAuthStore } from '@/app/store/useAuth'

const loading = ref(false)
const router = useRouter()
const authStore = useAuthStore()

function recoverTenant() {
    loading.value = true
    QueryTenantList()
        .then((res: any) => {
            const tenants = res.data || []
            const tenant = tenants.find((item: any) => item.currentTenant) || tenants[0]
            if (!tenant) {
                return
            }
            return ChangeTenantData(
                {
                    tenantId: tenant.id
                },
                tenant.id
            ).then((changeRes: any) => {
                authStore.applyAuthResponse(changeRes.data)
                router.replace('/workspace')
            })
        })
        .finally(() => {
            loading.value = false
        })
}

function logout() {
    authStore.$reset()
    router.replace({
        name: 'login'
    })
}
</script>

<style scoped lang="scss">
.system-page {
    height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
}
</style>

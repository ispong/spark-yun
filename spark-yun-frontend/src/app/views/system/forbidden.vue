<template>
    <div class="system-page">
        <el-result icon="warning" title="403" sub-title="当前账号无权访问该页面">
            <template #extra>
                <el-button type="primary" @click="goDefault">返回可访问区域</el-button>
            </template>
        </el-result>
    </div>
</template>

<script lang="ts" setup>
import { useAuthStore } from '@/app/store/useAuth'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()

function goDefault() {
    const fallbackRoutePath =
        authStore.userInfo?.platformSuperAdmin || (authStore.userInfo?.platformAdmin && !authStore.tenantId)
            ? '/platform'
            : '/workspace/ai'
    const routePath =
        authStore.userInfo?.defaultArea === 'workspace'
            ? '/workspace/ai'
            : authStore.userInfo?.defaultArea
              ? `/${authStore.userInfo.defaultArea}`
              : fallbackRoutePath
    router.replace(routePath)
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

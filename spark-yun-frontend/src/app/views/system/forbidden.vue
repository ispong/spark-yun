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
    router.replace(authStore.userInfo?.systemAdmin ? '/platform' : '/workspace')
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

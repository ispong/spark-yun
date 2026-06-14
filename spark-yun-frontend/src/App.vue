<template>
    <el-config-provider :locale="locale">
        <!-- 进入路由 -->
        <router-view />
    </el-config-provider>
</template>

<script lang="ts" setup>
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { reactive } from 'vue'

// 设置elementPlus的语言
const { locale } = reactive({
    locale: zhCn
})
</script>

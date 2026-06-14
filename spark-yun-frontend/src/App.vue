<template>
    <el-config-provider :locale="locale">
        <router-view />
    </el-config-provider>
</template>
<script lang="ts" setup>
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { reactive } from 'vue'

const { locale } = reactive({
    locale: zhCn
})
</script>

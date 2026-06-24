<template>
    <el-config-provider :locale="elementLocale">
        <!-- 进入路由，展示哪个组件 -->
        <router-view />
    </el-config-provider>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { getElementLocale } from '@/app/i18n'
import { useLocaleStore } from '@/app/store/useLocale'

const localeStore = useLocaleStore()
const elementLocale = computed(() => getElementLocale(localeStore.locale))
</script>

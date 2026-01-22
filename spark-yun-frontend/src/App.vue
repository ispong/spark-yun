<template>
  <el-config-provider :locale="locale">
    <router-view />
  </el-config-provider>
</template>
<script lang="ts" setup>
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import enUS from 'element-plus/dist/locale/en.mjs'
import { computed } from 'vue'
import { useLocaleStore } from '@/store/useLocale'

const localeStore = useLocaleStore()

const locale = computed(() => {
  return localeStore.locale === 'zh-CN' ? zhCn : enUS
})
</script>

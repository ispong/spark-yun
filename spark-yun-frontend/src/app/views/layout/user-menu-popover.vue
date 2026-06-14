<template>
    <el-popover
        v-model:visible="menuVisible"
        placement="top-end"
        :show-arrow="false"
        trigger="click"
        popper-class="zqy-layout__menu-avatar"
    >
        <template #reference>
            <el-avatar class="zqy-layout__avatar" :size="32">
                {{ username }}
            </el-avatar>
        </template>
        <div class="zqy-layout__menu-options" @mouseleave="menuVisible = false">
            <div v-if="!isSystemAdmin" class="zqy-layout__menu-option" @click="handleGoArea('/workspace')">
                <el-icon>
                    <Monitor />
                </el-icon>
                工作台
            </div>
            <div v-if="isPlatformAdmin" class="zqy-layout__menu-option" @click="handleGoArea('/platform')">
                <el-icon>
                    <Setting />
                </el-icon>
                平台管理
            </div>
            <div v-if="isTenantManager" class="zqy-layout__menu-option" @click="handleGoArea('/admin')">
                <el-icon>
                    <Tools />
                </el-icon>
                后台管理
            </div>
            <div v-if="!isSystemAdmin" class="zqy-layout__menu-option" @click="handleOpenTenantDialog">
                <el-icon>
                    <OfficeBuilding />
                </el-icon>
                <EllipsisTooltip class="zqy-layout__menu-text" :label="activeTenantName" />
            </div>
            <div class="zqy-layout__menu-option" @click="handleGoPersonalInfo">
                <el-icon>
                    <Setting />
                </el-icon>
                个人中心
            </div>
            <div class="zqy-layout__menu-option" @click="handleLogout">
                <el-icon>
                    <SwitchButton />
                </el-icon>
                退出登录
            </div>
        </div>
    </el-popover>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Monitor, OfficeBuilding, Setting, SwitchButton, Tools } from '@element-plus/icons-vue'

import EllipsisTooltip from '@/app/components/ellipsis-tooltip/ellipsis-tooltip.vue'

const props = defineProps<{
    username?: string
    isSystemAdmin: boolean
    isPlatformAdmin: boolean
    isTenantManager: boolean
    activeTenantName: string
}>()

const emit = defineEmits<{
    (event: 'go-area', path: string): void
    (event: 'go-personal-info'): void
    (event: 'open-tenant-dialog'): void
    (event: 'logout'): void
}>()

const menuVisible = ref(false)
const username = computed(() => props.username || '')

function closeMenu() {
    menuVisible.value = false
}

function handleGoArea(path: string) {
    closeMenu()
    emit('go-area', path)
}

function handleGoPersonalInfo() {
    closeMenu()
    emit('go-personal-info')
}

function handleOpenTenantDialog() {
    closeMenu()
    emit('open-tenant-dialog')
}

function handleLogout() {
    closeMenu()
    emit('logout')
}
</script>

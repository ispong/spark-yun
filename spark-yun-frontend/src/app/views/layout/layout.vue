<template>
    <div :class="layoutClass">
        <div class="zqy-layout__sidebar">
            <div class="zqy-layout__nav">
                <img class="zqy-layout__logo" :src="isCollapse ? logoURLSmall : logoURL" alt="logo" />
            </div>
            <div
                class="zqy-layout__menu-wrap"
                @mouseenter="isCollapse = false"
                @mouseleave="isCollapse = true"
            >
                <el-menu
                    class="zqy-layout__menu"
                    :unique-opened="true"
                    :collapse="isCollapse"
                    :default-active="currentMenu?.code"
                    @select="handleSelect"
                >
                    <template v-for="menuData in menuViewData" :key="menuData.code">
                        <el-sub-menu
                            v-if="menuData.children?.length"
                            :index="menuData.code"
                        >
                            <template #title>
                                <el-icon class="zqy-layout__icon">
                                    <component :is="resolveIcon(menuData.icon)" />
                                </el-icon>
                                <span v-if="!isCollapse" class="zqy-layout__text">{{ menuData.name }}</span>
                            </template>
                            <el-menu-item
                                v-for="menu in menuData.children"
                                :key="menu.code"
                                :index="menu.code"
                            >
                                <el-icon class="zqy-layout__icon">
                                    <component :is="resolveIcon(menu.icon)" />
                                </el-icon>
                                <template #title>
                                    <span class="zqy-layout__text">{{ menu.name }}</span>
                                </template>
                            </el-menu-item>
                        </el-sub-menu>
                        <el-menu-item v-else :index="menuData.code">
                            <el-icon class="zqy-layout__icon">
                                <component :is="resolveIcon(menuData.icon)" />
                            </el-icon>
                            <template #title>
                                <span class="zqy-layout__text">{{ menuData.name }}</span>
                            </template>
                        </el-menu-item>
                    </template>
                </el-menu>
                <div class="zqy-layout__menu-footer">
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
                        <div v-if="!isSystemAdmin" class="zqy-layout__menu-option" @click="goArea('/workspace')">
                            <el-icon>
                                <Monitor />
                            </el-icon>
                            工作台
                        </div>
                        <div v-if="isPlatformAdmin" class="zqy-layout__menu-option" @click="goArea('/platform')">
                            <el-icon>
                                <Setting />
                            </el-icon>
                            平台管理
                        </div>
                        <div v-if="isTenantManager" class="zqy-layout__menu-option" @click="goArea('/admin')">
                            <el-icon>
                                <Tools />
                            </el-icon>
                            后台管理
                        </div>
                        <div v-if="!isSystemAdmin" class="zqy-layout__menu-option" @click="openTenantDialog">
                            <el-icon>
                                <OfficeBuilding />
                            </el-icon>
                            <EllipsisTooltip class="zqy-layout__menu-text" :label="activeTenantName" />
                        </div>
                        <div class="zqy-layout__menu-option" @click="goPersonalInfo">
                            <el-icon>
                                <Setting />
                            </el-icon>
                            个人中心
                        </div>
                        <div class="zqy-layout__menu-option" @click="handleCommand('logout')">
                            <el-icon>
                                <SwitchButton />
                            </el-icon>
                            退出登录
                        </div>
                    </el-popover>
                </div>
            </div>
        </div>
        <div class="zqy-layout__main">
            <router-view :key="authStore.tenantId" />
        </div>

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
                        <EllipsisTooltip class="zqy-layout__menu-text" :label="tenant.name" />
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
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, resolveComponent, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Monitor, OfficeBuilding, Search, Setting, SwitchButton, Tools } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

import { ChangeTenantData } from '@/app/api'
import logoURLSmall from '@/app/assets/imgs/logo.png'
import logoURL from '@/app/assets/imgs/logo-a.png'
import EllipsisTooltip from '@/app/components/ellipsis-tooltip/ellipsis-tooltip.vue'
import { useSwitchTenant, type TenantInfo } from '@/app/hooks/switch-tenant'
import { CheckLicenseStatus } from '@/app/management/license/api'
import { useAuthStore } from '@/app/store/useAuth'
import { http } from '@/app/utils/http'
import {
    filterVipMenus,
    getLicenseApiAvailable,
    getVipLicenseEnabled,
    resetVipLicenseCache
} from '@/app/utils/vip-license'
import { adminMenuListData, platformMenuListData, workspaceMenuListData, type Menu } from './menu.config'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const vipEnabled = ref(false)
const licenseApiAvailable = ref(true)
const vipChecked = ref(false)
const isCollapse = ref(true)
const menuVisible = ref(false)
const tenantDialogVisible = ref(false)
const selectedTenantId = ref('')
const switchTenantLoading = ref(false)
const tenantKeyword = ref('')

const menuListData = computed(() => {
    if (route.path.startsWith('/platform')) {
        return platformMenuListData
    }
    if (route.path.startsWith('/admin')) {
        return adminMenuListData
    }
    return workspaceMenuListData
})

const platformMenuPaths: Record<string, string> = {
    'user-center': '/platform/users',
    'tenant-list': '/platform/tenants',
    license: '/platform/license',
    'oauth-management': '/platform/auth'
}

const adminMenuPaths: Record<string, string> = {
    'tenant-user': '/admin/members',
    'role-management': '/admin/roles',
    'org-management': '/admin/orgs'
}

const menuViewData = computed(() => {
    const areaMenus = route.path.startsWith('/workspace')
        ? filterWorkspaceMenus(
              menuListData.value,
              authStore.userInfo?.permissions || [],
              !!authStore.userInfo?.workspaceAllPermissions
          )
        : menuListData.value
    return filterVipMenus(areaMenus, vipEnabled.value, licenseApiAvailable.value)
})

const currentMenu = computed(() => {
    const routeMenuCode = getAreaMenuCode(route.path) || String(route.name || '')
    const matchedMenu = menuViewData.value.find((menuData) => menuData.code === routeMenuCode)
    return matchedMenu || getCurrentMenu(menuViewData.value, routeMenuCode)
})

const username = computed(() => {
    return authStore.userInfo?.username?.slice(0, 1)
})

const { tenantList, initSwitchTenant, onTenantChange } = useSwitchTenant()

const isSystemAdmin = computed(() => !!authStore.userInfo?.systemAdmin)
const isPlatformAdmin = computed(() => !!authStore.userInfo?.platformAdmin)
const isTenantManager = computed(() => !!authStore.userInfo?.tenantAdmin || !!authStore.userInfo?.normalAdmin)
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

const layoutClass = computed<Record<string, boolean>>(() => ({
    'zqy-layout': true,
    'is-collapse': isCollapse.value
}))

function resolveIcon(icon: string) {
    return resolveComponent(icon)
}

function getCurrentMenu(menuList: Menu[], routeMenu: string, targetMenu?: Menu): Menu | null {
    let currentMenu: Menu | null = targetMenu || null
    menuList.forEach((menu) => {
        if (menu.children?.length) {
            currentMenu = getCurrentMenu(menu.children, routeMenu, currentMenu || undefined)
        }
        if (menu.code === routeMenu || menu.childPage?.includes(routeMenu)) {
            currentMenu = menu
        }
    })
    return currentMenu
}

function filterWorkspaceMenus(menuList: Menu[], permissions: string[], allPermissions: boolean): Menu[] {
    return menuList.reduce<Menu[]>((result, menu) => {
        const children = menu.children ? filterWorkspaceMenus(menu.children, permissions, allPermissions) : undefined
        const allowed =
            allPermissions ||
            permissions.includes(menu.permission || `workspace:${menu.code}:menu`) ||
            !!children?.length
        if (allowed) {
            result.push({
                ...menu,
                children
            })
        }
        return result
    }, [])
}

function firstLeafMenu(menuList: Menu[]): Menu | undefined {
    for (const menu of menuList) {
        if (menu.children?.length) {
            const child = firstLeafMenu(menu.children)
            if (child) {
                return child
            }
        } else {
            return menu
        }
    }
}

function getAreaMenuCode(path: string): string | undefined {
    if (path.startsWith('/platform')) {
        return Object.entries(platformMenuPaths).find(([, menuPath]) => menuPath === path)?.[0]
    }
    if (path.startsWith('/admin')) {
        return Object.entries(adminMenuPaths).find(([, menuPath]) => menuPath === path)?.[0]
    }
}

async function loadVipLicense(forceRefresh = false) {
    vipChecked.value = false
    vipEnabled.value = await getVipLicenseEnabled(forceRefresh)
    licenseApiAvailable.value = getLicenseApiAvailable()
    vipChecked.value = true
}

function handleSelect(index: Menu['code']) {
    if (route.path.startsWith('/platform') && platformMenuPaths[index]) {
        router.push(platformMenuPaths[index])
        return
    }
    if (route.path.startsWith('/admin') && adminMenuPaths[index]) {
        router.push(adminMenuPaths[index])
        return
    }
    router.push({
        name: index
    })
}

function doLogout() {
    resetVipLicenseCache()
    setTimeout(() => {
        authStore.$reset()
    })
    ElMessage.success('退出成功')
    router.push({
        name: 'login'
    })
}

function goPersonalInfo() {
    menuVisible.value = false
    router.push({
        name: 'personalInfo'
    })
}

function goArea(path: string) {
    menuVisible.value = false
    router.push(path)
}

function handleCommand(command: 'logout') {
    if (command === 'logout') {
        CheckLicenseStatus()
            .catch(() => {
                // 退出时仅做许可证状态刷新，失败不影响退出流程
            })
            .finally(() => {
                doLogout()
            })
    }
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
            getVipLicenseEnabled(true).finally(() => {
                const needBackToWorkflowList = ['workflow-page', 'work-item', 'workflow-detail'].includes(
                    String(route.name || '')
                )
                const applyTenantContext = () => {
                    authStore.applyAuthResponse(res.data)
                    http.setHeader({
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
                } else if (route.path.startsWith('/admin') && !res.data.tenantAdmin && !res.data.normalAdmin) {
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

function openTenantDialog() {
    menuVisible.value = false
    selectedTenantId.value = authStore.tenantId
    tenantKeyword.value = ''
    tenantDialogVisible.value = true
}

onMounted(async () => {
    await loadVipLicense()
})

if (!isSystemAdmin.value) {
    initSwitchTenant()
}

watch(
    () => authStore.tenantId,
    async (tenantId, oldTenantId) => {
        if (!tenantId || tenantId === oldTenantId) {
            return
        }
        await loadVipLicense()
    }
)

watch(
    () => [vipChecked.value, route.name, menuViewData.value.length],
    () => {
        if (!vipChecked.value) {
            return
        }
        if (route.name === 'personalInfo') {
            return
        }
        if (!currentMenu.value && menuViewData.value.length) {
            const target = firstLeafMenu(menuViewData.value)
            if (!target) {
                return
            }
            const targetPath = route.path.startsWith('/platform')
                ? platformMenuPaths[target.code]
                : route.path.startsWith('/admin')
                  ? adminMenuPaths[target.code]
                  : undefined
            if (targetPath) {
                router.replace(targetPath)
                return
            }
            router.replace({
                name: target.code
            })
        }
    },
    {
        immediate: true
    }
)

watch(
    () => isCollapse.value,
    (newVal) => {
        authStore.setCollapse(newVal)
    }
)
</script>


<style lang="scss">
.zqy-layout {
    display: flex;
    width: 100vw;
    height: 100vh;

    &.is-collapse {
        .zqy-layout__sidebar {
            width: 80px;
            z-index: 999;
            .zqy-layout__nav {
                display: flex;
                justify-content: center;
                align-items: center;
            }
        }

        .zqy-layout__main {
            padding-left: 80px;
        }

        .zqy-layout__menu-footer {
            // width: 80px;
            padding-left: 24px;
            // height: 100px;
            // flex-direction: column;
            // justify-content: space-evenly;
        }

        .zqy-layout__title {
            display: none;
        }
    }

    .zqy-layout__sidebar {
        --el-transition-duration: 0.3s;

        display: flex;
        position: absolute;
        left: 0;
        top: 0;
        flex-direction: column;
        width: 220px;
        height: 100%;
        overflow: hidden;
        background-color: getCssVar('color', 'white');
        transition: width getCssVar('transition-duration') ease-in-out;
        border-right: 1px solid var(--el-border-color);
        z-index: 2000;
    }

    .zqy-layout__main {
        width: 100vw;
        background-color: getCssVar('color', 'white');
        padding-left: 80px;
        box-sizing: border-box;
        transition: all getCssVar('transition-duration') ease-in-out;
    }

    .zqy-layout__nav {
        width: 100%;
        height: 80px;
        box-sizing: border-box;
        padding: 10px 20px;
        display: flex;
        justify-content: center;
        align-items: center;
    }

    .zqy-layout__logo {
        height: 35px;
        // width: 40px;
    }

    .zqy-layout__title {
        font-size: 28px;
        font-weight: bold;
        line-height: 46px;
        vertical-align: bottom;
        color: getCssVar('color', 'primary');
    }

    .zqy-layout__icon {
        color: getCssVar('color', 'info');
    }

    .zqy-layout__ops {
        cursor: pointer;
        &:hover {
            color: getCssVar('color', 'primary');
        }
    }

    .zqy-layout__avatar {
        cursor: pointer;
        background-color: getCssVar('color', 'primary');
        color: getCssVar('color', 'white');
        font-size: getCssVar('font-size', 'extra-small');
    }

    .zqy-layout__menu-wrap {
        padding: 10px 8px 60px 8px;
        box-sizing: border-box;
        flex: 1;
        overflow-x: hidden;
        overflow-y: auto;
        width: 100%;
        .zqy-layout__menu.el-menu {
            border-right: 0;
            .el-sub-menu {
                .el-menu {
                    .el-menu-item {
                        padding-left: 44px;
                    }
                }
            }
            .el-menu-item {
                border-radius: getCssVar('border-radius', 'base');

                &.is-active {
                    background-color: getCssVar('menu', 'hover-bg-color');

                    .zqy-layout__text {
                        font-weight: bold;
                    }
                }
            }
        }
    }

    .zqy-layout__menu-footer {
        position: absolute;
        bottom: 0;
        left: 0;
        display: flex;
        // justify-content: center;
        align-items: center;
        height: 60px;
        box-sizing: border-box;
        padding: 0 16px;
        width: 100%;
        background-color: getCssVar('color', 'white');
        transition: getCssVar('transition-duration') width ease-in-out;
    }
}

.zqy-layout__menu-tenant {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 5px 12px;

    .zqy-layout__menu-title {
        max-width: 72px;
    }
}

.zqy-layout__menu-icon {
    cursor: pointer;

    &:hover {
        color: getCssVar('color', 'primary');
    }
}

.zqy-layout__menu-avatar.el-popover.el-popper {
    min-width: 160px !important;
    width: 160px !important;
    padding: 4px;
}

.zqy-layout__tenant-popover.el-popover {
    padding: 4px;
}

.zqy-layout__tenant-dialog {
    .el-dialog__header {
        display: none;
    }

    .el-dialog__body {
        padding: 16px 16px 8px 16px;
    }
}

.zqy-layout__tenant-dialog-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 12px;

    .zqy-layout__tenant-dialog-title {
        flex-shrink: 0;
        font-size: 16px;
        line-height: 24px;
        font-weight: 600;
        color: getCssVar('text-color', 'primary');
    }
}

.zqy-layout__tenant-dialog-search {
    flex: 1;
    min-width: 240px;
    max-width: 100%;
}

.zqy-layout__tenant-dialog-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
    height: 232px;
    overflow: auto;
    overflow-x: hidden;
    padding-right: 4px;
}

.zqy-layout__tenant-dialog-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    min-height: 40px;
    padding: 8px 12px;
    box-sizing: border-box;
    border-radius: 6px;
    cursor: pointer;
    border: 1px solid transparent;

    &:hover {
        background-color: getCssVar('color-primary-light-9');
        color: getCssVar('color-primary');
    }

    &.is-current {
        border-color: getCssVar('color-primary-light-7');
    }

    &.is-selected {
        background-color: getCssVar('color-primary-light-9');
        color: getCssVar('color-primary');
        border-color: getCssVar('color-primary');
        box-shadow: 0 0 0 1px rgba(var(--el-color-primary-rgb), 0.12);
    }

    .zqy-layout__tenant-name {
        flex: 1;
        min-width: 0;
    }

    .zqy-layout__tenant-current {
        font-size: 12px;
        color: getCssVar('color-primary');
        margin-left: 12px;
        flex-shrink: 0;
    }
}

.zqy-layout__tenant-dialog-footer {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: 8px;
}

.zqy-layout__tenant-dialog-empty {
    height: 96px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: getCssVar('text-color', 'placeholder');
    font-size: 13px;
}

.zqy-layout__menu-option {
    height: 32px;
    display: flex;
    align-items: center;
    padding: 0 12px;
    cursor: pointer;
    border-radius: 4px;

    .el-icon {
        margin-right: 12px;
    }

    &:hover {
        background-color: getCssVar('color-primary-light-9');
        color: getCssVar('color-primary');
    }

    .zqy-layout__menu-text {
        max-width: 120px;
    }
}

</style>

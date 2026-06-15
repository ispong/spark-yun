<template>
    <div class="zqy-layout">
        <div
            class="zqy-layout__sidebar"
            :class="{ 'is-collapse': isCollapse }"
            @mouseenter="isCollapse = false"
            @mouseleave="isCollapse = true"
        >
            <div class="zqy-layout__nav">
                <img class="zqy-layout__logo" :src="isCollapse ? logoURLSmall : logoURL" alt="logo" />
            </div>

            <div class="zqy-layout__menu-wrap">
                <el-menu
                    class="zqy-layout__menu"
                    :unique-opened="true"
                    :collapse="isCollapse"
                    :default-active="currentMenu?.code"
                    @select="handleSelect"
                >
                    <template v-for="menuData in menuViewData" :key="menuData.code">
                        <el-sub-menu v-if="menuData.children?.length" :index="menuData.code">
                            <template #title>
                                <el-icon class="zqy-layout__icon">
                                    <component :is="resolveIcon(menuData.icon)" />
                                </el-icon>
                                <span v-show="!isCollapse" class="zqy-layout__text">{{ menuData.name }}</span>
                            </template>

                            <el-menu-item
                                v-for="menu in menuData.children"
                                :key="menu.code"
                                :index="menu.code"
                            >
                                <template #title>
                                    <el-icon class="zqy-layout__icon">
                                        <component :is="resolveIcon(menu.icon)" />
                                    </el-icon>
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
            </div>

            <div class="zqy-layout__menu-footer">
                <el-popover
                    v-model:visible="menuVisible"
                    placement="top-end"
                    :show-arrow="false"
                    trigger="click"
                    popper-class="zqy-layout__user-menu-popper"
                >
                    <template #reference>
                        <el-avatar class="zqy-layout__avatar" :size="32">
                            {{ username }}
                        </el-avatar>
                    </template>
                    <div class="zqy-layout__user-menu-options" @mouseleave="menuVisible = false">
                        <div v-if="showTenantSwitch" class="zqy-layout__user-menu-option" @click="openTenantDialog">
                            <el-icon>
                                <OfficeBuilding />
                            </el-icon>
                            <EllipsisTooltip class="zqy-layout__user-menu-text" :label="activeTenantName" />
                        </div>
                        <div v-if="showWorkspaceEntry" class="zqy-layout__user-menu-option" @click="goArea('/workspace')">
                            <el-icon>
                                <SetUp />
                            </el-icon>
                            工作台
                        </div>
                        <div v-if="showAdminEntry" class="zqy-layout__user-menu-option" @click="goArea('/admin')">
                            <el-icon>
                                <ScaleToOriginal />
                            </el-icon>
                            后台管理
                        </div>
                        <div v-if="showPlatformEntry" class="zqy-layout__user-menu-option" @click="goArea('/platform')">
                            <el-icon>
                                <Monitor />
                            </el-icon>
                            平台管理
                        </div>
                        <div v-if="showPersonalInfo" class="zqy-layout__user-menu-option" @click="goPersonalInfo">
                            <el-icon>
                                <User />
                            </el-icon>
                            个人中心
                        </div>
                        <div class="zqy-layout__user-menu-option" @click="handleCommand('logout')">
                            <el-icon>
                                <SwitchButton />
                            </el-icon>
                            退出登录
                        </div>
                    </div>
                </el-popover>
            </div>
        </div>

        <div class="zqy-layout__main">
            <el-empty
                v-if="showNoWorkspaceAccess"
                class="zqy-layout__empty"
                description="暂无可访问菜单，请联系管理员分配权限"
            />
            <router-view v-else :key="authStore.tenantId" />
        </div>

        <TenantSwitchDialog ref="tenantSwitchDialogRef" @tenant-name-change="activeTenantName = $event" />
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, resolveComponent, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Monitor, OfficeBuilding, ScaleToOriginal, SetUp, SwitchButton, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

import logoURLSmall from '@/app/assets/imgs/logo.png'
import logoURL from '@/app/assets/imgs/logo-a.png'
import EllipsisTooltip from '@/app/components/ellipsis-tooltip/ellipsis-tooltip.vue'
import { CheckLicenseStatus } from '@/app/management/license/api'
import { useAuthStore } from '@/app/store/useAuth'
import {
    filterVipMenus,
    getLicenseApiAvailable,
    getVipLicenseEnabled,
    resetVipLicenseCache
} from '@/app/utils/vip-license'
import {
    adminMenuListData,
    personalInfoMenuListData,
    platformMenuListData,
    workspaceMenuListData,
    type Menu
} from './menu.config'
import TenantSwitchDialog from './tenant-switch-dialog.vue'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const vipEnabled = ref(false)
const licenseApiAvailable = ref(true)
const vipChecked = ref(false)
const isCollapse = ref(true)
const menuVisible = ref(false)
const tenantSwitchDialogRef = ref<InstanceType<typeof TenantSwitchDialog>>()
const activeTenantName = ref('切换租户')

const currentArea = computed(() => {
    if (route.path.startsWith('/platform')) {
        return 'platform'
    }
    if (route.path.startsWith('/admin')) {
        return 'admin'
    }
    if (route.path.startsWith('/workspace')) {
        return 'workspace'
    }
    return ''
})

const menuListData = computed(() => {
    if (isPersonalInfoRoute.value) {
        return personalInfoMenuListData
    }
    if (currentArea.value === 'platform') {
        return platformMenuListData
    }
    if (currentArea.value === 'admin') {
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

const personalInfoRouteNames: Record<string, string> = {
    platform: 'platform-personalInfo',
    admin: 'admin-personalInfo',
    workspace: 'workspace-personalInfo'
}

// 菜单显示哪些
const menuViewData = computed(() => {
    const areaMenus = menuListData.value
    return filterVipMenus(areaMenus, vipEnabled.value, licenseApiAvailable.value)
})

const currentMenu = computed(() => {
    const routeMenuCode = isPersonalInfoRoute.value
        ? resolvePersonalInfoTab(route.query.tab)
        : getAreaMenuCode(route.path) || String(route.name || '')
    const matchedMenu = menuViewData.value.find((menuData) => menuData.code === routeMenuCode)
    return matchedMenu || getCurrentMenu(menuViewData.value, routeMenuCode)
})

const username = computed(() => {
    return authStore.userInfo?.username?.slice(0, 1)
})

const isPlatformSuperAdmin = computed(() => !!authStore.userInfo?.platformSuperAdmin)
const isPlatformAdmin = computed(() => !!authStore.userInfo?.platformAdmin)
const isTenantManager = computed(() => !!authStore.userInfo?.tenantSuperAdmin || !!authStore.userInfo?.tenantAdmin)
const hasTenant = computed(() => !!authStore.tenantId)
const canAccessAdmin = computed(() => hasTenant.value && (isPlatformAdmin.value || isTenantManager.value))
const hasWorkspaceAccess = computed(() => {
    return (
        hasTenant.value &&
        !isPlatformSuperAdmin.value &&
        (
            !!authStore.userInfo?.tenantSuperAdmin ||
            !!authStore.userInfo?.tenantAdmin ||
            !!authStore.userInfo?.tenantMember
        )
    )
})
const showWorkspaceEntry = computed(() => {
    return hasWorkspaceAccess.value && (isPersonalInfoRoute.value || currentArea.value !== 'workspace')
})
const showPlatformEntry = computed(() => {
    return (
        !isPlatformSuperAdmin.value &&
        !!isPlatformAdmin.value &&
        (isPersonalInfoRoute.value || currentArea.value !== 'platform')
    )
})
const showAdminEntry = computed(() => {
    if (isPersonalInfoRoute.value) {
        return canAccessAdmin.value
    }
    if (currentArea.value === 'workspace') {
        return canAccessAdmin.value
    }
    return currentArea.value === 'platform' && hasTenant.value && isTenantManager.value
})
const showTenantSwitch = computed(() => hasWorkspaceAccess.value)
const isPersonalInfoRoute = computed(() => !!route.meta.personalInfo || route.name === 'personalInfo')
const showPersonalInfo = computed(() => !isPlatformSuperAdmin.value && !isPersonalInfoRoute.value)
const showNoWorkspaceAccess = computed(() => {
    return currentArea.value === 'workspace' && vipChecked.value && !isPersonalInfoRoute.value && !menuViewData.value.length
})

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
    if (currentArea.value === 'platform') {
        return Object.entries(platformMenuPaths).find(([, menuPath]) => menuPath === path)?.[0]
    }
    if (currentArea.value === 'admin') {
        return Object.entries(adminMenuPaths).find(([, menuPath]) => menuPath === path)?.[0]
    }
}

function resolvePersonalInfoTab(tab: unknown): 'basic-info' | 'change-password' {
    return tab === 'change-password' ? 'change-password' : 'basic-info'
}

async function loadVipLicense(forceRefresh = false) {
    vipChecked.value = false
    vipEnabled.value = await getVipLicenseEnabled(forceRefresh)
    licenseApiAvailable.value = getLicenseApiAvailable()
    vipChecked.value = true
}

function handleSelect(index: Menu['code']) {
    if (isPersonalInfoRoute.value) {
        router.push({
            name: route.name || personalInfoRouteNames[currentArea.value] || 'personalInfo',
            query: {
                tab: resolvePersonalInfoTab(index)
            }
        })
        return
    }
    if (currentArea.value === 'platform' && platformMenuPaths[index]) {
        router.push(platformMenuPaths[index])
        return
    }
    if (currentArea.value === 'admin' && adminMenuPaths[index]) {
        router.push(adminMenuPaths[index])
        return
    }
    router.push({
        name: index
    })
}

function doLogout() {
    resetVipLicenseCache()
    authStore.$reset()
    ElMessage.success('退出成功')
    router.push({
        name: 'login'
    })
}

function goPersonalInfo() {
    menuVisible.value = false
    router.push({
        name: personalInfoRouteNames[currentArea.value] || 'personalInfo'
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

function openTenantDialog() {
    menuVisible.value = false
    tenantSwitchDialogRef.value?.open()
}

onMounted(async () => {
    await loadVipLicense()
})

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
        if (isPersonalInfoRoute.value) {
            return
        }
        if (!currentMenu.value && menuViewData.value.length) {
            const target = firstLeafMenu(menuViewData.value)
            if (!target) {
                return
            }
            const targetPath = currentArea.value === 'platform'
                ? platformMenuPaths[target.code]
                : currentArea.value === 'admin'
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

    .zqy-layout__sidebar {
        --el-transition-duration: 0.18s;

        display: flex;
        position: absolute;
        left: 0;
        top: 0;
        flex-direction: column;
        width: 220px;
        height: 100%;
        overflow: hidden;
        background-color: getCssVar('color', 'white');
        transition: width 0.2s cubic-bezier(0.4, 0, 0.2, 1);
        border-right: 1px solid var(--el-border-color);
        z-index: 2000;

        &.is-collapse {
            width: 80px;
            z-index: 999;

            .zqy-layout__nav {
                display: flex;
                justify-content: center;
                align-items: center;
            }
        }
    }

    .zqy-layout__main {
        width: 100vw;
        background-color: getCssVar('color', 'white');
        padding-left: 80px;
        box-sizing: border-box;
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

    .zqy-layout__icon {
        color: getCssVar('color', 'info');
    }

    .zqy-layout__avatar {
        cursor: pointer;
        background-color: getCssVar('color', 'primary');
        color: getCssVar('color', 'white');
        font-size: getCssVar('font-size', 'extra-small');
    }

    .zqy-layout__menu-wrap {
        padding: 10px 8px;
        box-sizing: border-box;
        flex: 1;
        min-height: 0;
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
        display: flex;
        align-items: center;
        flex-shrink: 0;
        height: 60px;
        box-sizing: border-box;
        padding: 0 24px;
        width: 100%;
        background-color: getCssVar('color', 'white');
    }
}

.zqy-layout__user-menu-popper.el-popover.el-popper {
    min-width: 160px !important;
    width: 160px !important;
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

    .zqy-layout__tenant-name-text {
        max-width: 100%;
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

.zqy-layout__user-menu-option {
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

    .zqy-layout__user-menu-text {
        max-width: 120px;
    }
}

</style>

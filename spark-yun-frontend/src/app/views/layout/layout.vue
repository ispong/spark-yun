<template>
    <div :class="layoutClass">
        <div
            class="zqy-layout__sidebar"
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
                        <el-sub-menu
                            v-if="menuData.children?.length"
                            :index="menuData.code"
                        >
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
            </div>
            <div class="zqy-layout__menu-footer">
                <UserMenuPopover
                    :username="username"
                    :is-system-admin="isSystemAdmin"
                    :is-platform-admin="isPlatformAdmin"
                    :is-tenant-manager="isTenantManager"
                    :active-tenant-name="activeTenantName"
                    @go-area="goArea"
                    @go-personal-info="goPersonalInfo"
                    @open-tenant-dialog="openTenantDialog"
                    @logout="handleCommand('logout')"
                />
            </div>
        </div>

        <div class="zqy-layout__main">
            <router-view :key="authStore.tenantId" />
        </div>

        <TenantSwitchDialog ref="tenantSwitchDialogRef" @tenant-name-change="activeTenantName = $event" />
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, resolveComponent, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import logoURLSmall from '@/app/assets/imgs/logo.png'
import logoURL from '@/app/assets/imgs/logo-a.png'
import { CheckLicenseStatus } from '@/app/management/license/api'
import { useAuthStore } from '@/app/store/useAuth'
import {
    filterVipMenus,
    getLicenseApiAvailable,
    getVipLicenseEnabled,
    resetVipLicenseCache
} from '@/app/utils/vip-license'
import { adminMenuListData, platformMenuListData, workspaceMenuListData, type Menu } from './menu.config'
import TenantSwitchDialog from './tenant-switch-dialog.vue'
import UserMenuPopover from './user-menu-popover.vue'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const vipEnabled = ref(false)
const licenseApiAvailable = ref(true)
const vipChecked = ref(false)
const isCollapse = ref(true)
const tenantSwitchDialogRef = ref<InstanceType<typeof TenantSwitchDialog>>()
const activeTenantName = ref('切换租户')

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

const isSystemAdmin = computed(() => !!authStore.userInfo?.systemAdmin)
const isPlatformAdmin = computed(() => !!authStore.userInfo?.platformAdmin)
const isTenantManager = computed(() => !!authStore.userInfo?.tenantAdmin || !!authStore.userInfo?.normalAdmin)

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
    router.push({
        name: 'personalInfo'
    })
}

function goArea(path: string) {
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

        .zqy-layout__title {
            display: none;
        }
    }

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
    }

    .zqy-layout__main {
        width: 100vw;
        background-color: getCssVar('color', 'white');
        padding-left: 80px;
        box-sizing: border-box;
        transition: padding-left 0.2s cubic-bezier(0.4, 0, 0.2, 1);
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

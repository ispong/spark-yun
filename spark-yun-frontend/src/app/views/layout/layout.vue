<template>
    <div class="zqy-layout">
        <div
            class="zqy-layout__sidebar"
            :class="{ 'is-collapse': isCollapse }"
            @mouseenter="isCollapse = false"
            @mouseleave="isCollapse = true"
        >
            <div class="zqy-layout__nav">
                <img class="zqy-layout__logo" :key="menuLogoKey" :src="menuLogoSrc" alt="logo" />
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
                                <el-icon class="zqy-layout__icon">
                                    <component :is="resolveIcon(menu.icon)" />
                                </el-icon>
                                <span class="zqy-layout__text">{{ menu.name }}</span>
                            </el-menu-item>
                        </el-sub-menu>

                        <el-menu-item v-else :index="menuData.code">
                          <el-icon class="zqy-layout__icon">
                            <component :is="resolveIcon(menuData.icon)" />
                          </el-icon>
                          <span class="zqy-layout__text">{{ menuData.name }}</span>
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
                            切换租户
                        </div>
                        <div v-if="showApplyTenant" class="zqy-layout__user-menu-option" @click="openApplyTenantDialog">
                            <el-icon>
                                <School />
                            </el-icon>
                            加入租户
                        </div>
                        <div v-if="showWorkspaceEntry" class="zqy-layout__user-menu-option" @click="goArea('/workspace')">
                            <el-icon>
                                <SetUp />
                            </el-icon>
                            工作空间
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

        <TenantSwitchDialog ref="tenantSwitchDialogRef" />
        <el-dialog
            v-model="applyTenantDialogVisible"
            class="zqy-layout__apply-tenant-dialog"
            title="加入租户"
            width="420px"
        >
            <el-form class="zqy-layout__apply-tenant-form" label-position="top">
                <el-form-item label="邀请码">
                    <el-input
                        v-model="applyTenantForm.inviteCode"
                        placeholder="请输入邀请码"
                        clearable
                        :maxlength="64"
                        @keyup.enter="submitApplyTenant"
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="zqy-layout__apply-tenant-footer">
                    <el-button @click="applyTenantDialogVisible = false">取消</el-button>
                    <el-button type="primary" :loading="applyTenantLoading" @click="submitApplyTenant">提交</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, resolveComponent, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Monitor, OfficeBuilding, ScaleToOriginal, School, SetUp, SwitchButton, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

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
import { ApplyTenantInviteCode } from '@/app/management/tenant-user/api'
import { brandSetting, loadBrandSetting } from '@/app/shared/branding'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const vipEnabled = ref(false)
const licenseApiAvailable = ref(true)
const vipChecked = ref(false)
const isCollapse = ref(true)
const menuVisible = ref(false)
const tenantSwitchDialogRef = ref<InstanceType<typeof TenantSwitchDialog>>()
const applyTenantDialogVisible = ref(false)
const applyTenantLoading = ref(false)
const applyTenantForm = reactive({
    inviteCode: ''
})

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
    'platform-tenant-user': '/platform/tenant-members',
    'login-method': '/platform/login-method',
    'login-log': '/platform/login-log',
    'user-log': '/platform/user-log',
    license: '/platform/license',
    'platform-setting': '/platform/settings',
    'oauth-management': '/platform/auth'
}

const adminMenuPaths: Record<string, string> = {
    'tenant-user': '/admin/members',
    'role-management': '/admin/roles',
    'ai-config': '/admin/ai-configs',
    'backend-setting': '/admin/settings',
    'org-management': '/admin/orgs'
}

const personalInfoRouteNames: Record<string, string> = {
    platform: 'platform-personalInfo',
    admin: 'admin-personalInfo',
    workspace: 'workspace-personalInfo'
}

// 菜单显示哪些
const menuViewData = computed(() => {
    if (isPersonalInfoRoute.value) {
        return menuListData.value
    }
    if (currentArea.value !== 'workspace') {
        return menuListData.value
    }
    const areaMenus = filterWorkspaceMenus(menuListData.value)
    return filterVipMenus(areaMenus, vipEnabled.value, licenseApiAvailable.value)
})

const menuLogoSrc = computed(() => (isCollapse.value ? brandSetting.topLogoSmallUrl : brandSetting.topLogoUrl))

const menuLogoKey = computed(() => `${isCollapse.value ? 'small' : 'large'}-${menuLogoSrc.value}`)

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
const showApplyTenant = computed(() => !isPlatformSuperAdmin.value)
const showNoWorkspaceAccess = computed(() => {
    return currentArea.value === 'workspace' && vipChecked.value && !isPersonalInfoRoute.value && !menuViewData.value.length
})

function resolveIcon(icon: string) {
    return resolveComponent(icon)
}

function filterWorkspaceMenus(menuList: Menu[]): Menu[] {
    if (authStore.userInfo?.workspaceAllPermissions) {
        return menuList
    }
    const permissions = [
        ...(authStore.userInfo?.frontendPermissionCodes || []),
        ...(authStore.userInfo?.permissions || [])
    ]
    return menuList
        .map((menu) => {
            if (menu.children?.length) {
                const children = filterWorkspaceMenus(menu.children)
                return children.length ? { ...menu, children } : null
            }
            return permissions.includes(`workspace:${menu.code}:menu`) ? menu : null
        })
        .filter((menu): menu is Menu => !!menu)
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

function resolvePersonalInfoTab(tab: unknown): 'basic-info' | 'change-password' | 'change-phone' | 'change-email' {
    return tab === 'change-password' || tab === 'change-phone' || tab === 'change-email' ? tab : 'basic-info'
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

function openApplyTenantDialog() {
    menuVisible.value = false
    applyTenantForm.inviteCode = ''
    applyTenantDialogVisible.value = true
}

function submitApplyTenant() {
    const inviteCode = applyTenantForm.inviteCode.trim()
    if (!inviteCode) {
        ElMessage.warning('请输入邀请码')
        return
    }
    applyTenantLoading.value = true
    ApplyTenantInviteCode({
        inviteCode
    })
        .then((res: any) => {
            ElMessage.success(res.msg || '申请提交成功')
            applyTenantDialogVisible.value = false
            applyTenantForm.inviteCode = ''
        })
        .finally(() => {
            applyTenantLoading.value = false
        })
}

onMounted(async () => {
    loadBrandSetting()
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
    min-width: 132px !important;
    width: 132px !important;
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

.zqy-layout__apply-tenant-dialog {
    --apply-tenant-dialog-x-padding: 20px;
    --apply-tenant-dialog-border-color: #ebeef5;

    border-radius: 2px;

    .el-dialog__header {
        position: relative;
        min-height: 46px;
        padding: 9px var(--apply-tenant-dialog-x-padding) 8px !important;
        margin-right: 0;
        box-sizing: border-box;
        border-bottom: none;

        &::after {
            position: absolute;
            right: 0;
            bottom: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--apply-tenant-dialog-border-color);
        }
    }

    .el-dialog__title {
        display: block;
        font-size: 16px;
        line-height: 28px;
        color: getCssVar('text-color', 'primary');
    }

    .el-dialog__headerbtn {
        top: 0;
        width: 42px;
        height: 46px;
    }

    .el-dialog__body {
        padding: 18px var(--apply-tenant-dialog-x-padding) 4px !important;
    }

    .el-dialog__footer {
        position: relative;
        min-height: 56px;
        padding: 12px var(--apply-tenant-dialog-x-padding);
        box-sizing: border-box;
        border-top: none;

        &::before {
            position: absolute;
            top: 0;
            right: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--apply-tenant-dialog-border-color);
        }
    }

    .zqy-layout__apply-tenant-form {
        .el-form-item {
            margin-bottom: 20px;
        }

        .el-form-item__label {
            width: 100%;
            padding: 0;
            margin-bottom: 4px;
            line-height: 16px;
            color: getCssVar('text-color', 'regular');
        }

        .el-form-item__content,
        .el-input {
            width: 100%;
        }

        .el-input__wrapper {
            border-radius: 0;
        }
    }

    .zqy-layout__apply-tenant-footer {
        display: flex;
        align-items: center;
        justify-content: flex-end;
        gap: 12px;
        width: 100%;

        .el-button + .el-button {
            margin-left: 0;
        }
    }
}

</style>

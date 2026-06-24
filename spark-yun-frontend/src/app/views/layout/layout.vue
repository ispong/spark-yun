<template>
    <div class="zqy-layout">
        <div
            class="zqy-layout__sidebar"
            :class="{ 'is-collapse': menuDisplayCollapse, 'is-resizing': isResizing }"
            :style="{ width: `${sidebarWidth}px` }"
            @mouseenter="handleSidebarMouseEnter"
            @mousemove="handleSidebarMouseEnter"
            @mouseleave="handleSidebarMouseLeave"
        >
            <div class="zqy-layout__nav">
                <img class="zqy-layout__logo" :key="menuLogoKey" :src="menuLogoSrc" alt="logo" />
            </div>

            <div class="zqy-layout__menu-wrap">
                <el-menu
                    class="zqy-layout__menu"
                    :unique-opened="true"
                    :collapse="menuDisplayCollapse"
                    :default-active="currentMenu?.code"
                    @select="handleSelect"
                >
                    <template v-for="menuData in menuViewData" :key="menuData.code">
                        <el-sub-menu v-if="menuData.children?.length && !menuDisplayCollapse" :index="menuData.code">
                            <template #title>
                                <el-icon class="zqy-layout__icon">
                                    <component :is="resolveIcon(menuData.icon)" />
                                </el-icon>
                                <span v-show="!menuDisplayCollapse" class="zqy-layout__text">{{ translateMenuName(menuData) }}</span>
                            </template>

                            <el-menu-item
                                v-for="menu in menuData.children"
                                :key="menu.code"
                                :index="menu.code"
                            >
                                <el-icon class="zqy-layout__icon">
                                    <component :is="resolveIcon(menu.icon)" />
                                </el-icon>
                                <span class="zqy-layout__text">{{ translateMenuName(menu) }}</span>
                            </el-menu-item>
                        </el-sub-menu>

                        <el-menu-item
                            v-else
                            :index="resolveMenuIndex(menuData)"
                            :class="{ 'is-active': menuDisplayCollapse && isMenuActive(menuData) }"
                        >
                          <el-icon class="zqy-layout__icon">
                            <component :is="resolveIcon(menuData.icon)" />
                          </el-icon>
                          <span class="zqy-layout__text">{{ translateMenuName(menuData) }}</span>
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
                        <div
                            v-if="showTenantSwitch"
                            class="zqy-layout__user-menu-option zqy-layout__user-menu-option--current-tenant"
                            @click="openTenantDialog"
                        >
                            <el-icon>
                                <OfficeBuilding />
                            </el-icon>
                            <EllipsisTooltip class="zqy-layout__user-menu-text" :label="currentTenantMenuName" />
                        </div>
                        <div v-if="showTenantSwitch" class="zqy-layout__user-menu-option" @click="openTenantDialog">
                            <el-icon>
                                <Switch />
                            </el-icon>
                            {{ t('layout.selectTenant') }}
                        </div>
                        <div v-if="showApplyTenant" class="zqy-layout__user-menu-option" @click="openApplyTenantDialog">
                            <el-icon>
                                <School />
                            </el-icon>
                            {{ t('layout.joinTenant') }}
                        </div>
                        <div v-if="showWorkspaceEntry" class="zqy-layout__user-menu-option" @mousedown.prevent.stop="goArea('workspace')">
                            <el-icon>
                                <SetUp />
                            </el-icon>
                            {{ t('layout.workspace') }}
                        </div>
                        <div v-if="showAdminEntry" class="zqy-layout__user-menu-option" @mousedown.prevent.stop="goArea('admin')">
                            <el-icon>
                                <ScaleToOriginal />
                            </el-icon>
                            {{ t('layout.admin') }}
                        </div>
                        <div v-if="showPlatformEntry" class="zqy-layout__user-menu-option" @mousedown.prevent.stop="goArea('platform')">
                            <el-icon>
                                <Monitor />
                            </el-icon>
                            {{ t('layout.platform') }}
                        </div>
                        <div v-if="showPersonalInfo" class="zqy-layout__user-menu-option" @mousedown.prevent.stop="goPersonalInfo">
                            <el-icon>
                                <User />
                            </el-icon>
                            {{ t('layout.personalInfo') }}
                        </div>
                        <div
                            class="zqy-layout__user-menu-option zqy-layout__user-menu-option--language"
                            @click.stop="openLanguageDialog"
                        >
                            <el-icon>
                                <Reading />
                            </el-icon>
                            <span class="zqy-layout__user-menu-language-label">
                                {{ t('layout.switchLanguage') }}
                            </span>
                        </div>
                        <div class="zqy-layout__user-menu-option" @click="handleCommand('logout')">
                            <el-icon>
                                <SwitchButton />
                            </el-icon>
                            {{ t('layout.logout') }}
                        </div>
                    </div>
                </el-popover>
            </div>
            <div class="zqy-layout__resize-handle" @mousedown.prevent="startResize" />
            <div
                v-if="isCollapse"
                class="zqy-layout__resize-handle zqy-layout__resize-handle--collapsed"
                @mousedown.stop.prevent="startResize($event, SIDEBAR_COLLAPSED_WIDTH)"
            />
        </div>

        <div class="zqy-layout__main" :style="{ paddingLeft: `${mainPaddingLeft}px` }">
            <el-empty
                v-if="showNoWorkspaceAccess"
                class="zqy-layout__empty"
                :description="t('layout.noWorkspaceAccess')"
            />
            <router-view v-else :key="authStore.tenantId" />
        </div>

        <TenantSwitchDialog ref="tenantSwitchDialogRef" @tenant-name-change="currentTenantName = $event" />
        <el-dialog
            v-model="applyTenantDialogVisible"
            class="zqy-layout__apply-tenant-dialog"
            :title="t('layout.joinTenant')"
            width="420px"
        >
            <el-form class="zqy-layout__apply-tenant-form" label-position="top">
                <el-form-item :label="t('layout.inviteCode')">
                    <el-input
                        v-model="applyTenantForm.inviteCode"
                        :placeholder="t('layout.inputInviteCode')"
                        clearable
                        :maxlength="64"
                        @keyup.enter="submitApplyTenant"
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="zqy-layout__apply-tenant-footer">
                    <el-button @click="applyTenantDialogVisible = false">{{ t('common.cancel') }}</el-button>
                    <el-button type="primary" :loading="applyTenantLoading" @click="submitApplyTenant">{{ t('common.submit') }}</el-button>
                </div>
            </template>
        </el-dialog>
        <el-dialog
            v-model="languageDialogVisible"
            class="zqy-layout__language-dialog"
            :title="t('layout.switchLanguage')"
            width="360px"
        >
            <div class="zqy-layout__language-dialog-body">
                <div class="zqy-layout__language-options">
                    <button
                        v-for="localeOption in supportedLocales"
                        :key="localeOption"
                        class="zqy-layout__language-option"
                        :class="{ 'is-active': localeOption === languageForm.locale }"
                        type="button"
                        @click="languageForm.locale = localeOption"
                    >
                        {{ t(`app.locale.${localeOption}`) }}
                    </button>
                </div>
            </div>
            <template #footer>
                <div class="zqy-layout__language-dialog-footer">
                    <el-button @click="languageDialogVisible = false">{{ t('common.cancel') }}</el-button>
                    <el-button type="primary" :loading="languageUpdating" @click="handleLocaleChange">
                        {{ t('common.confirm') }}
                    </el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, resolveComponent, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Monitor, OfficeBuilding, Reading, ScaleToOriginal, School, SetUp, Switch, SwitchButton, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'

import EllipsisTooltip from '@/app/components/ellipsis-tooltip/ellipsis-tooltip.vue'
import { SUPPORTED_LOCALES, type AppLocale } from '@/app/i18n/locales'
import { CheckLicenseStatus } from '@/app/management/license/api'
import { UpdateMyLocale } from '@/app/management/personal-info/api'
import { useAuthStore } from '@/app/store/useAuth'
import { useLocaleStore } from '@/app/store/useLocale'
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
const localeStore = useLocaleStore()
const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const vipEnabled = ref(false)
const licenseApiAvailable = ref(true)
const vipChecked = ref(false)
const SIDEBAR_EXPANDED_WIDTH = 220
const SIDEBAR_COLLAPSED_WIDTH = 80
const SIDEBAR_COLLAPSE_THRESHOLD = 120
const SIDEBAR_EXPAND_THRESHOLD = 160
const SIDEBAR_COLLAPSE_STORAGE_KEY = 'spark-yun-layout-sidebar-collapse'
const SIDEBAR_RESIZE_HOT_ZONE = 14
const isCollapse = ref(false)
const isHoverExpanded = ref(false)
const isResizing = ref(false)
const resizeWidth = ref(SIDEBAR_EXPANDED_WIDTH)
let removeSidebarResizeListeners: (() => void) | null = null
const menuVisible = ref(false)
const languageDialogVisible = ref(false)
const languageUpdating = ref(false)
const tenantSwitchDialogRef = ref<InstanceType<typeof TenantSwitchDialog>>()
const currentTenantName = ref('')
const applyTenantDialogVisible = ref(false)
const applyTenantLoading = ref(false)
const applyTenantForm = reactive({
    inviteCode: ''
})
const languageForm = reactive({
    locale: localeStore.locale as AppLocale
})
const supportedLocales = SUPPORTED_LOCALES

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

// Resolve visible menus for the current area.
const menuViewData = computed(() => {
    if (isPersonalInfoRoute.value) {
        return menuListData.value
    }
    if (currentArea.value === 'workspace') {
        const areaMenus = filterWorkspaceMenus(menuListData.value)
        return filterVipMenus(areaMenus, vipEnabled.value, licenseApiAvailable.value)
    }
    return filterVipMenus(menuListData.value, vipEnabled.value, licenseApiAvailable.value)
})

const menuDisplayCollapse = computed(() => isCollapse.value && !isHoverExpanded.value)

const menuLogoSrc = computed(() => (menuDisplayCollapse.value ? brandSetting.topLogoSmallUrl : brandSetting.topLogoUrl))

const menuLogoKey = computed(() => `${menuDisplayCollapse.value ? 'small' : 'large'}-${menuLogoSrc.value}`)

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
const canAccessAdmin = computed(() => hasTenant.value && isTenantManager.value)
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
const currentTenantMenuName = computed(() => currentTenantName.value || t('layout.currentTenant'))
const isPersonalInfoRoute = computed(() => !!route.meta.personalInfo || route.name === 'personalInfo')
const showPersonalInfo = computed(() => !isPlatformSuperAdmin.value && !isPersonalInfoRoute.value)
const showApplyTenant = computed(() => !isPlatformSuperAdmin.value)
const showNoWorkspaceAccess = computed(() => {
    return currentArea.value === 'workspace' && vipChecked.value && !isPersonalInfoRoute.value && !menuViewData.value.length
})
const sidebarWidth = computed(() => {
    if (isResizing.value) {
        return resizeWidth.value
    }
    return menuDisplayCollapse.value ? SIDEBAR_COLLAPSED_WIDTH : SIDEBAR_EXPANDED_WIDTH
})
const mainPaddingLeft = computed(() => {
    if (isResizing.value) {
        return resizeWidth.value
    }
    return isCollapse.value ? SIDEBAR_COLLAPSED_WIDTH : SIDEBAR_EXPANDED_WIDTH
})

function resolveIcon(icon: string) {
    return resolveComponent(icon)
}

function resolveMenuIndex(menu: Menu) {
    return firstLeafMenu([menu])?.code || menu.code
}

function isMenuActive(menu: Menu) {
    if (!currentMenu.value) {
        return false
    }
    if (menu.code === currentMenu.value.code || menu.childPage?.includes(currentMenu.value.code)) {
        return true
    }
    return !!menu.children?.some((child) => isMenuActive(child))
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

function resolvePersonalInfoTab(tab: unknown): 'basic-info' | 'change-password' | 'change-phone' | 'change-email' | 'change-language' {
    return tab === 'change-password' || tab === 'change-phone' || tab === 'change-email' || tab === 'change-language'
        ? tab
        : 'basic-info'
}

function translateMenuName(menu: Menu) {
    return t(menu.nameKey || `menu.${menu.code}`)
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
    ElMessage.success(t('common.logoutSuccess'))
    router.push({
        name: 'login'
    })
}

function goPersonalInfo() {
    menuVisible.value = false
    languageDialogVisible.value = false
    router.push({
        name: personalInfoRouteNames[currentArea.value] || 'personalInfo'
    })
}

function goArea(area: 'workspace' | 'admin' | 'platform') {
    menuVisible.value = false
    languageDialogVisible.value = false
    const routeNames: Record<typeof area, string> = {
        workspace: 'zhiqing-ai',
        admin: 'admin-tenant-user',
        platform: 'platform-user-center'
    }
    router.push({
        name: routeNames[area]
    })
}

function openLanguageDialog() {
    languageForm.locale = localeStore.locale
    menuVisible.value = false
    languageDialogVisible.value = true
}

async function handleLocaleChange() {
    const locale = languageForm.locale as AppLocale
    if (languageUpdating.value || locale === localeStore.locale) {
        languageDialogVisible.value = false
        return
    }
    languageUpdating.value = true
    try {
        const res = await UpdateMyLocale({
            locale
        })
        localeStore.setLocale(locale)
        authStore.setUserInfo({
            ...authStore.userInfo,
            locale
        })
        ElMessage.success(res.msg || t('personal.languageSaved'))
        languageDialogVisible.value = false
        menuVisible.value = false
    } finally {
        languageUpdating.value = false
    }
}

function handleCommand(command: 'logout') {
    if (command === 'logout') {
        languageDialogVisible.value = false
        CheckLicenseStatus()
            .catch(() => {
                // Refresh license status before logout; failure should not block logout.
            })
            .finally(() => {
                doLogout()
            })
    }
}

function openTenantDialog() {
    menuVisible.value = false
    languageDialogVisible.value = false
    tenantSwitchDialogRef.value?.open()
}

function openApplyTenantDialog() {
    menuVisible.value = false
    languageDialogVisible.value = false
    applyTenantForm.inviteCode = ''
    applyTenantDialogVisible.value = true
}

function submitApplyTenant() {
    const inviteCode = applyTenantForm.inviteCode.trim()
    if (!inviteCode) {
        ElMessage.warning(t('layout.inputInviteCode'))
        return
    }
    applyTenantLoading.value = true
    ApplyTenantInviteCode({
        inviteCode
    })
        .then((res: any) => {
            ElMessage.success(res.msg || t('layout.applyTenantSuccess'))
            applyTenantDialogVisible.value = false
            applyTenantForm.inviteCode = ''
        })
        .finally(() => {
            applyTenantLoading.value = false
        })
}

function applySidebarCollapse(collapse: boolean) {
    isCollapse.value = collapse
    isHoverExpanded.value = false
    resizeWidth.value = collapse ? SIDEBAR_COLLAPSED_WIDTH : SIDEBAR_EXPANDED_WIDTH
    localStorage.setItem(SIDEBAR_COLLAPSE_STORAGE_KEY, collapse ? 'true' : 'false')
}

function handleSidebarMouseEnter(event: MouseEvent) {
    const isInCollapsedResizeZone = event.clientX >= SIDEBAR_COLLAPSED_WIDTH - SIDEBAR_RESIZE_HOT_ZONE
    if (isCollapse.value && !isResizing.value && !isInCollapsedResizeZone) {
        isHoverExpanded.value = true
    }
}

function handleSidebarMouseLeave() {
    isHoverExpanded.value = false
}

function handleDocumentMouseMove(event: MouseEvent) {
    if (isHoverExpanded.value && !isResizing.value && event.clientX > SIDEBAR_EXPANDED_WIDTH) {
        isHoverExpanded.value = false
    }
}

function startResize(event: MouseEvent, fixedStartWidth?: number) {
    removeSidebarResizeListeners?.()
    const startX = event.clientX
    const startWidth = fixedStartWidth ?? sidebarWidth.value
    isHoverExpanded.value = false
    isResizing.value = true
    resizeWidth.value = startWidth

    const handleResizeMove = (moveEvent: MouseEvent) => {
        const nextWidth = Math.min(
            SIDEBAR_EXPANDED_WIDTH,
            Math.max(SIDEBAR_COLLAPSED_WIDTH, startWidth + moveEvent.clientX - startX)
        )
        resizeWidth.value = nextWidth
    }

    const handleResizeEnd = () => {
        removeSidebarResizeListeners?.()
        removeSidebarResizeListeners = null

        const shouldCollapse = resizeWidth.value <= SIDEBAR_COLLAPSE_THRESHOLD
        const shouldExpand = resizeWidth.value >= SIDEBAR_EXPAND_THRESHOLD
        const nextCollapse = shouldCollapse ? true : shouldExpand ? false : isCollapse.value
        isResizing.value = false
        applySidebarCollapse(nextCollapse)
    }

    document.addEventListener('mousemove', handleResizeMove)
    document.addEventListener('mouseup', handleResizeEnd)
    removeSidebarResizeListeners = () => {
        document.removeEventListener('mousemove', handleResizeMove)
        document.removeEventListener('mouseup', handleResizeEnd)
    }
}

onMounted(async () => {
    const savedCollapse = localStorage.getItem(SIDEBAR_COLLAPSE_STORAGE_KEY)
    if (savedCollapse === 'true' || savedCollapse === 'false') {
        applySidebarCollapse(savedCollapse === 'true')
    }
    document.addEventListener('mousemove', handleDocumentMouseMove)
    loadBrandSetting()
    await loadVipLicense()
})

onBeforeUnmount(() => {
    removeSidebarResizeListeners?.()
    document.removeEventListener('mousemove', handleDocumentMouseMove)
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
        border-right: 1px solid var(--el-border-color-lighter);
        z-index: 2000;

        &.is-resizing {
            transition: none;
        }

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
        transition: padding-left 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    }

    .zqy-layout__resize-handle {
        position: absolute;
        top: 0;
        right: 0;
        width: 8px;
        height: 100%;
        cursor: col-resize;
        z-index: 4;

        &:hover {
            background-color: getCssVar('color', 'primary', 'light-9');
        }

        &.zqy-layout__resize-handle--collapsed {
            left: 76px;
            right: auto;
            background-color: transparent;

            &:hover {
                background-color: transparent;
            }
        }
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
    min-width: 176px !important;
    width: 176px !important;
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

    &.zqy-layout__user-menu-option--current-tenant {
        color: getCssVar('color-primary');
        font-weight: 500;
    }

    .zqy-layout__user-menu-text {
        flex: 1;
        min-width: 0;
        max-width: 128px;
    }

    &.zqy-layout__user-menu-option--language {
        justify-content: space-between;

        .el-icon {
            flex-shrink: 0;
        }
    }
}

.zqy-layout__user-menu-language-label {
    flex: 1;
    min-width: 0;
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

.zqy-layout__language-dialog {
    --language-dialog-x-padding: 20px;
    --language-dialog-border-color: #ebeef5;

    border-radius: 2px;

    .el-dialog__header {
        position: relative;
        min-height: 46px;
        padding: 9px var(--language-dialog-x-padding) 8px !important;
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
            background-color: var(--language-dialog-border-color);
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
        padding: 18px var(--language-dialog-x-padding) 20px !important;
    }

    .el-dialog__footer {
        position: relative;
        min-height: 56px;
        padding: 12px var(--language-dialog-x-padding);
        box-sizing: border-box;
        border-top: none;

        &::before {
            position: absolute;
            top: 0;
            right: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--language-dialog-border-color);
        }
    }

    .zqy-layout__language-options {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 8px;
    }

    .zqy-layout__language-option {
        height: 36px;
        padding: 0 12px;
        border: 1px solid getCssVar('border-color');
        border-radius: 4px;
        box-sizing: border-box;
        font: inherit;
        font-size: 13px;
        line-height: 34px;
        color: getCssVar('text-color', 'regular');
        text-align: center;
        background-color: getCssVar('fill-color', 'blank');
        cursor: pointer;

        &:hover {
            border-color: getCssVar('color-primary');
            color: getCssVar('color-primary');
        }

        &.is-active {
            border-color: getCssVar('color-primary');
            color: getCssVar('color-primary');
            background-color: getCssVar('color-primary-light-9');
        }
    }

    .zqy-layout__language-dialog-footer {
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

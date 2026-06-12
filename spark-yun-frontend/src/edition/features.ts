import type { Menu } from '@/views/home/menu.config'
import { CheckLicenseStatus } from '@/services/license.service'

const COMMERCIAL_MENU_CODE_SET = new Set([
    // 免密登录
    'oauth-management',
    // 数据规划
    'data-planning',
    'data-layer',
    'layer-area',
    'field-format',
    'data-model',
    'model-field',
    // 全局变量
    'global-variables',
    // 实时计算
    'realtime-computing',
    'computing-detail',
    // 计算容器
    'spark-container',
    // 依赖合集
    'lib-package',
    // 元数据
    'metadata-page',
    'metadata-management',
    'acquisition-task',
    'acquisition-instance',
    // 基线告警
    'message-management',
    'message-notifications',
    'warning-config',
    'warning-schedule',
    // 数据服务
    'data-server',
    'report-components',
    'report-item',
    'report-views',
    'report-views-detail',
    'access-rule',
    'custom-api',
    'custom-form',
    'form-list',
    'form-query',
    'form-setting'
])

let cachedCommercialEnabled: boolean | null = null
let pendingCheckPromise: Promise<boolean> | null = null
let cachedLicenseApiAvailable = true

function isLicenseMenuCode(code?: string): boolean {
    return code === 'license'
}

function isNotFoundError(error: any): boolean {
    return error?.status === 404 || error?.code === 404 || error?.httpStatus === 404
}

export function isCommercialMenuCode(code?: string): boolean {
    return !!code && COMMERCIAL_MENU_CODE_SET.has(code)
}

export async function getCommercialEditionEnabled(forceRefresh = false): Promise<boolean> {
    if (!forceRefresh && cachedCommercialEnabled !== null) {
        return cachedCommercialEnabled
    }

    if (pendingCheckPromise) {
        return pendingCheckPromise
    }

    pendingCheckPromise = CheckLicenseStatus()
        .then((res: any) => {
            const status = res?.data?.status
            cachedLicenseApiAvailable = true
            cachedCommercialEnabled = status === 'ENABLE'
            return cachedCommercialEnabled
        })
        .catch((error: any) => {
            cachedLicenseApiAvailable = !isNotFoundError(error)
            cachedCommercialEnabled = false
            return false
        })
        .finally(() => {
            pendingCheckPromise = null
        })

    return pendingCheckPromise
}

export function resetCommercialEditionCache(): void {
    cachedCommercialEnabled = null
    pendingCheckPromise = null
    cachedLicenseApiAvailable = true
}

export function getLicenseApiAvailable(): boolean {
    return cachedLicenseApiAvailable
}

export function filterCommercialMenus(
    menuList: Menu[],
    commercialEnabled: boolean,
    licenseApiAvailable = true
): Menu[] {
    const result: Menu[] = []

    menuList.forEach((menu) => {
        if (isLicenseMenuCode(menu.code) && !licenseApiAvailable) {
            return
        }

        const isCommercial = isCommercialMenuCode(menu.code)
        if (isCommercial && !commercialEnabled) {
            return
        }

        const hasChildren = !!menu.children?.length
        const children = hasChildren
            ? filterCommercialMenus(menu.children || [], commercialEnabled, licenseApiAvailable)
            : undefined

        if (hasChildren && (!children || !children.length)) {
            return
        }

        result.push({
            ...menu,
            children
        })
    })

    return result
}
